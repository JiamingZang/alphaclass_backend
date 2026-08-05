package com.imct.alphaclass.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.TextToImageService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 文生图接口：生成/历史/删除，业务逻辑见 {@link TextToImageService}（百度 SD-XL → OSS）。
 */
@RestController
public class TextToImageController {

    private final TextToImageService service;

    public TextToImageController(TextToImageService service) {
        this.service = service;
    }

    /** 文生图：返回图片 id/url 及落库信息 */
    @RequestMapping(value = "/services/text-to-image/generate-image", method = RequestMethod.POST)
    public JSONResult generateImage(@RequestBody Map<String, Object> params) throws com.aliyuncs.exceptions.ClientException {
        User user = TokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.generateImage(params.get("prompt").toString(), user.getId()));
    }

    /** 当前用户的文生图历史 */
    @RequestMapping(value = "/services/text-to-image/history", method = RequestMethod.GET)
    public JSONResult getHistory() {
        User user = TokenUtils.getCurrentUser();
        return JSONResult.successWithData(service.getHistory(user.getId()));
    }

    /** 删除一条文生图历史（软删除） */
    @RequestMapping(value = "/services/text-to-image/history/{id}", method = RequestMethod.DELETE)
    public void deleteHistory(@PathVariable int id) {
        service.deleteHistory(id);
    }
}
