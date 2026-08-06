package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.KeywordService;
import com.imct.alphaclass.utils.TokenUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "关键词", description = "课程下关键词增删改查（嵌套媒体列表）；写操作需登录且仅课程创建者可操作")
public class KeywordController {
    private final KeywordService service;
    private final TokenUtils tokenUtils;

    public KeywordController(KeywordService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/keywords",method = RequestMethod.GET)
    @Operation(summary = "关键词列表", description = "返回关键词数组（嵌套 medias 列表），不含 kid/cid 等内部字段")
    public JSONResult getAllKeywordsByCourse(@PathVariable String owner, @PathVariable String course) {
        List<Map<String, Object>> result = service.getAllKeywordsByCourse(owner, course);
        return JSONResult.successWithData(result);   
    }
    /** 新增关键词（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/keywords",method = RequestMethod.POST)
    @Operation(summary = "新增关键词", description = "需登录且仅创建者；body: {name}，返回新增关键词")
    public JSONResult addKeywordByCourse(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        Map<String, Object> result = service.addKeywordByCourse(owner, course, params);
        return JSONResult.successWithData(result);   
    }

    /** 删除关键词（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.DELETE)
    @Operation(summary = "删除关键词", description = "需登录且仅创建者；成功返回 204")
    public JSONResult deleteKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        service.deleteKeywordById(owner, course, keyword);
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.GET)
    @Operation(summary = "关键词详情", description = "返回关键词（含 medias 列表），不存在时 404")
    public JSONResult getKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword) {
        return JSONResult.successWithData(service.getKeywordByCourse(owner, course, keyword));
    }

    /** 修改关键词（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.PUT)
    @Operation(summary = "修改关键词", description = "需登录且仅创建者；body: {name}，返回更新后的关键词")
    public JSONResult modifyKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        return JSONResult.successWithData(service.modifyKeywordByCourse(owner, course, keyword, params));
    }

}
