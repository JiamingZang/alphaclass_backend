package com.imct.alphaclass.service;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

/**
 * 课程域访问与公共响应组装：user/course/keyword 逐级归属校验（404 兜底）、
 * URL 拼装与 user 子对象统一组装。各 Service 复用它，避免 requireXxx/toUserMap 重复实现。
 */
@Service
@RequiredArgsConstructor
public class AccessService {
    private final UserDAO userdao;
    private final CourseDAO coursedao;
    private final KeywordDAO keyworddao;

    /** 对外 URL 前缀（部署时用 APP_BASE_URL 覆盖，代码不写死具体环境地址） */
    @Value("${app.base-url:http://localhost:8080/v2}")
    private String baseUrl;

    /** 用户不存在时抛 404（替代链式 NPE） */
    public User requireUser(String username) {
        User user = userdao.getByUsername(username);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        return user;
    }

    /** user/course 任一不存在时抛 404 */
    public Course requireCourse(String ownername, String coursename) {
        User user = requireUser(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        if (course == null) {
            throw new ServiceException(Constants.CODE_404, "课程不存在");
        }
        return course;
    }

    /** user/course/keyword 任一不存在时抛 404 */
    public Keyword requireKeyword(String ownername, String coursename, String keywordname) {
        Course course = requireCourse(ownername, coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        if (keyword == null) {
            throw new ServiceException(Constants.CODE_404, "关键词不存在");
        }
        return keyword;
    }

    // ---------- URL 拼装（所有响应链接统一走这里，格式与历史接口一致） ----------

    public String userUrl(String username) {
        return baseUrl + "/users/" + username;
    }

    public String userCoursesUrl(String username) {
        return baseUrl + "/users/" + username + "/courses";
    }

    /** 课程链接（无 /courses 前缀，选课接口 course_url 历史格式） */
    public String courseUrl(String username, String coursename) {
        return baseUrl + "/" + username + "/" + coursename;
    }

    /** 课程链接（带 /courses 前缀，课程修改接口 url 历史格式） */
    public String courseDetailUrl(String username, String coursename) {
        return baseUrl + "/courses/" + username + "/" + coursename;
    }

    /** 关键词链接（无 /courses 前缀，关键词响应 url 历史格式） */
    public String keywordUrl(String username, String coursename, String keywordname) {
        return baseUrl + "/" + username + "/" + coursename + "/" + keywordname;
    }

    public String keywordsUrl(String username, String coursename) {
        return baseUrl + "/courses/" + username + "/" + coursename + "/keywords";
    }

    public String anchorsUrl(String username, String coursename) {
        return baseUrl + "/courses/" + username + "/" + coursename + "/anchors";
    }

    /** user 子对象公共组装：password 移除 + url 填充 + id 转字符串（所有嵌套 user 统一） */
    public Map<String, Object> toUserMap(User user) {
        Map<String, Object> result = MapUtils.toMap(user);
        result.put("url", userUrl(user.getUsername()));
        result.put("id", result.get("id").toString());
        result.remove("password");
        return result;
    }
}
