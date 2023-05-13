package com.springcloud.remind_project.SpringCloudConfig;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Router {
    
    @Bean
    public RouteLocator gatewayLocator(RouteLocatorBuilder builder){
        return builder.routes()
            .route(r -> r.path("/first").uri("http://localhost:8001"))
            .route(r -> r.path("/second").uri("https://naver.com")
            ).build();
    }
}
