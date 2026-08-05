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
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.service.CourseService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * CourseController 路由与响应契约测试（MockMvc，不依赖数据库）。
 */
@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService service;

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
        // JwtInterceptor 会用 audience 中的 userId 查用户并验证签名
        when(userService.getById(1)).thenReturn(currentUser());
        return JWT.create().withAudience("1")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(Algorithm.HMAC256("secret"));
    }

    @Test
    void getAllByUser_returnsCourses() throws Exception {
        List<Map<String, Object>> courses = new ArrayList<>();
        Map<String, Object> c = new HashMap<>();
        c.put("id", "10");
        c.put("name", "math");
        courses.add(c);
        when(service.getAllByUser("alice")).thenReturn(courses);

        mockMvc.perform(get("/users/alice/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("10"))
                .andExpect(jsonPath("$[0].name").value("math"));
    }

    @Test
    void getByUserAndName_returnsCourse() throws Exception {
        Map<String, Object> c = new HashMap<>();
        c.put("id", "10");
        c.put("name", "math");
        when(service.getByUserAndName("alice", "math")).thenReturn(c);

        mockMvc.perform(get("/courses/alice/math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("10"));
    }

    @Test
    void getByUserAndName_notFound_returns404() throws Exception {
        when(service.getByUserAndName("alice", "nope")).thenReturn(null);

        mockMvc.perform(get("/courses/alice/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getById_returnsCourse() throws Exception {
        Map<String, Object> c = new HashMap<>();
        c.put("id", "10");
        c.put("name", "math");
        when(service.getById(10)).thenReturn(c);

        mockMvc.perform(get("/courses/actions/get-project-by-id").param("id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("10"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(service.getById(999)).thenReturn(null);

        mockMvc.perform(get("/courses/actions/get-project-by-id").param("id", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("课程不存在"));
    }

    @Test
    void addCourse_returnsCreatedCourse() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "10");
        result.put("name", "math");
        when(service.addCourse(eq("alice"), any(Course.class))).thenReturn(result);
        when(userService.getById(1)).thenReturn(currentUser());

        try (MockedStatic<TokenUtils> mocked = mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());
            mockMvc.perform(post("/user/courses")
                    .header("token", buildToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"math\",\"description\":\"d\",\"cover_url\":\"http://x\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("10"));
        }
    }

    @Test
    void addCourse_wrongToken_returns401() throws Exception {
        mockMvc.perform(post("/user/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"math\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("无token,请重新登陆"));
    }

    @Test
    void modifyByUserAndName_onlyOwnerCanModify() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "10");
        result.put("name", "physics");
        when(service.modifyByUserAndName(eq("alice"), eq("math"), anyMap())).thenReturn(result);

        try (MockedStatic<TokenUtils> mocked = mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());
            mockMvc.perform(put("/courses/alice/math")
                    .header("token", buildToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"physics\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("10"));
        }
    }

    @Test
    void modifyByUserAndName_notOwner_returns401() throws Exception {
        User other = new User();
        other.setId(2);
        other.setUsername("bob");
        other.setPassword("secret");

        try (MockedStatic<TokenUtils> mocked = mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(other);
            mockMvc.perform(put("/courses/alice/math")
                    .header("token", buildToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"physics\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("仅课程创建者可修改"));
        }
    }

    @Test
    void deleteByUserAndName_returns204NoBody() throws Exception {
        when(userService.getById(1)).thenReturn(currentUser());

        try (MockedStatic<TokenUtils> mocked = mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());
            mockMvc.perform(delete("/courses/alice/math").header("token", buildToken()))
                    .andExpect(status().isNoContent());
        }
    }
}
