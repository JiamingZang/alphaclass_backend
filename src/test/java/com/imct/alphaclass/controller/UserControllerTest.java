package com.imct.alphaclass.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * UserController 路由与响应契约测试（MockMvc，不依赖数据库）。
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService service;

    @MockBean
    private TokenUtils tokenUtils;

    private String loginJson() {
        return "{\"username\":\"alice\",\"password\":\"secret\",\"role\":\"teacher\"}";
    }

    @Test
    void findAll_returnsUserList() throws Exception {
        List<Map<String, Object>> users = new ArrayList<>();
        Map<String, Object> u = new HashMap<>();
        u.put("id", "1");
        u.put("username", "alice");
        u.put("url", "http://localhost:8080/v2/users/alice");
        users.add(u);
        when(service.findAll()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    void getByUsername_returnsUser() throws Exception {
        Map<String, Object> u = new HashMap<>();
        u.put("id", "1");
        u.put("username", "alice");
        when(service.getByUsername("alice")).thenReturn(u);

        mockMvc.perform(get("/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void register_success_returns200() throws Exception {
        Map<String, Object> u = new HashMap<>();
        u.put("id", "2");
        u.put("username", "bob");
        when(service.register(any(User.class))).thenReturn(u);

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(loginJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("2"));
    }

    @Test
    void register_duplicateUsername_returns401() throws Exception {
        when(service.register(any(User.class))).thenReturn(null);

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(loginJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名已被注册"));
    }

    @Test
    void login_success_addsToken() throws Exception {
        Map<String, Object> u = new HashMap<>();
        u.put("id", "1");
        u.put("username", "alice");
        u.put("password", "secret");
        when(service.login(any(User.class))).thenReturn(u);
        when(tokenUtils.getToken(anyString(), anyString())).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/users/actions/login").contentType(MediaType.APPLICATION_JSON).content(loginJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }

    @Test
    void login_fail_returns401() throws Exception {
        when(service.login(any(User.class))).thenReturn(null);

        mockMvc.perform(post("/users/actions/login").contentType(MediaType.APPLICATION_JSON).content(loginJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("认证失败"));
    }
}
