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
import com.imct.alphaclass.service.ModelGenerationService;
import com.imct.alphaclass.utils.TokenUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 3D 模型生成接口：文生模型/图生模型/历史/删除，业务逻辑见 {@link ModelGenerationService}
 * （腾讯混元 3D，任务结果由 ModelTaskScheduler 定时轮询落库）。
 */
@RestController
@Tag(name = "3D 模型生成", description = "腾讯混元 3D 文生/图生模型、历史查询/删除；写操作需登录")
public class ModelGenerationController {

    private final ModelGenerationService service;
    private final TokenUtils tokenUtils;

    public ModelGenerationController(ModelGenerationService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    /** 文生 3D 模型：提交任务，返回 request_id/job_id */
    @RequestMapping(value = "/services/generate-model/text-to-model", method = RequestMethod.POST)
    @Operation(summary = "文生 3D 模型", description = "需登录；body: {prompt, result_format?, enable_pbr?, enable_geometry?}；返回 {request_id, job_id, task_status}")
    public JSONResult textToModel(@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.textToModel(params, user.getId()));
    }

    /** 图生 3D 模型：提交任务，返回 request_id/job_id */
    @RequestMapping(value = "/services/generate-model/image-to-model", method = RequestMethod.POST)
    @Operation(summary = "图生 3D 模型", description = "需登录；body: {image_url, result_format?, enable_pbr?, enable_geometry?}；返回 {request_id, job_id, task_status}")
    public JSONResult imageToModel(@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.imageToModel(params, user.getId()));
    }

    /** 前端回调更新任务结果（仅允许更新当前用户的记录） */
    @RequestMapping(value = "/services/generate-model/update", method = RequestMethod.POST)
    @Operation(summary = "模型任务结果回调", description = "需登录；body: {request_id, state, url, thumbnail_url, pologen_count, size}；成功返回空字符串")
    public JSONResult updateModelResult(@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        service.updateModelResult(params, user.getId());
        return JSONResult.successWithData("");
    }

    /** 当前用户的模型生成历史（无有效 token 时 401，防止伪造 token 越权读取） */
    @RequestMapping(value = "/services/generate-model/history", method = RequestMethod.GET)
    @Operation(summary = "模型生成历史", description = "需识别用户；返回记录数组（created_at 倒序），字段：id/request_id/job_id/type/prompt/url/thumbnail_url/polygon_count/size/task_status 等")
    public JSONResult getHistory() {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        return JSONResult.successWithData(service.getHistory(user.getId()));
    }

    /** 删除一条模型生成历史（软删除，仅当前用户自己的记录） */
    @RequestMapping(value = "/services/generate-model/history/{id}", method = RequestMethod.DELETE)
    @Operation(summary = "删除模型历史", description = "需登录；软删除当前用户记录，成功返回 200 空体")
    public void deleteHistory(@PathVariable int id) {
        User user = tokenUtils.getCurrentUser();
        if (user != null) {
            service.deleteHistory(id, user.getId());
        }
    }
}
