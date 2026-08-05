package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.WikiService;
import com.imct.alphaclass.service.WikiService.WikiResult;

/**
 * 百科查询接口：百度百科网页抓取，业务逻辑见 {@link WikiService}。
 */
@RestController
public class WikiController {

    private final WikiService service;

    public WikiController(WikiService service) {
        this.service = service;
    }

    /** URL 代理抓取：返回清洗后的 HTML（供前端绕过跨域限制） */
    @RequestMapping(value = "/services/get-proxy", method = RequestMethod.GET)
    public String getDataFromUrl(@RequestParam(name = "url", required = true) String url) {
        return service.getDataFromUrl(url);
    }

    /** 按关键词搜索百科条目 */
    @RequestMapping(value = "/services/get-wiki-items", method = RequestMethod.GET)
    public JSONResult getWikiItems(@RequestParam(name = "keyword", required = true) String keyword) {
        List<WikiResult> results = service.getWikiItems(keyword);
        return JSONResult.successWithData(results);
    }

    /** 抓取百科条目详情页长描述 */
    @RequestMapping(value = "/services/get-long-description", method = RequestMethod.GET)
    public JSONResult getLongDescription(@RequestParam(name = "uri", required = true) String uri) {
        Map<String, Object> result = service.getLongDescription(uri);
        return JSONResult.successWithData(result);
    }
}
