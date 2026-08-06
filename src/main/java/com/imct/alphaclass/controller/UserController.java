package com.imct.alphaclass.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "用户", description = "注册/登录/查询/改密/改昵称；登录返回 token，写操作需携带 token 请求头")
public class UserController {
    private final UserService service;
    private final TokenUtils tokenUtils;

    public UserController(UserService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/users",method = RequestMethod.GET)
    @Operation(summary = "用户列表", description = "返回全部用户，字段：id(字符串)/username/name/role/url/courses_url，不含 password")
    public JSONResult findAll() {
        return JSONResult.successWithData(service.findAll());
    }

    /**
     * @param request
     * @return
     */
    @RequestMapping(value = "/users",method = RequestMethod.POST)
    @Operation(summary = "注册", description = "body: {username, name, password, role}；role 仅限 teacher/student；成功返回用户对象(不含 password)，用户名已存在返回 401")
    public JSONResult register(@RequestBody User user){
        Map<String,Object> result = service.register(user);
        if (result==null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_USERNAME_TAKEN);
        }else{
            return JSONResult.successWithData(result);
        }
    }

    @RequestMapping(value = "/users/{user}",method = RequestMethod.GET)
    @Operation(summary = "用户详情", description = "按用户名查询，返回用户对象(不含 password)")
    public JSONResult getByUsername(@PathVariable String user){
        return JSONResult.successWithData(service.getByUsername(user));
    }

    @RequestMapping(value = "/users/actions/login",method = RequestMethod.POST)
    @Operation(summary = "登录", description = "body: {username, password}；成功返回用户对象 + token（后续请求放入请求头 token），失败返回 401")
    public JSONResult login(@RequestBody User user){
        Map<String,Object> result = service.login(user);
        if (result!=null) {
            // sign 为签名密钥（密码），仅用于签发 token，不随响应返回
            String sign = result.remove("sign").toString();
            String token = tokenUtils.getToken(result.get("id").toString(), sign);
            result.put("token", token);
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_AUTH_FAILED);
        }
    }

    /** 修改密码（需登录）：旧密码校验通过后更新，响应携带新 token 用密码 */
    @RequestMapping(value = "/user/actions/change-password",method = RequestMethod.POST)
    @Operation(summary = "修改密码", description = "需登录；body: {password(旧), new_password}；成功后旧 token 失效需重新登录")
    public JSONResult changePassword(@RequestBody Map<String, String> params) {
        User user = tokenUtils.getCurrentUser();
        if (user != null) {
            Map<String, Object> result = service.changePassword(user.getUsername(), params);
            if (result != null) {
                return JSONResult.successWithData(result);
            }
        }
        return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_VERIFY_FAILED);
    }

    /** 修改昵称（需登录） */
    @RequestMapping(value = "/user/actions/change-profile",method = RequestMethod.POST)
    @Operation(summary = "修改昵称", description = "需登录；body: {name}，返回更新后的用户对象")
    public JSONResult changeProfile(@RequestBody Map<String, String> params) {
        User user = tokenUtils.getCurrentUser();
        if (user != null) {
            Map<String, Object> result = service.changeProfile(user.getUsername(), params);
            if (result != null) {
                return JSONResult.successWithData(result);
            }
        }
        return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_VERIFY_FAILED);
    }
    
}
