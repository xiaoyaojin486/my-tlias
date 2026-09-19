package com.itheima.exception;

import com.itheima.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 定义一个全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 声明异常处理的方法 - 所有的Exception
     */
    @ExceptionHandler
    public Result handleException(Exception ex) {
        log.error("服务器异常", ex);
        return Result.error("对不起, 您的网络有问题, 请稍后重试 ~");
    }

    /**
     * 声明异常处理的方法 - BusinessException
     */
    @ExceptionHandler
    public Result handleBuinessException(BusinessException businessException) {
        log.error("服务器异常", businessException);
        return Result.error(businessException.getMessage());
    }

}
