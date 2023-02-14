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

    @Select("select id,keyword,wiki,translation_english,phonetic_UK,phonetic_US,sentence_CN,sentence_EN from keyword where cid = #{cid} and keyword = #{name}")
    public Keyword getKeywordByCidAndName(int cid, String name);

    @Select("select * from keyword where id = #{id}")
    public Keyword getKeywordById(int id);

    @Update("update keyword set keyword = #{new_keyword}, translation_english = #{new_translation_english}, phonetic_UK = #{new_phonetic_UK},phonetic_US = #{new_phonetic_US},sentence_CN=#{new_sentence_CN},sentence_EN=#{new_sentence_EN},wiki=#{new_wiki} where cid = #{cid} and keyword = #{name}")
    public int updateKeywordByCidAndName(String new_keyword, String new_translation_english, String new_phonetic_UK,String new_phonetic_US,String new_sentence_CN,String new_sentence_EN,String new_wiki,int cid, String name);

    @Delete("delete from keyword where cid = #{cid} and keyword = #{name}")
    public boolean deleteKeywordByCidAndName(int cid, String name);

    @Insert("Insert into keyword (cid, keyword,wiki,translation_english,phonetic_UK,phonetic_US,sentence_CN,sentence_EN) values(#{cid},#{keyword},#{wiki},#{translation_english},#{phonetic_UK},#{phonetic_US},#{sentence_CN},#{sentence_EN})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addKeyword(Keyword keyword);
}
