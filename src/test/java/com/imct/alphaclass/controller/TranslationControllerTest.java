package com.imct.alphaclass.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.service.TranslationService;
import com.imct.alphaclass.service.TranslationService.BasicResult;
import com.imct.alphaclass.service.TranslationService.YoudaoTranslationResult;
import com.imct.alphaclass.service.UserService;

/**
 * TranslationController 路由与响应契约测试（MockMvc，不依赖网络）。
 */
@WebMvcTest(TranslationController.class)
class TranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranslationService service;

    @MockBean
    private UserService userService;

    private YoudaoTranslationResult buildTranslation() {
        YoudaoTranslationResult t = new YoudaoTranslationResult();
        t.translation = Arrays.asList("hello");
        t.basic = new BasicResult();
        t.basic.phonetic = "[həlo]";
        return t;
    }

    @Test
    void zhToEn_returnsKeywordAndExplains() throws Exception {
        when(service.translateCN("苹果")).thenReturn(buildTranslation());

        mockMvc.perform(get("/services/zh-to-en").param("word", "苹果"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("苹果"))
                .andExpect(jsonPath("$.explains[0]").value("hello"));
    }

    @Test
    void enToZh_returnsKeywordAndPhonetic() throws Exception {
        when(service.translateEN("apple")).thenReturn(buildTranslation());

        mockMvc.perform(get("/services/en-to-zh").param("word", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("apple"))
                .andExpect(jsonPath("$.phonetic").value("[həlo]"));
    }

    @Test
    void enToZh_missingBasic_returnsEmptyPhoneticNot500() throws Exception {
        YoudaoTranslationResult t = buildTranslation();
        t.basic = null;
        when(service.translateEN("apple")).thenReturn(t);

        mockMvc.perform(get("/services/en-to-zh").param("word", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phonetic").value(""));
    }

    @Test
    void zhToEn_serviceDown_returns503() throws Exception {
        when(service.translateCN("苹果"))
                .thenThrow(new ServiceException(Constants.CODE_503, "翻译服务不可用"));

        mockMvc.perform(get("/services/zh-to-en").param("word", "苹果"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("翻译服务不可用"));
    }
}
