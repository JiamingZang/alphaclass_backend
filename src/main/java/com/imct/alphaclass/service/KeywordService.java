package com.imct.alphaclass.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.MediaModel;
import com.imct.alphaclass.bean.MediaTranslation;
import com.imct.alphaclass.bean.MediaWiki;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.bean.Part;
import com.imct.alphaclass.dao.AnchorDAO;
import com.imct.alphaclass.dao.AnimationDAO;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.dao.MediaDAO;
import com.imct.alphaclass.dao.MediaModelDAO;
import com.imct.alphaclass.dao.MediaTranslationDAO;
import com.imct.alphaclass.dao.MediaWikiDAO;
import com.imct.alphaclass.dao.PartDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;

@Service
public class KeywordService {
    @Resource
    private KeywordDAO dao;
    @Resource
    private UserDAO userdao;
    @Resource
    private CourseDAO coursedao;
    @Resource
    private AssetDAO assetdao;
    @Resource
    private AnchorDAO anchordao;
    @Resource
    private MediaDAO mediadao;
    @Resource
    private MediaModelDAO mediamodeldao;
    @Resource
    private PartDAO partDAO;
    @Resource
    private AnimationDAO animationDAO;
    @Resource
    private MediaTranslationDAO mediaTranslationDAO;
    @Resource
    private MediaWikiDAO mediaWikiDAO;


    public Map<String, Object> addKeywordByCourse(String ownername, String coursename, Map<String, Object> params) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = new Keyword();
        keyword.setCid(course.getId());
        keyword.setKeyword(params.get("keyword").toString());
        dao.addKeyword(keyword);

        keyword = dao.getKeywordById(keyword.getId());
        Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(keyword), new TypeReference<Map<String, Object>>() {
        });
        ac.put("url", "https://SERVER_IP_PLACEHOLDER/v2/" + ownername + "/" + coursename + "/" + keyword.getKeyword());
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
            List<Map<String, Object>> all_mediaresult = mediadao.getAllMediasByKid((int) ac.get("id"));
            for (Map<String, Object> am : all_mediaresult) {
                Map<String, Object> tempasset = null;
                if (am.get("assetid")!=null) {
                    tempasset = JSON.parseObject(
                        JSON.toJSONString(assetdao.getAssetById((int) am.get("assetid"))),
                        new TypeReference<Map<String, Object>>() {
                        });
                    tempasset.remove("uid");
                    tempasset.remove("deleted_at");
                    tempasset.put("id", tempasset.get("id").toString());

                }
                Map<String, Object> tempanchor = JSON.parseObject(
                        JSON.toJSONString(anchordao.getAnchorById((int) am.get("anchorid"))),
                        new TypeReference<Map<String, Object>>() {
                        });
                tempanchor.remove("cid");
                am.remove("kid");
                am.remove("anchorid");
                am.remove("assetid");

                Map<String, Object> temppos = new HashMap<String, Object>();
                temppos.put("pos_x", tempanchor.get("pos_x"));
                temppos.put("pos_y", tempanchor.get("pos_y"));
                temppos.put("pos_z", tempanchor.get("pos_z"));
                tempanchor.remove("pos_x");
                tempanchor.remove("pos_y");
                tempanchor.remove("pos_z");
                tempanchor.put("pos", temppos);
                Map<String, Object> tempeuler = new HashMap<String, Object>();
                tempeuler.put("euler_x", tempanchor.get("euler_x"));
                tempeuler.put("euler_y", tempanchor.get("euler_y"));
                tempeuler.put("euler_z", tempanchor.get("euler_z"));
                tempanchor.remove("euler_x");
                tempanchor.remove("euler_y");
                tempanchor.remove("euler_z");
                tempanchor.put("euler", tempeuler);

                tempanchor.put("id", tempanchor.get("id").toString());

                Map<String, Object> tempcolor = new HashMap<String, Object>();
                tempcolor.put("r", am.get("color_r"));
                tempcolor.put("g", am.get("color_g"));
                tempcolor.put("b", am.get("color_b"));
                am.remove("color_r");
                am.remove("color_g");
                am.remove("color_b");
                am.put("asset", tempasset);
                am.put("anchor", tempanchor);
                am.put("color", tempcolor);
                am.put("id", am.get("id").toString());

                if (am.get("type").toString().equals("model")) {
                    MediaModel mediaModel = mediamodeldao.getModelinfoById(Integer.valueOf(am.get("id").toString()));
                    // "model_info": {
                    // "anime_to_play": "take 001",
                    // "scale": {
                    // "scale_x": 1,
                    // "scale_y": 1,
                    // "scale_z": 1
                    // },
                    // "animations": [
                    // "take 001",
                    // "take 002",
                    // "take 003"
                    // ]
                    // }
                    if (mediaModel != null) {
                        Map<String, Object> scale = new HashMap<String, Object>();
                        scale.put("scale_x", mediaModel.getScale_x());
                        scale.put("scale_y", mediaModel.getScale_y());
                        scale.put("scale_z", mediaModel.getScale_z());
                        List<String> animationsList = new ArrayList<String>();
                        for (Map<String, Object> animation : animationDAO
                                .getAnimationsByModelinfoId(mediaModel.getId())) {
                            animationsList.add(animation.get("name").toString());
                        }

                        Map<String, Object> model_info = new HashMap<String, Object>();
                        am.put("anime_to_play", mediaModel.getAnime_to_play());
                        am.put("scale", scale);
                        am.put("animations", animationsList);
                        List<Map<String, Object>> all_partsresult = partDAO.getAllByMediaID(mediaModel.getId());
                        for (Map<String, Object> partmessage : all_partsresult) {
                            partmessage.put("name", partmessage.get("part_name").toString());
                            partmessage.remove("part_name");
                            Map<String, Object> originpos = new HashMap<String, Object>();
                            originpos.put("pos_x", Float.valueOf(partmessage.get("originpos_x").toString()));
                            originpos.put("pos_y", Float.valueOf(partmessage.get("originpos_y").toString()));
                            originpos.put("pos_z", Float.valueOf(partmessage.get("originpos_z").toString()));
                            partmessage.put("origin_pos", originpos);
                            partmessage.remove("originpos_x");
                            partmessage.remove("originpos_y");
                            partmessage.remove("originpos_z");
                            Map<String, Object> origineuler = new HashMap<String, Object>();
                            origineuler.put("euler_x", Float.valueOf(partmessage.get("origineuler_x").toString()));
                            origineuler.put("euler_y", Float.valueOf(partmessage.get("origineuler_y").toString()));
                            origineuler.put("euler_z", Float.valueOf(partmessage.get("origineuler_z").toString()));
                            partmessage.put("origin_euler", origineuler);
                            partmessage.remove("origineuler_x");
                            partmessage.remove("origineuler_y");
                            partmessage.remove("origineuler_z");
                            Map<String, Object> targetpos = new HashMap<String, Object>();
                            targetpos.put("pos_x", Float.valueOf(partmessage.get("targetpos_x").toString()));
                            targetpos.put("pos_y", Float.valueOf(partmessage.get("targetpos_y").toString()));
                            targetpos.put("pos_z", Float.valueOf(partmessage.get("targetpos_z").toString()));
                            partmessage.put("target_pos", targetpos);
                            partmessage.remove("targetpos_x");
                            partmessage.remove("targetpos_y");
                            partmessage.remove("targetpos_z");
                            Map<String, Object> targeteuler = new HashMap<String, Object>();
                            targeteuler.put("euler_x", Float.valueOf(partmessage.get("targeteuler_x").toString()));
                            targeteuler.put("euler_y", Float.valueOf(partmessage.get("targeteuler_y").toString()));
                            targeteuler.put("euler_z", Float.valueOf(partmessage.get("targeteuler_z").toString()));
                            partmessage.put("target_euler", targeteuler);
                            partmessage.remove("targeteuler_x");
                            partmessage.remove("targeteuler_y");
                            partmessage.remove("targeteuler_z");
                        }
                        am.put("parts", all_partsresult);
                    }
                }else if (am.get("type").toString().equals("translation")) {
                    MediaTranslation mediaTranslation = mediaTranslationDAO.getMediaTranslationById(Integer.valueOf(am.get("id").toString()));
                    if (mediaTranslation != null) {
                        Map<String, Object> res = JSON.parseObject(JSON.toJSONString(mediaTranslation), new TypeReference<Map<String, Object>>() {});                    
                        res.remove("id");
                        am.put("media_translation", res);
                    }
                }else if (am.get("type").toString().equals("wiki")) {
                    MediaWiki mediaWiki = mediaWikiDAO.getWikiinfoById(Integer.valueOf(am.get("id").toString()));
                    if (mediaWiki != null) {
                        Map<String, Object> res = JSON.parseObject(JSON.toJSONString(mediaWiki), new TypeReference<Map<String, Object>>() {});                    
                        res.remove("id");
                        am.put("media_wiki", res);
                    }
                }
            }
            ac.put("id", ac.get("id").toString());
            ac.put("medias", all_mediaresult);
        }
        return all_keywordresult;
    }

    public Map<String, Object> getKeywordByCourse(String ownername, String coursename, String keywordname) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = dao.getKeywordByCidAndName(course.getId(), keywordname);
        if (keyword == null) {
            throw new ServiceException("404", "关键词不存在");
        }
        Map<String, Object> result = JSON.parseObject(JSON.toJSONString(keyword),
                new TypeReference<Map<String, Object>>() {
                });
        result.remove("cid");

        List<Map<String, Object>> all_mediaresult = mediadao.getAllMediasByKid((int) result.get("id"));
        for (Map<String, Object> am : all_mediaresult) {
            Map<String, Object> tempasset = null;
            if (am.get("assetid")!=null) {
                tempasset = JSON.parseObject(
                    JSON.toJSONString(assetdao.getAssetById((int) am.get("assetid"))),
                    new TypeReference<Map<String, Object>>() {
                    });
                tempasset.remove("uid");
                tempasset.remove("deleted_at");
                tempasset.put("id", tempasset.get("id").toString());

            }
            Map<String, Object> tempanchor = JSON.parseObject(
                    JSON.toJSONString(anchordao.getAnchorById((int) am.get("anchorid"))),
                    new TypeReference<Map<String, Object>>() {
                    });
            tempanchor.remove("cid");
            am.remove("kid");
            am.remove("anchorid");
            am.remove("assetid");

            Map<String, Object> temppos = new HashMap<String, Object>();
            temppos.put("pos_x", tempanchor.get("pos_x"));
            temppos.put("pos_y", tempanchor.get("pos_y"));
            temppos.put("pos_z", tempanchor.get("pos_z"));
            tempanchor.remove("pos_x");
            tempanchor.remove("pos_y");
            tempanchor.remove("pos_z");
            tempanchor.put("pos", temppos);
            Map<String, Object> tempeuler = new HashMap<String, Object>();
            tempeuler.put("euler_x", tempanchor.get("euler_x"));
            tempeuler.put("euler_y", tempanchor.get("euler_y"));
            tempeuler.put("euler_z", tempanchor.get("euler_z"));
            tempanchor.remove("euler_x");
            tempanchor.remove("euler_y");
            tempanchor.remove("euler_z");
            tempanchor.put("euler", tempeuler);

            tempanchor.put("id", tempanchor.get("id").toString());

            Map<String, Object> tempcolor = new HashMap<String, Object>();
            tempcolor.put("r", am.get("color_r"));
            tempcolor.put("g", am.get("color_g"));
            tempcolor.put("b", am.get("color_b"));
            am.remove("color_r");
            am.remove("color_g");
            am.remove("color_b");
            am.put("asset", tempasset);
            am.put("anchor", tempanchor);
            am.put("color", tempcolor);
            am.put("id", am.get("id").toString());

            if (am.get("type").toString().equals("model")) {
                MediaModel mediaModel = mediamodeldao.getModelinfoById(Integer.valueOf(am.get("id").toString()));
                // "model_info": {
                // "anime_to_play": "take 001",
                // "scale": {
                // "scale_x": 1,
                // "scale_y": 1,
                // "scale_z": 1
                // },
                // "animations": [
                // "take 001",
                // "take 002",
                // "take 003"
                // ]
                // }
                if (mediaModel != null) {
                    Map<String, Object> scale = new HashMap<String, Object>();
                    scale.put("scale_x", mediaModel.getScale_x());
                    scale.put("scale_y", mediaModel.getScale_y());
                    scale.put("scale_z", mediaModel.getScale_z());
                    List<String> animationsList = new ArrayList<String>();
                    for (Map<String, Object> animation : animationDAO.getAnimationsByModelinfoId(mediaModel.getId())) {
                        animationsList.add(animation.get("name").toString());
                    }

                    Map<String, Object> model_info = new HashMap<String, Object>();
                    am.put("anime_to_play", mediaModel.getAnime_to_play());
                    am.put("scale", scale);
                    am.put("animations", animationsList);
                    List<Map<String, Object>> all_partsresult = partDAO.getAllByMediaID(mediaModel.getId());
                    for (Map<String, Object> partmessage : all_partsresult) {
                        partmessage.put("name", partmessage.get("part_name").toString());
                        partmessage.remove("part_name");

                        Map<String, Object> originpos = new HashMap<String, Object>();
                        originpos.put("pos_x", Float.valueOf(partmessage.get("originpos_x").toString()));
                        originpos.put("pos_y", Float.valueOf(partmessage.get("originpos_y").toString()));
                        originpos.put("pos_z", Float.valueOf(partmessage.get("originpos_z").toString()));
                        partmessage.put("origin_pos", originpos);
                        partmessage.remove("originpos_x");
                        partmessage.remove("originpos_y");
                        partmessage.remove("originpos_z");
                        Map<String, Object> origineuler = new HashMap<String, Object>();
                        origineuler.put("euler_x", Float.valueOf(partmessage.get("origineuler_x").toString()));
                        origineuler.put("euler_y", Float.valueOf(partmessage.get("origineuler_y").toString()));
                        origineuler.put("euler_z", Float.valueOf(partmessage.get("origineuler_z").toString()));
                        partmessage.put("origin_euler", origineuler);
                        partmessage.remove("origineuler_x");
                        partmessage.remove("origineuler_y");
                        partmessage.remove("origineuler_z");
                        Map<String, Object> targetpos = new HashMap<String, Object>();
                        targetpos.put("pos_x", Float.valueOf(partmessage.get("targetpos_x").toString()));
                        targetpos.put("pos_y", Float.valueOf(partmessage.get("targetpos_y").toString()));
                        targetpos.put("pos_z", Float.valueOf(partmessage.get("targetpos_z").toString()));
                        partmessage.put("target_pos", targetpos);
                        partmessage.remove("targetpos_x");
                        partmessage.remove("targetpos_y");
                        partmessage.remove("targetpos_z");
                        Map<String, Object> targeteuler = new HashMap<String, Object>();
                        targeteuler.put("euler_x", Float.valueOf(partmessage.get("targeteuler_x").toString()));
                        targeteuler.put("euler_y", Float.valueOf(partmessage.get("targeteuler_y").toString()));
                        targeteuler.put("euler_z", Float.valueOf(partmessage.get("targeteuler_z").toString()));
                        partmessage.put("target_euler", targeteuler);
                        partmessage.remove("targeteuler_x");
                        partmessage.remove("targeteuler_y");
                        partmessage.remove("targeteuler_z");
                    }
                    am.put("parts", all_partsresult);
                }
            }else if (am.get("type").toString().equals("translation")) {
                MediaTranslation mediaTranslation = mediaTranslationDAO.getMediaTranslationById(Integer.valueOf(am.get("id").toString()));
                if (mediaTranslation != null) {
                    Map<String, Object> res = JSON.parseObject(JSON.toJSONString(mediaTranslation), new TypeReference<Map<String, Object>>() {});                    
                    res.remove("id");
                    am.put("media_translation", res);
                }
            }else if (am.get("type").toString().equals("wiki")) {
                MediaWiki mediaWiki = mediaWikiDAO.getWikiinfoById(Integer.valueOf(am.get("id").toString()));
                if (mediaWiki != null) {
                    Map<String, Object> res = JSON.parseObject(JSON.toJSONString(mediaWiki), new TypeReference<Map<String, Object>>() {});                    
                    res.remove("id");
                    am.put("media_wiki", res);
                }
            }
        }

        result.put("medias", all_mediaresult);
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

        Map<String, Object> result = JSON.parseObject(
                JSON.toJSONString(dao.getKeywordByCidAndName(course.getId(),
                        params.get("keyword") == null ? keywordname : params.get("keyword").toString())),
                new TypeReference<Map<String, Object>>() {
                });
        ;
        result.remove("cid");

        List<Map<String, Object>> all_mediaresult = mediadao.getAllMediasByKid((int) result.get("id"));
        for (Map<String, Object> am : all_mediaresult) {

            Map<String, Object> tempasset = JSON.parseObject(
                    JSON.toJSONString(assetdao.getAssetById((int) am.get("assetid"))),
                    new TypeReference<Map<String, Object>>() {
                    });
            tempasset.remove("uid");
            tempasset.remove("deleted_at");
            Map<String, Object> tempanchor = JSON.parseObject(
                    JSON.toJSONString(anchordao.getAnchorById((int) am.get("anchorid"))),
                    new TypeReference<Map<String, Object>>() {
                    });
            tempanchor.remove("cid");
            am.remove("kid");
            am.remove("anchorid");
            am.remove("assetid");

            Map<String, Object> temppos = new HashMap<String, Object>();
            temppos.put("pos_x", tempanchor.get("pos_x"));
            temppos.put("pos_y", tempanchor.get("pos_y"));
            temppos.put("pos_z", tempanchor.get("pos_z"));
            tempanchor.remove("pos_x");
            tempanchor.remove("pos_y");
            tempanchor.remove("pos_z");
            tempanchor.put("pos", temppos);
            Map<String, Object> tempeuler = new HashMap<String, Object>();
            tempeuler.put("euler_x", tempanchor.get("euler_x"));
            tempeuler.put("euler_y", tempanchor.get("euler_y"));
            tempeuler.put("euler_z", tempanchor.get("euler_z"));
            tempanchor.remove("euler_x");
            tempanchor.remove("euler_y");
            tempanchor.remove("euler_z");
            tempanchor.put("euler", tempeuler);

            tempasset.put("id", tempasset.get("id").toString());
            tempanchor.put("id", tempanchor.get("id").toString());
            am.put("asset", tempasset);
            am.put("anchor", tempanchor);
            am.put("id", am.get("id").toString());

            if (am.get("type").toString().equals("model")) {
                MediaModel mediaModel = mediamodeldao.getModelinfoById(Integer.valueOf(am.get("id").toString()));
                // "model_info": {
                // "anime_to_play": "take 001",
                // "scale": {
                // "scale_x": 1,
                // "scale_y": 1,
                // "scale_z": 1
                // },
                // "animations": [
                // "take 001",
                // "take 002",
                // "take 003"
                // ]
                // }
                if (mediaModel != null) {
                    Map<String, Object> scale = new HashMap<String, Object>();
                    scale.put("scale_x", mediaModel.getScale_x());
                    scale.put("scale_y", mediaModel.getScale_y());
                    scale.put("scale_z", mediaModel.getScale_z());
                    List<String> animationsList = new ArrayList<String>();
                    for (Map<String, Object> animation : animationDAO.getAnimationsByModelinfoId(mediaModel.getId())) {
                        animationsList.add(animation.get("name").toString());
                    }

                    Map<String, Object> model_info = new HashMap<String, Object>();
                    am.put("anime_to_play", mediaModel.getAnime_to_play());
                    am.put("scale", scale);
                    am.put("animations", animationsList);
                }
            }
        }
        result.put("medias", all_mediaresult);
        result.put("id", result.get("id").toString());
        return result;
    }
}
