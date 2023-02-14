package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.Course;

@Mapper
public interface CourseDAO {
    @Select("select * from course where uid = #{uid}")
    public List<Map<String, Object>> getAllCourseByUid(int uid);

    @Select("select id,name,description,cover_url,created_at,updated_at from course where uid = #{uid} and name = #{name}")
    public Course getCourseByUidAndName(int uid, String name);

    @Select("select * from course where id = #{id}")
    public Course getCourseById(int id);

    @Update("update course set name = #{new_name}, description = #{new_description}, cover_url = #{new_cover_url},updated_at = #{updated_at} where uid = #{uid} and name = #{name}")
    public int updateCourseByUidAndName(String new_name, String new_description, String new_cover_url,String updated_at,int uid, String name);

    @Delete("delete from course where uid = #{uid} and name = #{name}")
    public boolean deleteCourseByUidAndName(int uid, String name);

    @Insert("Insert into course (uid, name,description,cover_url,created_at,updated_at) values(#{uid},#{name},#{description},#{cover_url},#{created_at},#{created_at})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addCourse(Course course);
}
