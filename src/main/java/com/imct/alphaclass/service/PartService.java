package com.imct.alphaclass.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Part;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.Media;
import com.imct.alphaclass.bean.MediaModel;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.AnchorDAO;
import com.imct.alphaclass.dao.AnimationDAO;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.dao.MediaDAO;
import com.imct.alphaclass.dao.MediaModelDAO;
import com.imct.alphaclass.dao.PartDAO;
import com.imct.alphaclass.dao.UserDAO;

@Service
public class PartService {
    @Resource
    private PartDAO dao;
    @Resource
    private UserDAO userdao;
    @Resource
    private CourseDAO coursedao;
    @Resource
    private AnchorDAO anchordao;
    @Resource
    private AssetDAO assetdao;
    @Resource
    private KeywordDAO keyworddao;
    @Resource
    private MediaDAO mediadao;
    @Resource
    private PartDAO partDAO;

    public Map<String, Object> addPartByMediaModel(String ownername, String coursename, String keywordname,
            String media_id, Map<String, Object> params) {
        // User user = userdao.getByUsername(ownername);
        // Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        // Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(),
        // keywordname);
        Media media = mediadao.getMediaById(Integer.parseInt(media_id));
        Part part = new Part();
        part.setMediaid(Integer.parseInt(media_id));
        part.setPartName(params.get("part_name").toString());
        part.setPart_index(Integer
                .parseInt("".equals(params.get("part_index").toString()) ? "0" : params.get("part_index").toString()));
        part.setPart_order(Integer
                .parseInt("".equals(params.get("part_order").toString()) ? "0" : params.get("part_order").toString()));
        Map<String, Object> originpos = (Map<String, Object>) params.get("originpos");
        part.setOriginPos_x(Float.valueOf(originpos.get("x").toString()));
        part.setOriginPos_y(Float.valueOf(originpos.get("y").toString()));
        part.setOriginPos_z(Float.valueOf(originpos.get("z").toString()));
        Map<String, Object> origineuler = (Map<String, Object>) params.get("origineuler");
        part.setOriginEuler_x(Float.valueOf(origineuler.get("x").toString()));
        part.setOriginEuler_y(Float.valueOf(origineuler.get("y").toString()));
        part.setOriginEuler_z(Float.valueOf(origineuler.get("z").toString()));
        Map<String, Object> targetpos = (Map<String, Object>) params.get("targetpos");
        part.setTargetPos_x(Float.valueOf(targetpos.get("x").toString()));
        part.setTargetPos_y(Float.valueOf(targetpos.get("y").toString()));
        part.setTargetPos_z(Float.valueOf(targetpos.get("z").toString()));
        Map<String, Object> targeteuler = (Map<String, Object>) params.get("targeteuler");
        part.setTargetEuler_x(Float.valueOf(targeteuler.get("x").toString()));
        part.setTargetEuler_y(Float.valueOf(targeteuler.get("y").toString()));
        part.setTargetEuler_z(Float.valueOf(targeteuler.get("z").toString()));

        dao.addPart(part);
        Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(part), new TypeReference<Map<String, Object>>() {
        });
        return ac;

    }

    public void deletePartsByMediaID(String coursename, String ownername, String keywordname, int media_id) {
        dao.deletePartsByMediaID(media_id);
    }

    public List<Map<String, Object>> getAllPartsByMediaID(String ownername, String coursename, String keywordname,
            int media_id) {
        List<Map<String, Object>> all_partsresult = dao.getAllByMediaID(media_id);
        return all_partsresult;
    }
}