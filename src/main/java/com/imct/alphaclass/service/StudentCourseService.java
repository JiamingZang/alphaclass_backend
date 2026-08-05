package com.imct.alphaclass.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.StudentCourse;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.StudentCourseDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

@Service
@RequiredArgsConstructor
public class StudentCourseService {
    private final StudentCourseDAO dao;
    private final CourseDAO coursedao;
    private final UserDAO userdao;

    @Value("${app.base-url:http://localhost:8080/v2}")
    private String baseUrl;

    /** 查询课程下全部学生（学生 user 信息嵌套，密码移除、url 填充） */
    public List<Map<String, Object>> getAllStudentsByCourse(String ownername,String coursename){
        Course course = requireCourse(ownername, coursename);
        return dao.getAllByCid(course.getId()).stream()
                .map(sc -> toUserMap(userdao.getById(Integer.valueOf(sc.get("sid").toString()))))
                .collect(Collectors.toList());
    }

    /** 按用户名批量添加学生（循环内逐个校验用户存在，整体事务回滚） */
    @Transactional
    public void addStudentsByUsername(List<String> students,String ownername,String coursename){
        Course course = requireCourse(ownername, coursename);
        int courseid = course.getId();
        students.forEach(studentname -> {
            User stu = requireUser(studentname);
            StudentCourse sc = new StudentCourse();
            sc.setSid(stu.getId());
            sc.setCid(courseid);
            dao.addStudentCourse(sc);
        });
    }

    /** 按用户名批量移除学生（整体事务回滚） */
    @Transactional
    public void deleteStudentsByUsername(List<String> students,String ownername,String coursename){
        Course course = requireCourse(ownername, coursename);
        int courseid = course.getId();
        students.forEach(studentname -> {
            User stu = requireUser(studentname);
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
        Map<String, Object> uresult = toUserMap(userdao.getById(Integer.valueOf(cresult.get("uid").toString())));
        cresult.remove("uid");
        cresult.remove("created_at");
        cresult.remove("updated_at");
        cresult.put("user", uresult);
        cresult.put("course_url", baseUrl + "/" + uresult.get("username") + "/" + cresult.get("name"));
        cresult.put("id", cresult.get("id").toString());
        return cresult;
    }

    /** 填充 user 的 url（baseUrl 可配置），并移除密码 */
    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> uresult = MapUtils.toMap(u);
        uresult.remove("password");
        uresult.put("url", baseUrl + "/users/" + uresult.get("username"));
        uresult.put("id", uresult.get("id").toString());
        return uresult;
    }

    /** user/course 任一不存在时抛 404（替代链式 NPE） */
    private Course requireCourse(String ownername, String coursename) {
        User user = userdao.getByUsername(ownername);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        if (course == null) {
            throw new ServiceException(Constants.CODE_404, "课程不存在");
        }
        return course;
    }

    private User requireUser(String username) {
        User user = userdao.getByUsername(username);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        return user;
    }
}
