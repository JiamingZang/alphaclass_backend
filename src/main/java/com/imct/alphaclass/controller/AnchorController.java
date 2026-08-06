package com.imct.alphaclass.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.AnchorService;
import com.imct.alphaclass.utils.TokenUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "锚点", description = "课程下锚点增删改查；写操作需登录且仅课程创建者可操作")
public class AnchorController {
    private final AnchorService service;
    private final TokenUtils tokenUtils;

    public AnchorController(AnchorService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/anchors",method = RequestMethod.GET)
    @Operation(summary = "锚点列表", description = "返回锚点数组；坐标已收拢为 pos/euler 嵌套对象")
    public JSONResult getAllAnchorsByCourse(@PathVariable String owner, @PathVariable String course) {
        List<Map<String, Object>> result = service.getAllAnchorsByCourse(owner, course);
        return JSONResult.successWithData(result);   
    }

    @RequestMapping(value = "/courses/{owner}/{course}/anchors",method = RequestMethod.POST)
    @Operation(summary = "新增锚点", description = "需登录且仅创建者；body: {name, pos{euler?}}，课程不存在返回 404")
    public JSONResult addAnchorByCourse(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        Map<String, Object> result = service.addAnchorByCourse(owner, course, params);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_COURSE_NOT_FOUND);
        }
    }

    /** 修改锚点（需登录，仅课程创建者可修改） */
    @RequestMapping(value = "/courses/{owner}/{course}/anchors/{anchor_id}",method = RequestMethod.PUT)
    @Operation(summary = "修改锚点", description = "需登录且仅创建者；body: {name, pos{euler?}}，锚点不存在返回 404")
    public JSONResult modifyAnchorByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable int anchor_id,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        Map<String, Object> result = service.modifyAnchorById(owner, course, anchor_id,params);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_ANCHOR_NOT_FOUND);
        }
    }
    
    /** 删除锚点（需登录，仅课程创建者可删除） */
    @RequestMapping(value = "/courses/{owner}/{course}/anchors/{anchor_id}",method = RequestMethod.DELETE)
    @Operation(summary = "删除锚点", description = "需登录且仅创建者；成功返回 204，锚点不存在返回 404")
    public JSONResult deleteAnchorByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable int anchor_id) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_DELETE);
        }
        if (service.deleteAnchorById(owner, course, anchor_id)) {
            return JSONResult.customWithStatus(Constants.CODE_204);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_ANCHOR_NOT_FOUND);
        }
    }
}
