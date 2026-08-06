package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.Asset;

@Mapper
public interface AssetDAO {
   @Select("select * from asset where uid = #{uid}")
    public List<Map<String, Object>> getAllAssetsByUid(int uid);
    //generated是mysql关键字，所以要用`包裹
    @Insert("Insert into asset (uid, name, type, url, thumbnail_url, created_at, updated_at,size,`generated`) values(#{uid},#{name},#{type},#{url},#{thumbnail_url},#{created_at},#{created_at},#{size},#{generated})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addAsset(Asset asset);

    @Select("select * from asset where id = #{id}")
    public Asset getAssetById(int id);

    @Update("update asset set name = #{new_name},updated_at = #{updated_at} where id = #{id} and uid = #{uid}")
    public int updateAssetByIdAndUid(String new_name, String updated_at, int id, int uid);

    @Update("update asset set deleted_at = #{deleted_at} where id = #{id} and uid = #{uid}")
    public int deleteAssetByIdAndUid(String deleted_at, int id, int uid);

    @Select("select * from asset where uid = #{uid} and type = #{type} limit #{page}, #{perpage}")
    public List<Map<String, Object>> getAllAssetsByUidAndPageAndType(int uid,int page,int perpage,String type);
}
