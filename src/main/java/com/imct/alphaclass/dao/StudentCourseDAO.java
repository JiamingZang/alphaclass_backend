package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.imct.alphaclass.bean.StudentCourse;

@Mapper
public interface StudentCourseDAO {
    @Select("select * from student_course where cid = #{cid}")
    public List<Map<String, Object>> getAllByCid(int cid);

    @Select("select * from student_course where sid = #{sid}")
    public List<Map<String, Object>> getAllBySid(int sid);

    @Select("select * from student_course where cid = #{cid} and sid = #{sid}")
    public StudentCourse getBySidAndCid(int cid, int sid);

    @Insert("Insert into student_course (cid, sid) values(#{cid},#{sid})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addStudentCourse(StudentCourse studentCourse);

    @Delete("delete from student_course where cid = #{cid} and sid = #{sid}")
    public boolean deleteCourseByUidAndName(int cid, int sid);

}
