package com.hc1.admin8012;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class Admin8012Application {

    public static void main(String[] args) {
        SpringApplication.run(Admin8012Application.class, args);
    }

}
