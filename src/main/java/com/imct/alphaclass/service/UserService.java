package com.imct.alphaclass.service;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.utils.MapUtils;

@Service
public class UserService {
    @Resource
    private UserDAO dao;

    @Value("${app.base-url:https://SERVER_IP_PLACEHOLDER/v2}")
    private String baseUrl;

    public List<Map<String,Object>> findAll(){
        List<Map<String,Object>> users = dao.findAll();
        for (Map<String,Object> user : users) {
            user.put("id", user.get("id").toString());
            fillUrls(user);
        }
        return users;
    }

    public Map<String,Object> register(User user){
        if (dao.getByUsername(user.getUsername())==null) {
            dao.register(user);
            Map<String, Object> result = MapUtils.toMap(user);
            result.put("id", result.get("id").toString());
            fillUrls(result);
            return result;
        }else{
            return null;
        }
        
    }

    public Map<String, Object> getByUsername(String username){
        User user = dao.getByUsername(username);
        if (user!=null) {
            Map<String, Object> result = MapUtils.toMap(user);
            result.put("id", result.get("id").toString());
            fillUrls(result);
            return result;
        }else{
            return null;
        }
    }

    public Map<String, Object> login(User user){
        User resultUser = dao.login(user);
        if (resultUser!=null){ 
            Map<String, Object> result = MapUtils.toMap(resultUser);
            result.put("id", result.get("id").toString());
            fillUrls(result);
            return result;
        }else{
            return null;
        }
    }

    public User getById(int id){
        return dao.getById(id);
    }

    public Map<String, Object> change_password(String username,Map<String,String> params){
        if (params.get("password") != null&&params.get("new_password")!=null){
            if (dao.updatePasswordByUsername(params.get("new_password"), username, params.get("password"))){ 
                Map<String, Object> result = getByUsername(username);
                result.remove("courses_url");
                result.put("password", params.get("new_password"));
                return result;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    public Map<String, Object> change_profile(String username, Map<String, String> params){
        if (params.get("name")!= null) {
            if (dao.updateNameByUsername(params.get("name"), username)) {
                Map<String, Object> result = getByUsername(username);
                result.remove("courses_url");
                return result;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    /** 填充 user 的 url/courses_url 链接（baseUrl 可配置） */
    private void fillUrls(Map<String, Object> user) {
        user.put("url", baseUrl + "/users/" + user.get("username"));
        user.put("courses_url", baseUrl + "/users/" + user.get("username") + "/courses");
    }
}
