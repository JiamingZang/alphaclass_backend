package com.imct.alphaclass.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

@Service
@RequiredArgsConstructor
public class UserService {
    /** 合法角色（注册白名单，防止自封任意角色） */
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_STUDENT = "student";

    private final UserDAO dao;
    private final AccessService access;

    /** 查询全部用户（id 转字符串、url/courses_url 填充） */
    public List<Map<String, Object>> findAll() {
        return dao.findAll().stream()
                .map(this::toUserMap)
                .collect(Collectors.toList());
    }
    /** 注册用户：用户名已存在时返回 null（由 Controller 转 401）；响应不含明文密码 */
    public Map<String, Object> register(User user) {
        validateRegisterParams(user);
        if (dao.getByUsername(user.getUsername()) != null) {
            return null;
        }
        dao.register(user);
        return toUserMap(user);
    }

    /** 注册参数校验：用户名/密码/角色必填，角色仅限 teacher/student */
    private void validateRegisterParams(User user) {
        boolean blankUsername = user.getUsername() == null || user.getUsername().trim().isEmpty();
        boolean blankPassword = user.getPassword() == null || user.getPassword().trim().isEmpty();
        boolean invalidRole = !ROLE_TEACHER.equals(user.getRole()) && !ROLE_STUDENT.equals(user.getRole());
        if (blankUsername || blankPassword || invalidRole) {
            throw new ServiceException(Constants.CODE_400, "用户名、密码或角色不合法");
        }
    }

    /** 按用户名查询用户；不存在时返回 null */
    public Map<String, Object> getByUsername(String username) {
        User user = dao.getByUsername(username);
        return user == null ? null : toUserMap(user);
    }

    /**
     * 登录校验（用户名+密码）；认证失败时返回 null。
     * 响应不含明文密码，密码经 sign 键单独传出（仅用于 Controller 签发 token）。
     */
    public Map<String, Object> login(User user) {
        User resultUser = dao.login(user);
        if (resultUser == null) {
            return null;
        }
        Map<String, Object> result = toUserMap(resultUser);
        result.put("sign", resultUser.getPassword());
        return result;
    }

    /** 按 id 查询用户实体（供 JwtInterceptor 等内部使用，不组装响应） */
    public User getById(int id) {
        return dao.getById(id);
    }

    /**
     * 修改密码：校验旧密码通过后更新，失败（旧密码错误/参数缺失）时返回 null。
     * 响应不含明文密码，前端需重新登录获取新 token（旧 token 因签名密钥变更已失效）。
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

    /** 用户响应公共组装：id 转字符串 + url/courses_url 填充 + 移除明文密码（所有用户响应统一，杜绝泄露） */
    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> result = fillUserUrls(MapUtils.toMap(user));
        result.remove("password");
        return result;
    }

    /** 用户列表组装：DAO 直接返回 Map（findAll 专用，复制一份避免污染 DAO 对象） */
    private Map<String, Object> toUserMap(Map<String, Object> userMap) {
        Map<String, Object> result = fillUserUrls(new HashMap<String, Object>(userMap));
        result.remove("password");
        return result;
    }

    /** 填充 id 字符串化及 url/courses_url 链接（baseUrl 可配置） */
    private Map<String, Object> fillUserUrls(Map<String, Object> result) {
        result.put("id", result.get("id").toString());
        result.put("url", access.userUrl(result.get("username").toString()));
        result.put("courses_url", access.userCoursesUrl(result.get("username").toString()));
        return result;
    }
}
