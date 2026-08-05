package com.imct.alphaclass.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.ModelGenerationService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 3D 模型生成接口：文生模型/图生模型/历史/删除，业务逻辑见 {@link ModelGenerationService}
 * （腾讯混元 3D，任务结果由 ModelTaskScheduler 定时轮询落库）。
 */
@RestController
public class ModelGenerationController {

    private final ModelGenerationService service;

    public ModelGenerationController(ModelGenerationService service) {
        this.service = service;
    }

    /** 文生 3D 模型：提交任务，返回 request_id/job_id */
    @RequestMapping(value = "/services/generate-model/text-to-model", method = RequestMethod.POST)
    public JSONResult textToModel(@RequestBody Map<String, Object> params) {
        User user = TokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.textToModel(params, user.getId()));
    }

    /** 图生 3D 模型：提交任务，返回 request_id/job_id */
    @RequestMapping(value = "/services/generate-model/image-to-model", method = RequestMethod.POST)
    public JSONResult imageToModel(@RequestBody Map<String, Object> params) {
        User user = TokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.imageToModel(params, user.getId()));
    }

    /** 前端回调更新任务结果 */
    @RequestMapping(value = "/services/generate-model/update", method = RequestMethod.POST)
    public JSONResult updateModelResult(@RequestBody Map<String, Object> params) {
        service.updateModelResult(params);
        return JSONResult.successWithData("");
    }

    /** 当前用户的模型生成历史 */
    @RequestMapping(value = "/services/generate-model/history", method = RequestMethod.GET)
    public JSONResult getHistory() {
        User user = TokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.getHistory(user.getId()));
    }

    /** 删除一条模型生成历史（软删除） */
    @RequestMapping(value = "/services/generate-model/history/{id}", method = RequestMethod.DELETE)
    public void deleteHistory(@PathVariable int id) {
        service.deleteHistory(id);
    }
}
