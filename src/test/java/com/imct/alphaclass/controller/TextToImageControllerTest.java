package com.imct.alphaclass.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.service.TextToImageService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * TextToImageController 路由与归属契约测试（MockMvc，不依赖数据库）。
 * 覆盖：prompt 缺失 400、历史无有效 token 401、删除仅限当前用户记录。
 */
@WebMvcTest(TextToImageController.class)
class TextToImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TextToImageService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    private User currentUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        user.setPassword("secret");
        return user;
    }

    private String buildToken() {
        when(userService.getById(1)).thenReturn(currentUser());
        return JWT.create().withAudience("1")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(Algorithm.HMAC256("secret"));
    }

    @Test
    void generateImage_missingPrompt_returns400() throws Exception {
        mockMvc.perform(post("/services/text-to-image/generate-image")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("缺少 prompt 参数"));
        verify(service, never()).generateImage(anyString(), anyInt());
    }

    @Test
    void generateImage_success_returns200() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", 5);
        result.put("url", "http://oss.example.com/img.jpg");
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());
        when(service.generateImage("a cat", 1)).thenReturn(result);

        mockMvc.perform(post("/services/text-to-image/generate-image")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"a cat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getHistory_noToken_returns401() throws Exception {
        mockMvc.perform(get("/services/text-to-image/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("无token"));
        verify(service, never()).getHistory(anyInt());
    }

    @Test
    void getHistory_withToken_returns200() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(get("/services/text-to-image/history").header("token", buildToken()))
                .andExpect(status().isOk());
        verify(service).getHistory(1);
    }

    @Test
    void deleteHistory_passesCurrentUserId() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(delete("/services/text-to-image/history/300").header("token", buildToken()))
                .andExpect(status().isOk());
        verify(service).deleteHistory(300, 1);
    }
}
