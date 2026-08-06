package com.imct.alphaclass.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.imct.alphaclass.exception.ServiceException;

/**
 * WikiService 安全契约测试：SSRF 防御（协议白名单 + 内网/环回/元数据地址拒绝）。
 * 校验在发起网络请求之前完成，因此这些用例不依赖外部网络。
 */
class WikiServiceTest {

    private final WikiService service = new WikiService();

    @Test
    void getDataFromUrl_nonHttpScheme_returns400() {
        ServiceException e = assertThrows(ServiceException.class,
                () -> service.getDataFromUrl("file:///etc/passwd"));
        assertEquals("400", e.getCode());
        assertEquals("仅支持 http/https 地址", e.getMessage());
    }

    @Test
    void getDataFromUrl_loopback_returns400() {
        ServiceException e = assertThrows(ServiceException.class,
                () -> service.getDataFromUrl("http://127.0.0.1:8080/secret"));
        assertEquals("400", e.getCode());
        assertEquals("不允许访问内网地址", e.getMessage());
    }

    @Test
    void getDataFromUrl_localhost_returns400() {
        assertThrows(ServiceException.class,
                () -> service.getDataFromUrl("http://localhost/x"));
    }

    @Test
    void getDataFromUrl_privateNetworks_returns400() {
        assertThrows(ServiceException.class,
                () -> service.getDataFromUrl("http://10.0.0.1/x"));
        assertThrows(ServiceException.class,
                () -> service.getDataFromUrl("http://192.168.1.1/x"));
        assertThrows(ServiceException.class,
                () -> service.getDataFromUrl("http://172.16.0.1/x"));
    }

    @Test
    void getDataFromUrl_nonStandardPort_returns400() {
        ServiceException e = assertThrows(ServiceException.class,
                () -> service.getDataFromUrl("http://example.com:8080/x"));
        assertEquals("400", e.getCode());
        assertEquals("仅支持 80/443 端口", e.getMessage());
    }

    @Test
    void getLongDescription_cloudMetadata_returns400() {
        ServiceException e = assertThrows(ServiceException.class,
                () -> service.getLongDescription("http://169.254.169.254/latest/meta-data/"));
        assertEquals("不允许访问内网地址", e.getMessage());
    }
}
