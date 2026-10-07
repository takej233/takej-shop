package com.takej.mall.user.exception;

import com.takej.mall.common.BusinessException;
import com.takej.mall.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;



/**
 * 用户服务全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常：把 BusinessException 中的错误码映射为 HTTP 状态码。
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
     * 处理参数校验失败：统一返回 400 和具体校验提示。
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
     * 处理请求方法不允许，例如对只读接口发起写请求。
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

        return ResponseEntity
                .internalServerError()
                .body(Result.failure(500, "internal server error"));
    }
}