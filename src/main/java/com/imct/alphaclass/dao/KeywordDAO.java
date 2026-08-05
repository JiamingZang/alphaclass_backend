package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.Keyword;

@Mapper
public interface KeywordDAO {
    @Select("select * from keyword where cid = #{cid}")
    public List<Map<String, Object>> getAllKeywordsByCid(int cid);

    @Select("select id,keyword from keyword where cid = #{cid} and keyword = #{name}")
    public Keyword getKeywordByCidAndName(int cid, String name);

    @Select("select * from keyword where id = #{id}")
    public Keyword getKeywordById(int id);

    @Update("update keyword set keyword = #{new_keyword} where cid = #{cid} and keyword = #{name}")
    public int updateKeywordByCidAndName(String new_keyword,int cid, String name);

    @Delete("delete from keyword where cid = #{cid} and keyword = #{name}")
    public boolean deleteKeywordByCidAndName(int cid, String name);

    @Insert("Insert into keyword (cid, keyword) values(#{cid},#{keyword})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addKeyword(Keyword keyword);
}
