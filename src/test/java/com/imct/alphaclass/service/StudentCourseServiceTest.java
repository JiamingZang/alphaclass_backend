package com.imct.alphaclass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.StudentCourseDAO;
import com.imct.alphaclass.dao.UserDAO;

/**
 * StudentCourseService 行为基线测试。
 */
@ExtendWith(MockitoExtension.class)
class StudentCourseServiceTest {

    @Mock
    private StudentCourseDAO dao;
    @Mock
    private CourseDAO coursedao;
    @Mock
    private UserDAO userdao;
    @Mock
    private AccessService access;

    @InjectMocks
    private StudentCourseService service;

    private User owner;
    private Course course;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1);
        owner.setUsername("alice");
        course = new Course();
        course.setId(10);
        course.setName("math");
        // lenient：getLoginUserCourses 不经过课程归属校验
        lenient().when(access.requireCourse("alice", "math")).thenReturn(course);
    }

    @Test
    void getAllStudentsByCourse_returnsUsersWithoutPassword() {
        List<Map<String, Object>> scRows = new ArrayList<>();
        Map<String, Object> sc = new HashMap<>();
        sc.put("id", 1);
        sc.put("cid", 10);
        sc.put("sid", 2);
        scRows.add(sc);
        when(dao.getAllByCid(10)).thenReturn(scRows);

        User student = new User();
        student.setId(2);
        student.setUsername("bob");
        student.setPassword("secret");
        student.setName("Bob");
        when(userdao.getById(2)).thenReturn(student);
        Map<String, Object> bobMap = new HashMap<>();
        bobMap.put("id", "2");
        bobMap.put("username", "bob");
        bobMap.put("url", "http://localhost:8080/v2/users/bob");
        when(access.toUserMap(student)).thenReturn(bobMap);

        List<Map<String, Object>> result = service.getAllStudentsByCourse("alice", "math");

        assertEquals(1, result.size());
        Map<String, Object> u = result.get(0);
        assertEquals("2", u.get("id"));
        assertEquals("bob", u.get("username"));
        assertNull(u.get("password"));
        assertEquals("http://localhost:8080/v2/users/bob", u.get("url"));
    }

    @Test
    void addStudentsByUsername_insertsEachStudent() {
        User bob = new User();
        bob.setId(2);
        bob.setUsername("bob");
        User carol = new User();
        carol.setId(3);
        carol.setUsername("carol");
        when(access.requireUser("bob")).thenReturn(bob);
        when(access.requireUser("carol")).thenReturn(carol);

        List<String> students = new ArrayList<>();
        students.add("bob");
        students.add("carol");

        service.addStudentsByUsername(students, "alice", "math");

        verify(dao).addStudentCourse(argThat(s -> s.getSid() == 2 && s.getCid() == 10));
        verify(dao).addStudentCourse(argThat(s -> s.getSid() == 3 && s.getCid() == 10));
    }

    @Test
    void deleteStudentsByUsername_deletesEachStudent() {
        User bob = new User();
        bob.setId(2);
        bob.setUsername("bob");
        when(access.requireUser("bob")).thenReturn(bob);

        List<String> students = new ArrayList<>();
        students.add("bob");

        service.deleteStudentsByUsername(students, "alice", "math");

        verify(dao).deleteCourseByUidAndName(10, 2);
    }

    @Test
    void getLoginUserCourses_returnsCoursesWithOwner() {
        List<Map<String, Object>> scRows = new ArrayList<>();
        Map<String, Object> sc = new HashMap<>();
        sc.put("id", 1);
        sc.put("cid", 10);
        sc.put("sid", 2);
        scRows.add(sc);
        when(dao.getAllBySid(2)).thenReturn(scRows);

        Course c = new Course();
        c.setId(10);
        c.setUid(1);
        c.setName("math");
        c.setDescription("math course");
        c.setCover_url("http://example.com/c.png");
        c.setCreated_at("2024-01-01 10:00:00");
        c.setUpdated_at("2024-01-01 10:00:00");
        when(coursedao.getCourseById(10)).thenReturn(c);

        User ownerUser = new User();
        ownerUser.setId(1);
        ownerUser.setUsername("alice");
        ownerUser.setName("Alice");
        ownerUser.setPassword("secret");
        when(userdao.getById(1)).thenReturn(ownerUser);
        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put("id", "1");
        ownerMap.put("username", "alice");
        when(access.toUserMap(ownerUser)).thenReturn(ownerMap);
        when(access.courseUrl("alice", "math")).thenReturn("http://localhost:8080/v2/alice/math");

        List<Map<String, Object>> result = service.getLoginUserCourses(2);

        assertEquals(1, result.size());
        Map<String, Object> courseResult = result.get(0);
        assertEquals("10", courseResult.get("id"));
        assertEquals("math", courseResult.get("name"));
        assertNull(courseResult.get("uid"));
        assertNull(courseResult.get("created_at"));
        assertNull(courseResult.get("updated_at"));
        assertEquals("http://localhost:8080/v2/alice/math", courseResult.get("course_url"));
        Map<String, Object> userResult = (Map<String, Object>) courseResult.get("user");
        assertEquals("alice", userResult.get("username"));
        assertNull(userResult.get("password"));
    }
}
