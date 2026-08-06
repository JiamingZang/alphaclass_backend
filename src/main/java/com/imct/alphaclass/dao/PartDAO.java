package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.imct.alphaclass.bean.Part;

@Mapper
public interface PartDAO {
    @Select("select * from media_parts where media_id = #{media_id}")
    public List<Map<String, Object>> getAllByMediaID(int media_id);

    @Insert("Insert into media_parts (media_id,part_name,part_index,part_order,originpos_x,originpos_y,originpos_z,origineuler_x,origineuler_y,origineuler_z,targetpos_x,targetpos_y,targetpos_z,targeteuler_x,targeteuler_y,targeteuler_z) values(#{media_id},#{part_name},#{part_index},#{part_order},#{originpos_x},#{originpos_y},#{originpos_z},#{origineuler_x},#{origineuler_y},#{origineuler_z},#{targetpos_x},#{targetpos_y},#{targetpos_z},#{targeteuler_x},#{targeteuler_y},#{targeteuler_z})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addPart(Part part);

    @Delete("delete from media_parts where media_id = #{media_id}")
    public boolean deletePartsByMediaID(int media_id);
}