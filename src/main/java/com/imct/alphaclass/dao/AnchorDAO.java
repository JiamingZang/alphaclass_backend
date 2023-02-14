package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.Anchor;

@Mapper
public interface AnchorDAO {
    @Select("select * from anchor where cid = #{cid}")
    public List<Map<String, Object>> getAllByCid(int cid);
    
    @Select("select * from anchor where id = #{id}")
    public Anchor getAnchorById(int id);
    
    @Insert("Insert into anchor (cid,name,pos_x,pos_y,pos_z,euler_x,euler_y,euler_z) values(#{cid},#{name},#{pos_x},#{pos_y},#{pos_z},#{euler_x},#{euler_y},#{euler_z})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addAnchor(Anchor anchor);

    @Delete("delete from anchor where id = #{id}")
    public boolean deleteAnchorById(int id);

    @Update("update anchor set name = #{name}, pos_x = #{pos_x}, pos_y = #{pos_y},pos_z = #{pos_z},euler_x = #{euler_x},euler_y = #{euler_y},euler_z = #{euler_z} where id = #{id}")
    public int updateAnchorById(String name,float pos_x,float pos_y,float pos_z,float euler_x,float euler_y,float euler_z, int id);
}
