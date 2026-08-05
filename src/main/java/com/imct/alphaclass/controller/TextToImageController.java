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
import com.imct.alphaclass.service.TextToImageService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 文生图接口：生成/历史/删除，业务逻辑见 {@link TextToImageService}（百度 SD-XL → OSS）。
 */
@RestController
public class TextToImageController {

    private final TextToImageService service;
    private final TokenUtils tokenUtils;

    public TextToImageController(TextToImageService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    /** 文生图：返回图片 id/url 及落库信息 */
    @RequestMapping(value = "/services/text-to-image/generate-image", method = RequestMethod.POST)
    public JSONResult generateImage(@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        Object prompt = params.get("prompt");
        if (prompt == null) {
            return JSONResult.failWithMsg(Constants.CODE_400, "缺少 prompt 参数");
        }
        return JSONResult.successWithData(service.generateImage(prompt.toString(), user.getId()));
    }

    /** 当前用户的文生图历史（无有效 token 时 401，防止伪造 token 越权读取） */
    @RequestMapping(value = "/services/text-to-image/history", method = RequestMethod.GET)
    public JSONResult getHistory() {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        return JSONResult.successWithData(service.getHistory(user.getId()));
    }

    /** 删除一条文生图历史（软删除，仅当前用户自己的记录） */
    @RequestMapping(value = "/services/text-to-image/history/{id}", method = RequestMethod.DELETE)
    public void deleteHistory(@PathVariable int id) {
        User user = tokenUtils.getCurrentUser();
        if (user != null) {
            service.deleteHistory(id, user.getId());
        }
    }
}
