package com.imct.alphaclass.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.User;

@Mapper
public interface UserDAO {
    @Select("select id,username,role,name from user")
    public List<Map<String,Object>> findAll();

    @Insert("Insert into user (username,password,role,name) values(#{username},#{password},#{role},#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void register(User user);

    @Select("select id,username,role,name from user where username = #{username}")
    public User getByUsername(String username);

    // 登录只按用户名+密码校验，不依赖前端传 role（role 缺失时旧 SQL 恒不匹配）
    @Select("select id,username,password,role,name from user where username = #{username} and password = #{password}")
    public User login(User user);

    @Select("select * from user where id = #{id}")
    public User getById(int id);

    // 存量密码迁移专用：全量读取（含密码列）与按 id 更新
    @Select("select id,username,password from user")
    public List<User> findAllWithPassword();

    @Update("update user set password = #{new_password} where id = #{id}")
    public boolean updatePasswordById(String new_password, int id);

    @Update("update user set password = #{new_password} where username = #{username} and password = #{password}")
    public boolean updatePasswordByUsername(String new_password, String username, String password);

    @Update("update user set name = #{new_name} where username = #{username}")
    public boolean updateNameByUsername(String new_name, String username);
    

} 