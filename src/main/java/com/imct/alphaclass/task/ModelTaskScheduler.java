package com.imct.alphaclass.task;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.imct.alphaclass.dao.ServiceDAO;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ai3d.v20250513.Ai3dClient;
import com.tencentcloudapi.ai3d.v20250513.models.QueryHunyuanTo3DRapidJobRequest;
import com.tencentcloudapi.ai3d.v20250513.models.QueryHunyuanTo3DRapidJobResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class ModelTaskScheduler {

    @Resource
    private ServiceDAO servicedao;

    private static String secretId = "REPLACED_TENCENT_SECRET_ID";
    private static String secretKey = "REPLACED_TENCENT_SECRET_KEY";

    @Scheduled(fixedDelay = 30000)
    public void pollModelTasks() {
        List<Map<String, Object>> results = servicedao.getAllModelResults();
        for (Map<String, Object> rMap : results) {
            String status = rMap.get("task_status").toString();
            if (status.equals("GENERATING")) {
                processGeneratingTask(rMap);
            }
        }
    }

    private void processGeneratingTask(Map<String, Object> rMap) {
        try {
            QueryHunyuanTo3DRapidJobResponse queryResponse = queryModelGenerateRequest(rMap.get("job_id").toString());
            if (queryResponse.getStatus().equals("FAIL")) {
                servicedao.updateModelResultById("FAILED", "", "", 0, 0, rMap.get("request_id").toString());
            } else if (queryResponse.getStatus().equals("DONE")) {
                String tencentUrl = "";
                String tencentPreviewUrl = "";
                if (queryResponse.getResultFile3Ds() != null && queryResponse.getResultFile3Ds().length > 0) {
                    tencentUrl = queryResponse.getResultFile3Ds()[0].getUrl();
                    tencentPreviewUrl = queryResponse.getResultFile3Ds()[0].getPreviewImageUrl();
                }
                String ossUrl = "";
                String ossThumbnailUrl = "";
                String jobId = rMap.get("job_id").toString();
                int polygonCount = 0;
                try {
                    if (tencentUrl.length() > 0) {
                        byte[] glbData = downloadFileBytes(tencentUrl);
                        ossUrl = "assets/aigc_models/models/" + jobId + ".glb";
                        String endpoint = "oss-cn-beijing.aliyuncs.com";
                        String bucketName = "alphaclass";
                        OSS ossClient = new OSSClientBuilder().build(endpoint, "REPLACED_ALIYUN_ACCESS_KEY_ID", "REPLACED_ALIYUN_ACCESS_KEY_SECRET");
                        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, ossUrl, new ByteArrayInputStream(glbData));
                        ossClient.putObject(putObjectRequest);
                        ossClient.shutdown();
                        polygonCount = countGlbTriangles(glbData);
                    }
                    if (tencentPreviewUrl.length() > 0) {
                        ossThumbnailUrl = downloadAndUploadToOss(tencentPreviewUrl, "assets/aigc_models/thumbnails/" + jobId + ".png");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                servicedao.updateModelResultById("FINISHED", ossUrl, ossThumbnailUrl, polygonCount, 0, rMap.get("request_id").toString());
            }
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
        }
    }

    private QueryHunyuanTo3DRapidJobResponse queryModelGenerateRequest(String jobId) throws TencentCloudSDKException {
        Credential cred = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ai3d.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        Ai3dClient client = new Ai3dClient(cred, "ap-guangzhou", clientProfile);
        QueryHunyuanTo3DRapidJobRequest req = new QueryHunyuanTo3DRapidJobRequest();
        req.setJobId(jobId);
        QueryHunyuanTo3DRapidJobResponse resp = client.QueryHunyuanTo3DRapidJob(req);
        return resp;
    }

    private String downloadAndUploadToOss(String sourceUrl, String objectName) throws IOException {
        String endpoint = "oss-cn-beijing.aliyuncs.com";
        String bucketName = "alphaclass";
        OSS ossClient = new OSSClientBuilder().build(endpoint, "REPLACED_ALIYUN_ACCESS_KEY_ID", "REPLACED_ALIYUN_ACCESS_KEY_SECRET");
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
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream(data));
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
