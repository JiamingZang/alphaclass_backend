package com.imct.alphaclass.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
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

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.videos.VideoCreateParams;
import ai.z.openapi.service.videos.VideoObject;
import ai.z.openapi.service.videos.VideoResult;
import ai.z.openapi.service.videos.VideosResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RestController
public class ServiceController {

    @Resource
    private ServiceDAO servicedao;

    private static String client_id = "REPLACED_BAIDU_CLIENT_ID";
    private static String client_secret = "REPLACED_BAIDU_CLIENT_SECRET";

    public class ServiceUsage {
        public int id;
        public int user_id;
        public int service_id;
        public String created_at;
        public int is_successful;
        
    }

    public class ServiceResult{
        public int id;
        public int usage_id;
        public String prompt;
        public String url;
        public String thumbnail_url;
        public int size;
        public String created_at;
        public int is_deleted;
    }

    @RequestMapping(value = "/services/text-to-image/generate-image", method = RequestMethod.POST)
    public JSONResult generateImage(@RequestBody Map<String, Object> params) throws com.aliyuncs.exceptions.ClientException{
        String token = getBaiduAccessToken();
        String base64String = "";
        base64String = generateImageRequest(token, params.get("prompt").toString());
        if (base64String.equals("timeout")) {
            System.out.println("[TextToImage] 百度API超时, prompt: " + params.get("prompt"));
            User user = TokenUtils.getCurrentUser();
            ServiceUsage serviceUsage = new ServiceUsage();
            serviceUsage.user_id = user.getId();
            serviceUsage.created_at  = new Timestamp(System.currentTimeMillis()).toString();
            serviceUsage.service_id = 1;
            serviceUsage.is_successful = 0;
            servicedao.addUsage(serviceUsage); 
            return JSONResult.failWithMsg("503", base64String);
        }
        byte[] result = new byte[0];
        try{
            result =  Base64.getDecoder().decode(base64String.getBytes());
        }catch (Exception e){
            System.out.println("[TextToImage] 百度API返回异常, prompt: " + params.get("prompt") + ", response: " + base64String);
            User user = TokenUtils.getCurrentUser();
            ServiceUsage serviceUsage = new ServiceUsage();
            serviceUsage.user_id = user.getId();
            serviceUsage.created_at  = new Timestamp(System.currentTimeMillis()).toString();
            serviceUsage.service_id = 1;
            serviceUsage.is_successful = 0;
            servicedao.addUsage(serviceUsage); 
            return JSONResult.failWithMsg("503", base64String);
        }
        UUID randomUUID = UUID.randomUUID();
        String filename =  randomUUID.toString().replaceAll("-", "");
        String url = uploadBytesToOss(result, filename);
        User user = TokenUtils.getCurrentUser();
        ServiceUsage serviceUsage = new ServiceUsage();
        serviceUsage.user_id = user.getId();
        serviceUsage.created_at  = new Timestamp(System.currentTimeMillis()).toString();
        serviceUsage.service_id = 1;
        serviceUsage.is_successful = 1;
        servicedao.addUsage(serviceUsage); 
        ServiceResult serviceResult = new ServiceResult();
        serviceResult.prompt = params.get("prompt").toString();
        serviceResult.url = url;serviceResult.thumbnail_url = url;
        serviceResult.size = result.length;
        serviceResult.usage_id = serviceUsage.id;
        serviceResult.created_at = new Timestamp(System.currentTimeMillis()).toString();
        serviceResult.is_deleted = 0;
        servicedao.addResult(serviceResult);
        
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("id",serviceResult.id);
        res.put("prompt",serviceResult.prompt);
        res.put("url",serviceResult.url);
        res.put("size",serviceResult.size);
        res.put("created_at",serviceResult.created_at);
        return JSONResult.successWithData(res);
        
    }

    @RequestMapping(value = "/services/text-to-image/history", method = RequestMethod.GET)
    public JSONResult GetHistoryById(){
        
        
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res  = servicedao.getAllResults();
        List<Map<String, Object>> finalRes  = new ArrayList<Map<String, Object>>();
        if (res != null) {
            for (Map<String,Object> rMap : res) {
                int usageid = Integer.valueOf(rMap.get("usage_id").toString());
                Map<String, Object> usage = servicedao.getUsageById(usageid);
                if (usage != null && Integer.valueOf(usage.get("user_id").toString()) == userId) {
                    if (Integer.valueOf(rMap.get("is_deleted").toString()) == 0) {
                        finalRes.add(rMap);
                    }
                }
            };
        }
        Collections.reverse(finalRes);
        return JSONResult.successWithData(finalRes);
        
    }

    @RequestMapping(value = "/services/text-to-image/history/{id}", method = RequestMethod.DELETE)
    public void DeleteHistoryById(@PathVariable int id){
        servicedao.deleteTextToImageResultById(id);
                
    }

    public class GenVideoResult {
        public int id;
        public int user_id;
        public String request_id;
        public String type;
        public String prompt;
        public String url;
        public String thumbnail_url;
        public String size;//视频的分辨率
        public String created_at;
        public int is_deleted;
        public String task_status;
    }

    public class GenModelResult {
        public int id;
        public int user_id;
        public String job_id;
        public String request_id;
        public String type;
        public String prompt;
        public String prompt_image_url;
        public String url;
        public String thumbnail_url;
        public String polygon_count;
        public String size;
        public String created_at;
        public int is_deleted;
        public String task_status;
    }

    public enum GenModelStatus{
        GENERATING,
        PROCESSING_FORMAT,
        FINISHED,
        FAILED
    }

    static String ZHIPU_API_SECRET_KEY = "REPLACED_ZHIPU_API_KEY";
    private static final ZhipuAiClient zhipu_client = ZhipuAiClient.builder().apiKey("REPLACED_ZHIPU_API_KEY").build();

    @RequestMapping(value = "/services/generate-video/text-to-video", method = RequestMethod.POST)
    public JSONResult textToVideo(@RequestBody Map<String, Object> params){
        // System.out.println(CheckExceedGenerationCount(10));
        if (CheckExceedGenerationCount(10)) {
            return JSONResult.failWithMsg("403", "You have exceeded generation limit per day.");
        }
        GenVideoResult res = new GenVideoResult();
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        res.user_id = userId;
        res.prompt = params.get("prompt").toString();
        String quality = params.get("quality").toString().equals("quality")?"quality":"speed";
        boolean with_audio = params.get("with_audio").toString().equals("true")?true :false;
        res.size = params.get("size").toString();
        int fps = Integer.valueOf(params.get("fps").toString()) == 60? 60:30;

        VideoObject result = generateVideoRequest(res.prompt, null, quality, with_audio, res.size, fps).getData();
        res.request_id = result.getId();res.task_status =result.getTaskStatus();
        res.type = "TextToVideo";res.is_deleted = 0;
        res.created_at =  new Timestamp(System.currentTimeMillis()).toString();
        servicedao.addVideoResult(res);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("id",res.request_id);
        resp.put("task_status",res.task_status);
        return JSONResult.successWithData(resp);
    }

    @RequestMapping(value = "/services/generate-video/image-to-video", method = RequestMethod.POST)
    public JSONResult imageToVideo(@RequestBody Map<String, Object> params){
        if (CheckExceedGenerationCount(10)) {
            return JSONResult.failWithMsg("403", "You have exceeded generation limit per day.");
        }
        GenVideoResult res = new GenVideoResult();
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        res.user_id = userId;
        res.prompt = params.get("prompt").toString();
        String imageUrl = params.get("image_url").toString();
        String quality = params.get("quality").toString().equals("quality")?"quality":"speed";
        boolean with_audio = params.get("with_audio").toString().equals("true")?true :false;
        res.size = params.get("size").toString();
        int fps = Integer.valueOf(params.get("fps").toString()) == 60? 60:30;
        VideoObject result = generateVideoRequest(res.prompt, imageUrl, quality, with_audio, res.size, fps).getData();
        res.request_id = result.getId();res.task_status =result.getTaskStatus();
        res.type = "ImageToVideo";res.is_deleted = 0;
        res.created_at =  new Timestamp(System.currentTimeMillis()).toString();
        servicedao.addVideoResult(res);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("id",res.request_id);
        resp.put("task_status",res.task_status);
        return JSONResult.successWithData(resp);
    }

    @RequestMapping(value = "/services/generate-model/text-to-model", method = RequestMethod.POST)
    public JSONResult textToModel(@RequestBody Map<String, Object> params){
        if (CheckExceedGenerationCount(10)) {
            return JSONResult.failWithMsg("403", "You have exceeded generation limit per day.");
        }
        GenModelResult res = new GenModelResult();
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        res.user_id = userId;
        res.prompt = params.get("prompt").toString();
        String resultFormat = params.containsKey("result_format") ? params.get("result_format").toString() : "GLB";
        Boolean enablePBR = params.containsKey("enable_pbr") ? (Boolean) params.get("enable_pbr") : null;
        Boolean enableGeometry = params.containsKey("enable_geometry") ? (Boolean) params.get("enable_geometry") : null;
        try{
            SubmitHunyuanTo3DRapidJobResponse result = generateModelRequest(res.prompt, null, resultFormat, enablePBR, enableGeometry);
            res.job_id = result.getJobId();
            res.request_id = result.getRequestId();
            res.created_at =  new Timestamp(System.currentTimeMillis()).toString();
            res.task_status = "GENERATING";
            res.type = "textToModel";
            servicedao.addModelResult(res);
            Map<String, Object> resp = new HashMap<String, Object>();
            resp.put("request_id",res.request_id);
            resp.put("job_id", res.job_id);
            resp.put("task_status","GENERATING");
            return JSONResult.successWithData(resp);
        } catch (TencentCloudSDKException e) {
            return JSONResult.failWithMsg("403", e.toString());
        }
    }

    @RequestMapping(value = "/services/generate-model/image-to-model", method = RequestMethod.POST)
    public JSONResult imageToModel(@RequestBody Map<String, Object> params){
        if (CheckExceedGenerationCount(10)) {
            return JSONResult.failWithMsg("403", "You have exceeded generation limit per day.");
        }
        GenModelResult res = new GenModelResult();
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        res.user_id = userId;
        res.prompt_image_url = params.get("image_url").toString();
        String resultFormat = params.containsKey("result_format") ? params.get("result_format").toString() : "GLB";
        Boolean enablePBR = params.containsKey("enable_pbr") ? (Boolean) params.get("enable_pbr") : null;
        Boolean enableGeometry = params.containsKey("enable_geometry") ? (Boolean) params.get("enable_geometry") : null;
        try{
            SubmitHunyuanTo3DRapidJobResponse result = generateModelRequest(null, res.prompt_image_url, resultFormat, enablePBR, enableGeometry);
            res.job_id = result.getJobId();
            res.request_id = result.getRequestId();
            res.created_at =  new Timestamp(System.currentTimeMillis()).toString();
            res.type = "imageToModel";
            res.task_status = "GENERATING";
            servicedao.addModelResult(res);
            Map<String, Object> resp = new HashMap<String, Object>();
            resp.put("request_id",res.request_id);
            resp.put("job_id", res.job_id);
            resp.put("task_status","GENERATING");
            return JSONResult.successWithData(resp);
        } catch (TencentCloudSDKException e) {
            return JSONResult.failWithMsg("403", e.toString());
        }
    }

    @RequestMapping(value = "/services/generate-model/update", method = RequestMethod.POST)
    public JSONResult updateModelResult(@RequestBody Map<String, Object> params){
        String request_id = params.get("request_id").toString();
        String status = params.get("state").toString();
        String url= params.get("url").toString();
        String thumbnailUrl= params.get("thumbnail_url").toString();
        int polygon_count = Integer.valueOf(params.get("pologen_count").toString());
        int size = Integer.valueOf(params.get("size").toString());
        servicedao.updateModelResultById(status, url, thumbnailUrl, polygon_count, size, request_id);
        return JSONResult.successWithData("");
    }

    @RequestMapping(value = "/services/generate-video/history", method = RequestMethod.GET)
    public JSONResult GetVideoHistoryById(){    
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res  = servicedao.getAllVideoResults();
        List<Map<String, Object>> finalRes  = new ArrayList<Map<String, Object>>();
        for (Map<String,Object> rMap : res) {
            if (Integer.valueOf(rMap.get("user_id").toString()) == userId) {
                if (Integer.valueOf(rMap.get("is_deleted").toString()) == 0) {
                    if (rMap.get("task_status").toString().equals("PROCESSING")) {
                        // VideoCreateParams build = VideoCreateParams.builder()
                        //         .id(rMap.get("request_id").toString())
                        //         .build();

                        // VideosResponse apply = new VideosClientApiService(zhipu_client.getConfig().getHttpClient(), zhipu_client.getConfig().getBaseUrl())
                        //         .videoGenerationsResult(build)
                        //         .apply(zhipu_client);
                        // System.out.println(rMap.get("request_id").toString());
                        VideosResponse apply = zhipu_client.videos()
                            .videoGenerationsResult(rMap.get("request_id").toString());
                        VideoObject response = apply.getData();
                        String status = response.getTaskStatus();
                        if (!status.equals("PROCESSING")) {
                            VideoResult o = apply.getData().getVideoResult().get(0);
                            servicedao.updateVideoResultById(
                                apply.getData().getTaskStatus(),
                                o.getUrl().replace("https://","https://SERVER_IP_PLACEHOLDER/proxy/"), 
                                o.getCoverImageUrl().replace("https://","https://SERVER_IP_PLACEHOLDER/proxy/"), 
                                rMap.get("request_id").toString());
                            rMap.replace("task_status",apply.getData().getTaskStatus());
                            rMap.put("url",o.getUrl());
                            rMap.put("thumbnail_url",o.getCoverImageUrl());

                        }
                    }
                    finalRes.add(rMap);
                }
            }
        };
        Collections.reverse(finalRes);
        return JSONResult.successWithData(finalRes);
    }

    @RequestMapping(value = "/services/generate-model/history", method = RequestMethod.GET)
    public JSONResult GetModelHistoryById(){    
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res  = servicedao.getAllModelResults();
        List<Map<String, Object>> finalRes  = new ArrayList<Map<String, Object>>();
        if (res != null) {
            for (Map<String,Object> rMap : res) {
                if (Integer.valueOf(rMap.get("user_id").toString()) == userId) {
                    if (Integer.valueOf(rMap.get("is_deleted").toString()) == 0) {
                        finalRes.add(rMap);
                    }
                }
            };
        }
        Collections.reverse(finalRes);
        return JSONResult.successWithData(finalRes);
    }





    @RequestMapping(value = "/services/generate-video/history/{id}", method = RequestMethod.DELETE)
    public void DeleteVidepHistoryById(@PathVariable int id){
        servicedao.deleteVideoResultById(id);
                
    }

    @RequestMapping(value = "/services/generate-model/history/{id}", method = RequestMethod.DELETE)
    public void DeleteModelHistoryById(@PathVariable int id){
        servicedao.deleteModelResultById(id);
    }

    public boolean CheckExceedGenerationCount(int count){
        User user = TokenUtils.getCurrentUser();
        int userId = user.getId();
        List<Map<String, Object>> res  = servicedao.getAllVideoResults();
        int finalCount = 0;
        for (Map<String,Object> rMap : res) {
            if (Integer.valueOf(rMap.get("user_id").toString()) == userId) {
                LocalDate time = Timestamp.valueOf(LocalDateTime.parse(rMap.get("created_at").toString())).toLocalDateTime().toLocalDate();
                LocalDate today = LocalDate.now();

                if (time.isEqual(today)) {
                    finalCount=finalCount+1;
                }
            }
        }
        if (finalCount > count) {
            return true;
        }else{
            return false;
        }
    }


    public VideosResponse generateVideoRequest(String prompt,String imageUrl,String quality,boolean with_audio,String size,int fps){
        // VideoCreateParams build = null;
        // if (imageUrl != null) {
            // build = VideoCreateParams.builder()
            //     .prompt(prompt)
            //     .model("cogvideox-2")
            //     .imageUrl(imageUrl)
            //     .withAudio(with_audio)
            //     .quality(quality)
            //     .size(size)
            //     .fps(fps)
            //     .build();
        // }else{
        //     build = VideoCreateParams.builder()
        //         .prompt(prompt)
        //         .model("cogvideox-2")
        //         .withAudio(with_audio)
        //         .quality(quality)
        //         .size(size)
        //         .fps(fps)
        //         .build();
        // }
        
        // VideosResponse apply = new VideosClientApiService(zhipu_client.getConfig().getHttpClient(), zhipu_client.getConfig().getBaseUrl())
        //         .videoGenerations(build)
        //         .apply(zhipu_client);

        
        VideoCreateParams request = VideoCreateParams.builder()
            .model("cogvideox-3")
            .prompt(prompt)
            .quality(quality)
            .withAudio(with_audio)
            .size(size)
            .fps(fps).build();

        VideosResponse apply = zhipu_client.videos().videoGenerations(request);
        return apply;
    }

    public SubmitHunyuanTo3DRapidJobResponse generateModelRequest(String prompt, String imageUrl, String resultFormat, Boolean enablePBR, Boolean enableGeometry) throws TencentCloudSDKException{
            Credential cred = new Credential("REPLACED_TENCENT_SECRET_ID", "REPLACED_TENCENT_SECRET_KEY");
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
            SubmitHunyuanTo3DRapidJobResponse resp = client.SubmitHunyuanTo3DRapidJob(req);
            return resp;
    }

    public QueryHunyuanTo3DRapidJobResponse queryModelGenerateRequest(String job_id){
        try{
            Credential cred = new Credential("REPLACED_TENCENT_SECRET_ID", "REPLACED_TENCENT_SECRET_KEY");
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("ai3d.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            Ai3dClient client = new Ai3dClient(cred, "ap-guangzhou", clientProfile);
            QueryHunyuanTo3DRapidJobRequest req = new QueryHunyuanTo3DRapidJobRequest();
            req.setJobId(job_id);
            QueryHunyuanTo3DRapidJobResponse resp = client.QueryHunyuanTo3DRapidJob(req);
            return resp;
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
        return null;
    }
    

    public class TokenAccessResult {
        public String refresh_token;
        public int expires_in;
        public String session_key;
        public String access_token;
        public String scope;
        public String session_secret;        
    }

    public class GenImageResult{
        public String id;
        public String object;
        public int created;
        public List<ImageData> data;
        public Usage usage;
    }

    public class ImageData{
        public String object;
        public String b64_image;
        public int index;
    }

    public class Usage{
        public int prompt_tokens;
        public int total_tokens;
    }



    public String getBaiduAccessToken(){
        String URL = "https://aip.baidubce.com/oauth/2.0/token" + "?grant_type=client_credentials"
                    + "&client_id=" + client_id + "&client_secret=" + client_secret;
        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        Request request = new Request.Builder()
            .url(URL)
            .method("GET", null)
            .build();
        try {
            Response response = client.newCall(request).execute();
            String res = response.body().string();
            return JSON.parseObject(res,new TypeReference<TokenAccessResult>(){}).access_token;
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public String generateImageRequest(String accessToken, String prompt){
        String URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/text2image/sd_xl?access_token="
                    + accessToken;
        OkHttpClient client = new OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
        String json = "{\"prompt\":\""+prompt+"\",\"size\":\"1024x576\"}";
        // String json = "{\"prompt\":\""+prompt+"\"}";
        okhttp3.RequestBody body = okhttp3.RequestBody.create(json,MediaType.get("application/json"));
        Request request = new Request.Builder()
            .url(URL)
            .header("Content-Type", "application/json")
            .method("POST",body )
            .build();
        
        String tempres = "";
        try {
            Response response = client.newCall(request).execute();
            String res = response.body().string();
            System.out.println("[TextToImage] 百度API原始响应: " + res);
            tempres = res;
            if (JSON.parseObject(res,new TypeReference<GenImageResult>(){}).data!=null) {
                return JSON.parseObject(res,new TypeReference<GenImageResult>(){}).data.get(0).b64_image;
                
            }else{
                
                return tempres;
            }
            
            
        } catch (Exception e) {
            // e.printStackTrace();
            if (e.getMessage().equals("timeout")) {
                return "timeout";
            }
            return tempres;
        }
    }

    public String uploadBytesToOss(byte[] bytes,String filename) throws com.aliyuncs.exceptions.ClientException{

        
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = "oss-cn-beijing.aliyuncs.com";
        // 填写Bucket名称，例如examplebucket。
        String bucketName = "alphaclass";
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = "assets/aigc_images/"+filename+".jpg";


        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint,"REPLACED_ALIYUN_ACCESS_KEY_ID", "REPLACED_ALIYUN_ACCESS_KEY_SECRET");

        try {
            
            // 填写Byte数组。
            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream(bytes));
            
            // 创建PutObject请求。
            PutObjectResult result = ossClient.putObject(putObjectRequest);    
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
		return "https://"+ bucketName+"."+endpoint+"/"+objectName;

        
    }

    public String downloadAndUploadToOss(String sourceUrl, String objectName) throws IOException {
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
