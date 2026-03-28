package com.FormFlow.FormFlow.Config;

import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.List;


@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI myCustomConfig(){
        return new OpenAPI().info(
                new Info().title("FormFlow API")
                        .description("API documentation for FormFlow application")
        )
                .servers(List.of(new Server().url("http://localhost:8082/formflow").description("Local server"),
                        new Server().url("https://formflow.onrender.com/formflow").description("Production server")
                ));
    }
}

//Add Tag on each controller method to group them in Swagger UI, e.g. @Tag(name = "Form Management", description = "Endpoints for creating and retrieving forms")
