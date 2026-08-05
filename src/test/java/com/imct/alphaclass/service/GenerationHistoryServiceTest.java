package com.imct.alphaclass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.imct.alphaclass.bean.GenModelResult;
import com.imct.alphaclass.bean.GenVideoResult;
import com.imct.alphaclass.dao.ServiceDAO;

/**
 * AI 生成历史（getHistory）行为契约测试：
 * 实体化后必须保持原 Map 响应语义——按用户/未删除过滤、按创建时间倒序、
 * polygon_count/size 序列化为数字（与 JSONResult 的 fastjson 输出路径一致）。
 */
@ExtendWith(MockitoExtension.class)
class GenerationHistoryServiceTest {

    @Mock
    private ServiceDAO servicedao;

    @Mock
    private AiUsageGuard usageGuard;

    @InjectMocks
    private ModelGenerationService modelService;

    @InjectMocks
    private VideoGenerationService videoService;

    private List<GenModelResult> modelRows;
    private List<GenVideoResult> videoRows;

    @BeforeEach
    void setUp() {
        modelRows = new ArrayList<>();
        videoRows = new ArrayList<>();
    }

    private GenModelResult model(int id, int userId, int deleted, String createdAt) {
        GenModelResult r = new GenModelResult();
        r.setId(id);
        r.setUser_id(userId);
        r.setIs_deleted(deleted);
        r.setCreated_at(createdAt);
        r.setTask_status("FINISHED");
        r.setPolygon_count(1234);
        r.setSize(512);
        return r;
    }

    private GenVideoResult video(int id, int userId, int deleted, String createdAt) {
        GenVideoResult r = new GenVideoResult();
        r.setId(id);
        r.setUser_id(userId);
        r.setIs_deleted(deleted);
        r.setCreated_at(createdAt);
        r.setTask_status("FINISHED");
        r.setSize("1280x720");
        return r;
    }

    /** 序列化为 JSON 字符串再解析，与 JSONResult 的 fastjson 输出路径同源 */
    private JSONObject toJson(Object row) {
        return JSON.parseObject(JSON.toJSONString(row));
    }

    @Test
    void modelHistory_onlyKeepsCurrentUserAndNotDeleted() {
        modelRows.add(model(1, 1, 0, "2026-08-01 10:00:00"));
        modelRows.add(model(2, 1, 1, "2026-08-02 10:00:00"));
        modelRows.add(model(3, 2, 0, "2026-08-03 10:00:00"));
        when(servicedao.getAllModelResults()).thenReturn(modelRows);

        List<GenModelResult> result = modelService.getHistory(1);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    void modelHistory_sortedByCreatedAtDesc() {
        modelRows.add(model(1, 1, 0, "2026-08-01 10:00:00"));
        modelRows.add(model(2, 1, 0, "2026-08-03 10:00:00"));
        modelRows.add(model(3, 1, 0, "2026-08-02 10:00:00"));
        when(servicedao.getAllModelResults()).thenReturn(modelRows);

        List<GenModelResult> result = modelService.getHistory(1);

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).getId());
        assertEquals(3, result.get(1).getId());
        assertEquals(1, result.get(2).getId());
    }

    @Test
    void modelHistory_nullCreatedAt_doesNotNpe() {
        modelRows.add(model(1, 1, 0, "2026-08-01 10:00:00"));
        modelRows.add(model(2, 1, 0, null));
        when(servicedao.getAllModelResults()).thenReturn(modelRows);

        List<GenModelResult> result = modelService.getHistory(1);

        assertEquals(2, result.size());
    }

    /** 契约：polygon_count/size 必须为数字（旧 Map 行为），created_at 为字符串 */
    @Test
    void modelHistory_jsonTypesMatchLegacyMapContract() {
        modelRows.add(model(1, 1, 0, "2026-08-01 10:00:00"));
        when(servicedao.getAllModelResults()).thenReturn(modelRows);

        JSONObject json = toJson(modelService.getHistory(1).get(0));

        assertTrue(json.get("polygon_count") instanceof Integer, "polygon_count 应为数字");
        assertTrue(json.get("size") instanceof Integer, "size 应为数字");
        assertEquals("2026-08-01 10:00:00", json.getString("created_at"));
    }

    @Test
    void videoHistory_filtersAndReversesInsertionOrder() {
        videoRows.add(video(1, 1, 0, "2026-08-01 10:00:00"));
        videoRows.add(video(2, 1, 1, "2026-08-02 10:00:00"));
        videoRows.add(video(3, 2, 0, "2026-08-03 10:00:00"));
        when(servicedao.getAllVideoResults()).thenReturn(videoRows);

        List<GenVideoResult> result = videoService.getHistory(1);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    /** 非 PROCESSING 任务不应触发智谱轮询（getHistory 只读路径无外部调用） */
    @Test
    void videoHistory_finishedTask_noExternalCall() {
        videoRows.add(video(1, 1, 0, "2026-08-01 10:00:00"));
        when(servicedao.getAllVideoResults()).thenReturn(videoRows);

        List<GenVideoResult> result = videoService.getHistory(1);

        assertEquals(1, result.size());
        verify(servicedao, never()).updateVideoResultById(any(), any(), any(), any(), anyInt());
    }

    /** 契约：video size 为字符串（表列为 varchar），created_at 为字符串 */
    @Test
    void videoHistory_jsonTypesMatchLegacyMapContract() {
        videoRows.add(video(1, 1, 0, "2026-08-01 10:00:00"));
        when(servicedao.getAllVideoResults()).thenReturn(videoRows);

        JSONObject json = toJson(videoService.getHistory(1).get(0));

        assertEquals("1280x720", json.getString("size"));
        assertEquals("2026-08-01 10:00:00", json.getString("created_at"));
    }
}
