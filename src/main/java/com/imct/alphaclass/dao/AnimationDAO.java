package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;


import com.imct.alphaclass.bean.Animation;

@Mapper
public interface AnimationDAO {
    @Select("select * from animation where mid = #{mid}")
    public List<Map<String, Object>> getAnimationsByModelinfoId(int mid);

    @Delete("delete from animation where mid = #{mid}")
    public boolean deleteAnimationByModelinfoId(int mid);

    @Insert("Insert into animation (name,mid) values(#{name},#{mid})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addAnimation(Animation animation);
}
