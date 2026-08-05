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
import com.imct.alphaclass.service.MediaService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * MediaController 路由与响应契约测试（MockMvc，不依赖数据库）。
 */
@WebMvcTest(MediaController.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService service;

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
    void getAllMediasByCourse_returnsMediaList() throws Exception {
        List<Map<String, Object>> medias = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("id", "200");
        m.put("name", "media200");
        m.put("type", "model");
        medias.add(m);
        when(service.getAllMediasByKeyword("alice", "math", "k1")).thenReturn(medias);

        mockMvc.perform(get("/courses/alice/math/k1/medias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("200"))
                .andExpect(jsonPath("$[0].name").value("media200"));
    }

    @Test
    void getMediaById_returnsMedia() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("id", "200");
        m.put("type", "model");
        when(service.getMediaById("math", "alice", "k1", 200)).thenReturn(m);

        mockMvc.perform(get("/courses/alice/math/k1/medias/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("200"));
    }

    @Test
    void getMediaById_notFound_returns404() throws Exception {
        when(service.getMediaById("math", "alice", "k1", 999)).thenReturn(null);

        mockMvc.perform(get("/courses/alice/math/k1/medias/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("媒体不存在"));
    }

    @Test
    void addMediaByKeyword_returnsCreatedMedia() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "200");
        result.put("name", "newMedia");
        when(service.addMediaByKeyword(eq("alice"), eq("math"), eq("k1"), anyMap())).thenReturn(result);
        when(tokenUtils.getCurrentUser()).thenReturn(owner());

        mockMvc.perform(post("/courses/alice/math/k1/medias")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"newMedia\",\"type\":\"model\",\"style\":\"default\","
                        + "\"color\":{\"r\":\"1.0\",\"g\":\"0.0\",\"b\":\"0.0\"},\"anchor_id\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("200"));
    }

    @Test
    void addMediaByKeyword_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/courses/alice/math/k1/medias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"newMedia\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("无token,请重新登陆"));
    }

    @Test
    void deleteMediaById_returns204() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(owner());
        mockMvc.perform(delete("/courses/alice/math/k1/medias/200").header("token", buildToken()))
                .andExpect(status().isNoContent());
        verify(service).deleteMediaById("math", "alice", "k1", 200);
    }

    @Test
    void modifyMediaById_returnsUpdatedMedia() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "200");
        result.put("name", "renamed");
        when(service.modifyMediaById(eq("math"), eq("alice"), eq("k1"), eq(200), anyMap())).thenReturn(result);
        when(tokenUtils.getCurrentUser()).thenReturn(owner());

        mockMvc.perform(put("/courses/alice/math/k1/medias/200")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("renamed"));
    }

    @Test
    void addTransOrWikiMedia_returnsMedia() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "200");
        result.put("type", "translation");
        when(service.addMediaTranslationOrWikiByKeyword(eq("alice"), eq("math"), eq("k1"), anyMap()))
                .thenReturn(result);
        when(tokenUtils.getCurrentUser()).thenReturn(owner());

        mockMvc.perform(post("/courses/alice/math/k1/medias/trans_or_wiki")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"apple\",\"type\":\"translation\",\"style\":\"default\","
                        + "\"color\":{\"r\":\"1.0\",\"g\":\"0.0\",\"b\":\"0.0\"},\"anchor_id\":\"1\","
                        + "\"media_translation\":{\"word\":\"apple\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("200"));
    }

    @Test
    void modifyMediaById_notOwner_returns401() throws Exception {
        User other = new User();
        other.setId(2);
        other.setUsername("bob");
        other.setPassword("secret");
        when(tokenUtils.getCurrentUser()).thenReturn(other);

        mockMvc.perform(put("/courses/alice/math/k1/medias/200")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("仅课程创建者可修改"));
        verify(service, never()).modifyMediaById(anyString(), anyString(), anyString(), anyInt(), anyMap());
    }
}
