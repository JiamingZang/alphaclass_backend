package com.imct.alphaclass.service;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.ServiceUsage;
import com.imct.alphaclass.bean.TextToImageResult;
import com.imct.alphaclass.common.AiConstants;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.HttpClients;
import com.imct.alphaclass.utils.MapUtils;

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
@Slf4j
public class TextToImageService {

    private final ServiceDAO servicedao;
    private final OssService ossService;

    @Value("${ai.baidu.client-id}")
    private String baiduClientId;
    @Value("${ai.baidu.client-secret}")
    private String baiduClientSecret;

    /**
     * 文生图主流程：取百度 token → 调 SD-XL → base64 解码 → 上传 OSS → 落库。
     * 生成失败（超时/解码失败）时抛 {@link ServiceException}（503），并记录失败的使用记录。
     */
    public Map<String, Object> generateImage(String prompt, int userId) {
        String base64String = generateImageRequest(getBaiduAccessToken(), prompt);
        if (AiConstants.TIMEOUT_MARK.equals(base64String)) {
            recordUsage(userId, 0);
            throw new ServiceException(Constants.CODE_503, base64String);
        }
        byte[] result;
        try {
            result = Base64.getDecoder().decode(base64String.getBytes());
        } catch (Exception e) {
            recordUsage(userId, 0);
            throw new ServiceException(Constants.CODE_503, base64String);
        }
        String filename = UUID.randomUUID().toString().replaceAll("-", "");
        String objectName = AiConstants.OSS_IMAGE_DIR + filename + ".jpg";
        String url = ossService.urlOf(ossService.uploadBytes(result, objectName));
        ServiceUsage serviceUsage = recordUsage(userId, 1);
        TextToImageResult serviceResult = new TextToImageResult();
        serviceResult.setPrompt(prompt);
        serviceResult.setUrl(url);
        serviceResult.setThumbnail_url(url);
        serviceResult.setSize(result.length);
        serviceResult.setUsage_id(serviceUsage.getId());
        serviceResult.setCreated_at(MapUtils.now());
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

    /** 删除一条文生图历史（软删除，仅当前用户自己的记录） */
    public void deleteHistory(int id, int userId) {
        servicedao.deleteTextToImageResultById(id, userId);
    }

    /** 记录服务使用情况（文生图），失败时同样记录 */
    private ServiceUsage recordUsage(int userId, int isSuccessful) {
        ServiceUsage serviceUsage = new ServiceUsage();
        serviceUsage.setUser_id(userId);
        serviceUsage.setCreated_at(MapUtils.now());
        serviceUsage.setService_id(AiConstants.SERVICE_ID_TEXT_TO_IMAGE);
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

    /** 获取百度 OAuth access token（失败时返回错误信息文本，由主流程兜底为 503） */
    private String getBaiduAccessToken() {
        String url = AiConstants.BAIDU_TOKEN_URL + "?grant_type=client_credentials"
                + "&client_id=" + baiduClientId + "&client_secret=" + baiduClientSecret;
        OkHttpClient client = HttpClients.defaultClient();
        Request request = new Request.Builder().url(url).method("GET", null).build();
        try {
            Response response = client.newCall(request).execute();
            String res = response.body().string();
            return JSON.parseObject(res, new TypeReference<TokenAccessResult>() {
            }).access_token;
        } catch (IOException e) {
            log.error("获取百度 access_token 失败: {}", e.getMessage());
            return e.getMessage();
        }
    }

    /** 调用文心 SD-XL 文生图，返回图片 base64（超时返回 timeout 标记） */
    private String generateImageRequest(String accessToken, String prompt) {
        String url = AiConstants.BAIDU_SDXL_URL + "?access_token=" + accessToken;
        OkHttpClient client = HttpClients.timeoutClient(60, 120);
        // JSON 序列化而非字符串拼接：prompt 含引号/换行时不会破坏请求体
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("prompt", prompt);
        bodyJson.put("size", AiConstants.TEXT_TO_IMAGE_SIZE);
        RequestBody body = RequestBody.create(bodyJson.toJSONString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(url)
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
                return AiConstants.TIMEOUT_MARK;
            }
            return tempres;
        }
    }
}
