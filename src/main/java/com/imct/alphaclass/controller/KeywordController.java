package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.KeywordService;

import cn.hutool.json.JSON;

@RestController
public class KeywordController {
    @Autowired
    private KeywordService service;

    @RequestMapping(value = "/courses/{owner}/{course}/keywords",method = RequestMethod.GET)
    public JSONResult getAllKeywordsByCourse(@PathVariable String owner, @PathVariable String course) {
        List<Map<String, Object>> result = service.getAllKeywordsByCourse(owner, course);
        return JSONResult.successWithData(result);   
    }
    @RequestMapping(value = "/courses/{owner}/{course}/keywords",method = RequestMethod.POST)
    public JSONResult addKeywordByCourse(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        Map<String, Object> result = service.addKeywordByCourse(owner, course, params);
        return JSONResult.successWithData(result);   
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.DELETE)
    public void deleteKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword) {
        service.deleteKeywordById(owner, course, keyword);   
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.GET)
    public JSONResult getKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword) {
        return JSONResult.successWithData(service.getKeywordByCourse(owner, course, keyword));
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}",method = RequestMethod.PUT)
    public JSONResult modifyKeywordByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        return JSONResult.successWithData(service.modifyKeywordByCourse(owner, course, keyword, params));
    }

}
