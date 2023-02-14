package com.imct.alphaclass.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.UserDAO;

@Service
public class CourseService {
    @Resource
    private CourseDAO dao;

    @Resource
    private UserDAO userdao;

    public List<Map<String, Object>> getAllByUser(String username){
        User user = userdao.getByUsername(username);
        DateTimeFormatter simple = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> courses= dao.getAllCourseByUid(user.getId());
        for (Map<String,Object> course : courses) {
            course.put("keywords_url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+course.get("name")+"/keywords");
            course.put("anchors_url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+course.get("name")+"/anchors");
            Map<String, Object> userResult = JSON.parseObject(JSON.toJSONString(user), new TypeReference<Map<String, Object>>() {});
            // userResult.put("url", "https://123.56.224.193/courses/"+user.getUsername());
            course.put("user", userResult);
            course.remove("uid");
            course.put("id", course.get("id").toString());
            course.put("created_at", simple.format((LocalDateTime)course.get("created_at")));
            course.put("updated_at", simple.format((LocalDateTime)course.get("updated_at")));
        }
        return courses;
    }

    public Map<String, Object> addCourse(String username,Course course){
        User user = userdao.getByUsername(username);
        course.setUid(user.getId());
        course.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        course.setUpdated_at(new Timestamp(System.currentTimeMillis()).toString());
        dao.addCourse(course);
        Course courseResult = dao.getCourseById(course.getId());
        Map<String, Object> result = JSON.parseObject(JSON.toJSONString(courseResult), new TypeReference<Map<String, Object>>() {});
        result.remove("uid");
        result.put("keywords_url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+result.get("name")+"/keywords");
        result.put("anchors_url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+result.get("name")+"/anchors");
        return result;
    }

    public Map<String, Object> getByUserAndName(String username,String coursename){
        User user = userdao.getByUsername(username);
        Course course = dao.getCourseByUidAndName(user.getId(),coursename);
        // DateTimeFormatter simple = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (course!=null) {
            Map<String, Object> result = JSON.parseObject(JSON.toJSONString(course), new TypeReference<Map<String, Object>>() {});
            result.put("keywords_url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+result.get("name")+"/keywords");
            result.put("anchors_url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+result.get("name")+"/anchors");
            Map<String, Object> userResult = JSON.parseObject(JSON.toJSONString(user), new TypeReference<Map<String, Object>>() {});
            userResult.put("url", "https://123.56.224.193/users/"+user.getUsername());
            result.put("user", userResult);
            result.remove("uid");
            result.put("id", result.get("id").toString());

            // result.put("created_at", simple.format((LocalDateTime)result.get("created_at")));
            // result.put("updated_at", simple.format((LocalDateTime)result.get("updated_at")));
            return result;
        }else{
            return null;
        }
    } 

    public Map<String, Object> getById(int id){
        Course course = dao.getCourseById(id);
        if(course != null){

            User user = userdao.getById(course.getUid());
            Map<String, Object> result = JSON.parseObject(JSON.toJSONString(course), new TypeReference<Map<String, Object>>() {});
            result.put("keywords_url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+result.get("name")+"/keywords");
            result.put("anchors_url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+result.get("name")+"/anchors");
            Map<String, Object> userResult = JSON.parseObject(JSON.toJSONString(user), new TypeReference<Map<String, Object>>() {});
            userResult.put("url", "https://123.56.224.193/users/"+user.getUsername());
            userResult.remove("password");
            result.put("user", userResult);
            result.remove("uid");
            result.put("id", result.get("id").toString());
            
            // result.put("created_at", simple.format((LocalDateTime)result.get("created_at")));
            // result.put("updated_at", simple.format((LocalDateTime)result.get("updated_at")));
            return result;
        }else{
            return null;
        }
    } 

    public Map<String, Object> modifyByUserAndName(String username, String Coursename, Map<String, Object> params){
        User user = userdao.getByUsername(username);
        dao.updateCourseByUidAndName(
            params.get("name").toString(), params.get("description").toString(), params.get("cover_url").toString(),
            new Timestamp(System.currentTimeMillis()).toString(),user.getId(), Coursename);
        Map<String, Object> result = getByUserAndName(username, params.get("name").toString());
        result.put("url", "https://123.56.224.193/courses/"+user.getUsername()+"/"+result.get("name"));
        return result;
        
    }

    public void deleteByUserAndName(String username, String Coursename){
        User user = userdao.getByUsername(username);
        dao.deleteCourseByUidAndName(user.getId(), Coursename);
    }
}
