package com.imct.alphaclass.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.VideoGenerationService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 视频生成接口：文生视频/图生视频/历史/删除，业务逻辑见 {@link VideoGenerationService}
 * （智谱 CogVideoX）。
 */
@RestController
public class VideoGenerationController {

    private final VideoGenerationService service;

    public VideoGenerationController(VideoGenerationService service) {
        this.service = service;
    }

    /** 文生视频：提交任务，返回 id/task_status */
    @RequestMapping(value = "/services/generate-video/text-to-video", method = RequestMethod.POST)
    public JSONResult textToVideo(@RequestBody Map<String, Object> params) {
        User user = TokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.textToVideo(params, user.getId()));
    }

    /** 图生视频：提交任务，返回 id/task_status */
    @RequestMapping(value = "/services/generate-video/image-to-video", method = RequestMethod.POST)
    public JSONResult imageToVideo(@RequestBody Map<String, Object> params) {
        User user = TokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.imageToVideo(params, user.getId()));
    }

    /** 当前用户的视频生成历史（处理中任务实时轮询更新） */
    @RequestMapping(value = "/services/generate-video/history", method = RequestMethod.GET)
    public JSONResult getHistory() {
        User user = TokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.getHistory(user.getId()));
    }

    /** 删除一条视频生成历史（软删除） */
    @RequestMapping(value = "/services/generate-video/history/{id}", method = RequestMethod.DELETE)
    public void deleteHistory(@PathVariable int id) {
        service.deleteHistory(id);
    }
}
