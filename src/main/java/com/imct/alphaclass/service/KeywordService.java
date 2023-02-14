package com.imct.alphaclass.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.Keyword;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.AnchorDAO;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.dao.MediaDAO;
import com.imct.alphaclass.dao.UserDAO;

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

    public Map<String, Object> addKeywordByCourse(String ownername, String coursename, Map<String, Object> params){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = new Keyword();
        keyword.setCid(course.getId());
        keyword.setKeyword(params.get("keyword").toString());
        keyword.setWiki(params.get("wiki").toString());
        keyword.setTranslation_english(params.get("translation_english").toString());
        keyword.setPhonetic_UK(params.get("phonetic_UK").toString());
        keyword.setPhonetic_US(params.get("phonetic_US").toString());
        keyword.setSentence_CN(params.get("sentence_CN").toString());
        keyword.setSentence_EN(params.get("sentence_EN").toString());
        dao.addKeyword(keyword);
        
        keyword = dao.getKeywordById(keyword.getId());
        Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(keyword), new TypeReference<Map<String, Object>>() {});
        ac.put("url", "https://123.56.224.193/"+ownername+"/"+coursename+"/"+keyword.getKeyword());
        ac.remove("cid");
        ac.put("id", ac.get("id").toString());
        return ac;
    }

    public void deleteKeywordById(String ownername, String coursename, String keyword){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        dao.deleteKeywordByCidAndName(course.getId(), keyword);
    }

    public List<Map<String, Object>> getAllKeywordsByCourse(String ownername,String coursename){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        List<Map<String, Object>> all_keywordresult = dao.getAllKeywordsByCid(course.getId());
        for (Map<String,Object> ac :all_keywordresult) {
            ac.remove("cid");
            List<Map<String, Object>> all_mediaresult = mediadao.getAllMediasByKid((int)ac.get("id"));
            for (Map<String,Object> am : all_mediaresult) {
                
                Map<String, Object> tempasset = JSON.parseObject(JSON.toJSONString(assetdao.getAssetById((int)am.get("assetid"))), new TypeReference<Map<String, Object>>() {});
                tempasset.remove("uid");tempasset.remove("deleted_at");
                Map<String, Object> tempanchor = JSON.parseObject(JSON.toJSONString(anchordao.getAnchorById((int)am.get("anchorid"))), new TypeReference<Map<String, Object>>() {});
                tempanchor.remove("cid");
                am.remove("kid");am.remove("anchorid");am.remove("assetid");
                
                Map<String, Object> temppos = new HashMap<String, Object>();
                temppos.put("pos_x", tempanchor.get("pos_x"));temppos.put("pos_y", tempanchor.get("pos_y"));temppos.put("pos_z", tempanchor.get("pos_z"));
                tempanchor.remove("pos_x");tempanchor.remove("pos_y");tempanchor.remove("pos_z");
                tempanchor.put("pos", temppos);
                Map<String, Object> tempeuler = new HashMap<String, Object>();
                tempeuler.put("euler_x", tempanchor.get("euler_x"));tempeuler.put("euler_y", tempanchor.get("euler_y"));tempeuler.put("euler_z", tempanchor.get("euler_z"));
                tempanchor.remove("euler_x");tempanchor.remove("euler_y");tempanchor.remove("euler_z");
                tempanchor.put("euler", tempeuler);
                
                tempanchor.put("id", tempanchor.get("id").toString());
                tempasset.put("id", tempasset.get("id").toString());

                Map<String,Object> tempcolor = new HashMap<String,Object>();
                tempcolor.put("r", am.get("color_r"));
                tempcolor.put("g", am.get("color_g"));
                tempcolor.put("b", am.get("color_b"));
                am.remove("color_r");am.remove("color_g");am.remove("color_b");
                am.put("asset", tempasset);
                am.put("anchor", tempanchor);
                am.put("color", tempcolor);
                am.put("id", am.get("id").toString());
            }
            ac.put("id", ac.get("id").toString());
            ac.put("medias", all_mediaresult);
        }
        return all_keywordresult;
    } 

    public Map<String, Object> getKeywordByCourse(String ownername,String coursename,String keywordname){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Map<String, Object> result = JSON.parseObject(JSON.toJSONString(dao.getKeywordByCidAndName(course.getId(), keywordname)), new TypeReference<Map<String, Object>>() {});;
        result.remove("cid");
        
        List<Map<String, Object>> all_mediaresult = mediadao.getAllMediasByKid((int)result.get("id"));
        for (Map<String,Object> am : all_mediaresult) {
            
            Map<String, Object> tempasset = JSON.parseObject(JSON.toJSONString(assetdao.getAssetById((int)am.get("assetid"))), new TypeReference<Map<String, Object>>() {});
            tempasset.remove("uid");tempasset.remove("deleted_at");
            Map<String, Object> tempanchor = JSON.parseObject(JSON.toJSONString(anchordao.getAnchorById((int)am.get("anchorid"))), new TypeReference<Map<String, Object>>() {});
            tempanchor.remove("cid");
            am.remove("kid");am.remove("anchorid");am.remove("assetid");

            Map<String, Object> temppos = new HashMap<String, Object>();
            temppos.put("pos_x", tempanchor.get("pos_x"));temppos.put("pos_y", tempanchor.get("pos_y"));temppos.put("pos_z", tempanchor.get("pos_z"));
            tempanchor.remove("pos_x");tempanchor.remove("pos_y");tempanchor.remove("pos_z");
            tempanchor.put("pos", temppos);
            Map<String, Object> tempeuler = new HashMap<String, Object>();
            tempeuler.put("euler_x", tempanchor.get("euler_x"));tempeuler.put("euler_y", tempanchor.get("euler_y"));tempeuler.put("euler_z", tempanchor.get("euler_z"));
            tempanchor.remove("euler_x");tempanchor.remove("euler_y");tempanchor.remove("euler_z");
            tempanchor.put("euler", tempeuler);
            
            tempasset.put("id", tempasset.get("id").toString());
            tempanchor.put("id", tempanchor.get("id").toString());

            Map<String,Object> tempcolor = new HashMap<String,Object>();
            tempcolor.put("r", am.get("color_r"));
            tempcolor.put("g", am.get("color_g"));
            tempcolor.put("b", am.get("color_b"));
            am.remove("color_r");am.remove("color_g");am.remove("color_b");
            am.put("asset", tempasset);
            am.put("anchor", tempanchor);
            am.put("color", tempcolor);
            am.put("id", am.get("id").toString());
        }

        result.put("medias", all_mediaresult);
        result.put("id", result.get("id").toString());
        return result;
    } 

    public Map<String, Object> modifyKeywordByCourse(String ownername,String coursename,String keywordname, Map<String, Object> params){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = dao.getKeywordByCidAndName(course.getId(), keywordname);
        dao.updateKeywordByCidAndName(
            params.get("keyword")==null?keyword.getKeyword():params.get("keyword").toString(),
            params.get("translation_english")==null?keyword.getTranslation_english():params.get("translation_english").toString(),
            params.get("phonetic_UK")==null?keyword.getPhonetic_UK():params.get("phonetic_UK").toString(),
            params.get("phonetic_US")==null?keyword.getPhonetic_US():params.get("phonetic_US").toString(),
            params.get("sentence_CN")==null?keyword.getSentence_CN():params.get("sentence_CN").toString(),
            params.get("sentence_EN")==null?keyword.getSentence_EN():params.get("sentence_EN").toString(),
            params.get("wiki")==null?keyword.getWiki():params.get("wiki").toString(),
            course.getId(), 
            keywordname);

        Map<String, Object> result = JSON.parseObject(JSON.toJSONString(dao.getKeywordByCidAndName(course.getId(), params.get("keyword")==null?keywordname:params.get("keyword").toString())), new TypeReference<Map<String, Object>>() {});;
        result.remove("cid");
        
        List<Map<String, Object>> all_mediaresult = mediadao.getAllMediasByKid((int)result.get("id"));
        for (Map<String,Object> am : all_mediaresult) {
            
            Map<String, Object> tempasset = JSON.parseObject(JSON.toJSONString(assetdao.getAssetById((int)am.get("assetid"))), new TypeReference<Map<String, Object>>() {});
            tempasset.remove("uid");tempasset.remove("deleted_at");
            Map<String, Object> tempanchor = JSON.parseObject(JSON.toJSONString(anchordao.getAnchorById((int)am.get("anchorid"))), new TypeReference<Map<String, Object>>() {});
            tempanchor.remove("cid");
            am.remove("kid");am.remove("anchorid");am.remove("assetid");

            Map<String, Object> temppos = new HashMap<String, Object>();
            temppos.put("pos_x", tempanchor.get("pos_x"));temppos.put("pos_y", tempanchor.get("pos_y"));temppos.put("pos_z", tempanchor.get("pos_z"));
            tempanchor.remove("pos_x");tempanchor.remove("pos_y");tempanchor.remove("pos_z");
            tempanchor.put("pos", temppos);
            Map<String, Object> tempeuler = new HashMap<String, Object>();
            tempeuler.put("euler_x", tempanchor.get("euler_x"));tempeuler.put("euler_y", tempanchor.get("euler_y"));tempeuler.put("euler_z", tempanchor.get("euler_z"));
            tempanchor.remove("euler_x");tempanchor.remove("euler_y");tempanchor.remove("euler_z");
            tempanchor.put("euler", tempeuler);
            
            tempasset.put("id", tempasset.get("id").toString());
            tempanchor.put("id", tempanchor.get("id").toString());
            am.put("asset", tempasset);
            am.put("anchor", tempanchor);
            am.put("id", am.get("id").toString());
        }
        result.put("medias", all_mediaresult);
        result.put("id", result.get("id").toString());
        return result;
    } 
}
