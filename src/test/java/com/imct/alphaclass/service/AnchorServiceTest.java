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

import com.imct.alphaclass.bean.Anchor;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.dao.AnchorDAO;

/**
 * AnchorService 行为基线测试：pos/euler 嵌套响应组装与归属校验。
 */
@ExtendWith(MockitoExtension.class)
class AnchorServiceTest {

    @Mock
    private AnchorDAO dao;
    @Mock
    private AccessService access;

    @InjectMocks
    private AnchorService service;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10);
        course.setName("math");
        when(access.requireCourse("alice", "math")).thenReturn(course);
    }

    private Map<String, Object> buildAnchorRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 400);
        row.put("cid", 10);
        row.put("name", "anchor1");
        row.put("pos_x", 1.0f);
        row.put("pos_y", 2.0f);
        row.put("pos_z", 3.0f);
        row.put("euler_x", 10.0f);
        row.put("euler_y", 20.0f);
        row.put("euler_z", 30.0f);
        return row;
    }

    private Anchor buildAnchor() {
        Anchor anchor = new Anchor();
        anchor.setId(400);
        anchor.setCid(10);
        anchor.setName("anchor1");
        anchor.setPos_x(1.0f);
        anchor.setPos_y(2.0f);
        anchor.setPos_z(3.0f);
        anchor.setEuler_x(10.0f);
        anchor.setEuler_y(20.0f);
        anchor.setEuler_z(30.0f);
        return anchor;
    }

    @Test
    void getAllAnchorsByCourse_returnsPosEulerNested() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(buildAnchorRow());
        when(dao.getAllByCid(10)).thenReturn(rows);

        List<Map<String, Object>> result = service.getAllAnchorsByCourse("alice", "math");

        assertEquals(1, result.size());
        Map<String, Object> anchor = result.get(0);
        assertEquals("400", anchor.get("id"));
        assertNull(anchor.get("cid"));
        assertNull(anchor.get("pos_x"));
        Map<String, Object> pos = (Map<String, Object>) anchor.get("pos");
        assertEquals(1.0f, pos.get("pos_x"));
        assertEquals(2.0f, pos.get("pos_y"));
        assertEquals(3.0f, pos.get("pos_z"));
        Map<String, Object> euler = (Map<String, Object>) anchor.get("euler");
        assertEquals(10.0f, euler.get("euler_x"));
        assertEquals(30.0f, euler.get("euler_z"));
    }

    @Test
    void addAnchorByCourse_insertsAndReturnsNestedStructure() {
        doAnswer(invocation -> {
            Anchor a = invocation.getArgument(0);
            a.setId(400);
            return null;
        }).when(dao).addAnchor(any(Anchor.class));
        when(dao.getAnchorById(400)).thenReturn(buildAnchor());

        Map<String, Object> params = new HashMap<>();
        params.put("name", "anchor1");
        Map<String, Object> pos = new HashMap<>();
        pos.put("pos_x", "1.0");
        pos.put("pos_y", "2.0");
        pos.put("pos_z", "3.0");
        params.put("pos", pos);
        Map<String, Object> euler = new HashMap<>();
        euler.put("euler_x", "10.0");
        euler.put("euler_y", "20.0");
        euler.put("euler_z", "30.0");
        params.put("euler", euler);

        Map<String, Object> result = service.addAnchorByCourse("alice", "math", params);

        assertNotNull(result);
        assertEquals("400", result.get("id"));
        assertEquals("anchor1", result.get("name"));
        Map<String, Object> resultPos = (Map<String, Object>) result.get("pos");
        // fastjson 反序列化后 pos 值为 BigDecimal，按数值比较
        assertEquals(1.0, ((Number) resultPos.get("pos_x")).doubleValue());
        Map<String, Object> resultEuler = (Map<String, Object>) result.get("euler");
        assertEquals(10.0, ((Number) resultEuler.get("euler_x")).doubleValue());
        verify(dao).addAnchor(argThat(a -> a.getCid() == 10));
    }

    @Test
    void deleteAnchorById_belongsToCourse_deletesAndReturnsTrue() {
        // 第一次返回 anchor（校验归属），删除后再次查询返回 null（确认删除成功）
        when(dao.getAnchorById(400)).thenReturn(buildAnchor(), null);

        boolean result = service.deleteAnchorById("alice", "math", 400);

        assertTrue(result);
        verify(dao).deleteAnchorById(400);
    }

    @Test
    void deleteAnchorById_otherCourse_returnsFalse() {
        Anchor other = buildAnchor();
        other.setCid(999);
        when(dao.getAnchorById(400)).thenReturn(other);

        boolean result = service.deleteAnchorById("alice", "math", 400);

        assertFalse(result);
        verify(dao, never()).deleteAnchorById(anyInt());
    }

    @Test
    void modifyAnchorById_updatesAndReturnsNestedStructure() {
        Anchor updated = buildAnchor();
        updated.setName("renamed");
        updated.setPos_x(5.0f);
        updated.setPos_y(6.0f);
        updated.setPos_z(7.0f);
        // 第一次返回旧数据（读旧值），update 后第二次返回新数据（回读组装）
        when(dao.getAnchorById(400)).thenReturn(buildAnchor(), updated);
        when(dao.updateAnchorById(anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                anyFloat(), eq(400))).thenReturn(1);

        Map<String, Object> params = new HashMap<>();
        params.put("name", "renamed");
        Map<String, Object> pos = new HashMap<>();
        pos.put("pos_x", "5.0");
        pos.put("pos_y", "6.0");
        pos.put("pos_z", "7.0");
        params.put("pos", pos);

        Map<String, Object> result = service.modifyAnchorById("alice", "math", 400, params);

        assertNotNull(result);
        assertEquals("renamed", result.get("name"));
        Map<String, Object> resultPos = (Map<String, Object>) result.get("pos");
        assertEquals(5.0, ((Number) resultPos.get("pos_x")).doubleValue());
        // 未传 euler 保留旧值
        Map<String, Object> resultEuler = (Map<String, Object>) result.get("euler");
        assertEquals(10.0, ((Number) resultEuler.get("euler_x")).doubleValue());
        verify(dao).updateAnchorById(eq("renamed"), eq(5.0f), eq(6.0f), eq(7.0f), eq(10.0f), eq(20.0f), eq(30.0f),
                eq(400));
    }

    @Test
    void modifyAnchorById_otherCourse_returnsNull() {
        Anchor other = buildAnchor();
        other.setCid(999);
        when(dao.getAnchorById(400)).thenReturn(other);

        assertNull(service.modifyAnchorById("alice", "math", 400, new HashMap<>()));
    }
}
