package com.wl2c.elswherebatchservice.batch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    @Value("${spring.application.name}")
    private String service;

    @GetMapping("/health_check")
    public String status() {
        return "It's Working in " + service;
    }
}
