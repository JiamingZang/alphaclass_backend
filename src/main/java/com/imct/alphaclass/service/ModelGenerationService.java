package com.imct.alphaclass.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.imct.alphaclass.bean.GenModelResult;
import com.imct.alphaclass.common.AiConstants;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.HttpClients;
import com.imct.alphaclass.utils.MapUtils;
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
@Slf4j
public class ModelGenerationService {

    private final ServiceDAO servicedao;
    private final AiUsageGuard usageGuard;
    private final OssService ossService;

    @Value("${ai.tencent.secret-id}")
    private String tencentSecretId;
    @Value("${ai.tencent.secret-key}")
    private String tencentSecretKey;

    /** 文生 3D 模型：提交腾讯云任务并落库，返回任务信息 */
    public Map<String, Object> textToModel(Map<String, Object> params, int userId) {
        usageGuard.checkExceedGenerationCount(userId);
        GenModelResult res = new GenModelResult();
        res.setUser_id(userId);
        res.setPrompt(params.get("prompt").toString());
        String resultFormat = params.containsKey("result_format") ? params.get("result_format").toString()
                : AiConstants.RESULT_FORMAT_GLB;
        Boolean enablePBR = params.containsKey("enable_pbr") ? (Boolean) params.get("enable_pbr") : null;
        Boolean enableGeometry = params.containsKey("enable_geometry") ? (Boolean) params.get("enable_geometry") : null;
        try {
            SubmitHunyuanTo3DRapidJobResponse result = generateModelRequest(res.getPrompt(), null, resultFormat,
                    enablePBR, enableGeometry);
            return saveModelTask(res, result, AiConstants.TYPE_TEXT_TO_MODEL);
        } catch (TencentCloudSDKException e) {
            throw new ServiceException(Constants.CODE_403, e.toString());
        }
    }

    /** 图生 3D 模型：提交腾讯云任务并落库，返回任务信息 */
    public Map<String, Object> imageToModel(Map<String, Object> params, int userId) {
        usageGuard.checkExceedGenerationCount(userId);
        GenModelResult res = new GenModelResult();
        res.setUser_id(userId);
        res.setPrompt_image_url(params.get("image_url").toString());
        String resultFormat = params.containsKey("result_format") ? params.get("result_format").toString()
                : AiConstants.RESULT_FORMAT_GLB;
        Boolean enablePBR = params.containsKey("enable_pbr") ? (Boolean) params.get("enable_pbr") : null;
        Boolean enableGeometry = params.containsKey("enable_geometry") ? (Boolean) params.get("enable_geometry") : null;
        try {
            SubmitHunyuanTo3DRapidJobResponse result = generateModelRequest(null, res.getPrompt_image_url(),
                    resultFormat, enablePBR, enableGeometry);
            return saveModelTask(res, result, AiConstants.TYPE_IMAGE_TO_MODEL);
        } catch (TencentCloudSDKException e) {
            throw new ServiceException(Constants.CODE_403, e.toString());
        }
    }

    /** 保存提交结果：落库并返回 request_id/job_id/task_status */
    private Map<String, Object> saveModelTask(GenModelResult res, SubmitHunyuanTo3DRapidJobResponse result,
            String type) {
        res.setJob_id(result.getJobId());
        res.setRequest_id(result.getRequestId());
        res.setCreated_at(MapUtils.now());
        res.setTask_status(AiConstants.TASK_GENERATING);
        res.setType(type);
        servicedao.addModelResult(res);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("request_id", res.getRequest_id());
        resp.put("job_id", res.getJob_id());
        resp.put("task_status", AiConstants.TASK_GENERATING);
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

    /** 提交混元 3D 快速建模任务（prompt 与 imageUrl 二选一） */
    private SubmitHunyuanTo3DRapidJobResponse generateModelRequest(String prompt, String imageUrl,
            String resultFormat, Boolean enablePBR, Boolean enableGeometry) throws TencentCloudSDKException {
        Ai3dClient client = buildAi3dClient();
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
        QueryHunyuanTo3DRapidJobRequest req = new QueryHunyuanTo3DRapidJobRequest();
        req.setJobId(jobId);
        return buildAi3dClient().QueryHunyuanTo3DRapidJob(req);
    }

    /** 构建腾讯云 AI3D 客户端（endpoint/region 常量统一） */
    private Ai3dClient buildAi3dClient() {
        Credential cred = new Credential(tencentSecretId, tencentSecretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(AiConstants.TENCENT_AI3D_ENDPOINT);
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new Ai3dClient(cred, AiConstants.TENCENT_AI3D_REGION, clientProfile);
    }

    /** 下载远程文件并上传到 OSS，返回 objectName */
    public String downloadAndUploadToOss(String sourceUrl, String objectName) throws IOException {
        return ossService.uploadBytes(downloadFileBytes(sourceUrl), objectName);
    }

    /** 下载远程文件字节流 */
    public byte[] downloadFileBytes(String sourceUrl) throws IOException {
        OkHttpClient client = HttpClients.timeoutClient(60, 60);
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
            log.error("GLB 三角面数解析失败: {}", e.getMessage());
            return 0;
        }
    }
}
