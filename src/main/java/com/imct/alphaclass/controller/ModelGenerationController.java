package com.imct.alphaclass.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.imct.alphaclass.bean.GenModelResult;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.utils.TokenUtils;
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
 * 3D 模型生成 AI 服务：调用腾讯混元 3D 快速建模。
 */
@RestController
public class ModelGenerationController {

    @Resource
    private ServiceDAO servicedao;

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

    @RequestMapping(value = "/services/generate-model/text-to-model", method = RequestMethod.POST)
    public JSONResult textToModel(@RequestBody Map<String, Object> params) {
        if (CheckExceedGenerationCount(10)) {
            return JSONResult.failWithMsg("403", "You have exceeded generation limit per day.");
        }
        GenModelResult res = new GenModelResult();
        User user = TokenUtils.getCurrentUser();
        res.setUser_id(user.getId());
        res.setPrompt(params.get("prompt").toString());
        String resultFormat = params.containsKey("result_format") ? params.get("result_format").toString() : "GLB";
        Boolean enablePBR = params.containsKey("enable_pbr") ? (Boolean) params.get("enable_pbr") : null;
        Boolean enableGeometry = params.containsKey("enable_geometry") ? (Boolean) params.get("enable_geometry") : null;
        try {
            SubmitHunyuanTo3DRapidJobResponse result = generateModelRequest(res.getPrompt(), null, resultFormat,
                    enablePBR, enableGeometry);
            res.setJob_id(result.getJobId());
            res.setRequest_id(result.getRequestId());
            res.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
            res.setTask_status("GENERATING");
            res.setType("textToModel");
            servicedao.addModelResult(res);
            Map<String, Object> resp = new HashMap<String, Object>();
            resp.put("request_id", res.getRequest_id());
            resp.put("job_id", res.getJob_id());
            resp.put("task_status", "GENERATING");
            return JSONResult.successWithData(resp);
        } catch (TencentCloudSDKException e) {
            return JSONResult.failWithMsg("403", e.toString());
        }
    }

    @RequestMapping(value = "/services/generate-model/image-to-model", method = RequestMethod.POST)
    public JSONResult imageToModel(@RequestBody Map<String, Object> params) {
        if (CheckExceedGenerationCount(10)) {
            return JSONResult.failWithMsg("403", "You have exceeded generation limit per day.");
        }
        GenModelResult res = new GenModelResult();
        User user = TokenUtils.getCurrentUser();
        res.setUser_id(user.getId());
        res.setPrompt_image_url(params.get("image_url").toString());
        String resultFormat = params.containsKey("result_format") ? params.get("result_format").toString() : "GLB";
        Boolean enablePBR = params.containsKey("enable_pbr") ? (Boolean) params.get("enable_pbr") : null;
        Boolean enableGeometry = params.containsKey("enable_geometry") ? (Boolean) params.get("enable_geometry") : null;
        try {
            SubmitHunyuanTo3DRapidJobResponse result = generateModelRequest(null, res.getPrompt_image_url(),
                    resultFormat, enablePBR, enableGeometry);
            res.setJob_id(result.getJobId());
            res.setRequest_id(result.getRequestId());
            res.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
            res.setType("imageToModel");
            res.setTask_status("GENERATING");
            servicedao.addModelResult(res);
            Map<String, Object> resp = new HashMap<String, Object>();
            resp.put("request_id", res.getRequest_id());
            resp.put("job_id", res.getJob_id());
            resp.put("task_status", "GENERATING");
            return JSONResult.successWithData(resp);
        } catch (TencentCloudSDKException e) {
            return JSONResult.failWithMsg("403", e.toString());
        }
    }

    @RequestMapping(value = "/services/generate-model/update", method = RequestMethod.POST)
    public JSONResult updateModelResult(@RequestBody Map<String, Object> params) {
        String request_id = params.get("request_id").toString();
        String status = params.get("state").toString();
        String url = params.get("url").toString();
        String thumbnailUrl = params.get("thumbnail_url").toString();
        // TODO: 前端传参键名疑似拼写错误（pologen_count），与前端确认后再修正
        int polygon_count = Integer.valueOf(params.get("pologen_count").toString());
        int size = Integer.valueOf(params.get("size").toString());
        servicedao.updateModelResultById(status, url, thumbnailUrl, polygon_count, size, request_id);
        return JSONResult.successWithData("");
    }

    @RequestMapping(value = "/services/generate-model/history", method = RequestMethod.GET)
    public JSONResult GetModelHistoryById() {
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res = servicedao.getAllModelResults();
        List<Map<String, Object>> finalRes = new ArrayList<Map<String, Object>>();
        if (res != null) {
            for (Map<String, Object> rMap : res) {
                if (Integer.valueOf(rMap.get("user_id").toString()) == userId) {
                    if (Integer.valueOf(rMap.get("is_deleted").toString()) == 0) {
                        finalRes.add(rMap);
                    }
                }
            }
        }
        Collections.reverse(finalRes);
        return JSONResult.successWithData(finalRes);
    }

    @RequestMapping(value = "/services/generate-model/history/{id}", method = RequestMethod.DELETE)
    public void DeleteModelHistoryById(@PathVariable int id) {
        servicedao.deleteModelResultById(id);
    }

    /** 当日生成次数限制：超过 count 次则拒绝新任务 */
    private boolean CheckExceedGenerationCount(int count) {
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res = servicedao.getAllVideoResults();
        int finalCount = 0;
        for (Map<String, Object> rMap : res) {
            if (Integer.valueOf(rMap.get("user_id").toString()) == userId) {
                java.time.LocalDate time = java.sql.Timestamp
                        .valueOf(java.time.LocalDateTime.parse(rMap.get("created_at").toString()))
                        .toLocalDateTime().toLocalDate();
                java.time.LocalDate today = java.time.LocalDate.now();
                if (time.isEqual(today)) {
                    finalCount = finalCount + 1;
                }
            }
        }
        return finalCount > count;
    }

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

    /** 查询建模任务进度（供外部轮询使用，当前由 ModelTaskScheduler 定时轮询） */
    public QueryHunyuanTo3DRapidJobResponse queryModelGenerateRequest(String job_id) {
        try {
            Credential cred = new Credential(tencentSecretId, tencentSecretKey);
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("ai3d.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            Ai3dClient client = new Ai3dClient(cred, "ap-guangzhou", clientProfile);
            QueryHunyuanTo3DRapidJobRequest req = new QueryHunyuanTo3DRapidJobRequest();
            req.setJobId(job_id);
            return client.QueryHunyuanTo3DRapidJob(req);
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
        return null;
    }

    private String downloadAndUploadToOss(String sourceUrl, String objectName) throws IOException {
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

    private byte[] downloadFileBytes(String sourceUrl) throws IOException {
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

    /** 解析 GLB 文件的三角形数量 */
    private int countGlbTriangles(byte[] glbData) {
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
