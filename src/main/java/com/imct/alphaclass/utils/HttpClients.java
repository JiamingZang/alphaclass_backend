package com.imct.alphaclass.utils;

import java.util.concurrent.TimeUnit;

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
}
