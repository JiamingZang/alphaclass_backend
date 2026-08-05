package com.imct.alphaclass.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @Test
    void getAllByUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/user/assets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(""));
        verify(service, never()).getAllByUser(anyString(), anyInt(), anyInt(), any());
    }

    @Test
    void getAllByUser_withToken_returnsAssets() throws Exception {
        when(service.getAllByUser(eq("alice"), eq(1), eq(5), isNull())).thenReturn(new ArrayList<>());

        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        try (MockedStatic<TokenUtils> mocked = mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(user);
            mockMvc.perform(get("/user/assets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
