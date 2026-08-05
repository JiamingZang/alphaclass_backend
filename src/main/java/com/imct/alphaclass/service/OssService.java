package com.imct.alphaclass.service;

import java.io.ByteArrayInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;

/**
 * OSS 上传公共组件：文生图与模型生成共用的字节流上传入口。
 * 只负责上传并返回 objectName，URL 拼装（bucket.endpoint/objectName）由各 Service 按需处理。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OssService {

    @Value("${ai.oss.endpoint}")
    private String ossEndpoint;
    @Value("${ai.oss.bucket}")
    private String ossBucket;
    @Value("${ai.oss.access-key-id}")
    private String ossAccessKeyId;
    @Value("${ai.oss.access-key-secret}")
    private String ossAccessKeySecret;

    /** 上传字节流到 OSS，返回 objectName；失败时记录日志并抛出（不静默返回坏 URL） */
    public String uploadBytes(byte[] bytes, String objectName) {
        OSS ossClient = new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossBucket, objectName,
                    new ByteArrayInputStream(bytes));
            ossClient.putObject(putObjectRequest);
        } catch (OSSException | ClientException e) {
            log.error("OSS 上传失败: objectName={}, error={}", objectName, e.getMessage());
            throw e;
        } finally {
            ossClient.shutdown();
        }
        return objectName;
    }

    /** 由 objectName 拼出完整访问 URL */
    public String urlOf(String objectName) {
        return "https://" + ossBucket + "." + ossEndpoint + "/" + objectName;
    }
}
