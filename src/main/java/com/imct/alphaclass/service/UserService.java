package com.imct.alphaclass.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.utils.MapUtils;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDAO dao;
    private final AccessService access;

    /** 查询全部用户（id 转字符串、url/courses_url 填充） */
    public List<Map<String, Object>> findAll() {
        return dao.findAll().stream()
                .map(this::toUserMap)
                .collect(Collectors.toList());
    }
    /** 注册用户：用户名已存在时返回 null（由 Controller 转 401） */
    public Map<String, Object> register(User user) {
        if (dao.getByUsername(user.getUsername()) != null) {
            return null;
        }
        dao.register(user);
        return toUserMap(user);
    }

    /** 按用户名查询用户；不存在时返回 null */
    public Map<String, Object> getByUsername(String username) {
        User user = dao.getByUsername(username);
        return user == null ? null : toUserMap(user);
    }

    /** 登录校验（用户名+密码）；认证失败时返回 null */
    public Map<String, Object> login(User user) {
        User resultUser = dao.login(user);
        return resultUser == null ? null : toUserMap(resultUser);
    }

    /** 按 id 查询用户实体（供 JwtInterceptor 等内部使用，不组装响应） */
    public User getById(int id) {
        return dao.getById(id);
    }

    /**
     * 修改密码：校验旧密码通过后更新，失败（旧密码错误/参数缺失）时返回 null。
     * 响应中的 password 为明文新密码，供前端生成 token 使用。
     */
    public Map<String, Object> changePassword(String username, Map<String, String> params) {
        if (params.get("password") == null || params.get("new_password") == null) {
            return null;
        }
        if (!dao.updatePasswordByUsername(params.get("new_password"), username, params.get("password"))) {
            return null;
        }
        Map<String, Object> result = getByUsername(username);
        result.remove("courses_url");
        result.put("password", params.get("new_password"));
        return result;
    }

    /** 修改昵称；参数缺失时返回 null */
    public Map<String, Object> changeProfile(String username, Map<String, String> params) {
        if (params.get("name") == null || !dao.updateNameByUsername(params.get("name"), username)) {
            return null;
        }
        Map<String, Object> result = getByUsername(username);
        result.remove("courses_url");
        return result;
    }

    /** 用户响应公共组装：id 转字符串 + url/courses_url 填充（register/login/查询共用） */
    private Map<String, Object> toUserMap(User user) {
        return fillUserUrls(MapUtils.toMap(user));
    }

    /** 用户列表组装：DAO 直接返回 Map（findAll 专用，复制一份避免污染 DAO 对象） */
    private Map<String, Object> toUserMap(Map<String, Object> userMap) {
        return fillUserUrls(new HashMap<String, Object>(userMap));
    }

    /** 填充 id 字符串化及 url/courses_url 链接（baseUrl 可配置） */
    private Map<String, Object> fillUserUrls(Map<String, Object> result) {
        result.put("id", result.get("id").toString());
        result.put("url", access.userUrl(result.get("username").toString()));
        result.put("courses_url", access.userCoursesUrl(result.get("username").toString()));
        return result;
    }
}
