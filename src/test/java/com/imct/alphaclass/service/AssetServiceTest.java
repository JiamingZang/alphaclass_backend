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

import com.imct.alphaclass.bean.Asset;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.AnimationDAO;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.MediaModelDAO;

/**
 * AssetService 行为基线测试：分页/类型过滤/软删除过滤/响应组装。
 */
@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetDAO dao;
    @Mock
    private AccessService access;
    @Mock
    private MediaModelDAO modelinfodao;
    @Mock
    private AnimationDAO animationDAO;

    @InjectMocks
    private AssetService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("alice");
        // lenient：getAssetById/deleteById/modifyById 不经过 requireUser
        lenient().when(access.requireUser("alice")).thenReturn(user);
    }

    private Map<String, Object> buildAssetRow(int id, String type, String deletedAt) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("uid", 1);
        row.put("name", "asset" + id);
        row.put("type", type);
        row.put("url", "http://example.com/a.glb");
        row.put("thumbnail_url", "http://example.com/a.png");
        row.put("size", 1024);
        row.put("created_at", LocalDateTime.of(2024, 1, 1, 10, 30, 0));
        row.put("updated_at", LocalDateTime.of(2024, 1, 2, 11, 30, 0));
        row.put("deleted_at", deletedAt);
        return row;
    }

    private Asset buildAsset() {
        Asset asset = new Asset();
        asset.setId(300);
        asset.setUid(1);
        asset.setName("asset1");
        asset.setType("model");
        asset.setUrl("http://example.com/a.glb");
        asset.setThumbnail_url("http://example.com/a.png");
        asset.setSize(1024);
        asset.setCreated_at("2024-01-01 10:00:00");
        asset.setUpdated_at("2024-01-01 10:00:00");
        asset.setGenerated(false);
        return asset;
    }

    @Test
    void getAllByUser_withType_usesPageQueryAndFiltersDeleted() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(buildAssetRow(300, "model", null));
        rows.add(buildAssetRow(301, "model", "2024-02-01 00:00:00"));
        when(dao.getAllAssetsByUidAndPageAndType(1, 0, 5, "model")).thenReturn(rows);

        List<Map<String, Object>> result = service.getAllByUser("alice", 1, 5, "model");

        assertEquals(1, result.size());
        Map<String, Object> asset = result.get(0);
        assertEquals("300", asset.get("id"));
        assertNull(asset.get("uid"));
        assertNull(asset.get("deleted_at"));
        assertEquals("2024-01-01 10:30:00", asset.get("created_at"));
        assertEquals("2024-01-02 11:30:00", asset.get("updated_at"));
        verify(dao).getAllAssetsByUidAndPageAndType(1, 0, 5, "model");
    }

    @Test
    void getAllByUser_withoutType_usesUidQuery() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(buildAssetRow(300, "model", null));
        when(dao.getAllAssetsByUid(1)).thenReturn(rows);

        List<Map<String, Object>> result = service.getAllByUser("alice", 2, 5, null);

        assertEquals(1, result.size());
        verify(dao).getAllAssetsByUid(1);
        verify(dao, never()).getAllAssetsByUidAndPageAndType(anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void getAssetById_returnsAssetWithoutUid() {
        when(dao.getAssetById(300)).thenReturn(buildAsset());

        Map<String, Object> result = service.getAssetById(1, 300);

        assertNotNull(result);
        assertEquals("300", result.get("id"));
        assertEquals("asset1", result.get("name"));
        assertNull(result.get("uid"));
    }

    @Test
    void getAssetById_notOwner_returnsNull() {
        when(dao.getAssetById(300)).thenReturn(buildAsset());

        assertNull(service.getAssetById(2, 300));
    }

    @Test
    void addAsset_insertsAndReturnsResponse() {
        doAnswer(invocation -> {
            Asset a = invocation.getArgument(0);
            a.setId(300);
            return null;
        }).when(dao).addAsset(any(Asset.class));
        when(dao.getAssetById(300)).thenReturn(buildAsset());

        Map<String, Object> params = new HashMap<>();
        params.put("name", "asset1");
        params.put("type", "model");
        params.put("url", "http://example.com/a.glb");
        params.put("size", 1024);
        params.put("thumbnail_url", "http://example.com/a.png");
        params.put("generated", "true");

        Map<String, Object> result = service.addAsset("alice", params);

        assertNotNull(result);
        assertEquals("300", result.get("id"));
        assertEquals("asset1", result.get("name"));
        assertNull(result.get("uid"));
        // generated 解析为布尔并写入
        verify(dao).addAsset(argThat(a -> Boolean.TRUE.equals(a.getGenerated()) && a.getUid() == 1));
    }

    @Test
    void deleteById_softDeletesWithTimestamp() {
        when(dao.deleteAssetByIdAndUid(anyString(), eq(300), eq(1))).thenReturn(1);

        boolean result = service.deleteById(1, 300);

        assertTrue(result);
        verify(dao).deleteAssetByIdAndUid(anyString(), eq(300), eq(1));
    }

    @Test
    void deleteById_notOwner_returnsFalse() {
        when(dao.deleteAssetByIdAndUid(anyString(), eq(300), eq(9))).thenReturn(0);

        assertFalse(service.deleteById(9, 300));
    }

    @Test
    void modifyById_renamesAndReturnsAsset() {
        when(dao.updateAssetByIdAndUid(eq("renamed"), anyString(), eq(300), eq(1))).thenReturn(1);
        when(dao.getAssetById(300)).thenReturn(buildAsset());

        Map<String, Object> params = new HashMap<>();
        params.put("name", "renamed");

        Map<String, Object> result = service.modifyById(1, 300, params);

        assertNotNull(result);
        assertEquals("300", result.get("id"));
        verify(dao).updateAssetByIdAndUid(eq("renamed"), anyString(), eq(300), eq(1));
    }

    @Test
    void modifyById_withoutName_skipsUpdate() {
        when(dao.getAssetById(300)).thenReturn(buildAsset());

        Map<String, Object> params = new HashMap<>();

        Map<String, Object> result = service.modifyById(1, 300, params);

        assertNotNull(result);
        verify(dao, never()).updateAssetByIdAndUid(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void modifyById_notOwner_returnsNull() {
        Asset others = buildAsset();
        others.setUid(2);
        when(dao.getAssetById(300)).thenReturn(others);

        Map<String, Object> params = new HashMap<>();
        params.put("name", "renamed");

        assertNull(service.modifyById(1, 300, params));
    }
}
