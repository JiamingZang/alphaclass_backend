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

    public static HttpStatus num2HttpStatus(String code) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        for (HttpStatus httpStatus : HttpStatus.values()) {
            boolean b = Integer.parseInt(code) == httpStatus.value();
            if (b) {
                return httpStatus;
            }
        }
        return status;
    }

    // public static <T> JSONResult<T> success(T data) {
    //     return new JSONResult("200", "成功", data);
    // }

    // public static <T> JSONResult<T> failed(T data) {
    //     return new JSONResult("422", "失败", data);
    // }

    public static <T> JSONResult failWithMsg(String code,String msg) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("message", msg);
        return new JSONResult(code,message);
    }

    // public static JSONResult successWithData(Map<String, Object> data) {
    //     return new JSONResult("200", data);
    // }

    // public static JSONResult successWithData(List<Map<String, Object>> data) {
    //     return new JSONResult("200", data);
    // }

    public static <T> JSONResult successWithData(T data) {
        return new JSONResult("200", data);
    }
    

    public static JSONResult customWithStatus(String code){
        return new JSONResult(code);
    }
}


// class Message<T> {

//     // String status;
//     //向前端返回的内容
//     String message;

//     // T data;

//     public Message() {
//     }

//     public Message(String message) {
//         // this.status = status;
//         this.message = message;
//     }

//     // public Message(String status, String message, T data) {
//     //     this.data = data;
//     //     this.status = status;
//     //     this.message = message;
//     // }

//     // public static <T> Message<T> custom(String status, String message, T data) {
//     //     return new Message(status, message, data);
//     // }

//     public static <T> Message<T> custom( String message) {
//         return new Message( message);
//     }

//     public static HttpStatus num2HttpStatus(String code) {
//         HttpStatus status = HttpStatus.NOT_FOUND;
//         for (HttpStatus httpStatus : HttpStatus.values()) {
//             boolean b = Integer.parseInt(code) == httpStatus.value();
//             if (b) {
//                 return httpStatus;
//             }
//         }
//         return status;
//     }

//     // public String getStatus() {
//     //     return status;
//     // }

//     // public void setStatus(String status) {
//     //     this.status = status;
//     // }

//     public String getMessage() {
//         return message;
//     }

//     public void setMessage(String message) {
//         this.message = message;
//     }

//     // public T getData() {
//     //     return data;
//     // }

//     // public void setData(T data) {
//     //     this.data = data;
//     // }

// }

