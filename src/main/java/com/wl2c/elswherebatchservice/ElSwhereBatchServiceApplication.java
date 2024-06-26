package com.wl2c.elswherebatchservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ElSwhereBatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElSwhereBatchServiceApplication.class, args);
    }

}
