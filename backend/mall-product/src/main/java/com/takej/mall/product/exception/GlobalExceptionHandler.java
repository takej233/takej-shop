package com.takej.mall.product.exception;

import com.takej.mall.common.BusinessException;
import com.takej.mall.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;



/**
 * 商品服务全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理商品不存在等业务异常。
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
     * 处理请求参数校验失败，统一返回 400 和具体提示。
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
     * 处理请求方法不允许，例如普通用户调用已关闭的商品写接口。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {

        return ResponseEntity
                .status(405)
                .body(Result.failure(
                        405,
                        "method not allowed"
                ));
    }


    /**
     * 处理请求路径不存在，例如访问了已经关闭或从未实现的接口。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(
            NoResourceFoundException exception) {

        return ResponseEntity
                .status(404)
                .body(Result.failure(404, "resource not found"));
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