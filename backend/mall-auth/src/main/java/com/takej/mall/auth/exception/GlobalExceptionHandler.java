package com.takej.mall.auth.exception;

import com.takej.mall.common.BusinessException;
import com.takej.mall.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 认证服务全局异常处理器。
 * 返回约定与其他业务服务保持一致：HTTP 状态码就是业务错误码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常，例如用户名已存在、账号密码错误。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            BusinessException exception) {

        return ResponseEntity
                .status(exception.getCode())
                .body(Result.failure(
                        exception.getCode(),
                        exception.getMessage()
                ));
    }

    /**
     * 处理请求参数校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {

        FieldError fieldError =
                exception.getBindingResult().getFieldError();

        String message = fieldError == null
                ? "invalid parameter"
                : fieldError.getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(Result.failure(400, message));
    }

    /**
     * 处理请求方法不允许。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {

        return ResponseEntity
                .status(405)
                .body(Result.failure(405, "method not allowed"));
    }

    /**
     * 处理请求路径不存在。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(
            NoResourceFoundException exception) {

        return ResponseEntity
                .status(404)
                .body(Result.failure(404, "resource not found"));
    }

    /**
     * 处理请求体无法解析，例如 JSON 格式错误。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException exception) {

        return ResponseEntity
                .badRequest()
                .body(Result.failure(400, "invalid request body"));
    }

    /**
     * 处理未预期的系统异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(
            Exception exception) {

        log.error("Unhandled exception in mall-auth", exception);

        return ResponseEntity
                .internalServerError()
                .body(Result.failure(500, "internal server error"));
    }
}