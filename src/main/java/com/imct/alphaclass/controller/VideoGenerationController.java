package com.imct.alphaclass.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.VideoGenerationService;
import com.imct.alphaclass.utils.TokenUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 视频生成接口：文生视频/图生视频/历史/删除，业务逻辑见 {@link VideoGenerationService}
 * （智谱 CogVideoX）。
 */
@RestController
@Tag(name = "视频生成", description = "智谱 CogVideoX 文生/图生视频、历史查询/删除；写操作需登录")
public class VideoGenerationController {

    private final VideoGenerationService service;
    private final TokenUtils tokenUtils;

    public VideoGenerationController(VideoGenerationService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    /** 文生视频：提交任务，返回 id/task_status */
    @RequestMapping(value = "/services/generate-video/text-to-video", method = RequestMethod.POST)
    @Operation(summary = "文生视频", description = "需登录；body: {prompt, size, quality?, with_audio?, fps?}；返回 {id, task_status}")
    public JSONResult textToVideo(@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        if (params.get("prompt") == null || params.get("size") == null) {
            return JSONResult.failWithMsg(Constants.CODE_400, "缺少 prompt/size 参数");
        }
        return JSONResult.successWithData(service.textToVideo(params, user.getId()));
    }

    /** 图生视频：提交任务，返回 id/task_status */
    @RequestMapping(value = "/services/generate-video/image-to-video", method = RequestMethod.POST)
    @Operation(summary = "图生视频", description = "需登录；body: {prompt, size, image_url, quality?, with_audio?, fps?}；返回 {id, task_status}")
    public JSONResult imageToVideo(@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        if (params.get("prompt") == null || params.get("size") == null || params.get("image_url") == null) {
            return JSONResult.failWithMsg(Constants.CODE_400, "缺少 prompt/size/image_url 参数");
        }
        return JSONResult.successWithData(service.imageToVideo(params, user.getId()));
    }

    /** 当前用户的视频生成历史（处理中任务实时轮询更新；无有效 token 时 401） */
    @RequestMapping(value = "/services/generate-video/history", method = RequestMethod.GET)
    @Operation(summary = "视频生成历史", description = "需识别用户；返回记录数组（倒序），PROCESSING 任务实时轮询智谱更新状态与产物地址")
    public JSONResult getHistory() {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        return JSONResult.successWithData(service.getHistory(user.getId()));
    }

    /** 删除一条视频生成历史（软删除，仅当前用户自己的记录） */
    @RequestMapping(value = "/services/generate-video/history/{id}", method = RequestMethod.DELETE)
    @Operation(summary = "删除视频历史", description = "需登录；软删除当前用户记录，成功返回 200 空体")
    public void deleteHistory(@PathVariable int id) {
        User user = tokenUtils.getCurrentUser();
        if (user != null) {
            service.deleteHistory(id, user.getId());
        }
    }
}
