package com.imct.alphaclass.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.imct.alphaclass.controller.UserController;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 用户接口契约快照：注册成功 / 登录失败（401）。
 */
@WebMvcTest(UserController.class)
class UserControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private UserService service;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void register_success() throws Exception {
        Map<String, Object> user = new HashMap<>();
        user.put("id", "2");
        user.put("username", "bob");
        user.put("role", "student");
        user.put("name", "Bob");
        user.put("url", "http://localhost:8080/v2/users/bob");
        user.put("courses_url", "http://localhost:8080/v2/users/bob/courses");
        when(service.register(any())).thenReturn(user);

        verifySnapshot(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\",\"password\":\"pwd\",\"role\":\"student\",\"name\":\"Bob\"}"),
                200, "register_success");
    }

    @Test
    void login_fail_returns401() throws Exception {
        when(service.login(any())).thenReturn(null);

        verifySnapshot(post("/users/actions/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"wrong\"}"),
                401, "login_fail");
    }
}
