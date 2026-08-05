package com.imct.alphaclass.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

@Service
public class KeywordService {
    @Resource
    private KeywordDAO dao;
    @Resource
    private UserDAO userdao;
    @Resource
    private CourseDAO coursedao;
    @Resource
    private MediaService mediaservice;

    @Value("${app.base-url:https://SERVER_IP_PLACEHOLDER/v2}")
    private String baseUrl;

    /** 新增关键词：url 由 baseUrl + 路径拼装，cid 不出现在响应中 */
    public Map<String, Object> addKeywordByCourse(String ownername, String coursename, Map<String, Object> params) {
        Course course = requireCourse(ownername, coursename);
        Keyword keyword = new Keyword();
        keyword.setCid(course.getId());
        keyword.setKeyword(params.get("keyword").toString());
        dao.addKeyword(keyword);

        keyword = dao.getKeywordById(keyword.getId());
        Map<String, Object> ac = MapUtils.toMap(keyword);
        ac.put("url", baseUrl + "/" + ownername + "/" + coursename + "/" + keyword.getKeyword());
        ac.remove("cid");
        ac.put("id", ac.get("id").toString());
        return ac;
    }

    /** 删除关键词（按课程归属 + 名称定位） */
    public void deleteKeywordById(String ownername, String coursename, String keyword) {
        Course course = requireCourse(ownername, coursename);
        dao.deleteKeywordByCidAndName(course.getId(), keyword);
    }

    /** 查询课程下全部关键词（medias 嵌套、cid 移除、id 转字符串） */
    public List<Map<String, Object>> getAllKeywordsByCourse(String ownername, String coursename) {
        Course course = requireCourse(ownername, coursename);
        return dao.getAllKeywordsByCid(course.getId()).stream()
                .map(ac -> decorateKeyword(ac))
                .collect(Collectors.toList());
    }

    /** 查询单个关键词（medias 嵌套）；不存在时抛 404 */
    public Map<String, Object> getKeywordByCourse(String ownername, String coursename, String keywordname) {
        Keyword keyword = requireKeyword(ownername, coursename, keywordname);
        return decorateKeyword(MapUtils.toMap(keyword));
    }

    /** 修改关键词名称（未传时沿用旧名），返回更新后的关键词响应 */
    public Map<String, Object> modifyKeywordByCourse(String ownername, String coursename, String keywordname,
            Map<String, Object> params) {
        Keyword keyword = requireKeyword(ownername, coursename, keywordname);
        dao.updateKeywordByCidAndName(
                params.get("keyword") == null ? keyword.getKeyword() : params.get("keyword").toString(),
                keyword.getCid(),
                keywordname);

        Map<String, Object> result = MapUtils.toMap(dao.getKeywordByCidAndName(keyword.getCid(),
                params.get("keyword") == null ? keywordname : params.get("keyword").toString()));
        return decorateKeyword(result);
    }

    /** 复制并装饰单条关键词行（medias 嵌套、cid 移除、id 转字符串） */
    private Map<String, Object> decorateKeyword(Map<String, Object> ac) {
        Map<String, Object> result = new HashMap<String, Object>(ac);
        result.remove("cid");
        result.put("medias", mediaservice.getMediasByKid(((Number) result.get("id")).intValue()));
        result.put("id", result.get("id").toString());
        return result;
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

    /** 关键词不存在时抛 404（user/course 链路由 requireCourse 兜底） */
    private Keyword requireKeyword(String ownername, String coursename, String keywordname) {
        Course course = requireCourse(ownername, coursename);
        Keyword keyword = dao.getKeywordByCidAndName(course.getId(), keywordname);
        if (keyword == null) {
            throw new ServiceException(Constants.CODE_404, "关键词不存在");
        }
        return keyword;
    }
}
