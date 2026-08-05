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
import com.imct.alphaclass.service.ModelGenerationService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * ModelGenerationController 路由与归属契约测试（MockMvc，不依赖数据库）。
 * 覆盖：历史无有效 token 401、update 回调仅限当前用户、删除仅限当前用户记录。
 */
@WebMvcTest(ModelGenerationController.class)
class ModelGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModelGenerationService service;

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
    void getHistory_noToken_returns401() throws Exception {
        mockMvc.perform(get("/services/generate-model/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("无token"));
        verify(service, never()).getHistory(anyInt());
    }

    @Test
    void getHistory_withToken_returns200() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(get("/services/generate-model/history").header("token", buildToken()))
                .andExpect(status().isOk());
        verify(service).getHistory(1);
    }

    @Test
    void updateModelResult_noToken_returns401() throws Exception {
        mockMvc.perform(post("/services/generate-model/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"request_id\":\"r1\"}"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).updateModelResult(anyMap(), anyInt());
    }

    @Test
    void updateModelResult_passesCurrentUserId() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(post("/services/generate-model/update")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"request_id\":\"r1\",\"state\":\"DONE\",\"url\":\"u\","
                        + "\"thumbnail_url\":\"t\",\"pologen_count\":\"100\",\"size\":\"512\"}"))
                .andExpect(status().isOk());
        verify(service).updateModelResult(anyMap(), eq(1));
    }

    @Test
    void deleteHistory_passesCurrentUserId() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(delete("/services/generate-model/history/300").header("token", buildToken()))
                .andExpect(status().isOk());
        verify(service).deleteHistory(300, 1);
    }
}
