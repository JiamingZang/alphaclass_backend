package com.imct.alphaclass.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import com.imct.alphaclass.bean.ServiceUsage;
import com.imct.alphaclass.bean.TextToImageResult;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.exception.ServiceException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 文生图服务：调用百度文心 SD-XL 生成图片并上传到阿里云 OSS。
 * <p>
 * 每次调用都会写入 service_usage 使用记录（成功/失败均记录），成功时再写入
 * text_to_image_result 结果表；历史查询按当前用户过滤、按创建时间倒序返回。
 */
@Service
@RequiredArgsConstructor
public class TextToImageService {

    private final ServiceDAO servicedao;

    @Value("${ai.baidu.client-id}")
    private String baiduClientId;
    @Value("${ai.baidu.client-secret}")
    private String baiduClientSecret;
    @Value("${ai.oss.endpoint}")
    private String ossEndpoint;
    @Value("${ai.oss.bucket}")
    private String ossBucket;
    @Value("${ai.oss.access-key-id}")
    private String ossAccessKeyId;
    @Value("${ai.oss.access-key-secret}")
    private String ossAccessKeySecret;

    /**
     * 文生图主流程：取百度 token → 调 SD-XL → base64 解码 → 上传 OSS → 落库。
     * 生成失败（超时/解码失败）时抛 {@link ServiceException}（503），并记录失败的使用记录。
     */
    public Map<String, Object> generateImage(String prompt, int userId) throws com.aliyuncs.exceptions.ClientException {
        String base64String = generateImageRequest(getBaiduAccessToken(), prompt);
        if (base64String.equals("timeout")) {
            recordUsage(userId, 0);
            throw new ServiceException("503", base64String);
        }
        byte[] result;
        try {
            result = Base64.getDecoder().decode(base64String.getBytes());
        } catch (Exception e) {
            recordUsage(userId, 0);
            throw new ServiceException("503", base64String);
        }
        UUID randomUUID = UUID.randomUUID();
        String filename = randomUUID.toString().replaceAll("-", "");
        String url = uploadBytesToOss(result, filename);
        ServiceUsage serviceUsage = recordUsage(userId, 1);
        TextToImageResult serviceResult = new TextToImageResult();
        serviceResult.setPrompt(prompt);
        serviceResult.setUrl(url);
        serviceResult.setThumbnail_url(url);
        serviceResult.setSize(result.length);
        serviceResult.setUsage_id(serviceUsage.getId());
        serviceResult.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        serviceResult.setIs_deleted(0);
        servicedao.addResult(serviceResult);

        Map<String, Object> res = new HashMap<String, Object>();
        res.put("id", serviceResult.getId());
        res.put("prompt", serviceResult.getPrompt());
        res.put("url", serviceResult.getUrl());
        res.put("size", serviceResult.getSize());
        res.put("created_at", serviceResult.getCreated_at());
        return res;
    }

    /** 当前用户的文生图历史（未删除，按创建时间倒序；用户归属与过滤在 SQL join 中完成） */
    public List<Map<String, Object>> getHistory(int userId) {
        return servicedao.getHistoryByUserId(userId);
    }

    /** 删除一条文生图历史（软删除） */
    public void deleteHistory(int id) {
        servicedao.deleteTextToImageResultById(id);
    }

    /** 记录服务使用情况（service_id=1 文生图），失败时同样记录 */
    private ServiceUsage recordUsage(int userId, int isSuccessful) {
        ServiceUsage serviceUsage = new ServiceUsage();
        serviceUsage.setUser_id(userId);
        serviceUsage.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        serviceUsage.setService_id(1);
        serviceUsage.setIs_successful(isSuccessful);
        servicedao.addUsage(serviceUsage);
        return serviceUsage;
    }

    /** 百度 OAuth token 响应结构 */
    public static class TokenAccessResult {
        public String refresh_token;
        public int expires_in;
        public String session_key;
        public String access_token;
        public String scope;
        public String session_secret;
    }

    /** 文心 SD-XL 响应结构 */
    public static class GenImageResult {
        public String id;
        public String object;
        public int created;
        public List<ImageData> data;
        public Usage usage;
    }

    public static class ImageData {
        public String object;
        public String b64_image;
        public int index;
    }

    public static class Usage {
        public int prompt_tokens;
        public int total_tokens;
    }

    /** 获取百度 OAuth access token */
    private String getBaiduAccessToken() {
        String URL = "https://aip.baidubce.com/oauth/2.0/token" + "?grant_type=client_credentials"
                + "&client_id=" + baiduClientId + "&client_secret=" + baiduClientSecret;
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder().url(URL).method("GET", null).build();
        try {
            Response response = client.newCall(request).execute();
            String res = response.body().string();
            return JSON.parseObject(res, new TypeReference<TokenAccessResult>() {
            }).access_token;
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /** 调用文心 SD-XL 文生图，返回图片 base64（超时返回 "timeout"） */
    private String generateImageRequest(String accessToken, String prompt) {
        String URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/text2image/sd_xl?access_token="
                + accessToken;
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        String json = "{\"prompt\":\"" + prompt + "\",\"size\":\"1024x576\"}";
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(URL)
                .header("Content-Type", "application/json")
                .method("POST", body)
                .build();

        String tempres = "";
        try {
            Response response = client.newCall(request).execute();
            String res = response.body().string();
            tempres = res;
            GenImageResult parsed = JSON.parseObject(res, new TypeReference<GenImageResult>() {
            });
            if (parsed.data != null) {
                return parsed.data.get(0).b64_image;
            }
            return tempres;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().equals("timeout")) {
                return "timeout";
            }
            return tempres;
        }
    }

    /** 上传图片字节流到 OSS，返回可访问 URL */
    private String uploadBytesToOss(byte[] bytes, String filename) throws com.aliyuncs.exceptions.ClientException {
        String objectName = "assets/aigc_images/" + filename + ".jpg";
        OSS ossClient = new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossBucket, objectName,
                    new ByteArrayInputStream(bytes));
            ossClient.putObject(putObjectRequest);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return "https://" + ossBucket + "." + ossEndpoint + "/" + objectName;
    }
}
