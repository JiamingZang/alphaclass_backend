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

@RestController
public class AnchorController {
    private final AnchorService service;
    private final TokenUtils tokenUtils;

    public AnchorController(AnchorService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/anchors",method = RequestMethod.GET)
    public JSONResult getAllAnchorsByCourse(@PathVariable String owner, @PathVariable String course) {
        List<Map<String, Object>> result = service.getAllAnchorsByCourse(owner, course);
        return JSONResult.successWithData(result);   
    }

    @RequestMapping(value = "/courses/{owner}/{course}/anchors",method = RequestMethod.POST)
    public JSONResult addAnchorByCourse(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        Map<String, Object> result = service.addAnchorByCourse(owner, course, params);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_404, "课程不存在");
        }
    }

    /** 修改锚点（需登录，仅课程创建者可修改） */
    @RequestMapping(value = "/courses/{owner}/{course}/anchors/{anchor_id}",method = RequestMethod.PUT)
    public JSONResult modifyAnchorByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable int anchor_id,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        Map<String, Object> result = service.modifyAnchorById(owner, course, anchor_id,params);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_404, "锚点不存在");
        }
    }
    
    /** 删除锚点（需登录，仅课程创建者可删除） */
    @RequestMapping(value = "/courses/{owner}/{course}/anchors/{anchor_id}",method = RequestMethod.DELETE)
    public JSONResult deleteAnchorByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable int anchor_id) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可删除");
        }
        if (service.deleteAnchorById(owner, course, anchor_id)) {
            return JSONResult.customWithStatus(Constants.CODE_204);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_404, "锚点不存在");
        }
    }
}
