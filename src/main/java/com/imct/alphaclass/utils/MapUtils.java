package com.imct.alphaclass.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

/** bean 与 Map 互转、坐标字段收拢等通用工具 */
public final class MapUtils {

    private MapUtils() {
    }

    /** bean -> Map（fastjson 序列化再反序列化，注意数字可能变为 BigDecimal） */
    public static Map<String, Object> toMap(Object bean) {
        return JSON.parseObject(JSON.toJSONString(bean), new TypeReference<Map<String, Object>>() {
        });
    }

    /** 将扁平坐标字段（如 originpos_x/pos_x）收拢为嵌套结构（如 pos），并移除源字段 */
    public static Map<String, Object> nestVec(Map<String, Object> source, String from, String to) {
        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put(to + "_x", source.get(from + "_x"));
        nested.put(to + "_y", source.get(from + "_y"));
        nested.put(to + "_z", source.get(from + "_z"));
        source.remove(from + "_x");
        source.remove(from + "_y");
        source.remove(from + "_z");
        return nested;
    }

    /**
     * 将 Map 中字段安全解析为 float：字段缺失或空串时返回 0.0f，
     * 统一替代各 service 中重复的 "".equals(x) ? "0.0" : x 防御写法。
     */
    public static float parseFloat(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null || "".equals(value.toString())) {
            return 0.0f;
        }
        return Float.parseFloat(value.toString());
    }

    /** 将 Map 中字段安全解析为 int：字段缺失或空串时返回 0（同上，替代重复防御写法） */
    public static int parseInteger(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null || "".equals(value.toString())) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    /** URL 编码（UTF-8 为标准字符集，理论不可达异常包装为 IllegalStateException） */
    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported", e);
        }
    }

    /** 当前时间戳字符串（各 Service 落库统一使用，替代重复的 new Timestamp(...) 写法） */
    public static String now() {
        return new Timestamp(System.currentTimeMillis()).toString();
    }

    /** 时间字段统一格式化（各 Service 响应组装共用，替代重复的 DateTimeFormatter 定义） */
    public static String formatDateTime(LocalDateTime value) {
        return DATE_TIME.format(value);
    }

    /**
     * 解析时间字段：兼容 yyyy-MM-dd HH:mm:ss 与 Timestamp.toString() 的带毫秒格式
     * （截断毫秒）；非法/空值返回 null，由调用方兜底（避免各 Service 重复 try-catch）。
     */
    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.length() < 19) {
            return null;
        }
        return LocalDateTime.parse(value.substring(0, 19), DATE_TIME);
    }

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
