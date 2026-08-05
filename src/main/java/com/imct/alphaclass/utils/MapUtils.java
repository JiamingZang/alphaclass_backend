package com.imct.alphaclass.utils;

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
}
