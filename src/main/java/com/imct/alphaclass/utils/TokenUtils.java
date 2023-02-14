package com.imct.alphaclass.utils;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class TokenUtils {

    private static UserService staticUserService;

    @Resource
    private  UserService UserService;

    @PostConstruct
    public void setUserService(){
        staticUserService = UserService;
    }


    public static String getToken(String userId,String sign){
        return JWT.create().withAudience(userId) // 将 user id 保存到 token 里面
            .withExpiresAt(DateUtil.offsetHour(new Date(),5)) //五小时后token过期
            .sign(Algorithm.HMAC256(sign)); // 以 password 作为 token 的密钥
    }

    public static User getCurrentUser(){
        try{
            HttpServletRequest request =((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token =request.getHeader("token");
            if(StrUtil.isNotBlank(token)){
                String userId=JWT.decode(token).getAudience().get(0);
                return staticUserService.getById(Integer.valueOf(userId));
            }
        }catch (Exception e){
            return null;
        }
        return null;
    }
}
