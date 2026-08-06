package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.imct.alphaclass.controller.StudentCourseController;
import com.imct.alphaclass.service.StudentCourseService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 课程学生接口契约快照：学生列表（200）/ 非课程创建者添加（401）。
 */
@WebMvcTest(StudentCourseController.class)
class StudentCourseControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private StudentCourseService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getAllStudents() throws Exception {
        List<Map<String, Object>> students = new ArrayList<>();
        Map<String, Object> s = new HashMap<>();
        s.put("id", "2");
        s.put("username", "bob");
        s.put("name", "Bob");
        students.add(s);
        when(service.getAllStudentsByCourse("alice", "math")).thenReturn(students);

        verifySnapshot(get("/courses/alice/math/students"), 200, "getAllStudents");
    }

    @Test
    void addStudents_notOwner_returns401() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(null);

        verifySnapshot(post("/courses/alice/math/students")
                .header("token", buildToken(userService))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"students\":[\"bob\"]}"),
                401, "addStudents_notOwner");
    }
}
