package com.imct.alphaclass.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import com.imct.alphaclass.bean.ServiceUsage;
import com.imct.alphaclass.bean.TextToImageResult;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.dao.ServiceDAO;
import com.imct.alphaclass.utils.TokenUtils;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 文生图 AI 服务：调用百度文心 SD-XL 接口并上传结果到 OSS。
 */
@RestController
public class TextToImageController {

    @Resource
    private ServiceDAO servicedao;

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

    @RequestMapping(value = "/services/text-to-image/generate-image", method = RequestMethod.POST)
    public JSONResult generateImage(@RequestBody Map<String, Object> params) throws com.aliyuncs.exceptions.ClientException {
        String base64String = generateImageRequest(getBaiduAccessToken(), params.get("prompt").toString());
        if (base64String.equals("timeout")) {
            User user = TokenUtils.getCurrentUser();
            ServiceUsage serviceUsage = new ServiceUsage();
            serviceUsage.setUser_id(user.getId());
            serviceUsage.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
            serviceUsage.setService_id(1);
            serviceUsage.setIs_successful(0);
            servicedao.addUsage(serviceUsage);
            return JSONResult.failWithMsg("503", base64String);
        }
        byte[] result;
        try {
            result = Base64.getDecoder().decode(base64String.getBytes());
        } catch (Exception e) {
            User user = TokenUtils.getCurrentUser();
            ServiceUsage serviceUsage = new ServiceUsage();
            serviceUsage.setUser_id(user.getId());
            serviceUsage.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
            serviceUsage.setService_id(1);
            serviceUsage.setIs_successful(0);
            servicedao.addUsage(serviceUsage);
            return JSONResult.failWithMsg("503", base64String);
        }
        UUID randomUUID = UUID.randomUUID();
        String filename = randomUUID.toString().replaceAll("-", "");
        String url = uploadBytesToOss(result, filename);
        User user = TokenUtils.getCurrentUser();
        ServiceUsage serviceUsage = new ServiceUsage();
        serviceUsage.setUser_id(user.getId());
        serviceUsage.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        serviceUsage.setService_id(1);
        serviceUsage.setIs_successful(1);
        servicedao.addUsage(serviceUsage);
        TextToImageResult serviceResult = new TextToImageResult();
        serviceResult.setPrompt(params.get("prompt").toString());
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
        return JSONResult.successWithData(res);
    }

    @RequestMapping(value = "/services/text-to-image/history", method = RequestMethod.GET)
    public JSONResult GetHistoryById() {
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res = servicedao.getAllResults();
        List<Map<String, Object>> finalRes = new ArrayList<Map<String, Object>>();
        if (res != null) {
            for (Map<String, Object> rMap : res) {
                int usageid = Integer.valueOf(rMap.get("usage_id").toString());
                Map<String, Object> usage = servicedao.getUsageById(usageid);
                if (usage != null && Integer.valueOf(usage.get("user_id").toString()) == userId) {
                    if (Integer.valueOf(rMap.get("is_deleted").toString()) == 0) {
                        finalRes.add(rMap);
                    }
                }
            }
        }
        Collections.reverse(finalRes);
        return JSONResult.successWithData(finalRes);
    }

    @RequestMapping(value = "/services/text-to-image/history/{id}", method = RequestMethod.DELETE)
    public void DeleteHistoryById(@PathVariable int id) {
        servicedao.deleteTextToImageResultById(id);
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

    private String generateImageRequest(String accessToken, String prompt) {
        String URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/text2image/sd_xl?access_token="
                + accessToken;
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        String json = "{\"prompt\":\"" + prompt + "\",\"size\":\"1024x576\"}";
        okhttp3.RequestBody body = okhttp3.RequestBody.create(json, MediaType.get("application/json"));
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
