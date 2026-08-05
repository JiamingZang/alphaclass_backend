package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.Part;

@Mapper
public interface PartDAO {
    @Select("select * from media_parts where media_id = #{media_id}")
    public List<Map<String, Object>> getAllByMediaID(int media_id);

    @Select("select * from media_parts where id = #{id}")
    public Part getPartById(int id);

    @Insert("Insert into media_parts (media_id,part_name,part_index,part_order,originpos_x,originpos_y,originpos_z,origineuler_x,origineuler_y,origineuler_z,targetpos_x,targetpos_y,targetpos_z,targeteuler_x,targeteuler_y,targeteuler_z) values(#{media_id},#{part_name},#{part_index},#{part_order},#{originpos_x},#{originpos_y},#{originpos_z},#{origineuler_x},#{origineuler_y},#{origineuler_z},#{targetpos_x},#{targetpos_y},#{targetpos_z},#{targeteuler_x},#{targeteuler_y},#{targeteuler_z})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addPart(Part part);

    @Delete("delete from media_parts where id = #{id}")
    public boolean deletePartById(int id);

    @Delete("delete from media_parts where media_id = #{media_id}")
    public boolean deletePartsByMediaID(int media_id);

    @Update("update media_parts set part_name = #{part_name}, part_index = #{part_index}, part_order = #{part_order}, originpos_x = #{originpos_x}, originpos_y = #{originpos_y},originpos_z = #{originpos_z},origineuler_x = #{origineuler_x},origineuler_y = #{origineuler_y},origineuler_z = #{origineuler_z},targetpos_x = #{targetpos_x},targetpos_y = #{targetpos_y},targetpos_z = #{targetpos_z},targeteuler_x = #{targeteuler_x},targeteuler_y = #{targeteuler_y},targeteuler_z = #{targeteuler_z} where id = #{id}")
    public int updatePartById(String part_name, int part_index, int part_order, float originpos_x, float originpos_y,
            float originpos_z,
            float origineuler_x, float origineuler_y, float origineuler_z, float targetpos_x, float targetpos_y,
            float targetpos_z,
            float targeteuler_x, float targeteuler_y, float targeteuler_z, int id);
}