package com.cxy1ng.exception;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.cxy1ng.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice("com.cxy1ng.controller")
public class GlobalExceptionHandler {

    private static final Log log = LogFactory.get();

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result exception(HttpServletRequest request, Exception e) {
        log.error("异常信息：",e);
        return Result.error();
    }

    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public Result exception(HttpServletRequest request, CustomException e) {
        return Result.error(e.getCode(), e.getMsg());
    }
}
