package com.imct.alphaclass.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.service.WikiService;
import com.imct.alphaclass.service.WikiService.WikiResult;
import com.imct.alphaclass.service.UserService;

/**
 * WikiController 路由与响应契约测试（MockMvc，不依赖网络）。
 * SSRF 校验在 Service 发起请求前完成，Controller 层验证 400 语义与成功结构。
 */
@WebMvcTest(WikiController.class)
class WikiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WikiService service;

    @MockBean
    private UserService userService;

    @Test
    void getProxy_missingUrl_returns400() throws Exception {
        mockMvc.perform(get("/services/get-proxy"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("缺少必要参数: url"));
    }

    @Test
    void getProxy_wrongMethod_returns405() throws Exception {
        mockMvc.perform(post("/services/get-proxy").param("url", "https://example.com"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("请求方法不支持"));
    }

    @Test
    void getProxy_internalHost_returns400() throws Exception {
        when(service.getDataFromUrl("http://127.0.0.1:8080/secret"))
                .thenThrow(new ServiceException(Constants.CODE_400, "不允许访问内网地址"));
        mockMvc.perform(get("/services/get-proxy").param("url", "http://127.0.0.1:8080/secret"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不允许访问内网地址"));
    }

    @Test
    void getProxy_nonHttpScheme_returns400() throws Exception {
        when(service.getDataFromUrl("file:///etc/passwd"))
                .thenThrow(new ServiceException(Constants.CODE_400, "仅支持 http/https 地址"));
        mockMvc.perform(get("/services/get-proxy").param("url", "file:///etc/passwd"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("仅支持 http/https 地址"));
    }

    @Test
    void getProxy_nonStandardPort_returns400() throws Exception {
        when(service.getDataFromUrl("http://example.com:8080/x"))
                .thenThrow(new ServiceException(Constants.CODE_400, "仅支持 80/443 端口"));
        mockMvc.perform(get("/services/get-proxy").param("url", "http://example.com:8080/x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("仅支持 80/443 端口"));
    }

    @Test
    void getProxy_publicUrl_returnsCleanedHtml() throws Exception {
        when(service.getDataFromUrl("https://example.com/page"))
                .thenReturn("<html><body>x</body></html>");

        mockMvc.perform(get("/services/get-proxy").param("url", "https://example.com/page"))
                .andExpect(status().isOk())
                .andExpect(content().string("<html><body>x</body></html>"));
    }

    @Test
    void getWikiItems_returnsCandidateList() throws Exception {
        WikiResult r = new WikiResult("苹果", "苹果（水果）", "https://baike.baidu.com/item/苹果", "");
        when(service.getWikiItems("苹果")).thenReturn(Collections.singletonList(r));

        mockMvc.perform(get("/services/get-wiki-items").param("keyword", "苹果"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyword").value("苹果"))
                .andExpect(jsonPath("$[0].url").value("https://baike.baidu.com/item/苹果"));
    }

    @Test
    void getLongDescription_returnsKeywordAndDescription() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", "苹果");
        result.put("long_description", "苹果是蔷薇科植物");
        when(service.getLongDescription("https://baike.baidu.com/item/苹果")).thenReturn(result);

        mockMvc.perform(get("/services/get-long-description").param("uri", "https://baike.baidu.com/item/苹果"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("苹果"))
                .andExpect(jsonPath("$.long_description").value("苹果是蔷薇科植物"));
    }
}
