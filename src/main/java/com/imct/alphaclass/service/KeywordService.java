package com.imct.alphaclass.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.utils.MapUtils;

@Service
@RequiredArgsConstructor
public class KeywordService {
    private final KeywordDAO dao;
    private final AccessService access;
    private final MediaService mediaservice;

    /** 新增关键词：url 由 baseUrl + 路径拼装，cid 不出现在响应中 */
    public Map<String, Object> addKeywordByCourse(String ownername, String coursename, Map<String, Object> params) {
        Course course = access.requireCourse(ownername, coursename);
        Keyword keyword = new Keyword();
        keyword.setCid(course.getId());
        keyword.setKeyword(params.get("keyword").toString());
        dao.addKeyword(keyword);

        keyword = dao.getKeywordById(keyword.getId());
        Map<String, Object> ac = MapUtils.toMap(keyword);
        ac.put("url", access.keywordUrl(ownername, coursename, keyword.getKeyword()));
        ac.remove("cid");
        ac.put("id", ac.get("id").toString());
        return ac;
    }

    /** 删除关键词（按课程归属 + 名称定位） */
    public void deleteKeywordById(String ownername, String coursename, String keyword) {
        Course course = access.requireCourse(ownername, coursename);
        dao.deleteKeywordByCidAndName(course.getId(), keyword);
    }

    /** 查询课程下全部关键词（medias 嵌套、cid 移除、id 转字符串） */
    public List<Map<String, Object>> getAllKeywordsByCourse(String ownername, String coursename) {
        Course course = access.requireCourse(ownername, coursename);
        return dao.getAllKeywordsByCid(course.getId()).stream()
                .map(ac -> decorateKeyword(ac))
                .collect(Collectors.toList());
    }

    /** 查询单个关键词（medias 嵌套）；不存在时抛 404 */
    public Map<String, Object> getKeywordByCourse(String ownername, String coursename, String keywordname) {
        Keyword keyword = access.requireKeyword(ownername, coursename, keywordname);
        return decorateKeyword(MapUtils.toMap(keyword));
    }

    /** 修改关键词名称（未传时沿用旧名），返回更新后的关键词响应 */
    public Map<String, Object> modifyKeywordByCourse(String ownername, String coursename, String keywordname,
            Map<String, Object> params) {
        Keyword keyword = access.requireKeyword(ownername, coursename, keywordname);
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
}
