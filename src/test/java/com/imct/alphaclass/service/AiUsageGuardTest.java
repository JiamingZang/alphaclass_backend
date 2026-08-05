package com.imct.alphaclass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.imct.alphaclass.bean.GenVideoResult;
import com.imct.alphaclass.common.AiConstants;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.exception.ServiceException;

/**
 * AiUsageGuard 每日限额契约测试：
 * 当日任务数（仅当前用户）超过上限时抛 403，其他用户/历史记录不计入。
 */
@ExtendWith(MockitoExtension.class)
class AiUsageGuardTest {

    @Mock
    private ServiceDAO servicedao;

    @InjectMocks
    private AiUsageGuard guard;

    private GenVideoResult video(int userId, String createdAt) {
        GenVideoResult r = new GenVideoResult();
        r.setUser_id(userId);
        r.setCreated_at(createdAt);
        return r;
    }

    private String now() {
        return new Timestamp(System.currentTimeMillis()).toString();
    }

    @Test
    void atLimit_allows() {
        List<GenVideoResult> rows = new ArrayList<>();
        for (int i = 0; i < AiConstants.DAILY_GENERATION_LIMIT; i++) {
            rows.add(video(1, now()));
        }
        when(servicedao.getAllVideoResults()).thenReturn(rows);

        assertDoesNotThrow(() -> guard.checkExceedGenerationCount(1));
    }

    @Test
    void overLimit_throws403() {
        List<GenVideoResult> rows = new ArrayList<>();
        for (int i = 0; i < AiConstants.DAILY_GENERATION_LIMIT + 1; i++) {
            rows.add(video(1, now()));
        }
        when(servicedao.getAllVideoResults()).thenReturn(rows);

        ServiceException e = assertThrows(ServiceException.class, () -> guard.checkExceedGenerationCount(1));
        assertEquals("403", e.getCode());
        assertEquals(AiConstants.EXCEED_LIMIT_MESSAGE, e.getMessage());
    }

    @Test
    void otherUsersRecords_areExcluded() {
        List<GenVideoResult> rows = new ArrayList<>();
        rows.add(video(2, now()));
        rows.add(video(1, now()));
        when(servicedao.getAllVideoResults()).thenReturn(rows);

        assertDoesNotThrow(() -> guard.checkExceedGenerationCount(1));
    }

    @Test
    void yesterdayRecords_areExcluded() {
        Timestamp yesterday = new Timestamp(System.currentTimeMillis() - 24 * 3600 * 1000L);
        List<GenVideoResult> rows = new ArrayList<>();
        for (int i = 0; i < AiConstants.DAILY_GENERATION_LIMIT + 1; i++) {
            rows.add(video(1, yesterday.toString()));
        }
        when(servicedao.getAllVideoResults()).thenReturn(rows);

        assertDoesNotThrow(() -> guard.checkExceedGenerationCount(1));
    }
}
