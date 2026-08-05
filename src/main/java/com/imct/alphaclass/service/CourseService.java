package com.imct.alphaclass.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

@Service
public class CourseService {
    @Resource
    private CourseDAO dao;

    @Resource
    private UserDAO userdao;

    @Value("${app.base-url:https://SERVER_IP_PLACEHOLDER/v2}")
    private String baseUrl;

    public List<Map<String, Object>> getAllByUser(String username){
        User user = requireUser(username);
        DateTimeFormatter simple = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> courses= dao.getAllCourseByUid(user.getId());
        for (Map<String,Object> course : courses) {
            fillCourseUrls(course, user.getUsername());
            Map<String, Object> userResult = MapUtils.toMap(user);
            course.put("user", userResult);
            course.remove("uid");
            course.put("id", course.get("id").toString());
            course.put("created_at", simple.format((LocalDateTime)course.get("created_at")));
            course.put("updated_at", simple.format((LocalDateTime)course.get("updated_at")));
        }
        return courses;
    }

    public Map<String, Object> addCourse(String username,Course course){
        User user = requireUser(username);
        course.setUid(user.getId());
        course.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        course.setUpdated_at(new Timestamp(System.currentTimeMillis()).toString());
        dao.addCourse(course);
        Course courseResult = dao.getCourseById(course.getId());
        Map<String, Object> result = MapUtils.toMap(courseResult);
        result.remove("uid");
        fillCourseUrls(result, user.getUsername());
        return result;
    }

    public Map<String, Object> getByUserAndName(String username,String coursename){
        User user = requireUser(username);
        Course course = dao.getCourseByUidAndName(user.getId(),coursename);
        if (course!=null) {
            Map<String, Object> result = MapUtils.toMap(course);
            fillCourseUrls(result, user.getUsername());
            Map<String, Object> userResult = MapUtils.toMap(user);
            userResult.put("url", baseUrl + "/users/"+user.getUsername());
            result.put("user", userResult);
            result.remove("uid");
            result.put("id", result.get("id").toString());
            return result;
        }else{
            return null;
        }
    } 

    public Map<String, Object> getById(int id){
        Course course = dao.getCourseById(id);
        if(course != null){
            User user = userdao.getById(course.getUid());
            if (user == null) {
                throw new ServiceException(Constants.CODE_404, "用户不存在");
            }
            Map<String, Object> result = MapUtils.toMap(course);
            fillCourseUrls(result, user.getUsername());
            Map<String, Object> userResult = MapUtils.toMap(user);
            userResult.put("url", baseUrl + "/users/"+user.getUsername());
            userResult.remove("password");
            result.put("user", userResult);
            result.remove("uid");
            result.put("id", result.get("id").toString());
            return result;
        }else{
            return null;
        }
    } 

    public Map<String, Object> modifyByUserAndName(String username, String coursename, Map<String, Object> params){
        User user = requireUser(username);
        dao.updateCourseByUidAndName(
            params.get("name").toString(), params.get("description").toString(), params.get("cover_url").toString(),
            new Timestamp(System.currentTimeMillis()).toString(),user.getId(), coursename);
        Map<String, Object> result = getByUserAndName(username, params.get("name").toString());
        result.put("url", baseUrl + "/courses/"+user.getUsername()+"/"+result.get("name"));
        return result;
        
    }

    public void deleteByUserAndName(String username, String Coursename){
        User user = requireUser(username);
        dao.deleteCourseByUidAndName(user.getId(), Coursename);
    }

    /** 填充 course 的 keywords_url/anchors_url 链接（baseUrl 可配置） */
    private void fillCourseUrls(Map<String, Object> course, String username) {
        course.put("keywords_url", baseUrl + "/courses/" + username + "/" + course.get("name") + "/keywords");
        course.put("anchors_url", baseUrl + "/courses/" + username + "/" + course.get("name") + "/anchors");
    }

    /** 用户不存在时抛 404（替代链式 NPE） */
    private User requireUser(String username) {
        User user = userdao.getByUsername(username);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        return user;
    }
}
