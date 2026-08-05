package com.imct.alphaclass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.imct.alphaclass.bean.Anchor;
import com.imct.alphaclass.bean.Asset;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.Media;
import com.imct.alphaclass.bean.MediaModel;
import com.imct.alphaclass.bean.MediaTranslation;
import com.imct.alphaclass.bean.MediaWiki;
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

/**
 * MediaService 行为基线测试。
 * 覆盖 getMediaById / getAllMediasByKeyword / addMediaByKeyword /
 * addMediaTranslationOrWikiByKeyword / modifyMediaById / deleteMediaById 的响应组装契约。
 */
@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaDAO dao;
    @Mock
    private UserDAO userdao;
    @Mock
    private CourseDAO coursedao;
    @Mock
    private AnchorDAO anchordao;
    @Mock
    private AssetDAO assetdao;
    @Mock
    private KeywordDAO keyworddao;
    @Mock
    private MediaModelDAO mediamodeldao;
    @Mock
    private AnimationDAO animationDAO;
    @Mock
    private PartDAO partDAO;
    @Mock
    private MediaTranslationDAO mediaTranslationDAO;
    @Mock
    private MediaWikiDAO mediaWikiDAO;

    @InjectMocks
    private MediaService service;

    private User user;
    private Course course;
    private Keyword keyword;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("alice");
        user.setName("Alice");

        course = new Course();
        course.setId(10);
        course.setName("math");

        keyword = new Keyword();
        keyword.setId(100);
        keyword.setKeyword("k1");

        when(userdao.getByUsername("alice")).thenReturn(user);
        when(coursedao.getCourseByUidAndName(1, "math")).thenReturn(course);
        when(keyworddao.getKeywordByCidAndName(10, "k1")).thenReturn(keyword);
    }

    private Media buildMedia(int id, String type) {
        Media media = new Media();
        media.setId(id);
        media.setName("media" + id);
        media.setType(type);
        media.setStyle("default");
        media.setAnchorid(400);
        media.setAssetid(300);
        media.setKid(100);
        media.setColor_r(1.0f);
        media.setColor_g(0.5f);
        media.setColor_b(0.25f);
        return media;
    }

    private Anchor buildAnchor() {
        Anchor anchor = new Anchor();
        anchor.setId(400);
        anchor.setCid(10);
        anchor.setName("anchor1");
        anchor.setPos_x(1.0f);
        anchor.setPos_y(2.0f);
        anchor.setPos_z(3.0f);
        anchor.setEuler_x(10.0f);
        anchor.setEuler_y(20.0f);
        anchor.setEuler_z(30.0f);
        return anchor;
    }

    private Asset buildAsset() {
        Asset asset = new Asset();
        asset.setId(300);
        asset.setUid(1);
        asset.setName("asset1");
        asset.setType("model");
        asset.setUrl("http://example.com/a.glb");
        asset.setSize(1024);
        asset.setDeleted_at(null);
        return asset;
    }

    private MediaModel buildMediaModel() {
        MediaModel mm = new MediaModel();
        mm.setId(200);
        mm.setAnime_to_play("take 001");
        mm.setScale_x(1.0f);
        mm.setScale_y(2.0f);
        mm.setScale_z(3.0f);
        return mm;
    }

    private List<Map<String, Object>> buildAnimations() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> a1 = new HashMap<>();
        a1.put("name", "take 001");
        Map<String, Object> a2 = new HashMap<>();
        a2.put("name", "take 002");
        list.add(a1);
        list.add(a2);
        return list;
    }

    private List<Map<String, Object>> buildParts() {
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> p = new HashMap<>();
        p.put("part_name", "part1");
        p.put("part_index", 1);
        p.put("part_order", 1);
        p.put("originpos_x", 0.1f);
        p.put("originpos_y", 0.2f);
        p.put("originpos_z", 0.3f);
        p.put("origineuler_x", 1.1f);
        p.put("origineuler_y", 1.2f);
        p.put("origineuler_z", 1.3f);
        p.put("targetpos_x", 2.1f);
        p.put("targetpos_y", 2.2f);
        p.put("targetpos_z", 2.3f);
        p.put("targeteuler_x", 3.1f);
        p.put("targeteuler_y", 3.2f);
        p.put("targeteuler_z", 3.3f);
        parts.add(p);
        return parts;
    }

    /** 组装 model 类型媒体的公共 DAO mock */
    private void mockModelMediaAssemble(int mediaId) {
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());
        when(assetdao.getAssetById(300)).thenReturn(buildAsset());
        when(mediamodeldao.getModelinfoById(mediaId)).thenReturn(buildMediaModel());
        when(animationDAO.getAnimationsByModelinfoId(200)).thenReturn(buildAnimations());
        // parts 可能被 service 多次消费（每次组装都会改写 map），每次返回新副本
        when(partDAO.getAllByMediaID(200)).thenAnswer(invocation -> buildParts());
    }

    @Test
    void getMediaById_modelType_returnsAssembledStructure() {
        Media media = buildMedia(200, "model");
        when(dao.getMediaById(200)).thenReturn(media);
        mockModelMediaAssemble(200);

        Map<String, Object> result = service.getMediaById("math", "alice", "k1", 200);

        assertNotNull(result);
        assertEquals("200", result.get("id"));
        assertEquals("media200", result.get("name"));
        assertEquals("model", result.get("type"));
        assertNull(result.get("kid"));
        assertNull(result.get("anchorid"));
        assertNull(result.get("assetid"));

        // asset 嵌套
        Map<String, Object> asset = (Map<String, Object>) result.get("asset");
        assertNotNull(asset);
        assertEquals("300", asset.get("id"));
        assertNull(asset.get("uid"));
        assertNull(asset.get("deleted_at"));

        // anchor 嵌套：pos/euler 收拢为嵌套结构（与 getAll/modify 一致）
        Map<String, Object> anchor = (Map<String, Object>) result.get("anchor");
        assertNotNull(anchor);
        assertEquals("400", anchor.get("id"));
        assertNull(anchor.get("cid"));
        assertNull(anchor.get("pos_x"));
        assertNull(anchor.get("euler_x"));
        Map<String, Object> pos = (Map<String, Object>) anchor.get("pos");
        assertEquals(1.0, ((Number) pos.get("pos_x")).doubleValue());
        Map<String, Object> euler = (Map<String, Object>) anchor.get("euler");
        assertEquals(10.0, ((Number) euler.get("euler_x")).doubleValue());

        // color 嵌套
        Map<String, Object> color = (Map<String, Object>) result.get("color");
        assertEquals(1.0f, color.get("r"));
        assertEquals(0.5f, color.get("g"));
        assertEquals(0.25f, color.get("b"));

        // model 信息
        assertEquals("take 001", result.get("anime_to_play"));
        Map<String, Object> scale = (Map<String, Object>) result.get("scale");
        assertEquals(1.0f, scale.get("scale_x"));
        assertEquals(2.0f, scale.get("scale_y"));
        assertEquals(3.0f, scale.get("scale_z"));
        List<String> animations = (List<String>) result.get("animations");
        assertEquals(2, animations.size());
        assertEquals("take 001", animations.get(0));

        // parts 嵌套转换
        List<Map<String, Object>> parts = (List<Map<String, Object>>) result.get("parts");
        assertEquals(1, parts.size());
        Map<String, Object> part = parts.get(0);
        assertEquals("part1", part.get("name"));
        assertNull(part.get("part_name"));
        Map<String, Object> originPos = (Map<String, Object>) part.get("origin_pos");
        assertEquals(0.1f, originPos.get("pos_x"));
        Map<String, Object> targetEuler = (Map<String, Object>) part.get("target_euler");
        assertEquals(3.1f, targetEuler.get("euler_x"));
    }

    @Test
    void getMediaById_translationType_returnsTranslationNested() {
        Media media = buildMedia(200, "translation");
        media.setAssetid(null);
        when(dao.getMediaById(200)).thenReturn(media);
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());
        MediaTranslation mt = new MediaTranslation();
        mt.setId(200);
        mt.setWord("apple");
        mt.setTranslation_english("苹果");
        mt.setPhonetic_UK("/ˈæpl/");
        mt.setPhonetic_US("/ˈæpl/");
        mt.setSentence_CN("这是一个苹果");
        mt.setSentence_EN("This is an apple");
        when(mediaTranslationDAO.getMediaTranslationById(200)).thenReturn(mt);

        Map<String, Object> result = service.getMediaById("math", "alice", "k1", 200);

        assertNotNull(result);
        Map<String, Object> translation = (Map<String, Object>) result.get("media_translation");
        assertNotNull(translation);
        assertEquals("apple", translation.get("word"));
        assertEquals("苹果", translation.get("translation_english"));
        assertNull(translation.get("id"));
        // 无 asset 时 asset 为 null
        assertNull(result.get("asset"));
    }

    @Test
    void getMediaById_wikiType_returnsWikiNested() {
        Media media = buildMedia(200, "wiki");
        media.setAssetid(null);
        when(dao.getMediaById(200)).thenReturn(media);
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());
        MediaWiki mw = new MediaWiki();
        mw.setId(200);
        mw.setWord("apple");
        mw.setWiki("苹果是一种水果");
        when(mediaWikiDAO.getWikiinfoById(200)).thenReturn(mw);

        Map<String, Object> result = service.getMediaById("math", "alice", "k1", 200);

        assertNotNull(result);
        Map<String, Object> wiki = (Map<String, Object>) result.get("media_wiki");
        assertNotNull(wiki);
        assertEquals("苹果是一种水果", wiki.get("wiki"));
        assertNull(wiki.get("id"));
    }

    @Test
    void getMediaById_wrongKeyword_returnsNull() {
        Media media = buildMedia(200, "model");
        media.setKid(999);
        when(dao.getMediaById(200)).thenReturn(media);

        assertNull(service.getMediaById("math", "alice", "k1", 200));
        // 不属于该 keyword 的媒体不可见
        verify(anchordao, never()).getAnchorById(anyInt());
    }

    @Test
    void getAllMediasByKeyword_returnsListWithAssembledStructure() {
        Media media = buildMedia(200, "model");
        Map<String, Object> m = new HashMap<>();
        m.put("id", 200);
        m.put("name", "media200");
        m.put("type", "model");
        m.put("style", "default");
        m.put("assetid", 300);
        m.put("anchorid", 400);
        m.put("kid", 100);
        m.put("color_r", 1.0f);
        m.put("color_g", 0.5f);
        m.put("color_b", 0.25f);
        List<Map<String, Object>> mediaList = new ArrayList<>();
        mediaList.add(m);
        when(dao.getAllMediasByKid(100)).thenReturn(mediaList);
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());
        when(assetdao.getAssetById(300)).thenReturn(buildAsset());
        when(mediamodeldao.getModelinfoById(200)).thenReturn(buildMediaModel());
        when(animationDAO.getAnimationsByModelinfoId(200)).thenReturn(buildAnimations());
        when(partDAO.getAllByMediaID(200)).thenAnswer(invocation -> buildParts());

        List<Map<String, Object>> result = service.getAllMediasByKeyword("alice", "math", "k1");

        assertNotNull(result);
        assertEquals(1, result.size());
        Map<String, Object> am = result.get(0);
        assertEquals("200", am.get("id"));
        assertNull(am.get("kid"));
        assertNull(am.get("anchorid"));
        assertNull(am.get("assetid"));
        Map<String, Object> anchor = (Map<String, Object>) am.get("anchor");
        assertNotNull(anchor);
        assertEquals("400", anchor.get("id"));
        Map<String, Object> pos = (Map<String, Object>) anchor.get("pos");
        assertEquals(1.0, ((Number) pos.get("pos_x")).doubleValue());
        Map<String, Object> asset = (Map<String, Object>) am.get("asset");
        assertEquals("300", asset.get("id"));
        assertEquals("take 001", am.get("anime_to_play"));
        List<Map<String, Object>> parts = (List<Map<String, Object>>) am.get("parts");
        assertEquals(1, parts.size());
        assertEquals("part1", parts.get(0).get("name"));
    }

    @Test
    void addMediaByKeyword_modelType_insertsModelAndReturnsResponse() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "newMedia");
        params.put("type", "model");
        params.put("style", "default");
        Map<String, Object> color = new HashMap<>();
        color.put("r", "1.0");
        color.put("g", "0.5");
        color.put("b", "0.25");
        params.put("color", color);
        params.put("asset_id", "300");
        params.put("anchor_id", "400");
        Map<String, Object> mediaModel = new HashMap<>();
        mediaModel.put("anime_to_play", "take 001");
        Map<String, Object> scale = new HashMap<>();
        scale.put("scale_x", "1.0");
        scale.put("scale_y", "2.0");
        scale.put("scale_z", "3.0");
        mediaModel.put("scale", scale);
        List<String> animations = new ArrayList<>();
        animations.add("take 001");
        mediaModel.put("animations", animations);
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        part.put("media_id", "200");
        part.put("name", "part1");
        part.put("part_index", "1");
        part.put("part_order", "1");
        Map<String, Object> op = new HashMap<>();
        op.put("pos_x", "0.1");
        op.put("pos_y", "0.2");
        op.put("pos_z", "0.3");
        part.put("origin_pos", op);
        Map<String, Object> oe = new HashMap<>();
        oe.put("euler_x", "1.1");
        oe.put("euler_y", "1.2");
        oe.put("euler_z", "1.3");
        part.put("origin_euler", oe);
        Map<String, Object> tp = new HashMap<>();
        tp.put("pos_x", "2.1");
        tp.put("pos_y", "2.2");
        tp.put("pos_z", "2.3");
        part.put("target_pos", tp);
        Map<String, Object> te = new HashMap<>();
        te.put("euler_x", "3.1");
        te.put("euler_y", "3.2");
        te.put("euler_z", "3.3");
        part.put("target_euler", te);
        parts.add(part);
        mediaModel.put("parts", parts);
        params.put("media_model", mediaModel);

        Media inserted = buildMedia(200, "model");
        inserted.setName("newMedia");
        doAnswer(invocation -> {
            Media m = invocation.getArgument(0);
            m.setId(200);
            return null;
        }).when(dao).addMedia(any(Media.class));
        when(dao.getMediaById(200)).thenReturn(inserted);
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());
        when(assetdao.getAssetById(300)).thenReturn(buildAsset());
        when(mediamodeldao.getModelinfoById(200)).thenReturn(buildMediaModel());
        when(animationDAO.getAnimationsByModelinfoId(200)).thenReturn(buildAnimations());
        // 响应统一走 DB 回读组装，parts 每次返回新副本
        when(partDAO.getAllByMediaID(200)).thenAnswer(invocation -> buildParts());

        Map<String, Object> result = service.addMediaByKeyword("alice", "math", "k1", params);

        assertNotNull(result);
        assertEquals("200", result.get("id"));
        assertEquals("newMedia", result.get("name"));
        assertEquals("take 001", result.get("anime_to_play"));

        // 级联插入验证
        verify(mediamodeldao).addModelinfo(argThat(mm -> mm.getId() == 200));
        verify(animationDAO).addAnimation(argThat(a -> "take 001".equals(a.getName())));
        verify(partDAO).addPart(argThat(p -> "part1".equals(p.getPartName())));

        // asset 嵌套
        Map<String, Object> asset = (Map<String, Object>) result.get("asset");
        assertEquals("300", asset.get("id"));
    }

    @Test
    void addMediaTranslationOrWikiByKeyword_translation_insertsTranslation() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "appleMedia");
        params.put("type", "translation");
        params.put("style", "default");
        Map<String, Object> color = new HashMap<>();
        color.put("r", "1.0");
        color.put("g", "0.0");
        color.put("b", "0.0");
        params.put("color", color);
        params.put("anchor_id", "400");
        Map<String, Object> mt = new HashMap<>();
        mt.put("word", "apple");
        mt.put("translation_english", "苹果");
        mt.put("phonetic_UK", "/ˈæpl/");
        mt.put("phonetic_US", "/ˈæpl/");
        mt.put("sentence_CN", "这是一个苹果");
        mt.put("sentence_EN", "This is an apple");
        params.put("media_translation", mt);

        Media inserted = buildMedia(200, "translation");
        inserted.setAssetid(null);
        doAnswer(invocation -> {
            Media m = invocation.getArgument(0);
            m.setId(200);
            return null;
        }).when(dao).addMedia(any(Media.class));
        when(dao.getMediaById(200)).thenReturn(inserted);
        MediaTranslation saved = new MediaTranslation();
        saved.setId(200);
        saved.setWord("apple");
        saved.setTranslation_english("苹果");
        saved.setPhonetic_UK("/ˈæpl/");
        saved.setPhonetic_US("/ˈæpl/");
        saved.setSentence_CN("这是一个苹果");
        saved.setSentence_EN("This is an apple");
        doAnswer(invocation -> null).when(mediaTranslationDAO).addMediaTranslation(any(MediaTranslation.class));
        when(mediaTranslationDAO.getMediaTranslationById(200)).thenReturn(saved);
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());

        Map<String, Object> result = service.addMediaTranslationOrWikiByKeyword("alice", "math", "k1", params);

        assertNotNull(result);
        assertEquals("200", result.get("id"));
        // 契约：translation 数据嵌套在 "media_translation" 键下（与 getAll 一致）
        Map<String, Object> translation = (Map<String, Object>) result.get("media_translation");
        assertNotNull(translation);
        assertEquals("apple", translation.get("word"));
        assertEquals("苹果", translation.get("translation_english"));
        verify(mediaTranslationDAO).addMediaTranslation(any(MediaTranslation.class));
        verify(mediaWikiDAO, never()).addWikiinfo(any());
    }

    @Test
    void addMediaTranslationOrWikiByKeyword_wiki_insertsWiki() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "wikiMedia");
        params.put("type", "wiki");
        params.put("style", "default");
        Map<String, Object> color = new HashMap<>();
        color.put("r", "0.0");
        color.put("g", "1.0");
        color.put("b", "0.0");
        params.put("color", color);
        params.put("anchor_id", "400");
        Map<String, Object> mw = new HashMap<>();
        mw.put("word", "apple");
        mw.put("wiki", "苹果是一种水果");
        params.put("media_wiki", mw);

        Media inserted = buildMedia(200, "wiki");
        inserted.setAssetid(null);
        doAnswer(invocation -> {
            Media m = invocation.getArgument(0);
            m.setId(200);
            return null;
        }).when(dao).addMedia(any(Media.class));
        when(dao.getMediaById(200)).thenReturn(inserted);
        MediaWiki saved = new MediaWiki();
        saved.setId(200);
        saved.setWord("apple");
        saved.setWiki("苹果是一种水果");
        doAnswer(invocation -> null).when(mediaWikiDAO).addWikiinfo(any(MediaWiki.class));
        when(mediaWikiDAO.getWikiinfoById(200)).thenReturn(saved);
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());

        Map<String, Object> result = service.addMediaTranslationOrWikiByKeyword("alice", "math", "k1", params);

        assertNotNull(result);
        Map<String, Object> wiki = (Map<String, Object>) result.get("media_wiki");
        assertNotNull(wiki);
        assertEquals("苹果是一种水果", wiki.get("wiki"));
        verify(mediaWikiDAO).addWikiinfo(any(MediaWiki.class));
    }

    @Test
    void deleteMediaById_belongsToKeyword_deletes() {
        Media media = buildMedia(200, "model");
        when(dao.getMediaById(200)).thenReturn(media);

        service.deleteMediaById("math", "alice", "k1", 200);

        verify(dao).deleteMediaById(200);
    }

    @Test
    void deleteMediaById_notBelongsToKeyword_skips() {
        Media media = buildMedia(200, "model");
        media.setKid(999);
        when(dao.getMediaById(200)).thenReturn(media);

        service.deleteMediaById("math", "alice", "k1", 200);

        verify(dao, never()).deleteMediaById(anyInt());
    }

    @Test
    void modifyMediaById_updateNameOnly_keepsOldTypeAndAsset() {
        Media oldMedia = buildMedia(200, "model");
        when(dao.getMediaById(200)).thenReturn(oldMedia);
        Media updated = buildMedia(200, "model");
        updated.setName("renamed");
        when(dao.updateMediaById(anyString(), anyString(), any(), anyInt(), anyString(), anyFloat(), anyFloat(),
                anyFloat(), anyInt())).thenReturn(1);
        when(dao.getMediaById(200)).thenReturn(updated);
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());
        when(assetdao.getAssetById(300)).thenReturn(buildAsset());
        when(mediamodeldao.getModelinfoById(200)).thenReturn(buildMediaModel());
        when(animationDAO.getAnimationsByModelinfoId(200)).thenReturn(buildAnimations());
        when(partDAO.getAllByMediaID(200)).thenReturn(buildParts());

        Map<String, Object> params = new HashMap<>();
        params.put("name", "renamed");
        params.put("type", "model");

        Map<String, Object> result = service.modifyMediaById("math", "alice", "k1", 200, params);

        assertNotNull(result);
        assertEquals("200", result.get("id"));
        assertEquals("renamed", result.get("name"));
        // 未传 color 时保留旧值
        Map<String, Object> color = (Map<String, Object>) result.get("color");
        assertEquals(1.0f, color.get("r"));
        assertEquals(0.5f, color.get("g"));
        assertEquals(0.25f, color.get("b"));
        // 未传 asset_id 时保留旧 assetid（300）
        verify(dao).updateMediaById(eq("renamed"), eq("model"), eq(300), eq(400), eq("default"),
                eq(1.0f), eq(0.5f), eq(0.25f), eq(200));
    }

    @Test
    void modifyMediaById_switchFromTranslationToModel_cleansTranslationData() {
        Media oldMedia = buildMedia(200, "translation");
        Media updated = buildMedia(200, "model");
        updated.setName("changed");
        // 第一次返回旧数据（读旧类型），update 后第二次返回新数据（回读组装）
        when(dao.getMediaById(200)).thenReturn(oldMedia, updated);
        when(mediaWikiDAO.getWikiinfoById(200)).thenReturn(null);
        MediaTranslation oldMt = new MediaTranslation();
        oldMt.setId(200);
        oldMt.setWord("apple");
        when(mediaTranslationDAO.getMediaTranslationById(200)).thenReturn(oldMt);

        when(dao.updateMediaById(anyString(), anyString(), any(), anyInt(), anyString(), anyFloat(), anyFloat(),
                anyFloat(), anyInt())).thenReturn(1);
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());
        when(assetdao.getAssetById(300)).thenReturn(buildAsset());
        when(mediamodeldao.getModelinfoById(200)).thenReturn(buildMediaModel());
        when(animationDAO.getAnimationsByModelinfoId(200)).thenReturn(buildAnimations());
        when(partDAO.getAllByMediaID(200)).thenReturn(buildParts());

        Map<String, Object> params = new HashMap<>();
        params.put("name", "changed");
        params.put("type", "model");

        Map<String, Object> result = service.modifyMediaById("math", "alice", "k1", 200, params);

        assertNotNull(result);
        assertEquals("model", result.get("type"));
        // 切换类型时清理旧的 translation 数据
        verify(mediaTranslationDAO).deleteMediaTranslationById(200);
    }
}
