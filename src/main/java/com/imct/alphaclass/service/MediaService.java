package com.imct.alphaclass.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Animation;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.Media;
import com.imct.alphaclass.bean.MediaModel;
import com.imct.alphaclass.bean.MediaTranslation;
import com.imct.alphaclass.bean.MediaWiki;
import com.imct.alphaclass.bean.Part;
import com.imct.alphaclass.dao.AnchorDAO;
import com.imct.alphaclass.dao.AnimationDAO;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.MediaDAO;
import com.imct.alphaclass.dao.MediaModelDAO;
import com.imct.alphaclass.dao.MediaTranslationDAO;
import com.imct.alphaclass.dao.MediaWikiDAO;
import com.imct.alphaclass.dao.PartDAO;
import com.imct.alphaclass.utils.MapUtils;

@Service
@RequiredArgsConstructor
public class MediaService {
    /** 媒体类型常量（与数据库 type 字段取值一致） */
    private static final String TYPE_MODEL = "model";
    private static final String TYPE_TRANSLATION = "translation";
    private static final String TYPE_WIKI = "wiki";
    private static final String TYPE_ASSISTANT = "assistant";

    private final MediaDAO dao;
    private final AccessService access;
    private final AnchorDAO anchordao;
    private final AssetDAO assetdao;
    private final MediaModelDAO mediamodeldao;
    private final AnimationDAO animationdao;
    private final PartDAO partdao;
    private final MediaTranslationDAO mediatranslationdao;
    private final MediaWikiDAO mediawikidao;

    /**
     * 按关键词新增媒体：先落 media 主记录，若带 media_model 则同步写
     * modelinfo/animation/part 扩展表；返回与查询接口一致的组装响应。
     */
    @Transactional
    public Map<String, Object> addMediaByKeyword(String ownername, String coursename, String keywordname,
            Map<String, Object> params) {
        Keyword keyword = access.requireKeyword(ownername, coursename, keywordname);
        Media media = new Media();
        media.setName(params.get("name").toString());
        media.setType(params.get("type").toString());
        media.setStyle(params.get("style").toString());
        Map<String, Object> color = (Map<String, Object>) params.get("color");
        media.setColor_r(MapUtils.parseFloat(color, "r"));
        media.setColor_g(MapUtils.parseFloat(color, "g"));
        media.setColor_b(MapUtils.parseFloat(color, "b"));
        media.setAssetid(params.get("asset_id") == null ? null : Integer
                .parseInt(params.get("asset_id").toString()));
        media.setAnchorid(MapUtils.parseInteger(params, "anchor_id"));
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
            animations = (List<String>) media_model.get("animations");
            if (media_model.get("parts") != null) {
                parts = (List<Map<String, Object>>) media_model.get("parts");
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
            animations.forEach(animationName -> {
                Animation tempAnimation = new Animation();
                tempAnimation.setName(animationName);
                tempAnimation.setMid(mid);
                animationdao.addAnimation(tempAnimation);
            });
            // 添加 part
            parts.stream().map(MediaService::toPart).forEach(partdao::addPart);
        }

        return buildMediaResponse(media);
    }

    /**
     * 按关键词新增 translation/wiki 类型媒体：先落 media 主记录，
     * 再按 type 把专属信息写入 media_translation / media_wiki 扩展表。
     */
    @Transactional
    public Map<String, Object> addMediaTranslationOrWikiByKeyword(String ownername, String coursename, String keywordname,
            Map<String, Object> params) {
        Keyword keyword = access.requireKeyword(ownername, coursename, keywordname);
        Media media = new Media();
        media.setName(params.get("name").toString());
        media.setType(params.get("type").toString());
        media.setStyle(params.get("style").toString());
        Map<String, Object> color = (Map<String, Object>) params.get("color");
        media.setColor_r(MapUtils.parseFloat(color, "r"));
        media.setColor_g(MapUtils.parseFloat(color, "g"));
        media.setColor_b(MapUtils.parseFloat(color, "b"));
        media.setAnchorid(MapUtils.parseInteger(params, "anchor_id"));
        media.setKid(keyword.getId());
        dao.addMedia(media);
        media = dao.getMediaById(media.getId());
        // 按媒体类型写入专属扩展表（translation/wiki 二选一）
        if (TYPE_TRANSLATION.equals(media.getType())) {
            Map<String, Object> media_translation = (Map<String, Object>) params.get("media_translation");
            MediaTranslation media_translation_obj = new MediaTranslation();
            media_translation_obj.setId(media.getId());
            media_translation_obj.setWord(media_translation.get("word").toString());
            media_translation_obj.setTranslation_english(media_translation.get("translation_english").toString());
            media_translation_obj.setPhonetic_UK(media_translation.get("phonetic_UK").toString());
            media_translation_obj.setPhonetic_US(media_translation.get("phonetic_US").toString());
            media_translation_obj.setSentence_CN(media_translation.get("sentence_CN").toString());
            media_translation_obj.setSentence_EN(media_translation.get("sentence_EN").toString());
            mediatranslationdao.addMediaTranslation(media_translation_obj);
        } else if (TYPE_WIKI.equals(media.getType())) {
            Map<String, Object> media_wiki = (Map<String, Object>) params.get("media_wiki");
            MediaWiki media_wiki_obj = new MediaWiki();
            media_wiki_obj.setId(media.getId());
            media_wiki_obj.setWord(media_wiki.get("word").toString());
            media_wiki_obj.setWiki(media_wiki.get("wiki").toString());
            mediawikidao.addWikiinfo(media_wiki_obj);
        }

        return buildMediaResponse(media);
    }

    /** 删除媒体：仅当媒体属于该关键词时删除（归属校验，防止越权） */
    public void deleteMediaById(String coursename, String ownername, String keywordname, int media_id) {
        Keyword keyword = access.requireKeyword(ownername, coursename, keywordname);
        Media media = dao.getMediaById(media_id);
        if (media != null && media.getKid() == keyword.getId()) {
            dao.deleteMediaById(media_id);
        }
    }

    /** 查询关键词下全部媒体（内部按 kid 查询，复用 getMediasByKid 统一组装） */
    public List<Map<String, Object>> getAllMediasByKeyword(String ownername, String coursename, String keywordname) {
        Keyword keyword = access.requireKeyword(ownername, coursename, keywordname);
        return getMediasByKid(keyword.getId());
    }

    /**
     * 按 keyword id 返回组装好的 media 列表（asset/anchor/color 嵌套 + type 专属信息）。
     * 供 KeywordService 等复用，保证所有响应结构一致。
     * DAO 返回的是行 Map，这里通过 JSON 中转转成 Media 实体，
     * 避免手写 10+ 个 setter 拷贝，同时让后续组装统一走实体字段。
     */
    public List<Map<String, Object>> getMediasByKid(int kid) {
        return dao.getAllMediasByKid(kid).stream()
                .map(row -> buildMediaResponse(JSON.parseObject(JSON.toJSONString(row), Media.class)))
                .collect(Collectors.toList());
    }

    /** 查询单个媒体：仅当媒体属于该关键词时返回组装响应 */
    public Map<String, Object> getMediaById(String coursename, String ownername, String keywordname, int media_id) {
        Keyword keyword = access.requireKeyword(ownername, coursename, keywordname);
        Media media = dao.getMediaById(media_id);
        if (media != null && media.getKid() == keyword.getId()) {
            return buildMediaResponse(media);
        }
        return null;
    }

    /**
     * 修改媒体：主表字段未传时沿用旧值（部分更新语义）；
     * media_model/media_translation/media_wiki 扩展数据有则更新、无则新增，
     * 并在类型切换时清理不再适用的旧扩展数据。
     */
    @Transactional
    public Map<String, Object> modifyMediaById(String coursename, String ownername, String keywordname, int media_id,
            Map<String, Object> params) {
        Keyword keyword = access.requireKeyword(ownername, coursename, keywordname);
        Media old_media = dao.getMediaById(media_id);
        float color_r;
        float color_g;
        float color_b;
        if (params.get("color") != null) {
            Map<String, Object> color = (Map<String, Object>) params.get("color");
            color_r = MapUtils.parseFloat(color, "r");
            color_g = MapUtils.parseFloat(color, "g");
            color_b = MapUtils.parseFloat(color, "b");
        } else {
            color_r = old_media.getColor_r();
            color_g = old_media.getColor_g();
            color_b = old_media.getColor_b();
        }
        // 从 translation/wiki 切换到其他类型时，清理旧的扩展数据
        if ((TYPE_TRANSLATION.equals(old_media.getType())
                || TYPE_WIKI.equals(old_media.getType()))
                && params.get("type") != null
                && !(TYPE_TRANSLATION.equals(params.get("type"))
                        || TYPE_WIKI.equals(params.get("type")))) {
            if (mediawikidao.getWikiinfoById(media_id) != null) {
                mediawikidao.deleteWikiinfoById(media_id);
            }
            if (mediatranslationdao.getMediaTranslationById(media_id) != null) {
                mediatranslationdao.deleteMediaTranslationById(media_id);
            }
        }
        // 主表更新：每个字段未传时沿用旧值；asset 关联在无 asset 的类型下强制置空
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
                List<String> animations = (List<String>) model_info.get("animations");
                animationdao.deleteAnimationByModelinfoId(media_id);

                animations.forEach(animationName -> {
                    Animation tempAnimation = new Animation();
                    tempAnimation.setName(animationName);
                    tempAnimation.setMid(media_id);
                    animationdao.addAnimation(tempAnimation);
                });
            }
            // 更改 part 表，思路同上
            if (model_info.get("parts") != null) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) model_info.get("parts");
                partdao.deletePartsByMediaID(media_id);
                parts.stream().map(MediaService::toPart).forEach(partdao::addPart);
            }
        } else if (params.get("media_translation") != null) {
            Map<String, Object> media_translation = (Map<String, Object>) params.get("media_translation");
            MediaTranslation old_media_translation = mediatranslationdao.getMediaTranslationById(media_id);
            if (old_media_translation != null) {
                mediatranslationdao.updateMediaTranslationById(
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
                if (mediawikidao.getWikiinfoById(media_id) != null) {
                    mediawikidao.deleteWikiinfoById(media_id);
                }
                MediaTranslation temp_media_translation = new MediaTranslation();
                temp_media_translation.setWord(media_translation.get("word").toString());
                temp_media_translation.setTranslation_english(media_translation.get("translation_english").toString());
                temp_media_translation.setPhonetic_UK(media_translation.get("phonetic_UK").toString());
                temp_media_translation.setPhonetic_US(media_translation.get("phonetic_US").toString());
                temp_media_translation.setSentence_CN(media_translation.get("sentence_CN").toString());
                temp_media_translation.setSentence_EN(media_translation.get("sentence_EN").toString());
                temp_media_translation.setId(media_id);
                mediatranslationdao.addMediaTranslation(temp_media_translation);
            }
        } else if (params.get("media_wiki") != null) {
            Map<String, Object> media_wiki = (Map<String, Object>) params.get("media_wiki");
            MediaWiki old_media_wiki = mediawikidao.getWikiinfoById(media_id);
            if (old_media_wiki != null) {
                mediawikidao.updateWikiinfoById(
                        media_wiki.get("word") == null ? old_media_wiki.getWord()
                                : media_wiki.get("word").toString(),
                        media_wiki.get("wiki") == null ? old_media_wiki.getWiki()
                                : media_wiki.get("wiki").toString(),
                        media_id);
            } else {
                if (mediatranslationdao.getMediaTranslationById(media_id) != null) {
                    mediatranslationdao.deleteMediaTranslationById(media_id);
                }
                MediaWiki temp_media_wiki = new MediaWiki();
                temp_media_wiki.setWord(media_wiki.get("word").toString());
                temp_media_wiki.setWiki(media_wiki.get("wiki").toString());
                temp_media_wiki.setId(media_id);
                mediawikidao.addWikiinfo(temp_media_wiki);
            }
        }
        if (media != null && media.getKid() == keyword.getId()) {
            return buildMediaResponse(media);
        }
        return null;
    }

    /** translation/wiki/assistant 类型无 asset 关联，assetid 置空 */
    private boolean typeHasNoAsset(Object type) {
        return type != null && (TYPE_TRANSLATION.equals(type) || TYPE_WIKI.equals(type) || TYPE_ASSISTANT.equals(type));
    }

    /** 将 part 请求参数转换为 Part 实体（addMediaByKeyword 与 modifyMediaById 共用） */
    private static Part toPart(Map<String, Object> partmessage) {
        Part part = new Part();
        part.setMediaid(Integer.parseInt(partmessage.get("media_id").toString()));
        part.setPartName(partmessage.get("name").toString());
        part.setPart_index(Integer.parseInt(partmessage.get("part_index").toString()));
        part.setPart_order(Integer.parseInt(partmessage.get("part_order").toString()));
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
        return part;
    }

    /**
     * 组装媒体响应：asset/anchor/color 嵌套 + type 专属信息（model/translation/wiki）。
     * 所有媒体接口（增删改查）共用同一组装逻辑，保证响应结构一致。
     */
    private Map<String, Object> buildMediaResponse(Media media) {
        Map<String, Object> ac = MapUtils.toMap(media);
        // asset 嵌套（uid/deleted_at 移除，id 转字符串）
        Map<String, Object> tempasset = null;
        if (media.getAssetid() != null) {
            tempasset = MapUtils.toMap(assetdao.getAssetById(media.getAssetid()));
            tempasset.remove("uid");
            tempasset.remove("deleted_at");
            tempasset.put("id", tempasset.get("id").toString());
        }
        // anchor 嵌套（pos/euler 收拢，cid 移除，id 转字符串）
        Map<String, Object> tempanchor = MapUtils.toMap(anchordao.getAnchorById(media.getAnchorid()));
        tempanchor.remove("cid");
        tempanchor.put("pos", MapUtils.nestVec(tempanchor, "pos", "pos"));
        tempanchor.put("euler", MapUtils.nestVec(tempanchor, "euler", "euler"));
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
        if (TYPE_MODEL.equals(type)) {
            MediaModel mediaModel = mediamodeldao.getModelinfoById(mediaId);
            if (mediaModel != null) {
                Map<String, Object> scale = new HashMap<String, Object>();
                scale.put("scale_x", mediaModel.getScale_x());
                scale.put("scale_y", mediaModel.getScale_y());
                scale.put("scale_z", mediaModel.getScale_z());
                List<String> animationsList = animationdao.getAnimationsByModelinfoId(mediaModel.getId()).stream()
                        .map(animation -> animation.get("name").toString())
                        .collect(Collectors.toList());
                ac.put("anime_to_play", mediaModel.getAnime_to_play());
                ac.put("scale", scale);
                ac.put("animations", animationsList);
                ac.put("parts", buildParts(partdao.getAllByMediaID(mediaModel.getId())));
            }
        } else if (TYPE_TRANSLATION.equals(type)) {
            MediaTranslation mediaTranslation = mediatranslationdao.getMediaTranslationById(mediaId);
            if (mediaTranslation != null) {
                Map<String, Object> res = MapUtils.toMap(mediaTranslation);
                res.remove("id");
                ac.put("media_translation", res);
            }
        } else if (TYPE_WIKI.equals(type)) {
            MediaWiki mediaWiki = mediawikidao.getWikiinfoById(mediaId);
            if (mediaWiki != null) {
                Map<String, Object> res = MapUtils.toMap(mediaWiki);
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
        return all_partsresult.stream()
                .map(partmessage -> {
                    Map<String, Object> result = new HashMap<String, Object>(partmessage);
                    result.put("name", result.get("part_name").toString());
                    result.remove("part_name");
                    result.put("origin_pos", MapUtils.nestVec(result, "originpos", "pos"));
                    result.put("origin_euler", MapUtils.nestVec(result, "origineuler", "euler"));
                    result.put("target_pos", MapUtils.nestVec(result, "targetpos", "pos"));
                    result.put("target_euler", MapUtils.nestVec(result, "targeteuler", "euler"));
                    return result;
                })
                .collect(Collectors.toList());
    }
}
