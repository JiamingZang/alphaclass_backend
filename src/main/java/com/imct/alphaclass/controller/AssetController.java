package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "资产", description = "当前用户的 3D 资产增删改查（分页）；需识别用户")
public class AssetController {
    private final AssetService service;
    private final TokenUtils tokenUtils;

    public AssetController(AssetService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/user/assets", method = RequestMethod.GET)
    @Operation(summary = "资产列表", description = "需识别用户；query: page/perpage/type?；分页参数 <1 返回 400")
    public JSONResult getAllByUser(
        @RequestParam(value = "page", required = false, defaultValue = "1") int page,
        @RequestParam(value = "perpage", required = false, defaultValue = "5") int perpage,
        @RequestParam(name = "type", required = false) String type) {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_NO_TOKEN);
        }
        if (page < 1 || perpage < 1) {
            return JSONResult.failWithMsg(Constants.CODE_400, Constants.MSG_PAGE_INVALID);
        }
        return JSONResult.successWithData(service.getAllByUser(user.getUsername(), page, perpage, type));
    }

    // 参数为 Map 类型以便前端直接传 JSON
    @RequestMapping(value = "/user/assets", method = RequestMethod.POST)
    @Operation(summary = "新增资产", description = "需登录；body: {name, type?, url?, ...}，返回新增资产")
    public JSONResult addAsset(@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_NO_TOKEN);
        }
        return JSONResult.successWithData(service.addAsset(user.getUsername(), params));
    }

    /** 删除资产（需登录，仅资产归属者可删除） */
    @RequestMapping(value = "/user/assets/{id}", method = RequestMethod.DELETE)
    @Operation(summary = "删除资产", description = "需登录且仅归属者；成功返回 204，不存在返回 404")
    public JSONResult deleteById(@PathVariable int id) {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_NO_TOKEN);
        }
        if (!service.deleteById(user.getId(), id)) {
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_ASSET_NOT_FOUND);
        }
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    /** 修改资产（需登录，仅资产归属者可修改） */
    @RequestMapping(value = "/user/assets/{id}", method = RequestMethod.PUT)
    @Operation(summary = "修改资产", description = "需登录且仅归属者；body: {name, type?, url?, ...}，不存在返回 404")
    public JSONResult modifyById(@PathVariable int id, @RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_NO_TOKEN);
        }
        Map<String, Object> result = service.modifyById(user.getId(), id, params);
        if (result != null) {
            return JSONResult.successWithData(result);
        } else {
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_ASSET_NOT_FOUND);
        }
    }

    /** 查询单个资产（需登录，仅资产归属者可查看） */
    @RequestMapping(value = "/user/assets/{id}", method = RequestMethod.GET)
    @Operation(summary = "资产详情", description = "需识别用户且仅归属者可查看；不存在返回 404")
    public JSONResult getById(@PathVariable int id) {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_NO_TOKEN);
        }
        Map<String, Object> result = service.getAssetById(user.getId(), id);
        if (result != null) {
            return JSONResult.successWithData(result);
        } else {
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_ASSET_NOT_FOUND);
        }
    }
}