package com.minimarket.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.minimarket.api.tenant.TenantProperties;

@SpringBootApplication
@EnableConfigurationProperties(TenantProperties.class)
public class MinimarketApiJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimarketApiJavaApplication.class, args);
    }
}
