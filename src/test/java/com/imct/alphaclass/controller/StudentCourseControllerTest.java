package com.imct.alphaclass.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.imct.alphaclass.service.StudentCourseService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * StudentCourseController 路由与响应契约测试（MockMvc，不依赖数据库）。
 */
@WebMvcTest(StudentCourseController.class)
class StudentCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentCourseService service;

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

    private List<Map<String, Object>> buildStudents() {
        List<Map<String, Object>> students = new ArrayList<>();
        Map<String, Object> s = new HashMap<>();
        s.put("id", "2");
        s.put("username", "bob");
        s.put("name", "Bob");
        students.add(s);
        return students;
    }

    @Test
    void getAllStudentsByCourse_returnsStudentList() throws Exception {
        when(service.getAllStudentsByCourse("alice", "math")).thenReturn(buildStudents());

        mockMvc.perform(get("/courses/alice/math/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("2"))
                .andExpect(jsonPath("$[0].username").value("bob"));
    }

    @Test
    void addStudents_notOwner_returns401() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(null);

        mockMvc.perform(post("/courses/alice/math/students")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"students\":[\"bob\"]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("仅课程创建者可修改"));
        verify(service, never()).addStudentsByUsername(anyList(), anyString(), anyString());
    }

    @Test
    void addStudents_missingStudentsParam_returns400() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(owner());

        mockMvc.perform(post("/courses/alice/math/students")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("students 参数不合法"));
        verify(service, never()).addStudentsByUsername(anyList(), anyString(), anyString());
    }

    @Test
    void addStudents_success_returns204() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(owner());

        mockMvc.perform(post("/courses/alice/math/students")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"students\":[\"bob\",\"carol\"]}"))
                .andExpect(status().isNoContent());
        verify(service).addStudentsByUsername(Arrays.asList("bob", "carol"), "alice", "math");
    }

    @Test
    void deleteStudents_success_returns204() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(owner());

        mockMvc.perform(delete("/courses/alice/math/students")
                .header("token", buildToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"students\":[\"bob\"]}"))
                .andExpect(status().isNoContent());
        verify(service).deleteStudentsByUsername(Arrays.asList("bob"), "alice", "math");
    }

    @Test
    void registerCourses_noToken_returns401() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(null);

        mockMvc.perform(get("/user/register-courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("无token"));
        verify(service, never()).getLoginUserCourses(anyInt());
    }

    @Test
    void registerCourses_returnsCourseList() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(owner());
        List<Map<String, Object>> courses = new ArrayList<>();
        Map<String, Object> c = new HashMap<>();
        c.put("id", "10");
        c.put("name", "数学");
        courses.add(c);
        when(service.getLoginUserCourses(1)).thenReturn(courses);

        mockMvc.perform(get("/user/register-courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("10"))
                .andExpect(jsonPath("$[0].name").value("数学"));
    }
}
