package com.imct.alphaclass.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import com.imct.alphaclass.service.KeywordService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * KeywordController 路由与响应契约测试（MockMvc，不依赖数据库）。
 */
@WebMvcTest(KeywordController.class)
class KeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeywordService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    private User owner() {
        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        user.setPassword("secret");
        return user;
    }
    private String buildToken() {
        User user = new User();
        user.setId(1);
        user.setPassword("secret");
        when(userService.getById(1)).thenReturn(user);
        return JWT.create().withAudience("1")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(Algorithm.HMAC256("secret"));
    }

    @Test
    void getAllKeywordsByCourse_returnsKeywordList() throws Exception {
        List<Map<String, Object>> keywords = new ArrayList<>();
        Map<String, Object> k = new HashMap<>();
        k.put("id", "100");
        k.put("keyword", "k1");
        keywords.add(k);
        when(service.getAllKeywordsByCourse("alice", "math")).thenReturn(keywords);

        mockMvc.perform(get("/courses/alice/math/keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("100"));
    }

    @Test
    void addKeywordByCourse_returnsCreatedKeyword() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "101");
        result.put("keyword", "k2");
        when(service.addKeywordByCourse(eq("alice"), eq("math"), anyMap())).thenReturn(result);
        when(tokenUtils.requireOwner("alice")).thenReturn(owner());

        mockMvc.perform(post("/courses/alice/math/keywords")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"k2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("101"));
    }

    @Test
    void getKeywordByCourse_returnsKeyword() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "100");
        result.put("keyword", "k1");
        when(service.getKeywordByCourse("alice", "math", "k1")).thenReturn(result);

        mockMvc.perform(get("/courses/alice/math/k1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("100"));
    }

    @Test
    void modifyKeywordByCourse_returnsUpdatedKeyword() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "100");
        result.put("keyword", "k2");
        when(service.modifyKeywordByCourse(eq("alice"), eq("math"), eq("k1"), anyMap())).thenReturn(result);
        when(tokenUtils.requireOwner("alice")).thenReturn(owner());

        mockMvc.perform(put("/courses/alice/math/k1")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"k2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("k2"));
    }

    @Test
    void deleteKeywordByCourse_returns204() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(owner());
        mockMvc.perform(delete("/courses/alice/math/k1").header("token", buildToken()))
                .andExpect(status().isNoContent());
        verify(service).deleteKeywordById("alice", "math", "k1");
    }

    @Test
    void addKeywordByCourse_notOwner_returns401() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(null);

        mockMvc.perform(post("/courses/alice/math/keywords")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"k2\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("仅课程创建者可修改"));
        verify(service, never()).addKeywordByCourse(anyString(), anyString(), anyMap());
    }
}
