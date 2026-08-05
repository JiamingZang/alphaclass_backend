package com.imct.alphaclass.exception;


import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：业务异常（ServiceException）按 code 返回，
 * 参数类异常返回 400，其余未知异常兜底返回 500（统一 JSON 契约）。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public JSONResult handle(ServiceException e){
        return JSONResult.failWithMsg(e.getCode(), e.getMessage());
    }

    /** 请求体无法解析（JSON 格式错误/字段类型不匹配）→ 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public JSONResult handleUnreadable(HttpMessageNotReadableException e) {
        return JSONResult.failWithMsg(Constants.CODE_400, "请求参数格式错误");
    }

    /** 路径/查询参数类型不匹配（如非数字 id）→ 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public JSONResult handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return JSONResult.failWithMsg(Constants.CODE_400, "请求参数格式错误");
    }

    /** 未知异常兜底 → 500，避免泄露堆栈细节 */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public JSONResult handleUnknown(Exception e) {
        return JSONResult.failWithMsg(Constants.CODE_500, "服务器内部错误");
    }
}
