package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.MediaModel;

@Mapper
public interface MediaModelDAO {
    @Select("select * from media_model where id = #{id}")
    public MediaModel getModelinfoById(int id);

    @Update("update media_model set anime_to_play = #{new_anime_to_play},scale_x= #{new_scale_x}, scale_y= #{new_scale_y},scale_z= #{new_scale_z} where id = #{id}")
    public int updateModelinfoById(String new_anime_to_play,float new_scale_x,float new_scale_y,float new_scale_z,int id);

    @Insert("Insert into media_model (id, anime_to_play,scale_x, scale_y, scale_z) values(#{id},#{anime_to_play},#{scale_x},#{scale_y},#{scale_z})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addModelinfo(MediaModel modelinfo);
    
}