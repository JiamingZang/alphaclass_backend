package com.imct.alphaclass.dao;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.MediaWiki;

@Mapper
public interface MediaWikiDAO {
    @Select("select * from media_wiki where id = #{id}")
    public MediaWiki getWikiinfoById(int id);

    @Update("update media_wiki set word = #{new_word}, wiki = #{new_wiki} where id = #{id}")
    public int updateWikiinfoById(String new_word,String new_wiki,int id);

    @Delete("delete from media_wiki where id = #{id}")
    public boolean deleteWikiinfoById(int id);

    @Insert("Insert into media_wiki (id, word,wiki) values(#{id},#{word},#{wiki})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addWikiinfo(MediaWiki wikiinfo);
    
}