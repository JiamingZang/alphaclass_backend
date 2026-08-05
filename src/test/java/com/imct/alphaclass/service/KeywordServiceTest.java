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
 * KeywordService 行为基线测试：keyword CRUD 与 medias 嵌套响应组装契约。
 */
@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @Mock
    private KeywordDAO dao;
    @Mock
    private UserDAO userdao;
    @Mock
    private CourseDAO coursedao;
    @Mock
    private AssetDAO assetdao;
    @Mock
    private AnchorDAO anchordao;
    @Mock
    private MediaDAO mediadao;
    @Mock
    private MediaModelDAO mediamodeldao;
    @Mock
    private PartDAO partDAO;
    @Mock
    private AnimationDAO animationDAO;
    @Mock
    private MediaTranslationDAO mediaTranslationDAO;
    @Mock
    private MediaWikiDAO mediaWikiDAO;

    @InjectMocks
    private KeywordService service;

    private User user;
    private Course course;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("alice");

        course = new Course();
        course.setId(10);
        course.setName("math");

        when(userdao.getByUsername("alice")).thenReturn(user);
        when(coursedao.getCourseByUidAndName(1, "math")).thenReturn(course);
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
        return asset;
    }

    private Map<String, Object> buildMediaRow(int id, String type) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("name", "media" + id);
        m.put("type", type);
        m.put("style", "default");
        m.put("assetid", 300);
        m.put("anchorid", 400);
        m.put("kid", 100);
        m.put("color_r", 1.0f);
        m.put("color_g", 0.5f);
        m.put("color_b", 0.25f);
        return m;
    }

    private void mockMediaAssemble() {
        when(anchordao.getAnchorById(400)).thenReturn(buildAnchor());
        when(assetdao.getAssetById(300)).thenReturn(buildAsset());
    }

    @Test
    void addKeywordByCourse_returnsKeywordWithUrl() {
        Keyword keyword = new Keyword();
        keyword.setCid(10);
        keyword.setKeyword("k2");
        doAnswer(invocation -> {
            Keyword k = invocation.getArgument(0);
            k.setId(101);
            return null;
        }).when(dao).addKeyword(any(Keyword.class));
        Keyword saved = new Keyword();
        saved.setId(101);
        saved.setCid(10);
        saved.setKeyword("k2");
        when(dao.getKeywordById(101)).thenReturn(saved);

        Map<String, Object> result = service.addKeywordByCourse("alice", "math", new HashMap<String, Object>() {
            {
                put("keyword", "k2");
            }
        });

        assertNotNull(result);
        assertEquals("101", result.get("id"));
        assertEquals("k2", result.get("keyword"));
        assertNull(result.get("cid"));
        assertEquals("https://SERVER_IP_PLACEHOLDER/v2/alice/math/k2", result.get("url"));
    }

    @Test
    void deleteKeywordById_callsDelete() {
        service.deleteKeywordById("alice", "math", "k1");

        verify(dao).deleteKeywordByCidAndName(10, "k1");
    }

    @Test
    void getKeywordByCourse_returnsKeywordWithMedias() {
        Keyword keyword = new Keyword();
        keyword.setId(100);
        keyword.setCid(10);
        keyword.setKeyword("k1");
        when(dao.getKeywordByCidAndName(10, "k1")).thenReturn(keyword);

        List<Map<String, Object>> mediaList = new ArrayList<>();
        mediaList.add(buildMediaRow(200, "model"));
        when(mediadao.getAllMediasByKid(100)).thenReturn(mediaList);
        mockMediaAssemble();
        MediaModel mm = new MediaModel();
        mm.setId(200);
        mm.setAnime_to_play("take 001");
        mm.setScale_x(1.0f);
        mm.setScale_y(2.0f);
        mm.setScale_z(3.0f);
        when(mediamodeldao.getModelinfoById(200)).thenReturn(mm);
        when(animationDAO.getAnimationsByModelinfoId(200)).thenReturn(new ArrayList<>());

        Map<String, Object> result = service.getKeywordByCourse("alice", "math", "k1");

        assertNotNull(result);
        assertEquals("100", result.get("id"));
        assertEquals("k1", result.get("keyword"));
        assertNull(result.get("cid"));
        List<Map<String, Object>> medias = (List<Map<String, Object>>) result.get("medias");
        assertEquals(1, medias.size());
        Map<String, Object> am = medias.get(0);
        assertEquals("200", am.get("id"));
        assertEquals("take 001", am.get("anime_to_play"));
        Map<String, Object> anchor = (Map<String, Object>) am.get("anchor");
        assertEquals("400", anchor.get("id"));
        Map<String, Object> pos = (Map<String, Object>) anchor.get("pos");
        assertEquals(1.0, ((Number) pos.get("pos_x")).doubleValue());
    }

    @Test
    void getKeywordByCourse_translationMedia_nestedTranslation() {
        Keyword keyword = new Keyword();
        keyword.setId(100);
        keyword.setCid(10);
        keyword.setKeyword("k1");
        when(dao.getKeywordByCidAndName(10, "k1")).thenReturn(keyword);

        List<Map<String, Object>> mediaList = new ArrayList<>();
        mediaList.add(buildMediaRow(200, "translation"));
        when(mediadao.getAllMediasByKid(100)).thenReturn(mediaList);
        mockMediaAssemble();
        MediaTranslation mt = new MediaTranslation();
        mt.setId(200);
        mt.setWord("apple");
        mt.setTranslation_english("苹果");
        when(mediaTranslationDAO.getMediaTranslationById(200)).thenReturn(mt);

        Map<String, Object> result = service.getKeywordByCourse("alice", "math", "k1");

        List<Map<String, Object>> medias = (List<Map<String, Object>>) result.get("medias");
        Map<String, Object> am = medias.get(0);
        Map<String, Object> translation = (Map<String, Object>) am.get("media_translation");
        assertNotNull(translation);
        assertEquals("apple", translation.get("word"));
        assertNull(translation.get("id"));
    }

    @Test
    void getAllKeywordsByCourse_returnsKeywordsEachWithMedias() {
        List<Map<String, Object>> keywordRows = new ArrayList<>();
        Map<String, Object> k1 = new HashMap<>();
        k1.put("id", 100);
        k1.put("keyword", "k1");
        k1.put("cid", 10);
        keywordRows.add(k1);
        when(dao.getAllKeywordsByCid(10)).thenReturn(keywordRows);

        List<Map<String, Object>> mediaList = new ArrayList<>();
        mediaList.add(buildMediaRow(200, "wiki"));
        when(mediadao.getAllMediasByKid(100)).thenReturn(mediaList);
        mockMediaAssemble();
        MediaWiki mw = new MediaWiki();
        mw.setId(200);
        mw.setWord("apple");
        mw.setWiki("苹果是一种水果");
        when(mediaWikiDAO.getWikiinfoById(200)).thenReturn(mw);

        List<Map<String, Object>> result = service.getAllKeywordsByCourse("alice", "math");

        assertNotNull(result);
        assertEquals(1, result.size());
        Map<String, Object> k = result.get(0);
        assertEquals("100", k.get("id"));
        assertNull(k.get("cid"));
        List<Map<String, Object>> medias = (List<Map<String, Object>>) k.get("medias");
        assertEquals(1, medias.size());
        Map<String, Object> wiki = (Map<String, Object>) medias.get(0).get("media_wiki");
        assertEquals("苹果是一种水果", wiki.get("wiki"));
    }

    @Test
    void modifyKeywordByCourse_renamesKeyword() {
        Keyword oldKeyword = new Keyword();
        oldKeyword.setId(100);
        oldKeyword.setCid(10);
        oldKeyword.setKeyword("k1");
        when(dao.getKeywordByCidAndName(10, "k1")).thenReturn(oldKeyword);
        when(dao.updateKeywordByCidAndName(eq("k2"), eq(10), eq("k1"))).thenReturn(1);

        Keyword renamed = new Keyword();
        renamed.setId(100);
        renamed.setCid(10);
        renamed.setKeyword("k2");
        when(dao.getKeywordByCidAndName(10, "k2")).thenReturn(renamed);

        List<Map<String, Object>> mediaList = new ArrayList<>();
        when(mediadao.getAllMediasByKid(100)).thenReturn(mediaList);

        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "k2");

        Map<String, Object> result = service.modifyKeywordByCourse("alice", "math", "k1", params);

        assertNotNull(result);
        assertEquals("100", result.get("id"));
        assertEquals("k2", result.get("keyword"));
        assertEquals(0, ((List<?>) result.get("medias")).size());
    }
}
