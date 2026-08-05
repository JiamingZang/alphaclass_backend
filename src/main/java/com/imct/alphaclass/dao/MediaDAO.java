package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.Media;

@Mapper
public interface MediaDAO {
    @Select("select * from media where kid = #{kid}")
    public List<Map<String, Object>> getAllMediasByKid(int kid);

    @Select("select id,name,type,assetid,anchorid,kid,style,color_r,color_g,color_b from media where id = #{id}")
    public Media getMediaById(int id);

    @Update("update media set name = #{new_name},type= #{new_type}, assetid= #{new_asset_id},anchorid= #{new_anchor_id},style=#{style},color_r=#{color_r},color_g=#{color_g},color_b=#{color_b} where id = #{id}")
    public int updateMediaById(String new_name, String new_type, Integer new_asset_id, int new_anchor_id, String style,
            float color_r, float color_g, float color_b, int id);

    @Delete("delete from media where id = #{id}")
    public boolean deleteMediaById(int id);

    @Insert("Insert into media (name, type,assetid,anchorid,kid,style,color_r,color_g,color_b) values(#{name},#{type},#{assetid},#{anchorid},#{kid},#{style},#{color_r},#{color_g},#{color_b})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addMedia(Media media);

}
