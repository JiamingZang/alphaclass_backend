package com.imct.alphaclass.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.GenVideoResult;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.exception.ServiceException;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.videos.VideoCreateParams;
import ai.z.openapi.service.videos.VideoObject;
import ai.z.openapi.service.videos.VideoResult;
import ai.z.openapi.service.videos.VideosResponse;

/**
 * 视频生成服务：调用智谱 CogVideoX 生成视频。
 * <p>
 * 提交任务后落库；历史查询时会对 PROCESSING 状态的任务实时轮询智谱，
 * 完成则更新产物地址（经本地 /proxy 代理转发，前缀由 app.video-proxy-prefix 配置）到库并同步到响应。
 */
@Service
@RequiredArgsConstructor
public class VideoGenerationService {

    private final ServiceDAO servicedao;

    @Value("${ai.zhipu.api-key}")
    private String zhipuApiKey;

    /** 视频产物对外地址前缀（智谱直链经本服务 /proxy 转发；部署时用 VIDEO_PROXY_PREFIX 覆盖） */
    @Value("${app.video-proxy-prefix:http://localhost:8080/proxy/}")
    private String videoProxyPrefix;

    /** 当日生成次数上限 */
    private static final int DAILY_GENERATION_LIMIT = 10;

    /** 文生视频：提交智谱任务并落库，返回 id/task_status */
    public Map<String, Object> textToVideo(Map<String, Object> params, int userId) {
        checkExceedGenerationCount(userId);
        GenVideoResult res = new GenVideoResult();
        res.setUser_id(userId);
        res.setPrompt(params.get("prompt").toString());
        String quality = params.get("quality").toString().equals("quality") ? "quality" : "speed";
        boolean withAudio = params.get("with_audio").toString().equals("true") ? true : false;
        res.setSize(params.get("size").toString());
        int fps = Integer.valueOf(params.get("fps").toString()) == 60 ? 60 : 30;

        VideoObject result = generateVideoRequest(res.getPrompt(), null, quality, withAudio, res.getSize(), fps).getData();
        res.setRequest_id(result.getId());
        res.setTask_status(result.getTaskStatus());
        res.setType("TextToVideo");
        res.setIs_deleted(0);
        res.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        servicedao.addVideoResult(res);
        return taskResponse(res);
    }

    /** 图生视频：提交智谱任务并落库，返回 id/task_status */
    public Map<String, Object> imageToVideo(Map<String, Object> params, int userId) {
        checkExceedGenerationCount(userId);
        GenVideoResult res = new GenVideoResult();
        res.setUser_id(userId);
        res.setPrompt(params.get("prompt").toString());
        String imageUrl = params.get("image_url").toString();
        String quality = params.get("quality").toString().equals("quality") ? "quality" : "speed";
        boolean withAudio = params.get("with_audio").toString().equals("true") ? true : false;
        res.setSize(params.get("size").toString());
        int fps = Integer.valueOf(params.get("fps").toString()) == 60 ? 60 : 30;

        VideoObject result = generateVideoRequest(res.getPrompt(), imageUrl, quality, withAudio, res.getSize(), fps).getData();
        res.setRequest_id(result.getId());
        res.setTask_status(result.getTaskStatus());
        res.setType("ImageToVideo");
        res.setIs_deleted(0);
        res.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        servicedao.addVideoResult(res);
        return taskResponse(res);
    }

    /** 当前用户的视频生成历史（未删除，按创建时间倒序；处理中任务实时轮询更新） */
    public List<GenVideoResult> getHistory(int userId) {
        List<GenVideoResult> finalRes = new ArrayList<GenVideoResult>();
        for (GenVideoResult r : servicedao.getAllVideoResults()) {
            if (r.getUser_id() == userId && r.getIs_deleted() == 0) {
                if (r.getTask_status().equals("PROCESSING")) {
                    VideosResponse apply = zhipuClient().videos()
                            .videoGenerationsResult(r.getRequest_id());
                    VideoObject response = apply.getData();
                    String status = response.getTaskStatus();
                    if (!status.equals("PROCESSING")) {
                        VideoResult o = apply.getData().getVideoResult().get(0);
                        servicedao.updateVideoResultById(
                                status,
                                o.getUrl().replace("https://", videoProxyPrefix),
                                o.getCoverImageUrl().replace("https://", videoProxyPrefix),
                                r.getRequest_id());
                        r.setTask_status(status);
                        r.setUrl(o.getUrl());
                        r.setThumbnail_url(o.getCoverImageUrl());
                    }
                }
                finalRes.add(r);
            }
        }
        Collections.reverse(finalRes);
        return finalRes;
    }

    /** 删除一条视频生成历史（软删除） */
    public void deleteHistory(int id) {
        servicedao.deleteVideoResultById(id);
    }

    /** 当日生成次数限制：超过上限则拒绝新任务 */
    private void checkExceedGenerationCount(int userId) {
        long finalCount = servicedao.getAllVideoResults().stream()
                .filter(r -> r.getUser_id() == userId)
                .filter(r -> Timestamp.valueOf(LocalDateTime.parse(r.getCreated_at()))
                        .toLocalDateTime().toLocalDate().isEqual(LocalDate.now()))
                .count();
        if (finalCount > DAILY_GENERATION_LIMIT) {
            throw new ServiceException("403", "You have exceeded generation limit per day.");
        }
    }

    /** 组装提交结果响应（id + task_status） */
    private Map<String, Object> taskResponse(GenVideoResult res) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("id", res.getRequest_id());
        resp.put("task_status", res.getTask_status());
        return resp;
    }

    /** 调用智谱 CogVideoX 提交视频生成任务 */
    private VideosResponse generateVideoRequest(String prompt, String imageUrl, String quality, boolean withAudio,
            String size, int fps) {
        VideoCreateParams.VideoCreateParamsBuilder<?, ?> builder = VideoCreateParams.builder()
                .model("cogvideox-3")
                .prompt(prompt)
                .quality(quality)
                .withAudio(withAudio)
                .size(size)
                .fps(fps);
        if (imageUrl != null) {
            builder.imageUrl(imageUrl);
        }
        return zhipuClient().videos().videoGenerations(builder.build());
    }

    private ZhipuAiClient zhipuClient() {
        return ZhipuAiClient.builder().apiKey(zhipuApiKey).build();
    }
}
