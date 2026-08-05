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
import org.springframework.test.util.ReflectionTestUtils;

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;

/**
 * KeywordService 行为测试：keyword CRUD 与 medias 透传契约。
 * media 嵌套组装逻辑由 MediaService 负责（getMediasByKid），此处仅验证透传。
 */
@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @Mock
    private KeywordDAO dao;
    @Mock
    private UserDAO userdao;
    @Mock
    private CourseDAO coursedao;
    @Mock
    private MediaService mediaservice;

    @InjectMocks
    private KeywordService service;

    private User user;
    private Course course;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("alice");

        course = new Course();
        course.setId(10);
        course.setName("math");

        // lenient：守卫类测试不经过完整链路
        lenient().when(userdao.getByUsername("alice")).thenReturn(user);
        lenient().when(coursedao.getCourseByUidAndName(1, "math")).thenReturn(course);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080/v2");
    }

    private Keyword buildKeyword(int id, String name) {
        Keyword keyword = new Keyword();
        keyword.setId(id);
        keyword.setCid(10);
        keyword.setKeyword(name);
        return keyword;
    }

    private List<Map<String, Object>> buildMediaList() {
        List<Map<String, Object>> mediaList = new ArrayList<>();
        Map<String, Object> am = new HashMap<>();
        am.put("id", "200");
        am.put("name", "media200");
        am.put("type", "model");
        mediaList.add(am);
        return mediaList;
    }

    @Test
    void addKeywordByCourse_returnsKeywordWithUrl() {
        doAnswer(invocation -> {
            Keyword k = invocation.getArgument(0);
            k.setId(101);
            return null;
        }).when(dao).addKeyword(any(Keyword.class));
        when(dao.getKeywordById(101)).thenReturn(buildKeyword(101, "k2"));

        Map<String, Object> result = service.addKeywordByCourse("alice", "math", new HashMap<String, Object>() {
            {
                put("keyword", "k2");
            }
        });

        assertNotNull(result);
        assertEquals("101", result.get("id"));
        assertEquals("k2", result.get("keyword"));
        assertNull(result.get("cid"));
        assertEquals("http://localhost:8080/v2/alice/math/k2", result.get("url"));
    }

    @Test
    void deleteKeywordById_callsDelete() {
        service.deleteKeywordById("alice", "math", "k1");

        verify(dao).deleteKeywordByCidAndName(10, "k1");
    }

    @Test
    void getKeywordByCourse_returnsKeywordWithMedias() {
        when(dao.getKeywordByCidAndName(10, "k1")).thenReturn(buildKeyword(100, "k1"));
        List<Map<String, Object>> mediaList = buildMediaList();
        when(mediaservice.getMediasByKid(100)).thenReturn(mediaList);

        Map<String, Object> result = service.getKeywordByCourse("alice", "math", "k1");

        assertNotNull(result);
        assertEquals("100", result.get("id"));
        assertEquals("k1", result.get("keyword"));
        assertNull(result.get("cid"));
        List<Map<String, Object>> medias = (List<Map<String, Object>>) result.get("medias");
        assertEquals(1, medias.size());
        assertEquals("200", medias.get(0).get("id"));
    }

    @Test
    void getKeywordByCourse_keywordNotFound_throws404() {
        when(dao.getKeywordByCidAndName(10, "missing")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getKeywordByCourse("alice", "math", "missing"));
        assertEquals("404", ex.getCode());
    }

    @Test
    void getKeywordByCourse_userNotFound_throws404() {
        when(userdao.getByUsername("ghost")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getKeywordByCourse("ghost", "math", "k1"));
        assertEquals("404", ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void getKeywordByCourse_courseNotFound_throws404() {
        when(coursedao.getCourseByUidAndName(1, "nope")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getKeywordByCourse("alice", "nope", "k1"));
        assertEquals("404", ex.getCode());
        assertEquals("课程不存在", ex.getMessage());
    }

    @Test
    void getAllKeywordsByCourse_returnsKeywordsEachWithMedias() {
        List<Map<String, Object>> keywordRows = new ArrayList<>();
        Map<String, Object> k1 = new HashMap<>();
        k1.put("id", 100);
        k1.put("keyword", "k1");
        k1.put("cid", 10);
        keywordRows.add(k1);
        when(dao.getAllKeywordsByCid(10)).thenReturn(keywordRows);
        when(mediaservice.getMediasByKid(100)).thenReturn(buildMediaList());

        List<Map<String, Object>> result = service.getAllKeywordsByCourse("alice", "math");

        assertNotNull(result);
        assertEquals(1, result.size());
        Map<String, Object> k = result.get(0);
        assertEquals("100", k.get("id"));
        assertNull(k.get("cid"));
        List<Map<String, Object>> medias = (List<Map<String, Object>>) k.get("medias");
        assertEquals(1, medias.size());
        assertEquals("200", medias.get(0).get("id"));
    }

    @Test
    void modifyKeywordByCourse_renamesKeyword() {
        when(dao.getKeywordByCidAndName(10, "k1")).thenReturn(buildKeyword(100, "k1"));
        when(dao.updateKeywordByCidAndName(eq("k2"), eq(10), eq("k1"))).thenReturn(1);
        when(dao.getKeywordByCidAndName(10, "k2")).thenReturn(buildKeyword(100, "k2"));
        when(mediaservice.getMediasByKid(100)).thenReturn(buildMediaList());

        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "k2");

        Map<String, Object> result = service.modifyKeywordByCourse("alice", "math", "k1", params);

        assertNotNull(result);
        assertEquals("100", result.get("id"));
        assertEquals("k2", result.get("keyword"));
        assertEquals(1, ((List<?>) result.get("medias")).size());
    }
}
