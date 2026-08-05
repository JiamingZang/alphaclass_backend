package com.imct.alphaclass.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.GenVideoResult;
import com.imct.alphaclass.common.AiConstants;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.utils.MapUtils;

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
    private final AiUsageGuard usageGuard;

    @Value("${ai.zhipu.api-key}")
    private String zhipuApiKey;

    /** 视频产物对外地址前缀（智谱直链经本服务 /proxy 转发；部署时用 VIDEO_PROXY_PREFIX 覆盖） */
    @Value("${app.video-proxy-prefix:http://localhost:8080/proxy/}")
    private String videoProxyPrefix;

    /** 文生视频：提交智谱任务并落库，返回 id/task_status */
    public Map<String, Object> textToVideo(Map<String, Object> params, int userId) {
        usageGuard.checkExceedGenerationCount(userId);
        GenVideoResult res = new GenVideoResult();
        res.setUser_id(userId);
        res.setPrompt(params.get("prompt").toString());
        res.setSize(params.get("size").toString());
        return submitVideoTask(res, null, params, AiConstants.TYPE_TEXT_TO_VIDEO);
    }

    /** 图生视频：提交智谱任务并落库，返回 id/task_status */
    public Map<String, Object> imageToVideo(Map<String, Object> params, int userId) {
        usageGuard.checkExceedGenerationCount(userId);
        GenVideoResult res = new GenVideoResult();
        res.setUser_id(userId);
        res.setPrompt(params.get("prompt").toString());
        res.setSize(params.get("size").toString());
        return submitVideoTask(res, params.get("image_url").toString(), params, AiConstants.TYPE_IMAGE_TO_VIDEO);
    }

    /** 提交智谱任务并落库（文生/图生共用组装），返回 id/task_status */
    private Map<String, Object> submitVideoTask(GenVideoResult res, String imageUrl, Map<String, Object> params,
            String type) {
        String quality = AiConstants.VIDEO_QUALITY_QUALITY.equals(params.get("quality").toString())
                ? AiConstants.VIDEO_QUALITY_QUALITY
                : AiConstants.VIDEO_QUALITY_SPEED;
        boolean withAudio = "true".equals(params.get("with_audio").toString());
        int fps = Integer.valueOf(params.get("fps").toString()) == AiConstants.VIDEO_FPS_HIGH
                ? AiConstants.VIDEO_FPS_HIGH
                : AiConstants.VIDEO_FPS_DEFAULT;

        VideoObject result = generateVideoRequest(res.getPrompt(), imageUrl, quality, withAudio, res.getSize(), fps)
                .getData();
        res.setRequest_id(result.getId());
        res.setTask_status(result.getTaskStatus());
        res.setType(type);
        res.setIs_deleted(0);
        res.setCreated_at(MapUtils.now());
        servicedao.addVideoResult(res);
        return taskResponse(res);
    }

    /** 当前用户的视频生成历史（未删除，按创建时间倒序；处理中任务实时轮询更新） */
    public List<GenVideoResult> getHistory(int userId) {
        List<GenVideoResult> finalRes = new ArrayList<GenVideoResult>();
        for (GenVideoResult r : servicedao.getAllVideoResults()) {
            if (r.getUser_id() == userId && r.getIs_deleted() == 0) {
                if (AiConstants.TASK_PROCESSING.equals(r.getTask_status())) {
                    VideosResponse apply = zhipuClient().videos()
                            .videoGenerationsResult(r.getRequest_id());
                    VideoObject response = apply.getData();
                    String status = response.getTaskStatus();
                    if (!AiConstants.TASK_PROCESSING.equals(status)) {
                        VideoResult o = apply.getData().getVideoResult().get(0);
                        servicedao.updateVideoResultById(
                                status,
                                o.getUrl().replace(AiConstants.HTTPS_PREFIX, videoProxyPrefix),
                                o.getCoverImageUrl().replace(AiConstants.HTTPS_PREFIX, videoProxyPrefix),
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
                .model(AiConstants.VIDEO_MODEL)
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
