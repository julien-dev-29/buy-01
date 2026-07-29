package com.jurol.buy01.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/auth/**", "/api/users/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://user-service"))
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://product-service"))
                .route("media-service", r -> r
                        .path("/api/media/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://media-service"))
                .build();
    }
}