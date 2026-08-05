package com.imct.alphaclass.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Animation;
import com.imct.alphaclass.bean.Part;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.Media;
import com.imct.alphaclass.bean.MediaModel;
import com.imct.alphaclass.bean.MediaTranslation;
import com.imct.alphaclass.bean.MediaWiki;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.AnchorDAO;
import com.imct.alphaclass.dao.AnimationDAO;
import com.imct.alphaclass.dao.PartDAO;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.dao.MediaDAO;
import com.imct.alphaclass.dao.MediaModelDAO;
import com.imct.alphaclass.dao.MediaTranslationDAO;
import com.imct.alphaclass.dao.MediaWikiDAO;
import com.imct.alphaclass.dao.UserDAO;

@Service
public class MediaService {
    @Resource
    private MediaDAO dao;
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
    private MediaModelDAO mediamodeldao;
    @Resource
    private AnimationDAO animationDAO;
    @Resource
    private PartDAO partDAO;
    @Resource
    private MediaTranslationDAO mediaTranslationDAO;
    @Resource
    private MediaWikiDAO mediaWikiDAO;

    public Map<String, Object> addMediaByKeyword(String ownername, String coursename, String keywordname,
            Map<String, Object> params) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = new Media();
        media.setName(params.get("name").toString());
        media.setType(params.get("type").toString());
        media.setStyle(params.get("style").toString());
        Map<String, Object> color = (Map<String, Object>) params.get("color");
        media.setColor_r(Float.parseFloat("".equals(color.get("r").toString()) ? "0.0" : color.get("r").toString()));
        media.setColor_g(Float.parseFloat("".equals(color.get("g").toString()) ? "0.0" : color.get("g").toString()));
        media.setColor_b(Float.parseFloat("".equals(color.get("b").toString()) ? "0.0" : color.get("b").toString()));
        media.setAssetid(params.get("asset_id") == null ? null : Integer
                .parseInt(params.get("asset_id").toString()));
        media.setAnchorid(Integer
                .parseInt("".equals(params.get("anchor_id").toString()) ? "0" : params.get("anchor_id").toString()));
        media.setKid(keyword.getId());
        MediaModel mediaModel = new MediaModel();
        List<String> animations = new ArrayList<String>();
        List<Map<String, Object>> parts = new ArrayList<Map<String,Object>>();

        // 如果没有model_info就直接转为asset对象
        if (params.get("media_model") != null) {
            // 试一下能不能转成
            // asset = JSON.parseObject(params,new TypeReference<Asset>() {});
            Map<String, Object> media_model = (Map<String, Object>) params.get("media_model");
            mediaModel.setAnime_to_play(media_model.get("anime_to_play").toString());
            Map<String, Object> scale = (Map<String, Object>) media_model.get("scale");
            mediaModel.setScale_x(Float.valueOf(scale.get("scale_x").toString()));
            mediaModel.setScale_y(Float.valueOf(scale.get("scale_y").toString()));
            mediaModel.setScale_z(Float.valueOf(scale.get("scale_z").toString()));

            // 获取animation名称列表
            animations = (ArrayList<String>) media_model.get("animations");
            // parts = (ArrayList<Part>) media_model.get("parts");
            if (media_model.get("parts") != null) {
                parts = (ArrayList<Map<String, Object>>) media_model.get("parts");
            }
        }

        dao.addMedia(media);
        media = dao.getMediaById(media.getId());

        // 添加modelinfo
        if (params.get("media_model") != null) {
            int mid = media.getId();
            mediaModel.setId(mid);
            mediamodeldao.addModelinfo(mediaModel);
            // 添加animation
            for (String animationName : animations) {
                Animation tempAnimation = new Animation();
                tempAnimation.setName(animationName);
                tempAnimation.setMid(mid);
                animationDAO.addAnimation(tempAnimation);
            }
            // 添加part
            for (Map<String, Object> partmessage : parts) {
                Part part = new Part();
                part.setMediaid(Integer.parseInt(partmessage.get("media_id").toString()));
                part.setPartName(partmessage.get("name").toString());
                part.setPart_index(Integer
                        .parseInt(partmessage.get("part_index").toString()));
                part.setPart_order(Integer
                        .parseInt(partmessage.get("part_order").toString()));
                Map<String, Object> originpos = (Map<String, Object>) partmessage.get("origin_pos");
                part.setOriginPos_x(Float.valueOf(originpos.get("pos_x").toString()));
                part.setOriginPos_y(Float.valueOf(originpos.get("pos_y").toString()));
                part.setOriginPos_z(Float.valueOf(originpos.get("pos_z").toString()));
                Map<String, Object> origineuler = (Map<String, Object>) partmessage.get("origin_euler");
                part.setOriginEuler_x(Float.valueOf(origineuler.get("euler_x").toString()));
                part.setOriginEuler_y(Float.valueOf(origineuler.get("euler_y").toString()));
                part.setOriginEuler_z(Float.valueOf(origineuler.get("euler_z").toString()));
                Map<String, Object> targetpos = (Map<String, Object>) partmessage.get("target_pos");
                part.setTargetPos_x(Float.valueOf(targetpos.get("pos_x").toString()));
                part.setTargetPos_y(Float.valueOf(targetpos.get("pos_y").toString()));
                part.setTargetPos_z(Float.valueOf(targetpos.get("pos_z").toString()));
                Map<String, Object> targeteuler = (Map<String, Object>) partmessage.get("target_euler");
                part.setTargetEuler_x(Float.valueOf(targeteuler.get("euler_x").toString()));
                part.setTargetEuler_y(Float.valueOf(targeteuler.get("euler_y").toString()));
                part.setTargetEuler_z(Float.valueOf(targeteuler.get("euler_z").toString()));

                partDAO.addPart(part);
            }
        }

        Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(media), new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> tempasset = null; 
        if (media.getAssetid()!=null) {
            tempasset = JSON.parseObject(JSON.toJSONString(assetdao.getAssetById(media.getAssetid())),
            new TypeReference<Map<String, Object>>() {
            });
            tempasset.remove("uid");
            tempasset.remove("deleted_at");
            tempasset.put("id", tempasset.get("id").toString());
        }
        Map<String, Object> tempanchor = JSON.parseObject(
                JSON.toJSONString(anchordao.getAnchorById(media.getAnchorid())),
                new TypeReference<Map<String, Object>>() {
                });
        tempanchor.remove("pos_x");
        tempanchor.remove("pos_y");
        tempanchor.remove("pos_z");
        tempanchor.remove("euler_x");
        tempanchor.remove("euler_y");
        tempanchor.remove("euler_z");
        tempanchor.remove("cid");
        ac.remove("kid");
        ac.remove("anchorid");
        ac.remove("assetid");

        tempanchor.put("id", tempanchor.get("id").toString());

        Map<String, Object> tempcolor = new HashMap<String, Object>();
        tempcolor.put("r", media.getColor_r());
        tempcolor.put("g", media.getColor_g());
        tempcolor.put("b", media.getColor_b());
        ac.remove("color_r");
        ac.remove("color_g");
        ac.remove("color_b");
        ac.remove("kid");
        ac.remove("anchorid");
        ac.remove("assetid");

        if (media.getAssetid()!=null) {   
            ac.put("asset", tempasset);
        }
        ac.put("anchor", tempanchor);
        ac.put("color", tempcolor);

        if (ac.get("type").toString().equals("model")) {
            MediaModel mm = mediamodeldao.getModelinfoById(Integer.valueOf(ac.get("id").toString()));
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
            if (mm != null) {
                Map<String, Object> scale = new HashMap<String, Object>();
                scale.put("scale_x", mm.getScale_x());
                scale.put("scale_y", mm.getScale_y());
                scale.put("scale_z", mm.getScale_z());
                List<String> animationsList = new ArrayList<String>();
                for (Map<String, Object> animation : animationDAO.getAnimationsByModelinfoId(mm.getId())) {
                    animationsList.add(animation.get("name").toString());
                }

                Map<String, Object> model_info = new HashMap<String, Object>();

                ac.put("anime_to_play", mm.getAnime_to_play());
                ac.put("scale", scale);
                ac.put("animations", animationsList);
                ac.put("parts", parts);
                // model_info.put("anime_to_play", mm.getAnime_to_play());
                // model_info.put("scale", scale);
                // model_info.put("animations", animationsList);
                // // 加入asset中
                // ac.put("media_model", model_info);
            }

        }
        ac.put("id", ac.get("id").toString());
        return ac;
    }

    public Map<String, Object> addMediaTranslationOrWikiByKeyword(String ownername, String coursename, String keywordname,
            Map<String, Object> params) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = new Media();
        media.setName(params.get("name").toString());
        media.setType(params.get("type").toString());
        media.setStyle(params.get("style").toString());
        Map<String, Object> color = (Map<String, Object>) params.get("color");
        media.setColor_r(Float.parseFloat("".equals(color.get("r").toString()) ? "0.0" : color.get("r").toString()));
        media.setColor_g(Float.parseFloat("".equals(color.get("g").toString()) ? "0.0" : color.get("g").toString()));
        media.setColor_b(Float.parseFloat("".equals(color.get("b").toString()) ? "0.0" : color.get("b").toString()));
        media.setAnchorid(Integer
                .parseInt("".equals(params.get("anchor_id").toString()) ? "0" : params.get("anchor_id").toString()));
        media.setKid(keyword.getId());
        dao.addMedia(media);
        media = dao.getMediaById(media.getId());
        Map<String, Object> res = new HashMap<String, Object>();
        if (media.getType().equals("translation")) {
            Map<String, Object> media_translation = (Map<String, Object>) params.get("media_translation");
            MediaTranslation media_translation_obj = new MediaTranslation();
            media_translation_obj.setId(media.getId());
            media_translation_obj.setWord(media_translation.get("word").toString());
            media_translation_obj.setTranslation_english(media_translation.get("translation_english").toString());
            media_translation_obj.setPhonetic_UK(media_translation.get("phonetic_UK").toString());
            media_translation_obj.setPhonetic_US(media_translation.get("phonetic_US").toString());
            media_translation_obj.setSentence_CN(media_translation.get("sentence_CN").toString());
            media_translation_obj.setSentence_EN(media_translation.get("sentence_EN").toString());
            mediaTranslationDAO.addMediaTranslation(media_translation_obj);
            media_translation_obj = mediaTranslationDAO.getMediaTranslationById(media_translation_obj.getId());
            res = JSON.parseObject(JSON.toJSONString(media_translation_obj), new TypeReference<Map<String, Object>>() {});
        }else if (media.getType().equals("wiki")) {
            Map<String, Object> media_wiki = (Map<String, Object>) params.get("media_wiki");
            MediaWiki media_wiki_obj = new MediaWiki();
            media_wiki_obj.setId(media.getId());
            media_wiki_obj.setWord(media_wiki.get("word").toString());
            media_wiki_obj.setWiki(media_wiki.get("wiki").toString());
            mediaWikiDAO.addWikiinfo(media_wiki_obj);
            media_wiki_obj = mediaWikiDAO.getWikiinfoById(media_wiki_obj.getId());
            res = JSON.parseObject(JSON.toJSONString(media_wiki_obj), new TypeReference<Map<String, Object>>() {});
        }

        Map<String, Object> resultmedia = JSON.parseObject(
                JSON.toJSONString(media),
                new TypeReference<Map<String, Object>>() {
                });
        resultmedia.put("media_wiki", res);
        resultmedia.put("color", color);
        resultmedia.remove("color_r");
        resultmedia.remove("color_g");
        resultmedia.remove("color_b");
        resultmedia.remove("kid");
        resultmedia.remove("assetid");
        // resultmedia.remove("id");
        resultmedia.remove("anchorid");
        resultmedia.put("id", resultmedia.get("id").toString());

        Map<String, Object> tempanchor = JSON.parseObject(
                JSON.toJSONString(anchordao.getAnchorById(media.getAnchorid())),
                new TypeReference<Map<String, Object>>() {
                });
        tempanchor.remove("pos_x");
        tempanchor.remove("pos_y");
        tempanchor.remove("pos_z");
        tempanchor.remove("euler_x");
        tempanchor.remove("euler_y");
        tempanchor.remove("euler_z");
        tempanchor.remove("cid");
        tempanchor.put("id", tempanchor.get("id").toString());
        resultmedia.put("anchor", tempanchor);

        return resultmedia;
    }

    public void deleteMediaById(String coursename, String ownername, String keywordname, int media_id) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = dao.getMediaById(media_id);
        if (media.getKid() == keyword.getId()) {
            dao.deleteMediaById(media_id);
        }
    }

    public List<Map<String, Object>> getAllMediasByKeyword(String ownername, String coursename, String keywordname) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        List<Map<String, Object>> all_mediaresult = dao.getAllMediasByKid(keyword.getId());
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

            if (am.get("type").toString().equals("model")) {
                MediaModel mediaModel = mediamodeldao.getModelinfoById(Integer.valueOf(am.get("id").toString()));
                if (mediaModel != null) {
                    Map<String, Object> scale = new HashMap<String, Object>();
                    scale.put("scale_x", mediaModel.getScale_x());
                    scale.put("scale_y", mediaModel.getScale_y());
                    scale.put("scale_z", mediaModel.getScale_z());
                    List<String> animationsList = new ArrayList<String>();
                    for (Map<String, Object> animation : animationDAO.getAnimationsByModelinfoId(mediaModel.getId())) {
                        animationsList.add(animation.get("name").toString());
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

            am.put("asset", tempasset);
            am.put("anchor", tempanchor);
            am.put("color", tempcolor);
            am.put("id", am.get("id").toString());
        }
        return all_mediaresult;
    }

    public Map<String, Object> getMediaById(String coursename, String ownername, String keywordname, int media_id) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = dao.getMediaById(media_id);
        if (media != null && media.getKid() == keyword.getId()) {
            Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(media),
                    new TypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> tempasset = null;
            if (media.getAssetid()!=null) {
                tempasset = JSON.parseObject(
                    JSON.toJSONString(assetdao.getAssetById((int) media.getAssetid())),
                    new TypeReference<Map<String, Object>>() {
                });
                tempasset.remove("uid");
                tempasset.remove("deleted_at");
                tempasset.put("id", tempasset.get("id").toString());
            }
            Map<String, Object> tempanchor = JSON.parseObject(
                    JSON.toJSONString(anchordao.getAnchorById(media.getAnchorid())),
                    new TypeReference<Map<String, Object>>() {
                    });
            tempanchor.remove("pos_x");
            tempanchor.remove("pos_y");
            tempanchor.remove("pos_z");
            tempanchor.remove("euler_x");
            tempanchor.remove("euler_y");
            tempanchor.remove("euler_z");
            tempanchor.remove("cid");
            ac.remove("kid");
            ac.remove("anchorid");
            ac.remove("assetid");

            Map<String, Object> tempcolor = new HashMap<String, Object>();
            tempcolor.put("r", media.getColor_r());
            tempcolor.put("g", media.getColor_g());
            tempcolor.put("b", media.getColor_b());
            ac.remove("color_r");
            ac.remove("color_g");
            ac.remove("color_b");
            ac.remove("kid");
            ac.remove("anchorid");
            ac.remove("assetid");

            tempanchor.put("id", tempanchor.get("id").toString());
            ac.put("asset", tempasset);
            ac.put("anchor", tempanchor);
            ac.put("color", tempcolor);

            if (ac.get("type").toString().equals("model")) {
                MediaModel mediaModel = mediamodeldao.getModelinfoById(Integer.valueOf(ac.get("id").toString()));
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

                    ac.put("anime_to_play", mediaModel.getAnime_to_play());
                    ac.put("scale", scale);
                    ac.put("animations", animationsList);
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
                    ac.put("parts", all_partsresult);
                }
            }else if (ac.get("type").toString().equals("translation")) {
                MediaTranslation mediaTranslation = mediaTranslationDAO.getMediaTranslationById(Integer.valueOf(ac.get("id").toString()));
                if (mediaTranslation != null) {
                    Map<String, Object> res = JSON.parseObject(JSON.toJSONString(mediaTranslation), new TypeReference<Map<String, Object>>() {});                    
                    res.remove("id");
                    ac.put("media_translation", res);
                }
            }else if (ac.get("type").toString().equals("wiki")) {
                MediaWiki mediaWiki = mediaWikiDAO.getWikiinfoById(Integer.valueOf(ac.get("id").toString()));
                if (mediaWiki != null) {
                    Map<String, Object> res = JSON.parseObject(JSON.toJSONString(mediaWiki), new TypeReference<Map<String, Object>>() {});                    
                    res.remove("id");
                    ac.put("media_wiki", res);
                }
            }

            ac.put("id", ac.get("id").toString());
            return ac;
        } else {
            return null;
        }
    }

    public Map<String, Object> modifyMediaById(String coursename, String ownername, String keywordname, int media_id,
            Map<String, Object> params) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media old_media = dao.getMediaById(media_id);
        float color_r;
        float color_g;
        float color_b;
        if (params.get("color") != null) {
            Map<String, Object> color = (Map<String, Object>) params.get("color");
            color_r = Float.parseFloat("".equals(color.get("r").toString()) ? "0.0" : color.get("r").toString());
            color_g = Float.parseFloat("".equals(color.get("g").toString()) ? "0.0" : color.get("g").toString());
            color_b = Float.parseFloat("".equals(color.get("b").toString()) ? "0.0" : color.get("b").toString());
        } else {
            color_r = old_media.getColor_r();
            color_g = old_media.getColor_g();
            color_b = old_media.getColor_b();
        }
        if ((old_media.getType().equals("translation")
            ||old_media.getType().equals("wiki"))
            && params.get("type")!=null
            && !(params.get("type").equals("translation")
                ||params.get("type").equals("wiki"))) {
            if(mediaWikiDAO.getWikiinfoById(media_id)!=null){
                    mediaWikiDAO.deleteWikiinfoById(media_id);
            }
            if(mediaTranslationDAO.getMediaTranslationById(media_id)!=null){
                mediaTranslationDAO.deleteMediaTranslationById(media_id);
            }
            
        }
        dao.updateMediaById(
                params.get("name") == null ? old_media.getName() : params.get("name").toString(),
                params.get("type") == null ? old_media.getType() : params.get("type").toString(),
                params.get("type")!=null 
                && (params.get("type").equals("translation") || 
                    params.get("type").equals("wiki"))|| 
                    params.get("type").equals("assistant")
                    ? null : params.get("asset_id")==null
                    ? old_media.getAssetid() : Integer.valueOf(params.get("asset_id").toString()),
                params.get("anchor_id") == null ? old_media.getAnchorid()
                        : Integer.parseInt(params.get("anchor_id").toString()),
                params.get("style") == null ? old_media.getStyle() : params.get("style").toString(),
                color_r, color_g, color_b, media_id);

        Media media = dao.getMediaById(media_id);

        if (params.get("media_model") != null) {

            MediaModel modelinfo = new MediaModel();
            Map<String, Object> model_info = (Map<String, Object>) params.get("media_model");
            Map<String, Object> scale = (Map<String, Object>) model_info.get("scale");
            if (mediamodeldao.getModelinfoById(media_id) != null) {

                mediamodeldao.updateModelinfoById(
                        model_info.get("anime_to_play").toString(),
                        Float.valueOf(scale.get("scale_x").toString()),
                        Float.valueOf(scale.get("scale_y").toString()),
                        Float.valueOf(scale.get("scale_z").toString()),
                        media_id);
            } else {
                modelinfo.setId(media_id);
                modelinfo.setAnime_to_play(model_info.get("anime_to_play").toString());
                modelinfo.setScale_x(Float.valueOf(scale.get("scale_x").toString()));
                modelinfo.setScale_y(Float.valueOf(scale.get("scale_y").toString()));
                modelinfo.setScale_z(Float.valueOf(scale.get("scale_z").toString()));
                mediamodeldao.addModelinfo(modelinfo);
            }

            // 更改animation表，思路是先删除对应modelinfo所有的animation再加上去
            if (model_info.get("animations") != null) {
                // 获取animation名称列表
                List<String> animations = (ArrayList<String>) model_info.get("animations");
                animationDAO.deleteAnimationByModelinfoId(media_id);

                // 添加animation
                for (String animationName : animations) {
                    Animation tempAnimation = new Animation();
                    tempAnimation.setName(animationName);
                    tempAnimation.setMid(media_id);
                    animationDAO.addAnimation(tempAnimation);
                }
            }
            // 更改part表，思路同上
            if (model_info.get("parts") != null) {
                // 获取part名称列表
                /*
                 * List<Part> parts = (ArrayList<Part>) model_info.get("parts");
                 * // System.out.print(parts);
                 * partDAO.deletePartsByMediaID(media_id);
                 * // 添加part
                 * for (LinkedHashMap<string,Object> part : parts) {
                 * partDAO.addPart(part);
                 * }
                 */
                List<Map<String, Object>> parts = (ArrayList<Map<String, Object>>) model_info.get("parts");
                partDAO.deletePartsByMediaID(media_id);
                for (Map<String, Object> partmessage : parts) {
                    Part part = new Part();
                    part.setMediaid(Integer.parseInt(partmessage.get("media_id").toString()));
                    part.setPartName(partmessage.get("name").toString());
                    part.setPart_index(Integer
                            .parseInt(partmessage.get("part_index").toString()));
                    part.setPart_order(Integer
                            .parseInt(partmessage.get("part_order").toString()));
                    Map<String, Object> originpos = (Map<String, Object>) partmessage.get("origin_pos");
                    part.setOriginPos_x(Float.valueOf(originpos.get("pos_x").toString()));
                    part.setOriginPos_y(Float.valueOf(originpos.get("pos_y").toString()));
                    part.setOriginPos_z(Float.valueOf(originpos.get("pos_z").toString()));
                    Map<String, Object> origineuler = (Map<String, Object>) partmessage.get("origin_euler");
                    part.setOriginEuler_x(Float.valueOf(origineuler.get("euler_x").toString()));
                    part.setOriginEuler_y(Float.valueOf(origineuler.get("euler_y").toString()));
                    part.setOriginEuler_z(Float.valueOf(origineuler.get("euler_z").toString()));
                    Map<String, Object> targetpos = (Map<String, Object>) partmessage.get("target_pos");
                    part.setTargetPos_x(Float.valueOf(targetpos.get("pos_x").toString()));
                    part.setTargetPos_y(Float.valueOf(targetpos.get("pos_y").toString()));
                    part.setTargetPos_z(Float.valueOf(targetpos.get("pos_z").toString()));
                    Map<String, Object> targeteuler = (Map<String, Object>) partmessage.get("target_euler");
                    part.setTargetEuler_x(Float.valueOf(targeteuler.get("euler_x").toString()));
                    part.setTargetEuler_y(Float.valueOf(targeteuler.get("euler_y").toString()));
                    part.setTargetEuler_z(Float.valueOf(targeteuler.get("euler_z").toString()));

                    partDAO.addPart(part);
                }
            }
        }else if(params.get("media_translation")!=null){
            Map<String, Object> media_translation = (Map<String, Object>) params.get("media_translation");
            MediaTranslation old_media_translation = mediaTranslationDAO.getMediaTranslationById(media_id);
            if (old_media_translation != null) {
                mediaTranslationDAO.updateMediaTranslationById(
                        media_translation.get("word") == null ? old_media_translation.getWord()
                                : media_translation.get("word").toString(),
                        media_translation.get("translation_english") == null
                                ? old_media_translation.getTranslation_english()
                                : media_translation.get("translation_english").toString(),
                        media_translation.get("phonetic_UK") == null ? old_media_translation.getPhonetic_UK()
                                : media_translation.get("phonetic_UK").toString(),
                        media_translation.get("phonetic_US") == null ? old_media_translation.getPhonetic_US()
                                : media_translation.get("phonetic_US").toString(),
                        media_translation.get("sentence_CN") == null ? old_media_translation.getSentence_CN()
                                : media_translation.get("sentence_CN").toString(),
                        media_translation.get("sentence_EN") == null ? old_media_translation.getSentence_EN()
                                : media_translation.get("sentence_EN").toString(),
                        media_id);
            } else {
                if(mediaWikiDAO.getWikiinfoById(media_id)!=null){
                    mediaWikiDAO.deleteWikiinfoById(media_id);
                }
                MediaTranslation temp_media_translation = new MediaTranslation();
                temp_media_translation.setWord(media_translation.get("word").toString());
                temp_media_translation.setTranslation_english(media_translation.get("translation_english").toString());
                temp_media_translation.setPhonetic_UK(media_translation.get("phonetic_UK").toString());
                temp_media_translation.setPhonetic_US(media_translation.get("phonetic_US").toString());
                temp_media_translation.setSentence_CN(media_translation.get("sentence_CN").toString());
                temp_media_translation.setSentence_EN(media_translation.get("sentence_EN").toString());
                temp_media_translation.setId(media_id);
                mediaTranslationDAO.addMediaTranslation(temp_media_translation);
            }
        }else if(params.get("media_wiki")!=null){
            Map<String, Object> media_wiki = (Map<String, Object>) params.get("media_wiki");
            MediaWiki old_media_wiki = mediaWikiDAO.getWikiinfoById(media_id);
            if (old_media_wiki != null) {
                mediaWikiDAO.updateWikiinfoById(
                        media_wiki.get("word") == null ? old_media_wiki.getWord()
                                : media_wiki.get("word").toString(),
                        media_wiki.get("wiki") == null ? old_media_wiki.getWiki()
                                : media_wiki.get("wiki").toString(),
                        media_id);
            }else {
                if (mediaTranslationDAO.getMediaTranslationById(media_id)!=null) {
                    mediaTranslationDAO.deleteMediaTranslationById(media_id);
                }
                MediaWiki temp_media_wiki = new MediaWiki();
                temp_media_wiki.setWord(media_wiki.get("word").toString());
                temp_media_wiki.setWiki(media_wiki.get("wiki").toString());
                temp_media_wiki.setId(media_id);
                mediaWikiDAO.addWikiinfo(temp_media_wiki);
            }
            
        }
        if (media != null && media.getKid() == keyword.getId()) {
            System.out.println("111");
            Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(media),
                    new TypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> tempasset = null;
            if (media.getAssetid()!=null) {
                tempasset = JSON.parseObject(
                    JSON.toJSONString(assetdao.getAssetById((int) media.getAssetid())),
                    new TypeReference<Map<String, Object>>() {
                });
                tempasset.remove("uid");
                tempasset.remove("deleted_at");
                tempasset.put("id", tempasset.get("id").toString());
            }
            Map<String, Object> tempanchor = JSON.parseObject(
                    JSON.toJSONString(anchordao.getAnchorById(media.getAnchorid())),
                    new TypeReference<Map<String, Object>>() {
                    });

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
            tempanchor.remove("cid");
            Map<String, Object> tempcolor = new HashMap<String, Object>();
            tempcolor.put("r", media.getColor_r());
            tempcolor.put("g", media.getColor_g());
            tempcolor.put("b", media.getColor_b());
            ac.remove("color_r");
            ac.remove("color_g");
            ac.remove("color_b");
            ac.remove("kid");
            ac.remove("anchorid");
            ac.remove("assetid");

            tempanchor.put("id", tempanchor.get("id").toString());
            ac.put("asset", tempasset);
            ac.put("anchor", tempanchor);
            ac.put("color", tempcolor);

            if (ac.get("type").toString().equals("model")) {
                MediaModel mediaModel = mediamodeldao.getModelinfoById(Integer.valueOf(ac.get("id").toString()));
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
                    ac.put("anime_to_play", mediaModel.getAnime_to_play());
                    ac.put("scale", scale);
                    ac.put("animations", animationsList);
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
                    ac.put("parts", all_partsresult);
                }
            }else if (ac.get("type").toString().equals("translation")) {
                MediaTranslation mediaTranslation = mediaTranslationDAO.getMediaTranslationById(Integer.valueOf(ac.get("id").toString()));
                if (mediaTranslation != null) {
                    Map<String, Object> res = JSON.parseObject(JSON.toJSONString(mediaTranslation), new TypeReference<Map<String, Object>>() {});                    
                    res.remove("id");
                    ac.put("media_translation", res);
                }
            }else if (ac.get("type").toString().equals("wiki")) {
                MediaWiki mediaWiki = mediaWikiDAO.getWikiinfoById(Integer.valueOf(ac.get("id").toString()));
                if (mediaWiki != null) {
                    Map<String, Object> res = JSON.parseObject(JSON.toJSONString(mediaWiki), new TypeReference<Map<String, Object>>() {});                    
                    res.remove("id");
                    ac.put("media_wiki", res);
                }
            }

            ac.put("id", ac.get("id").toString());
            return ac;
        } else {
            return null;
        }
    }
}
