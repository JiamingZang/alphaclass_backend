package com.imct.alphaclass.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.imct.alphaclass.common.AiConstants;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

/**
 * AI 生成每日限额校验：模型生成与视频生成共用同一统计口径
 * （统计 video_generate_result 表中当前用户当日任务数），超过上限抛 403。
 */
@Service
@RequiredArgsConstructor
public class AiUsageGuard {

    private final ServiceDAO servicedao;

    /** 当日生成次数限制：超过上限则拒绝新任务 */
    public void checkExceedGenerationCount(int userId) {
        long finalCount = servicedao.getAllVideoResults().stream()
                .filter(r -> r.getUser_id() == userId)
                .filter(r -> {
                    LocalDateTime createdAt = MapUtils.parseDateTime(r.getCreated_at());
                    return createdAt != null && createdAt.toLocalDate().isEqual(LocalDate.now());
                })
                .count();
        if (finalCount > AiConstants.DAILY_GENERATION_LIMIT) {
            throw new ServiceException(Constants.CODE_403, AiConstants.EXCEED_LIMIT_MESSAGE);
        }
    }
}
