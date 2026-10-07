package com.takej.mall.common;

public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(400, "invalid parameter"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "resource not found"),
    STOCK_NOT_ENOUGH(409, "insufficient stock"),
    USERNAME_EXISTS(409, "username already exists"),
    INVALID_CREDENTIALS(401, "username or password is incorrect"),
    INTERNAL_ERROR(500, "internal server error");


    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}