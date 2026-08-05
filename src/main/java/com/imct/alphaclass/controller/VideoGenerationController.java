package com.imct.alphaclass.controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.GenVideoResult;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.utils.TokenUtils;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.videos.VideoCreateParams;
import ai.z.openapi.service.videos.VideoObject;
import ai.z.openapi.service.videos.VideoResult;
import ai.z.openapi.service.videos.VideosResponse;

/**
 * 视频生成 AI 服务：调用智谱 CogVideoX 生成视频。
 */
@RestController
public class VideoGenerationController {

    @Resource
    private ServiceDAO servicedao;

    @Value("${ai.zhipu.api-key}")
    private String zhipuApiKey;

    private ZhipuAiClient zhipuClient() {
        return ZhipuAiClient.builder().apiKey(zhipuApiKey).build();
    }

    @RequestMapping(value = "/services/generate-video/text-to-video", method = RequestMethod.POST)
    public JSONResult textToVideo(@RequestBody Map<String, Object> params) {
        if (CheckExceedGenerationCount(10)) {
            return JSONResult.failWithMsg("403", "You have exceeded generation limit per day.");
        }
        GenVideoResult res = new GenVideoResult();
        User user = TokenUtils.getCurrentUser();
        res.setUser_id(user.getId());
        res.setPrompt(params.get("prompt").toString());
        String quality = params.get("quality").toString().equals("quality") ? "quality" : "speed";
        boolean with_audio = params.get("with_audio").toString().equals("true") ? true : false;
        res.setSize(params.get("size").toString());
        int fps = Integer.valueOf(params.get("fps").toString()) == 60 ? 60 : 30;

        VideoObject result = generateVideoRequest(res.getPrompt(), null, quality, with_audio, res.getSize(), fps).getData();
        res.setRequest_id(result.getId());
        res.setTask_status(result.getTaskStatus());
        res.setType("TextToVideo");
        res.setIs_deleted(0);
        res.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        servicedao.addVideoResult(res);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("id", res.getRequest_id());
        resp.put("task_status", res.getTask_status());
        return JSONResult.successWithData(resp);
    }

    @RequestMapping(value = "/services/generate-video/image-to-video", method = RequestMethod.POST)
    public JSONResult imageToVideo(@RequestBody Map<String, Object> params) {
        if (CheckExceedGenerationCount(10)) {
            return JSONResult.failWithMsg("403", "You have exceeded generation limit per day.");
        }
        GenVideoResult res = new GenVideoResult();
        User user = TokenUtils.getCurrentUser();
        res.setUser_id(user.getId());
        res.setPrompt(params.get("prompt").toString());
        String imageUrl = params.get("image_url").toString();
        String quality = params.get("quality").toString().equals("quality") ? "quality" : "speed";
        boolean with_audio = params.get("with_audio").toString().equals("true") ? true : false;
        res.setSize(params.get("size").toString());
        int fps = Integer.valueOf(params.get("fps").toString()) == 60 ? 60 : 30;

        VideoObject result = generateVideoRequest(res.getPrompt(), imageUrl, quality, with_audio, res.getSize(), fps).getData();
        res.setRequest_id(result.getId());
        res.setTask_status(result.getTaskStatus());
        res.setType("ImageToVideo");
        res.setIs_deleted(0);
        res.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        servicedao.addVideoResult(res);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("id", res.getRequest_id());
        resp.put("task_status", res.getTask_status());
        return JSONResult.successWithData(resp);
    }

    @RequestMapping(value = "/services/generate-video/history", method = RequestMethod.GET)
    public JSONResult GetVideoHistoryById() {
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res = servicedao.getAllVideoResults();
        List<Map<String, Object>> finalRes = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> rMap : res) {
            if (Integer.valueOf(rMap.get("user_id").toString()) == userId) {
                if (Integer.valueOf(rMap.get("is_deleted").toString()) == 0) {
                    if (rMap.get("task_status").toString().equals("PROCESSING")) {
                        VideosResponse apply = zhipuClient().videos()
                                .videoGenerationsResult(rMap.get("request_id").toString());
                        VideoObject response = apply.getData();
                        String status = response.getTaskStatus();
                        if (!status.equals("PROCESSING")) {
                            VideoResult o = apply.getData().getVideoResult().get(0);
                            servicedao.updateVideoResultById(
                                    apply.getData().getTaskStatus(),
                                    o.getUrl().replace("https://", "https://SERVER_IP_PLACEHOLDER/proxy/"),
                                    o.getCoverImageUrl().replace("https://", "https://SERVER_IP_PLACEHOLDER/proxy/"),
                                    rMap.get("request_id").toString());
                            rMap.replace("task_status", apply.getData().getTaskStatus());
                            rMap.put("url", o.getUrl());
                            rMap.put("thumbnail_url", o.getCoverImageUrl());
                        }
                    }
                    finalRes.add(rMap);
                }
            }
        }
        Collections.reverse(finalRes);
        return JSONResult.successWithData(finalRes);
    }

    @RequestMapping(value = "/services/generate-video/history/{id}", method = RequestMethod.DELETE)
    public void DeleteVidepHistoryById(@PathVariable int id) {
        servicedao.deleteVideoResultById(id);
    }

    /** 当日生成次数限制：超过 count 次则拒绝新任务 */
    public boolean CheckExceedGenerationCount(int count) {
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res = servicedao.getAllVideoResults();
        int finalCount = 0;
        for (Map<String, Object> rMap : res) {
            if (Integer.valueOf(rMap.get("user_id").toString()) == userId) {
                LocalDate time = Timestamp.valueOf(LocalDateTime.parse(rMap.get("created_at").toString()))
                        .toLocalDateTime().toLocalDate();
                LocalDate today = LocalDate.now();
                if (time.isEqual(today)) {
                    finalCount = finalCount + 1;
                }
            }
        }
        return finalCount > count;
    }

    private VideosResponse generateVideoRequest(String prompt, String imageUrl, String quality, boolean with_audio,
            String size, int fps) {
        VideoCreateParams.VideoCreateParamsBuilder<?, ?> builder = VideoCreateParams.builder()
                .model("cogvideox-3")
                .prompt(prompt)
                .quality(quality)
                .withAudio(with_audio)
                .size(size)
                .fps(fps);
        if (imageUrl != null) {
            builder.imageUrl(imageUrl);
        }
        return zhipuClient().videos().videoGenerations(builder.build());
    }
}
