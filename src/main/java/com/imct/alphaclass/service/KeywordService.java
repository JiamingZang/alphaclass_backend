package com.imct.alphaclass.service;

import java.util.List;
import java.util.Map;

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

    public Map<String, Object> addKeywordByCourse(String ownername, String coursename, Map<String, Object> params) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
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

    public void deleteKeywordById(String ownername, String coursename, String keyword) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        dao.deleteKeywordByCidAndName(course.getId(), keyword);
    }

    public List<Map<String, Object>> getAllKeywordsByCourse(String ownername, String coursename) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        List<Map<String, Object>> all_keywordresult = dao.getAllKeywordsByCid(course.getId());
        for (Map<String, Object> ac : all_keywordresult) {
            ac.remove("cid");
            ac.put("medias", mediaservice.getMediasByKid(((Number) ac.get("id")).intValue()));
            ac.put("id", ac.get("id").toString());
        }
        return all_keywordresult;
    }

    public Map<String, Object> getKeywordByCourse(String ownername, String coursename, String keywordname) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = dao.getKeywordByCidAndName(course.getId(), keywordname);
        if (keyword == null) {
            throw new ServiceException(Constants.CODE_404, "关键词不存在");
        }
        Map<String, Object> result = MapUtils.toMap(keyword);
        result.remove("cid");
        result.put("medias", mediaservice.getMediasByKid(keyword.getId()));
        result.put("id", result.get("id").toString());
        return result;
    }

    public Map<String, Object> modifyKeywordByCourse(String ownername, String coursename, String keywordname,
            Map<String, Object> params) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = dao.getKeywordByCidAndName(course.getId(), keywordname);
        dao.updateKeywordByCidAndName(
                params.get("keyword") == null ? keyword.getKeyword() : params.get("keyword").toString(),
                course.getId(),
                keywordname);

        Map<String, Object> result = MapUtils.toMap(dao.getKeywordByCidAndName(course.getId(),
                params.get("keyword") == null ? keywordname : params.get("keyword").toString()));
        result.remove("cid");
        result.put("medias", mediaservice.getMediasByKid(keyword.getId()));
        result.put("id", result.get("id").toString());
        return result;
    }
}
