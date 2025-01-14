package com.ibkpoc.amn.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("prod")
public class WebConfigProd implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://ibkpoc.fingerservice.co.kr",
                        "http://localhost:3000", "http://172.17.10.13:8080","http://172.16.20.23:8080")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
