package com.imct.alphaclass.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.StudentCourse;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.StudentCourseDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.utils.MapUtils;

@Service
@RequiredArgsConstructor
public class StudentCourseService {
    private final StudentCourseDAO dao;
    private final CourseDAO coursedao;
    private final UserDAO userdao;
    private final AccessService access;

    /** 查询课程下全部学生（学生 user 信息嵌套，密码移除、url 填充） */
    public List<Map<String, Object>> getAllStudentsByCourse(String ownername,String coursename){
        Course course = access.requireCourse(ownername, coursename);
        return dao.getAllByCid(course.getId()).stream()
                .map(sc -> access.toUserMap(userdao.getById(Integer.valueOf(sc.get("sid").toString()))))
                .collect(Collectors.toList());
    }

    /** 按用户名批量添加学生（循环内逐个校验用户存在，整体事务回滚） */
    @Transactional
    public void addStudentsByUsername(List<String> students,String ownername,String coursename){
        Course course = access.requireCourse(ownername, coursename);
        int courseid = course.getId();
        students.forEach(studentname -> {
            User stu = access.requireUser(studentname);
            StudentCourse sc = new StudentCourse();
            sc.setSid(stu.getId());
            sc.setCid(courseid);
            dao.addStudentCourse(sc);
        });
    }

    /** 按用户名批量移除学生（整体事务回滚） */
    @Transactional
    public void deleteStudentsByUsername(List<String> students,String ownername,String coursename){
        Course course = access.requireCourse(ownername, coursename);
        int courseid = course.getId();
        students.forEach(studentname -> {
            User stu = access.requireUser(studentname);
            dao.deleteCourseByUidAndName(courseid, stu.getId());
        });
    }

    /** 查询登录学生选修的全部课程（user 嵌套 + course_url 填充） */
    public List<Map<String, Object>> getLoginUserCourses(int sid){
        return dao.getAllBySid(sid).stream()
                .map(sc -> buildCourseMap(Integer.valueOf(sc.get("cid").toString())))
                .collect(Collectors.toList());
    }

    /** 组装单条选课行：course 字段清理 + user 嵌套 + course_url */
    private Map<String, Object> buildCourseMap(int cid) {
        Course c = coursedao.getCourseById(cid);
        Map<String, Object> cresult = MapUtils.toMap(c);
        Map<String, Object> uresult = access.toUserMap(userdao.getById(Integer.valueOf(cresult.get("uid").toString())));
        cresult.remove("uid");
        cresult.remove("created_at");
        cresult.remove("updated_at");
        cresult.put("user", uresult);
        cresult.put("course_url", access.courseUrl(uresult.get("username").toString(), cresult.get("name").toString()));
        cresult.put("id", cresult.get("id").toString());
        return cresult;
    }
}
