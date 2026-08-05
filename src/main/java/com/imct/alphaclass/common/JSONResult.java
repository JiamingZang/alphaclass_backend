package com.imct.alphaclass.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JSONResult<T> extends ResponseEntity<String> {

    public JSONResult(HttpStatus status) {
        super(status);
    }

    public JSONResult(String code){
        super(num2HttpStatus(code));
    }

    public JSONResult(String code, T data) {
        super(JSON.toJSONString(data,SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat),num2HttpStatus(code));
    }

    /** 数字 code 映射 HTTP 状态；无法解析（非数字）时回退 500，未匹配时 404 */
    public static HttpStatus num2HttpStatus(String code) {
        try {
            HttpStatus status = HttpStatus.resolve(Integer.parseInt(code));
            return status == null ? HttpStatus.NOT_FOUND : status;
        } catch (NumberFormatException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    public static <T> JSONResult failWithMsg(String code,String msg) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("message", msg);
        return new JSONResult(code,message);
    }

    public static <T> JSONResult successWithData(T data) {
        return new JSONResult("200", data);
    }
    

    public static JSONResult customWithStatus(String code){
        return new JSONResult(code);
    }
}
