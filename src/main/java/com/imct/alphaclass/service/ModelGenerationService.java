package com.imct.alphaclass.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.imct.alphaclass.bean.GenModelResult;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ai3d.v20250513.Ai3dClient;
import com.tencentcloudapi.ai3d.v20250513.models.QueryHunyuanTo3DRapidJobRequest;
import com.tencentcloudapi.ai3d.v20250513.models.QueryHunyuanTo3DRapidJobResponse;
import com.tencentcloudapi.ai3d.v20250513.models.SubmitHunyuanTo3DRapidJobRequest;
import com.tencentcloudapi.ai3d.v20250513.models.SubmitHunyuanTo3DRapidJobResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 3D 模型生成服务：调用腾讯混元 3D 快速建模。
 * <p>
 * 提交任务（文生模型/图生模型）后由 {@code ModelTaskScheduler} 定时轮询 {@link #queryModelGenerateRequest}，
 * 任务完成时下载 GLB 到 OSS 并统计三角面数。本类同时提供下载/GLB 解析工具方法供定时任务复用。
 */
@Service
@RequiredArgsConstructor
public class ModelGenerationService {

    private final ServiceDAO servicedao;

    @Value("${ai.tencent.secret-id}")
    private String tencentSecretId;
    @Value("${ai.tencent.secret-key}")
    private String tencentSecretKey;
    @Value("${ai.oss.endpoint}")
    private String ossEndpoint;
    @Value("${ai.oss.bucket}")
    private String ossBucket;
    @Value("${ai.oss.access-key-id}")
    private String ossAccessKeyId;
    @Value("${ai.oss.access-key-secret}")
    private String ossAccessKeySecret;

    /** 当日生成次数上限 */
    private static final int DAILY_GENERATION_LIMIT = 10;

    /** 文生 3D 模型：提交腾讯云任务并落库，返回任务信息 */
    public Map<String, Object> textToModel(Map<String, Object> params, int userId) {
        checkExceedGenerationCount(userId);
        GenModelResult res = new GenModelResult();
        res.setUser_id(userId);
        res.setPrompt(params.get("prompt").toString());
        String resultFormat = params.containsKey("result_format") ? params.get("result_format").toString() : "GLB";
        Boolean enablePBR = params.containsKey("enable_pbr") ? (Boolean) params.get("enable_pbr") : null;
        Boolean enableGeometry = params.containsKey("enable_geometry") ? (Boolean) params.get("enable_geometry") : null;
        try {
            SubmitHunyuanTo3DRapidJobResponse result = generateModelRequest(res.getPrompt(), null, resultFormat,
                    enablePBR, enableGeometry);
            return saveModelTask(res, result, "textToModel");
        } catch (TencentCloudSDKException e) {
            throw new ServiceException("403", e.toString());
        }
    }

    /** 图生 3D 模型：提交腾讯云任务并落库，返回任务信息 */
    public Map<String, Object> imageToModel(Map<String, Object> params, int userId) {
        checkExceedGenerationCount(userId);
        GenModelResult res = new GenModelResult();
        res.setUser_id(userId);
        res.setPrompt_image_url(params.get("image_url").toString());
        String resultFormat = params.containsKey("result_format") ? params.get("result_format").toString() : "GLB";
        Boolean enablePBR = params.containsKey("enable_pbr") ? (Boolean) params.get("enable_pbr") : null;
        Boolean enableGeometry = params.containsKey("enable_geometry") ? (Boolean) params.get("enable_geometry") : null;
        try {
            SubmitHunyuanTo3DRapidJobResponse result = generateModelRequest(null, res.getPrompt_image_url(),
                    resultFormat, enablePBR, enableGeometry);
            return saveModelTask(res, result, "imageToModel");
        } catch (TencentCloudSDKException e) {
            throw new ServiceException("403", e.toString());
        }
    }

    /** 保存提交结果：落库并返回 request_id/job_id/task_status */
    private Map<String, Object> saveModelTask(GenModelResult res, SubmitHunyuanTo3DRapidJobResponse result,
            String type) {
        res.setJob_id(result.getJobId());
        res.setRequest_id(result.getRequestId());
        res.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        res.setTask_status("GENERATING");
        res.setType(type);
        servicedao.addModelResult(res);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("request_id", res.getRequest_id());
        resp.put("job_id", res.getJob_id());
        resp.put("task_status", "GENERATING");
        return resp;
    }

    /** 前端回调更新任务结果（任务状态/产物地址/面数/大小） */
    public void updateModelResult(Map<String, Object> params) {
        String requestId = params.get("request_id").toString();
        String status = params.get("state").toString();
        String url = params.get("url").toString();
        String thumbnailUrl = params.get("thumbnail_url").toString();
        // TODO: 前端传参键名疑似拼写错误（pologen_count），与前端确认后再修正
        int polygonCount = Integer.valueOf(params.get("pologen_count").toString());
        int size = Integer.valueOf(params.get("size").toString());
        servicedao.updateModelResultById(status, url, thumbnailUrl, polygonCount, size, requestId);
    }

    /** 当前用户的模型生成历史（未删除，按创建时间倒序；created_at 为空时垫底，避免排序 NPE） */
    public List<GenModelResult> getHistory(int userId) {
        return servicedao.getAllModelResults().stream()
                .filter(r -> r.getUser_id() == userId && r.getIs_deleted() == 0)
                .sorted(Comparator.comparing(GenModelResult::getCreated_at,
                        Comparator.nullsLast(String::compareTo)).reversed())
                .collect(Collectors.toList());
    }

    /** 删除一条模型生成历史（软删除） */
    public void deleteHistory(int id) {
        servicedao.deleteModelResultById(id);
    }

    /** 当日生成次数限制：超过上限则拒绝新任务 */
    private void checkExceedGenerationCount(int userId) {
        // 注意：与视频生成共用今日次数统计（统计 video_generate_result 表），如需独立限额请调整
        long finalCount = servicedao.getAllVideoResults().stream()
                .filter(r -> r.getUser_id() == userId)
                .filter(r -> Timestamp.valueOf(LocalDateTime.parse(r.getCreated_at()))
                        .toLocalDateTime().toLocalDate().isEqual(LocalDate.now()))
                .count();
        if (finalCount > DAILY_GENERATION_LIMIT) {
            throw new ServiceException("403", "You have exceeded generation limit per day.");
        }
    }

    /** 提交混元 3D 快速建模任务（prompt 与 imageUrl 二选一） */
    private SubmitHunyuanTo3DRapidJobResponse generateModelRequest(String prompt, String imageUrl,
            String resultFormat, Boolean enablePBR, Boolean enableGeometry) throws TencentCloudSDKException {
        Credential cred = new Credential(tencentSecretId, tencentSecretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ai3d.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        Ai3dClient client = new Ai3dClient(cred, "ap-guangzhou", clientProfile);
        SubmitHunyuanTo3DRapidJobRequest req = new SubmitHunyuanTo3DRapidJobRequest();
        if (prompt != null && prompt.length() > 0) {
            req.setPrompt(prompt);
        } else if (imageUrl != null && imageUrl.length() > 0) {
            req.setImageUrl(imageUrl);
        }
        if (resultFormat != null && !resultFormat.isEmpty()) {
            req.setResultFormat(resultFormat);
        }
        if (enablePBR != null) {
            req.setEnablePBR(enablePBR);
        }
        if (enableGeometry != null) {
            req.setEnableGeometry(enableGeometry);
        }
        return client.SubmitHunyuanTo3DRapidJob(req);
    }

    /** 查询建模任务进度（供 ModelTaskScheduler 定时轮询） */
    public QueryHunyuanTo3DRapidJobResponse queryModelGenerateRequest(String jobId) throws TencentCloudSDKException {
        Credential cred = new Credential(tencentSecretId, tencentSecretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ai3d.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        Ai3dClient client = new Ai3dClient(cred, "ap-guangzhou", clientProfile);
        QueryHunyuanTo3DRapidJobRequest req = new QueryHunyuanTo3DRapidJobRequest();
        req.setJobId(jobId);
        return client.QueryHunyuanTo3DRapidJob(req);
    }

    /** 下载远程文件并上传到 OSS，返回 objectName */
    public String downloadAndUploadToOss(String sourceUrl, String objectName) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(sourceUrl).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download: " + response.code());
            }
            byte[] data = response.body().bytes();
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossBucket, objectName,
                    new ByteArrayInputStream(data));
            ossClient.putObject(putObjectRequest);
        } finally {
            ossClient.shutdown();
        }
        return objectName;
    }

    /** 下载远程文件字节流 */
    public byte[] downloadFileBytes(String sourceUrl) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(sourceUrl).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download: " + response.code());
            }
            return response.body().bytes();
        }
    }

    /** 解析 GLB 文件的三角形数量（遍历 meshes/indices accessor 累加） */
    public int countGlbTriangles(byte[] glbData) {
        try {
            if (glbData.length < 20) return 0;
            int jsonChunkLength = ByteBuffer.wrap(glbData, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int jsonStart = 20;
            String jsonChunk = new String(glbData, jsonStart, jsonChunkLength, StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(jsonChunk);
            JSONArray accessors = json.getJSONArray("accessors");
            if (accessors == null) return 0;
            int totalTriangles = 0;
            JSONArray meshes = json.getJSONArray("meshes");
            if (meshes == null) return 0;
            for (int i = 0; i < meshes.size(); i++) {
                JSONObject mesh = meshes.getJSONObject(i);
                JSONArray primitives = mesh.getJSONArray("primitives");
                if (primitives == null) continue;
                for (int j = 0; j < primitives.size(); j++) {
                    JSONObject primitive = primitives.getJSONObject(j);
                    if (primitive.containsKey("indices")) {
                        int indicesAccessorIndex = primitive.getIntValue("indices");
                        JSONObject accessor = accessors.getJSONObject(indicesAccessorIndex);
                        int count = accessor.getIntValue("count");
                        totalTriangles += count / 3;
                    }
                }
            }
            return totalTriangles;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
