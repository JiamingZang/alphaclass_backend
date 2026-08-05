package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.KeywordService;
import com.imct.alphaclass.utils.TokenUtils;

@RestController
public class KeywordController {
    private final KeywordService service;
    private final TokenUtils tokenUtils;

    public KeywordController(KeywordService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/keywords",method = RequestMethod.GET)
    public JSONResult getAllKeywordsByCourse(@PathVariable String owner, @PathVariable String course) {
        List<Map<String, Object>> result = service.getAllKeywordsByCourse(owner, course);
        return JSONResult.successWithData(result);   
    }
    /** 新增关键词（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/keywords",method = RequestMethod.POST)
    public JSONResult addKeywordByCourse(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        if (user == null || !owner.equals(user.getUsername())) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        Map<String, Object> result = service.addKeywordByCourse(owner, course, params);
        return JSONResult.successWithData(result);   
    }

    /** 删除关键词（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.DELETE)
    public JSONResult deleteKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword) {
        User user = tokenUtils.getCurrentUser();
        if (user == null || !owner.equals(user.getUsername())) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        service.deleteKeywordById(owner, course, keyword);
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.GET)
    public JSONResult getKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword) {
        return JSONResult.successWithData(service.getKeywordByCourse(owner, course, keyword));
    }

    /** 修改关键词（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.PUT)
    public JSONResult modifyKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        User user = tokenUtils.getCurrentUser();
        if (user == null || !owner.equals(user.getUsername())) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        return JSONResult.successWithData(service.modifyKeywordByCourse(owner, course, keyword, params));
    }

}
