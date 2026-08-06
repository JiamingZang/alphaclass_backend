package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.service.UserService;

/**
 * Controller 契约快照测试基类：提供 token 构造与"状态码 + 完整响应体"双重校验入口。
 * 子类以 @WebMvcTest 限定被测 Controller，每个用例：
 * 1) mock Service 返回固定数据；2) verifySnapshot 校验 HTTP 状态并锁定响应体快照。
 */
public abstract class AbstractControllerContractTest {

    @Autowired
    protected MockMvc mockMvc;

    protected User buildUser(int id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("secret");
        return user;
    }

    /** 签发合法 token（与 Controller 测试一致：audience=1，密钥=密码） */
    protected String buildToken(UserService userService) {
        User user = new User();
        user.setId(1);
        user.setPassword("secret");
        when(userService.getById(1)).thenReturn(user);
        return JWT.create().withAudience("1")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(Algorithm.HMAC256("secret"));
    }

    /** 执行请求：断言 HTTP 状态码并校验响应体与契约快照一致 */
    protected void verifySnapshot(RequestBuilder request, int expectedStatus, String name) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        ContractSnapshot.verify(getClass(), name, result);
    }

    /** 执行请求：仅校验响应体快照（HTTP 状态不强制） */
    protected void verifySnapshot(RequestBuilder request, String name) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        ContractSnapshot.verify(getClass(), name, result);
    }
}
