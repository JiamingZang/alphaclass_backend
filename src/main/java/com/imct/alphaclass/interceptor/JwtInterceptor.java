package com.imct.alphaclass.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Method;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.service.UserService;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JwtInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public JwtInterceptor(UserService userService) {
        this.userService = userService;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("token");// 从 http 请求头中取出 token
        if (Method.GET.toString().equals(request.getMethod())) {
            return true;
        }
        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        if (StrUtil.isBlank(token)) {
            throw new ServiceException(Constants.CODE_401, "无token,请重新登陆");
        }

        String userId;
        try {
            userId = JWT.decode(token).getAudience().get(0);
        } catch (JWTDecodeException | IndexOutOfBoundsException j) {
            throw new ServiceException(Constants.CODE_401, "token验证失败,请重新登录");
        }

        User user;
        try {
            user = userService.getById(Integer.valueOf(userId));
        } catch (NumberFormatException e) {
            throw new ServiceException(Constants.CODE_401, "token验证失败,请重新登录");
        }
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }
        // 验证 token
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
        try {
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new ServiceException(Constants.CODE_401, "token验证失败,请重新登录");
        }
        return true;

    }
}
