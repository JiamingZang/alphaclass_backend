package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.MediaTranslation;

@Mapper
public interface MediaTranslationDAO {
    @Select("select * from media_translation where id = #{id}")
    public MediaTranslation getMediaTranslationById(int id);

    @Update("update media_translation set word = #{new_word},translation_english= #{new_translation_english}, phonetic_UK= #{new_phonetic_UK},phonetic_US= #{new_phonetic_US},sentence_CN=#{new_sentence_CN},sentence_EN=#{new_sentence_EN} where id = #{id}")
    public int updateMediaTranslationById(String new_word,String new_translation_english,String new_phonetic_UK,String new_phonetic_US,String new_sentence_CN,String new_sentence_EN,int id);

    @Delete("delete from media_translation where id = #{id}")
    public boolean deleteMediaTranslationById(int id);

    @Insert("Insert into media_translation (id, word,translation_english,phonetic_UK,phonetic_US,sentence_CN,sentence_EN) values(#{id},#{word},#{translation_english},#{phonetic_UK},#{phonetic_US},#{sentence_CN},#{sentence_EN})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addMediaTranslation(MediaTranslation translationinfo);
}
