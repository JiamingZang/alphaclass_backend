package com.imct.alphaclass.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CourseService {
    private final CourseDAO dao;
    private final UserDAO userdao;

    @Value("${app.base-url:http://localhost:8080/v2}")
    private String baseUrl;

    /** 查询用户全部课程（user 嵌套、url 填充、时间格式化） */
    public List<Map<String, Object>> getAllByUser(String username){
        User user = requireUser(username);
        DateTimeFormatter simple = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dao.getAllCourseByUid(user.getId()).stream()
                .map(course -> decorateCourse(course, user, simple))
                .collect(Collectors.toList());
    }

    /** 新增课程：uid/时间戳由服务端填充，返回去除 uid 并填充 url 的响应 */
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

    /** 按用户名+课程名查询单个课程（user 嵌套、url 填充）；不存在时返回 null */
    public Map<String, Object> getByUserAndName(String username, String coursename) {
        User user = requireUser(username);
        Course course = dao.getCourseByUidAndName(user.getId(), coursename);
        return course == null ? null : decorateSingleCourse(course, user);
    }

    /** 按课程 id 查询（user 嵌套、url 填充）；不存在时返回 null */
    public Map<String, Object> getById(int id) {
        Course course = dao.getCourseById(id);
        if (course == null) {
            return null;
        }
        User user = userdao.getById(course.getUid());
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        return decorateSingleCourse(course, user);
    }

    /** 修改课程（name/description/cover_url 全量更新），返回更新后的课程响应 */
    public Map<String, Object> modifyByUserAndName(String username, String coursename, Map<String, Object> params){
        User user = requireUser(username);
        dao.updateCourseByUidAndName(
            params.get("name").toString(), params.get("description").toString(), params.get("cover_url").toString(),
            new Timestamp(System.currentTimeMillis()).toString(),user.getId(), coursename);
        Map<String, Object> result = getByUserAndName(username, params.get("name").toString());
        result.put("url", baseUrl + "/courses/"+user.getUsername()+"/"+result.get("name"));
        return result;
        
    }

    /** 删除课程（按用户名+课程名定位，归属由 uid 保证） */
    public void deleteByUserAndName(String username, String Coursename){
        User user = requireUser(username);
        dao.deleteCourseByUidAndName(user.getId(), Coursename);
    }

    /** 填充 course 的 keywords_url/anchors_url 链接（baseUrl 可配置） */
    private void fillCourseUrls(Map<String, Object> course, String username) {
        course.put("keywords_url", baseUrl + "/courses/" + username + "/" + course.get("name") + "/keywords");
        course.put("anchors_url", baseUrl + "/courses/" + username + "/" + course.get("name") + "/anchors");
    }

    /** 复制并装饰单条课程行（id 转字符串/时间格式化/user 嵌套/移除 uid），不污染 DAO 返回的列表 */
    private Map<String, Object> decorateCourse(Map<String, Object> course, User user, DateTimeFormatter simple) {
        Map<String, Object> result = new HashMap<String, Object>(course);
        fillCourseUrls(result, user.getUsername());
        result.put("user", toUserMap(user));
        result.remove("uid");
        result.put("id", result.get("id").toString());
        result.put("created_at", simple.format((LocalDateTime) result.get("created_at")));
        result.put("updated_at", simple.format((LocalDateTime) result.get("updated_at")));
        return result;
    }

    /** 单条课程详情组装：user 嵌套 + url 填充 + uid 移除 + id 转字符串（getByUserAndName/getById 共用） */
    private Map<String, Object> decorateSingleCourse(Course course, User user) {
        Map<String, Object> result = MapUtils.toMap(course);
        fillCourseUrls(result, user.getUsername());
        result.put("user", toUserMap(user));
        result.remove("uid");
        result.put("id", result.get("id").toString());
        return result;
    }

    /** 用户子对象公共组装：url 填充 + 密码移除（所有 user 嵌套统一，避免密码泄露） */
    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> userResult = MapUtils.toMap(user);
        userResult.put("url", baseUrl + "/users/" + user.getUsername());
        userResult.remove("password");
        return userResult;
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
