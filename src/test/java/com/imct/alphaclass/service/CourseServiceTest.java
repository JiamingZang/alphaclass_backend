package com.imct.alphaclass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.UserDAO;

/**
 * CourseService 行为基线测试。
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseDAO dao;
    @Mock
    private UserDAO userdao;

    @InjectMocks
    private CourseService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("alice");
        user.setName("Alice");
        // lenient：getById 系列测试不经过 getByUsername
        lenient().when(userdao.getByUsername("alice")).thenReturn(user);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080/v2");
    }

    private Course buildCourse() {
        Course course = new Course();
        course.setId(10);
        course.setUid(1);
        course.setName("math");
        course.setDescription("math course");
        course.setCover_url("http://example.com/c.png");
        course.setCreated_at("2024-01-01 10:00:00");
        course.setUpdated_at("2024-01-01 10:00:00");
        return course;
    }

    @Test
    void getAllByUser_returnsCoursesWithUrlsAndUser() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 10);
        row.put("name", "math");
        row.put("description", "math course");
        row.put("cover_url", "http://example.com/c.png");
        row.put("uid", 1);
        row.put("created_at", LocalDateTime.of(2024, 1, 1, 10, 30, 0));
        row.put("updated_at", LocalDateTime.of(2024, 1, 2, 11, 30, 0));
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row);
        when(dao.getAllCourseByUid(1)).thenReturn(rows);

        List<Map<String, Object>> result = service.getAllByUser("alice");

        assertNotNull(result);
        assertEquals(1, result.size());
        Map<String, Object> course = result.get(0);
        assertEquals("10", course.get("id"));
        // URL 前缀与控制器路由一致（含 /courses）
        assertEquals("http://localhost:8080/v2/courses/alice/math/keywords", course.get("keywords_url"));
        assertEquals("http://localhost:8080/v2/courses/alice/math/anchors", course.get("anchors_url"));
        assertEquals("2024-01-01 10:30:00", course.get("created_at"));
        assertEquals("2024-01-02 11:30:00", course.get("updated_at"));
        assertNull(course.get("uid"));
        Map<String, Object> userResult = (Map<String, Object>) course.get("user");
        assertEquals("alice", userResult.get("username"));
    }

    @Test
    void addCourse_setsUidAndTimestamps() {
        Course course = buildCourse();
        doAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(10);
            return null;
        }).when(dao).addCourse(any(Course.class));
        when(dao.getCourseById(10)).thenReturn(course);

        Map<String, Object> result = service.addCourse("alice", course);

        assertNotNull(result);
        // 真实契约：addCourse 返回的 id 为 Integer（未字符串化）
        assertEquals(10, result.get("id"));
        assertEquals("math", result.get("name"));
        assertNull(result.get("uid"));
        assertEquals("http://localhost:8080/v2/courses/alice/math/keywords", result.get("keywords_url"));
        assertEquals("http://localhost:8080/v2/courses/alice/math/anchors", result.get("anchors_url"));
        // uid 被设置为当前用户
        verify(dao).addCourse(argThat(c -> c.getUid() == 1));
        // 时间戳非空
        verify(dao).addCourse(argThat(c -> c.getCreated_at() != null && c.getUpdated_at() != null));
    }

    @Test
    void getByUserAndName_returnsCourseWithUserAndUrls() {
        when(dao.getCourseByUidAndName(1, "math")).thenReturn(buildCourse());

        Map<String, Object> result = service.getByUserAndName("alice", "math");

        assertNotNull(result);
        assertEquals("10", result.get("id"));
        assertEquals("math", result.get("name"));
        assertNull(result.get("uid"));
        Map<String, Object> userResult = (Map<String, Object>) result.get("user");
        assertEquals("http://localhost:8080/v2/users/alice", userResult.get("url"));
    }

    @Test
    void getByUserAndName_notFound_returnsNull() {
        when(dao.getCourseByUidAndName(1, "nope")).thenReturn(null);

        assertNull(service.getByUserAndName("alice", "nope"));
    }

    @Test
    void getById_removesPasswordFromUser() {
        when(dao.getCourseById(10)).thenReturn(buildCourse());
        User fullUser = new User();
        fullUser.setId(1);
        fullUser.setUsername("alice");
        fullUser.setPassword("secret");
        fullUser.setName("Alice");
        when(userdao.getById(1)).thenReturn(fullUser);

        Map<String, Object> result = service.getById(10);

        assertNotNull(result);
        assertEquals("10", result.get("id"));
        Map<String, Object> userResult = (Map<String, Object>) result.get("user");
        assertEquals("alice", userResult.get("username"));
        assertNull(userResult.get("password"));
    }

    @Test
    void getById_notFound_returnsNull() {
        when(dao.getCourseById(99)).thenReturn(null);

        assertNull(service.getById(99));
    }

    @Test
    void modifyByUserAndName_updatesAndReturnsCourse() {
        when(dao.updateCourseByUidAndName(anyString(), anyString(), anyString(), anyString(), eq(1), eq("math")))
                .thenReturn(1);
        // 更新后按新名称查询
        when(dao.getCourseByUidAndName(1, "physics")).thenReturn(buildCourse());

        Map<String, Object> params = new HashMap<>();
        params.put("name", "physics");
        params.put("description", "physics course");
        params.put("cover_url", "http://example.com/p.png");

        Map<String, Object> result = service.modifyByUserAndName("alice", "math", params);

        assertNotNull(result);
        assertEquals("10", result.get("id"));
        assertEquals("http://localhost:8080/v2/courses/alice/math", result.get("url"));
        verify(dao).updateCourseByUidAndName(eq("physics"), eq("physics course"), eq("http://example.com/p.png"),
                anyString(), eq(1), eq("math"));
    }

    @Test
    void deleteByUserAndName_callsDelete() {
        when(dao.deleteCourseByUidAndName(1, "math")).thenReturn(true);

        service.deleteByUserAndName("alice", "math");

        verify(dao).deleteCourseByUidAndName(1, "math");
    }
}
