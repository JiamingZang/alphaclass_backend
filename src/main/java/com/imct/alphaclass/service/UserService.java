package com.imct.alphaclass.service;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.UserDAO;

@Service
public class UserService {
    @Resource
    private UserDAO dao;

    public List<Map<String,Object>> findAll(){
        List<Map<String,Object>> users = dao.findAll();
        for (Map<String,Object> user : users) {
            user.put("id", user.get("id").toString());
            user.put("url", "https://SERVER_IP_PLACEHOLDER/v2/users/"+user.get("username"));
            user.put("courses_url", "https://SERVER_IP_PLACEHOLDER/v2/users/"+user.get("username")+"/courses");
        }
        return users;
    }

    public Map<String,Object> register(User user){
        if (dao.getByUsername(user.getUsername())==null) {
            dao.register(user);
            Map<String, Object> result = JSON.parseObject(JSON.toJSONString(user), new TypeReference<Map<String, Object>>() {});
            result.put("id", result.get("id").toString());
            result.put("url", "https://SERVER_IP_PLACEHOLDER/v2/users/"+result.get("username"));
            result.put("courses_url", "https://SERVER_IP_PLACEHOLDER/v2/users/"+result.get("username")+"/courses");
            return result;
        }else{
            return null;
        }
        
    }

    public Map<String, Object> getByUsername(String username){
        User user = dao.getByUsername(username);
        if (user!=null) {
            Map<String, Object> result = JSON.parseObject(JSON.toJSONString(user), new TypeReference<Map<String, Object>>() {});
            result.put("id", result.get("id").toString());
            result.put("url", "https://SERVER_IP_PLACEHOLDER/v2/users/"+result.get("username"));
            result.put("courses_url", "https://SERVER_IP_PLACEHOLDER/v2/users/"+result.get("username")+"/courses");
            return result;
        }else{
            return null;
        }
    }

    public Map<String, Object> login(User user){
        User resultUser = dao.login(user);
        if (resultUser!=null){ 
            Map<String, Object> result = JSON.parseObject(JSON.toJSONString(resultUser), new TypeReference<Map<String, Object>>() {});
            result.put("id", result.get("id").toString());
            result.put("url", "https://SERVER_IP_PLACEHOLDER/v2/users/"+result.get("username"));
            result.put("courses_url", "https://SERVER_IP_PLACEHOLDER/v2/users/"+result.get("username")+"/courses");
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
}
