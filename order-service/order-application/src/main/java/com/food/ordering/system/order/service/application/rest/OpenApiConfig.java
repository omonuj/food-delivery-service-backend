package com.food.ordering.system.order.service.application.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Food Ordering System — Order Service API")
                        .description("REST API of the Order Service, the SAGA orchestrator of an event-driven, " +
                                "DDD / hexagonal food-ordering platform. Create orders and track their status " +
                                "through the PENDING → PAID → APPROVED / CANCELLED lifecycle.")
                        .version("v1")
                        .contact(new Contact().name("Food Ordering System"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8181").description("Local container")));
    }
}
