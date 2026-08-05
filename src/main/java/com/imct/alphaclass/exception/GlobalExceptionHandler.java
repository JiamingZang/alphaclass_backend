package com.imct.alphaclass.exception;


import com.imct.alphaclass.common.JSONResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public JSONResult handle(ServiceException e){
        return JSONResult.failWithMsg(e.getCode(),e.getMessage());
    }
}
