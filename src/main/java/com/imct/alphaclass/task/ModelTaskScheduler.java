package com.imct.alphaclass.task;

import java.io.IOException;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.imct.alphaclass.bean.GenModelResult;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.service.ModelGenerationService;
import com.tencentcloudapi.ai3d.v20250513.models.QueryHunyuanTo3DRapidJobResponse;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

/**
 * 模型生成任务定时轮询：每 30 秒查询腾讯云上 GENERATING 状态的任务，
 * 完成（DONE）则下载 GLB/预览图到 OSS 并更新落库；失败（FAIL）则标记 FAILED。
 * <p>
 * 腾讯云查询与 OSS 传输逻辑复用 {@link ModelGenerationService}，本类只负责调度与状态流转。
 */
@Component
@RequiredArgsConstructor
public class ModelTaskScheduler {

    private final ServiceDAO servicedao;
    private final ModelGenerationService modelService;

    @Scheduled(fixedDelay = 30000)
    public void pollModelTasks() {
        List<GenModelResult> results = servicedao.getAllModelResults();
        for (GenModelResult r : results) {
            if (r.getTask_status().equals("GENERATING")) {
                processGeneratingTask(r);
            }
        }
    }

    private void processGeneratingTask(GenModelResult r) {
        try {
            QueryHunyuanTo3DRapidJobResponse queryResponse = modelService
                    .queryModelGenerateRequest(r.getJob_id());
            if (queryResponse.getStatus().equals("FAIL")) {
                servicedao.updateModelResultById("FAILED", "", "", 0, 0, r.getRequest_id());
            } else if (queryResponse.getStatus().equals("DONE")) {
                String tencentUrl = "";
                String tencentPreviewUrl = "";
                if (queryResponse.getResultFile3Ds() != null && queryResponse.getResultFile3Ds().length > 0) {
                    tencentUrl = queryResponse.getResultFile3Ds()[0].getUrl();
                    tencentPreviewUrl = queryResponse.getResultFile3Ds()[0].getPreviewImageUrl();
                }
                String ossUrl = "";
                String ossThumbnailUrl = "";
                String jobId = r.getJob_id();
                int polygonCount = 0;
                try {
                    if (tencentUrl.length() > 0) {
                        byte[] glbData = modelService.downloadFileBytes(tencentUrl);
                        ossUrl = modelService.downloadAndUploadToOss(tencentUrl, "assets/aigc_models/models/" + jobId + ".glb");
                        polygonCount = modelService.countGlbTriangles(glbData);
                    }
                    if (tencentPreviewUrl.length() > 0) {
                        ossThumbnailUrl = modelService.downloadAndUploadToOss(tencentPreviewUrl,
                                "assets/aigc_models/thumbnails/" + jobId + ".png");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                servicedao.updateModelResultById("FINISHED", ossUrl, ossThumbnailUrl, polygonCount, 0,
                        r.getRequest_id());
            }
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
        }
    }
}
