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
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.service.AnchorService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * AnchorController 路由与响应契约测试（MockMvc，不依赖数据库）。
 */
@WebMvcTest(AnchorController.class)
class AnchorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnchorService service;

    @MockBean
    private UserService userService;

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
    void getAllAnchorsByCourse_returnsAnchorList() throws Exception {
        List<Map<String, Object>> anchors = new ArrayList<>();
        Map<String, Object> a = new HashMap<>();
        a.put("id", "400");
        a.put("name", "anchor1");
        anchors.add(a);
        when(service.getAllAnchorsByCourse("alice", "math")).thenReturn(anchors);

        mockMvc.perform(get("/courses/alice/math/anchors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("400"));
    }

    @Test
    void addAnchorByCourse_ownerOnly_returnsCreatedAnchor() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "400");
        result.put("name", "anchor1");
        when(service.addAnchorByCourse(eq("alice"), eq("math"), anyMap())).thenReturn(result);

        try (MockedStatic<TokenUtils> mocked = mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());
            mockMvc.perform(post("/courses/alice/math/anchors")
                    .header("token", buildToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"anchor1\",\"pos\":{\"pos_x\":\"1.0\",\"pos_y\":\"2.0\",\"pos_z\":\"3.0\"},"
                            + "\"euler\":{\"euler_x\":\"10.0\",\"euler_y\":\"20.0\",\"euler_z\":\"30.0\"}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("400"));
        }
    }

    @Test
    void addAnchorByCourse_notOwner_returns401() throws Exception {
        User other = new User();
        other.setId(2);
        other.setUsername("bob");
        other.setPassword("secret");

        try (MockedStatic<TokenUtils> mocked = mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(other);
            mockMvc.perform(post("/courses/alice/math/anchors")
                    .header("token", buildToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"anchor1\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("仅课程创建者可修改"));
        }
    }

    @Test
    void modifyAnchorByCourse_returnsUpdatedAnchor() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "400");
        result.put("name", "renamed");
        when(service.modifyAnchorById(eq("alice"), eq("math"), eq(400), anyMap())).thenReturn(result);

        mockMvc.perform(put("/courses/alice/math/anchors/400")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("renamed"));
    }

    @Test
    void deleteAnchorByCourse_success_returns204() throws Exception {
        when(service.deleteAnchorById("alice", "math", 400)).thenReturn(true);

        mockMvc.perform(delete("/courses/alice/math/anchors/400").header("token", buildToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAnchorByCourse_fail_returns404() throws Exception {
        when(service.deleteAnchorById("alice", "math", 400)).thenReturn(false);

        mockMvc.perform(delete("/courses/alice/math/anchors/400").header("token", buildToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("锚点不存在"));
    }
}
