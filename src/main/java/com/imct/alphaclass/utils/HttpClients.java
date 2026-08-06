package com.imct.alphaclass.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.OkHttpClient;

/**
 * OkHttpClient 统一构建：各 Service 不再各自 new 客户端，
 * 仅按需指定连接/读取超时（默认 10s/10s，AI 生成类接口传大超时）。
 */
public final class HttpClients {

    private HttpClients() {
    }

    private static final int DEFAULT_TIMEOUT = 10;

    /** 默认客户端（连接/读取各 10 秒） */
    public static OkHttpClient defaultClient() {
        return timeoutClient(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

    /** 自定义连接/读取超时（秒） */
    public static OkHttpClient timeoutClient(int connectSeconds, int readSeconds) {
        return new OkHttpClient().newBuilder()
                .connectTimeout(connectSeconds, TimeUnit.SECONDS)
                .readTimeout(readSeconds, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 公网抓取客户端（SSRF 防御）：DNS 解析时拒绝内网/环回/链路本地/任意地址。
     * 校验发生在请求实际使用的解析结果上，可同时覆盖 302 重定向与 DNS rebinding 绕过；
     * 拦截时抛 UnknownHostException，由调用方按 IO 异常处理。其余配置同 defaultClient。
     */
    public static OkHttpClient publicClient() {
        return defaultClient().newBuilder()
                .dns(new Dns() {
                    @Override
                    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                        List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname);
                        for (InetAddress addr : addresses) {
                            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                                throw new UnknownHostException("blocked internal address: " + hostname);
                            }
                        }
                        return addresses;
                    }
                })
                .build();
    }
}
