package com.imct.alphaclass.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
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
public class StudentCourseService {
    @Resource
    private StudentCourseDAO dao;
    @Resource
    private CourseDAO coursedao;
    @Resource
    private UserDAO userdao;

    @Value("${app.base-url:https://SERVER_IP_PLACEHOLDER/v2}")
    private String baseUrl;

    public List<Map<String, Object>> getAllStudentsByCourse(String ownername,String coursename){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        List<Map<String, Object>> all_scresult = dao.getAllByCid(course.getId());
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String,Object> sc : all_scresult) {
            User u = userdao.getById(Integer.valueOf(sc.get("sid").toString()));
            result.add(toUserMap(u));
        }
        return result;
    }

    @Transactional
    public void addStudentsByUsername(List<String> students,String ownername,String coursename){
        User owner = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(owner.getId(), coursename);
        int courseid = course.getId();
        for (String studentname : students) {
            User stu = userdao.getByUsername(studentname);
            StudentCourse sc = new StudentCourse();
            sc.setSid(stu.getId());
            sc.setCid(courseid);
            dao.addStudentCourse(sc);
        }
    }

    @Transactional
    public void deleteStudentsByUsername(List<String> students,String ownername,String coursename){
        User owner = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(owner.getId(), coursename);
        int courseid = course.getId();
        for (String studentname : students) {
            User stu = userdao.getByUsername(studentname);
            dao.deleteCourseByUidAndName(courseid, stu.getId());
        }
    }

    public List<Map<String, Object>> getLoginUserCourses(int sid){
        List<Map<String, Object>> all_scresult = dao.getAllBySid(sid);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String,Object> sc : all_scresult) {
            Course c = coursedao.getCourseById(Integer.valueOf(sc.get("cid").toString()));
            Map<String, Object> cresult = MapUtils.toMap(c);
            User u = userdao.getById(Integer.valueOf(cresult.get("uid").toString()));
            Map<String, Object> uresult = toUserMap(u);
            cresult.remove("uid");cresult.remove("created_at");cresult.remove("updated_at");
            cresult.put("user", uresult);
            cresult.put("course_url", baseUrl + "/"+uresult.get("username")+"/"+cresult.get("name"));
            cresult.put("id", cresult.get("id").toString());
            result.add(cresult);
        }
        return result;
    }

    /** 填充 user 的 url（baseUrl 可配置），并移除密码 */
    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> uresult = MapUtils.toMap(u);
        uresult.remove("password");
        uresult.put("url", baseUrl + "/users/" + uresult.get("username"));
        uresult.put("id", uresult.get("id").toString());
        return uresult;
    }
}
