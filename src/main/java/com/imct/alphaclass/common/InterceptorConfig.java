package com.imct.alphaclass.common;

import com.imct.alphaclass.interceptor.JwtInterceptor;
import com.imct.alphaclass.service.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final UserService userService;

    public InterceptorConfig(UserService userService) {
        this.userService = userService;
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
