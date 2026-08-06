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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 百科查询接口：百度百科网页抓取，业务逻辑见 {@link WikiService}。
 */
@RestController
@Tag(name = "百科", description = "百度百科条目搜索/详情抓取/URL 代理（含 SSRF 防御）")
public class WikiController {

    private final WikiService service;

    public WikiController(WikiService service) {
        this.service = service;
    }

    /** URL 代理抓取：返回清洗后的 HTML（供前端绕过跨域限制） */
    @RequestMapping(value = "/services/get-proxy", method = RequestMethod.GET)
    @Operation(summary = "URL 代理抓取", description = "query: url（仅公网 http/https + 80/443 端口，拒绝内网）；返回清洗后 HTML；违规地址返回 400")
    public String getDataFromUrl(@RequestParam(name = "url", required = true) String url) {
        return service.getDataFromUrl(url);
    }

    /** 按关键词搜索百科条目 */
    @RequestMapping(value = "/services/get-wiki-items", method = RequestMethod.GET)
    @Operation(summary = "百科条目搜索", description = "query: keyword；返回候选条目数组 [{keyword, title, url, description}]")
    public JSONResult getWikiItems(@RequestParam(name = "keyword", required = true) String keyword) {
        List<WikiResult> results = service.getWikiItems(keyword);
        return JSONResult.successWithData(results);
    }

    /** 抓取百科条目详情页长描述 */
    @RequestMapping(value = "/services/get-long-description", method = RequestMethod.GET)
    @Operation(summary = "百科长描述", description = "query: uri；返回 {keyword, long_description}")
    public JSONResult getLongDescription(@RequestParam(name = "uri", required = true) String uri) {
        Map<String, Object> result = service.getLongDescription(uri);
        return JSONResult.successWithData(result);
    }
}
