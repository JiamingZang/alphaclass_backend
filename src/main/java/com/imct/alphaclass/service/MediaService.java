package com.imct.alphaclass.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Animation;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.Media;
import com.imct.alphaclass.bean.MediaModel;
import com.imct.alphaclass.bean.MediaTranslation;
import com.imct.alphaclass.bean.MediaWiki;
import com.imct.alphaclass.bean.Part;
import com.imct.alphaclass.bean.User;
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
        List<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();

        // 如果没有 media_model 就直接转为 asset 对象
        if (params.get("media_model") != null) {
            Map<String, Object> media_model = (Map<String, Object>) params.get("media_model");
            mediaModel.setAnime_to_play(media_model.get("anime_to_play").toString());
            Map<String, Object> scale = (Map<String, Object>) media_model.get("scale");
            mediaModel.setScale_x(Float.valueOf(scale.get("scale_x").toString()));
            mediaModel.setScale_y(Float.valueOf(scale.get("scale_y").toString()));
            mediaModel.setScale_z(Float.valueOf(scale.get("scale_z").toString()));

            // 获取 animation 名称列表
            animations = (ArrayList<String>) media_model.get("animations");
            if (media_model.get("parts") != null) {
                parts = (ArrayList<Map<String, Object>>) media_model.get("parts");
            }
        }

        dao.addMedia(media);
        media = dao.getMediaById(media.getId());

        // 添加 modelinfo
        if (params.get("media_model") != null) {
            int mid = media.getId();
            mediaModel.setId(mid);
            mediamodeldao.addModelinfo(mediaModel);
            // 添加 animation
            for (String animationName : animations) {
                Animation tempAnimation = new Animation();
                tempAnimation.setName(animationName);
                tempAnimation.setMid(mid);
                animationDAO.addAnimation(tempAnimation);
            }
            // 添加 part
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

        return buildMediaResponse(media);
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
        } else if (media.getType().equals("wiki")) {
            Map<String, Object> media_wiki = (Map<String, Object>) params.get("media_wiki");
            MediaWiki media_wiki_obj = new MediaWiki();
            media_wiki_obj.setId(media.getId());
            media_wiki_obj.setWord(media_wiki.get("word").toString());
            media_wiki_obj.setWiki(media_wiki.get("wiki").toString());
            mediaWikiDAO.addWikiinfo(media_wiki_obj);
        }

        return buildMediaResponse(media);
    }

    public void deleteMediaById(String coursename, String ownername, String keywordname, int media_id) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = dao.getMediaById(media_id);
        if (media != null && media.getKid() == keyword.getId()) {
            dao.deleteMediaById(media_id);
        }
    }

    public List<Map<String, Object>> getAllMediasByKeyword(String ownername, String coursename, String keywordname) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        List<Map<String, Object>> all_mediaresult = dao.getAllMediasByKid(keyword.getId());
        for (Map<String, Object> am : all_mediaresult) {
            buildMediaResponse(am);
        }
        return all_mediaresult;
    }

    public Map<String, Object> getMediaById(String coursename, String ownername, String keywordname, int media_id) {
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = dao.getMediaById(media_id);
        if (media != null && media.getKid() == keyword.getId()) {
            return buildMediaResponse(media);
        }
        return null;
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
        // 从 translation/wiki 切换到其他类型时，清理旧的扩展数据
        if ((old_media.getType().equals("translation")
                || old_media.getType().equals("wiki"))
                && params.get("type") != null
                && !(params.get("type").equals("translation")
                        || params.get("type").equals("wiki"))) {
            if (mediaWikiDAO.getWikiinfoById(media_id) != null) {
                mediaWikiDAO.deleteWikiinfoById(media_id);
            }
            if (mediaTranslationDAO.getMediaTranslationById(media_id) != null) {
                mediaTranslationDAO.deleteMediaTranslationById(media_id);
            }
        }
        dao.updateMediaById(
                params.get("name") == null ? old_media.getName() : params.get("name").toString(),
                params.get("type") == null ? old_media.getType() : params.get("type").toString(),
                typeHasNoAsset(params.get("type")) ? null
                        : params.get("asset_id") == null ? old_media.getAssetid()
                                : Integer.valueOf(params.get("asset_id").toString()),
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

            // 更改 animation 表，思路是先删除对应 modelinfo 所有的 animation 再加上去
            if (model_info.get("animations") != null) {
                List<String> animations = (ArrayList<String>) model_info.get("animations");
                animationDAO.deleteAnimationByModelinfoId(media_id);

                for (String animationName : animations) {
                    Animation tempAnimation = new Animation();
                    tempAnimation.setName(animationName);
                    tempAnimation.setMid(media_id);
                    animationDAO.addAnimation(tempAnimation);
                }
            }
            // 更改 part 表，思路同上
            if (model_info.get("parts") != null) {
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
        } else if (params.get("media_translation") != null) {
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
                if (mediaWikiDAO.getWikiinfoById(media_id) != null) {
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
        } else if (params.get("media_wiki") != null) {
            Map<String, Object> media_wiki = (Map<String, Object>) params.get("media_wiki");
            MediaWiki old_media_wiki = mediaWikiDAO.getWikiinfoById(media_id);
            if (old_media_wiki != null) {
                mediaWikiDAO.updateWikiinfoById(
                        media_wiki.get("word") == null ? old_media_wiki.getWord()
                                : media_wiki.get("word").toString(),
                        media_wiki.get("wiki") == null ? old_media_wiki.getWiki()
                                : media_wiki.get("wiki").toString(),
                        media_id);
            } else {
                if (mediaTranslationDAO.getMediaTranslationById(media_id) != null) {
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
            return buildMediaResponse(media);
        }
        return null;
    }

    /** translation/wiki/assistant 类型无 asset 关联，assetid 置空 */
    private boolean typeHasNoAsset(Object type) {
        return type != null && (type.equals("translation") || type.equals("wiki") || type.equals("assistant"));
    }

    /**
     * 组装媒体响应：asset/anchor/color 嵌套 + type 专属信息（model/translation/wiki）。
     * 所有媒体接口（增删改查）共用同一组装逻辑，保证响应结构一致。
     */
    private Map<String, Object> buildMediaResponse(Media media) {
        Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(media), new TypeReference<Map<String, Object>>() {
        });
        // asset 嵌套（uid/deleted_at 移除，id 转字符串）
        Map<String, Object> tempasset = null;
        if (media.getAssetid() != null) {
            tempasset = toMap(assetdao.getAssetById(media.getAssetid()));
            tempasset.remove("uid");
            tempasset.remove("deleted_at");
            tempasset.put("id", tempasset.get("id").toString());
        }
        // anchor 嵌套（pos/euler 收拢，cid 移除，id 转字符串）
        Map<String, Object> tempanchor = toMap(anchordao.getAnchorById(media.getAnchorid()));
        tempanchor.remove("cid");
        tempanchor.put("pos", nestVec(tempanchor, "pos"));
        tempanchor.put("euler", nestVec(tempanchor, "euler"));
        tempanchor.put("id", tempanchor.get("id").toString());
        // color 嵌套
        Map<String, Object> tempcolor = new HashMap<String, Object>();
        tempcolor.put("r", media.getColor_r());
        tempcolor.put("g", media.getColor_g());
        tempcolor.put("b", media.getColor_b());
        // 清理内部关联字段
        ac.remove("kid");
        ac.remove("anchorid");
        ac.remove("assetid");
        ac.remove("color_r");
        ac.remove("color_g");
        ac.remove("color_b");
        ac.put("asset", tempasset);
        ac.put("anchor", tempanchor);
        ac.put("color", tempcolor);

        appendTypeSpecific(ac);
        ac.put("id", ac.get("id").toString());
        return ac;
    }

    /**
     * 按 media 类型追加专属信息：
     * model -> anime_to_play/scale/animations/parts；translation -> media_translation；wiki -> media_wiki。
     */
    private void appendTypeSpecific(Map<String, Object> ac) {
        String type = ac.get("type").toString();
        int mediaId = Integer.valueOf(ac.get("id").toString());
        if (type.equals("model")) {
            MediaModel mediaModel = mediamodeldao.getModelinfoById(mediaId);
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
                ac.put("parts", buildParts(partDAO.getAllByMediaID(mediaModel.getId())));
            }
        } else if (type.equals("translation")) {
            MediaTranslation mediaTranslation = mediaTranslationDAO.getMediaTranslationById(mediaId);
            if (mediaTranslation != null) {
                Map<String, Object> res = toMap(mediaTranslation);
                res.remove("id");
                ac.put("media_translation", res);
            }
        } else if (type.equals("wiki")) {
            MediaWiki mediaWiki = mediaWikiDAO.getWikiinfoById(mediaId);
            if (mediaWiki != null) {
                Map<String, Object> res = toMap(mediaWiki);
                res.remove("id");
                ac.put("media_wiki", res);
            }
        }
    }

    /**
     * 转换 parts 行结构：part_name -> name，originpos_x/y/z -> origin_pos 嵌套，
     * targeteuler_x/y/z -> target_euler 嵌套，其余字段类似。
     */
    private List<Map<String, Object>> buildParts(List<Map<String, Object>> all_partsresult) {
        for (Map<String, Object> partmessage : all_partsresult) {
            partmessage.put("name", partmessage.get("part_name").toString());
            partmessage.remove("part_name");
            partmessage.put("origin_pos", nestVec(partmessage, "originpos"));
            partmessage.put("origin_euler", nestVec(partmessage, "origineuler"));
            partmessage.put("target_pos", nestVec(partmessage, "targetpos"));
            partmessage.put("target_euler", nestVec(partmessage, "targeteuler"));
        }
        return all_partsresult;
    }

    /**
     * 将扁平坐标字段（如 pos_x/pos_y/pos_z）收拢为嵌套结构（如 pos）。
     * prefix 为字段前缀（pos/euler/originpos/targetpos 等）。
     */
    private Map<String, Object> nestVec(Map<String, Object> source, String prefix) {
        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put(prefix + "_x", source.get(prefix + "_x"));
        nested.put(prefix + "_y", source.get(prefix + "_y"));
        nested.put(prefix + "_z", source.get(prefix + "_z"));
        source.remove(prefix + "_x");
        source.remove(prefix + "_y");
        source.remove(prefix + "_z");
        return nested;
    }

    private Map<String, Object> toMap(Object bean) {
        return JSON.parseObject(JSON.toJSONString(bean), new TypeReference<Map<String, Object>>() {
        });
    }
}
