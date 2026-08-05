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

@RestController
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @RequestMapping(value = "/users",method = RequestMethod.GET)
    public JSONResult findAll() {
        return JSONResult.successWithData(service.findAll());
    }

    /**
     * @param request
     * @return
     */
    @RequestMapping(value = "/users",method = RequestMethod.POST)
    public JSONResult register(@RequestBody User user){
        Map<String,Object> result = service.register(user);
        if (result==null) {
            return JSONResult.failWithMsg(Constants.CODE_401,"用户名已被注册");
        }else{
            return JSONResult.successWithData(result);
        }
    }

    @RequestMapping(value = "/users/{user}",method = RequestMethod.GET)
    public JSONResult getByUsername(@PathVariable String user){
        return JSONResult.successWithData(service.getByUsername(user));
    }

    @RequestMapping(value = "/users/actions/login",method = RequestMethod.POST)
    public JSONResult login(@RequestBody User user){
        Map<String,Object> result = service.login(user);
        if (result!=null) {
            String token = TokenUtils.getToken(result.get("id").toString(), result.get("password").toString());
            result.put("token", token);
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_401,"认证失败");
        }
    }

    /** 修改密码（需登录）：旧密码校验通过后更新，响应携带新 token 用密码 */
    @RequestMapping(value = "/user/actions/change-password", method = RequestMethod.POST)
    public JSONResult changePassword(@RequestBody Map<String, String> params) {
        User user = TokenUtils.getCurrentUser();
        if (user != null) {
            Map<String, Object> result = service.changePassword(user.getUsername(), params);
            if (result != null) {
                return JSONResult.successWithData(result);
            }
        }
        return JSONResult.failWithMsg(Constants.CODE_401, "验证失败");
    }

    /** 修改昵称（需登录） */
    @RequestMapping(value = "/user/actions/change-profile", method = RequestMethod.POST)
    public JSONResult changeProfile(@RequestBody Map<String, String> params) {
        User user = TokenUtils.getCurrentUser();
        if (user != null) {
            Map<String, Object> result = service.changeProfile(user.getUsername(), params);
            if (result != null) {
                return JSONResult.successWithData(result);
            }
        }
        return JSONResult.failWithMsg(Constants.CODE_401, "验证失败");
    }
    
}
