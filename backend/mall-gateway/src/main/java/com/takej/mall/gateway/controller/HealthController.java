package com.takej.mall.gateway.controller;

import com.takej.mall.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("mall-gateway is running");
    }
}