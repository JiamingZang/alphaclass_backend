package com.imct.alphaclass.common;

import com.imct.alphaclass.interceptor.JwtInterceptor;
import com.imct.alphaclass.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final UserService userService;

    /** 跨域白名单（逗号分隔；部署时用 CORS_ALLOWED_ORIGINS 覆盖，默认全部放行） */
    @Value("${app.cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    public InterceptorConfig(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsAllowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor(userService))
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/users/actions/login",
                    "/users",
                    "/users/{user}"
                    // "/users/{owner}/courses"
                    );    // 拦截所有请求，通过判断token是否合法决定是否需要登录
    }
}
