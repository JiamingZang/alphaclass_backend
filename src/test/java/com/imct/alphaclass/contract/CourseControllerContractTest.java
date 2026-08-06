package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.imct.alphaclass.controller.CourseController;
import com.imct.alphaclass.service.CourseService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 课程接口契约快照：课程列表（200）/ 单课程不存在（404）。
 */
@WebMvcTest(CourseController.class)
class CourseControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private CourseService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getAllByUser() throws Exception {
        List<Map<String, Object>> courses = new ArrayList<>();
        Map<String, Object> c = new HashMap<>();
        c.put("id", "10");
        c.put("name", "数学");
        c.put("url", "http://localhost:8080/v2/courses/alice/math");
        courses.add(c);
        when(service.getAllByUser("alice")).thenReturn(courses);

        verifySnapshot(get("/users/alice/courses"), 200, "getAllByUser");
    }

    @Test
    void getByUserAndName_notFound_returns404() throws Exception {
        when(service.getByUserAndName("alice", "nope")).thenReturn(null);

        verifySnapshot(get("/courses/alice/nope"), 404, "getByUserAndName_notFound");
    }
}
