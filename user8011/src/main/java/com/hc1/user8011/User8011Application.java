package com.hc1.user8011;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableCaching
public class User8011Application {

    public static void main(String[] args) {
        SpringApplication.run(User8011Application.class, args);
    }

//    // 远程调用工具
//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder)
//    {
//        return builder.build();
//    }

}
