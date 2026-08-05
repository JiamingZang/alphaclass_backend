package com.imct.alphaclass.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
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
import com.imct.alphaclass.service.AssetService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * AssetController 路由与鉴权契约测试（MockMvc，不依赖数据库）。
 * 覆盖：无 token 时 GET /user/assets 优雅返回 401（而非 NPE 500）。
 */
@WebMvcTest(AssetController.class)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetService service;

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
    void getAllByUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/user/assets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("无token"));
        verify(service, never()).getAllByUser(anyString(), anyInt(), anyInt(), any());
    }

    @Test
    void getAllByUser_withToken_returnsAssets() throws Exception {
        when(service.getAllByUser(eq("alice"), eq(1), eq(5), isNull())).thenReturn(new ArrayList<>());

        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        when(tokenUtils.getCurrentUser()).thenReturn(user);
        mockMvc.perform(get("/user/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteAssetById_owner_success_returns204() throws Exception {
        when(service.deleteById(1, 300)).thenReturn(true);
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(delete("/user/assets/300").header("token", buildToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAssetById_notOwner_returns404() throws Exception {
        when(service.deleteById(1, 300)).thenReturn(false);
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(delete("/user/assets/300").header("token", buildToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    @Test
    void modifyAssetById_owner_returnsUpdatedAsset() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "300");
        result.put("name", "renamed");
        when(service.modifyById(eq(1), eq(300), anyMap())).thenReturn(result);
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(put("/user/assets/300")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("300"));
    }

    @Test
    void modifyAssetById_notOwner_returns404() throws Exception {
        when(service.modifyById(eq(1), eq(300), anyMap())).thenReturn(null);
        when(tokenUtils.getCurrentUser()).thenReturn(currentUser());

        mockMvc.perform(put("/user/assets/300")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }
}
