package com.imct.alphaclass.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class TokenUtils {

    private final UserService userService;

    /** 签发 JWT：以用户密码作为签名密钥（audience 存 userId） */
    public String getToken(String userId, String sign) {
        return JWT.create().withAudience(userId) // 将 user id 保存到 token 里面
            .withExpiresAt(DateUtil.offsetHour(new Date(), 5)) //五小时后token过期
            .sign(Algorithm.HMAC256(sign)); // 以 password 作为 token 的密钥
    }

    /** 从当前请求 token 解析用户；无 token/token 非法/用户不存在时返回 null */
    public User getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader("token");
            if (StrUtil.isNotBlank(token)) {
                String userId = JWT.decode(token).getAudience().get(0);
                return userService.getById(Integer.valueOf(userId));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * 校验当前登录用户是否为指定资源所有者：未登录或非所有者时返回 null，
     * 由 Controller 统一转为 401 响应（替代各处重复的 getCurrentUser + 名字比对）。
     */
    public User requireOwner(String owner) {
        User user = getCurrentUser();
        if (user == null || !owner.equals(user.getUsername())) {
            return null;
        }
        return user;
    }
}
