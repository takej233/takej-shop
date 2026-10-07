package com.takej.mall.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonResultTest {

    @Test
    void successShouldContainData() {
        Result<String> result = Result.success("hello");

        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    void failureShouldUseErrorCode() {
        Result<Void> result = Result.failure(ErrorCode.PARAM_ERROR);

        assertEquals(400, result.getCode());
        assertEquals("invalid parameter", result.getMessage());
    }

    @Test
    void businessExceptionShouldCarryErrorCode() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> {
                    throw new BusinessException(ErrorCode.NOT_FOUND);
                }
        );

        assertEquals(404, exception.getCode());
        assertEquals("resource not found", exception.getMessage());
    }
}