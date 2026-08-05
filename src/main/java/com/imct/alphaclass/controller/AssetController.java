package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.AssetService;
import com.imct.alphaclass.utils.TokenUtils;

@RestController
public class AssetController {
    @Autowired
    private AssetService service;

    @RequestMapping(value = "/user/assets", method = RequestMethod.GET)
    public JSONResult getAllByUser(
        @RequestParam(value = "page", required = false, defaultValue = "1") int page,
        @RequestParam(value = "perpage", required = false, defaultValue = "5") int perpage,
        @RequestParam(name = "type", required = false) String type) {
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        return JSONResult.successWithData(service.getAllByUser(user.getUsername(), page, perpage, type));
    }

    // 参数为 Map 类型以便前端直接传 JSON
    @RequestMapping(value = "/user/assets", method = RequestMethod.POST)
    public JSONResult addCourse(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = service.addAsset(TokenUtils.getCurrentUser().getUsername(), params);
        if (result != null) {
            return JSONResult.successWithData(result);
        } else {
            return JSONResult.failWithMsg(Constants.CODE_400, "参数错误");
        }
    }

    @RequestMapping(value = "/user/assets/{id}", method = RequestMethod.DELETE)
    public JSONResult deleteCourseById(@PathVariable int id) {
        service.deleteById(id);
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    @RequestMapping(value = "/user/assets/{id}", method = RequestMethod.PUT)
    public JSONResult modifyById(@PathVariable int id, @RequestBody Map<String, Object> params) {
        Map<String, Object> result = service.modifyById(id, params);
        if (result != null) {
            return JSONResult.successWithData(result);
        } else {
            return JSONResult.failWithMsg(Constants.CODE_404, "资源不存在");
        }
    }

    @RequestMapping(value = "/user/assets/{id}", method = RequestMethod.GET)
    public JSONResult getById(@PathVariable int id) {
        Map<String, Object> result = service.getAssetById(id);
        if (result != null) {
            return JSONResult.successWithData(result);
        } else {
            return JSONResult.failWithMsg(Constants.CODE_404, "资源不存在");
        }
    }
}