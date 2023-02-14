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
import com.imct.alphaclass.bean.Media;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.AnchorDAO;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.KeywordDAO;
import com.imct.alphaclass.dao.MediaDAO;
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

    public Map<String, Object> addMediaByKeyword(String ownername, String coursename,String keywordname, Map<String, Object> params){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = new Media();
        media.setName(params.get("name").toString());
        media.setType(params.get("type").toString());
        media.setStyle(params.get("style").toString());
        Map<String,Object> color = (Map<String,Object>)params.get("color");
        media.setColor_r(Float.parseFloat("".equals(color.get("r").toString())?"0.0":color.get("r").toString()));
        media.setColor_g(Float.parseFloat("".equals(color.get("g").toString())?"0.0":color.get("g").toString()));
        media.setColor_b(Float.parseFloat("".equals(color.get("b").toString())?"0.0":color.get("b").toString()));
        media.setAssetid(Integer.parseInt("".equals(params.get("asset_id").toString())?"0":params.get("asset_id").toString()));
        media.setAnchorid(Integer.parseInt("".equals(params.get("anchor_id").toString())?"0":params.get("anchor_id").toString()));
        media.setKid(keyword.getId());
        dao.addMedia(media);
        media = dao.getMediaById(media.getId());
        Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(media), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> tempasset = JSON.parseObject(JSON.toJSONString(assetdao.getAssetById(media.getAssetid())), new TypeReference<Map<String, Object>>() {});
        tempasset.remove("uid");tempasset.remove("deleted_at");
        Map<String, Object> tempanchor = JSON.parseObject(JSON.toJSONString(anchordao.getAnchorById(media.getAnchorid())), new TypeReference<Map<String, Object>>() {});
        tempanchor.remove("pos_x");tempanchor.remove("pos_y");tempanchor.remove("pos_z");tempanchor.remove("euler_x");tempanchor.remove("euler_y");tempanchor.remove("euler_z");
        tempanchor.remove("cid");
        ac.remove("kid");ac.remove("anchorid");ac.remove("assetid");

        tempanchor.put("id", tempanchor.get("id").toString());
        tempasset.put("id", tempasset.get("id").toString());

        Map<String,Object> tempcolor = new HashMap<String,Object>();
        tempcolor.put("r", media.getColor_r());
        tempcolor.put("g", media.getColor_g());
        tempcolor.put("b", media.getColor_b());
        ac.remove("color_r");ac.remove("color_g");ac.remove("color_b");
        ac.remove("kid");ac.remove("anchorid");ac.remove("assetid");

        ac.put("asset", tempasset);
        ac.put("anchor", tempanchor);
        ac.put("color", tempcolor);
        ac.put("id", ac.get("id").toString());
        return ac;
    }

    public void deleteMediaById(String coursename, String ownername, String keywordname,int media_id){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = dao.getMediaById(media_id);
        if (media.getKid() == keyword.getId()) {
            dao.deleteMediaById(media_id);
        }
    }

    public List<Map<String, Object>> getAllMediasByKeyword(String ownername,String coursename,String keywordname){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        List<Map<String, Object>> all_mediaresult = dao.getAllMediasByKid(keyword.getId());
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
        return all_mediaresult;
    }
    
    public Map<String, Object> getMediaById(String coursename, String ownername, String keywordname,int media_id){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media media = dao.getMediaById(media_id);
        if (media!=null&&media.getKid()==keyword.getId()) {
            Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(media), new TypeReference<Map<String, Object>>() {});
            Map<String, Object> tempasset = JSON.parseObject(JSON.toJSONString(assetdao.getAssetById(media.getAssetid())), new TypeReference<Map<String, Object>>() {});
            tempasset.remove("uid");tempasset.remove("deleted_at");
            Map<String, Object> tempanchor = JSON.parseObject(JSON.toJSONString(anchordao.getAnchorById(media.getAnchorid())), new TypeReference<Map<String, Object>>() {});
            tempanchor.remove("pos_x");tempanchor.remove("pos_y");tempanchor.remove("pos_z");tempanchor.remove("euler_x");tempanchor.remove("euler_y");tempanchor.remove("euler_z");
            tempanchor.remove("cid");
            ac.remove("kid");ac.remove("anchorid");ac.remove("assetid");

            Map<String,Object> tempcolor = new HashMap<String,Object>();
            tempcolor.put("r", media.getColor_r());
            tempcolor.put("g", media.getColor_g());
            tempcolor.put("b", media.getColor_b());
            ac.remove("color_r");ac.remove("color_g");ac.remove("color_b");
            ac.remove("kid");ac.remove("anchorid");ac.remove("assetid");

            tempanchor.put("id", tempanchor.get("id").toString());
            tempasset.put("id", tempasset.get("id").toString());
            ac.put("asset", tempasset);
            ac.put("anchor", tempanchor);
            ac.put("color", tempcolor);

            ac.put("id", ac.get("id").toString());
            return ac;
        }else{
            return null;
        }
    }

    public Map<String, Object> modifyMediaById(String coursename, String ownername, String keywordname,int media_id,Map<String,Object> params){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Keyword keyword = keyworddao.getKeywordByCidAndName(course.getId(), keywordname);
        Media old_media = dao.getMediaById(media_id);
        float color_r;float color_g;float color_b;
        if (params.get("color") != null) {   
            Map<String,Object> color = (Map<String,Object>)params.get("color");
            color_r = Float.parseFloat("".equals(color.get("r").toString())?"0.0":color.get("r").toString());
            color_g = Float.parseFloat("".equals(color.get("g").toString())?"0.0":color.get("g").toString());
            color_b = Float.parseFloat("".equals(color.get("b").toString())?"0.0":color.get("b").toString());
        }else{
            color_r = old_media.getColor_r();
            color_g = old_media.getColor_g();
            color_b = old_media.getColor_b();
        }
        dao.updateMediaById(
            params.get("name")==null?old_media.getName():params.get("name").toString(),
            params.get("type")==null?old_media.getType():params.get("type").toString(),
            params.get("asset_id")==null?old_media.getAssetid():(int)params.get("asset_id"), 
            params.get("anchor_id")==null?old_media.getAnchorid():(int)params.get("anchor_id"),
            params.get("style")==null?old_media.getStyle():params.get("style").toString(),
            color_r,color_g,color_b, media_id);
        
        Media media = dao.getMediaById(media_id);
        if (media!=null&&media.getKid()==keyword.getId()) {
            Map<String, Object> ac = JSON.parseObject(JSON.toJSONString(media), new TypeReference<Map<String, Object>>() {});
            Map<String, Object> tempasset = JSON.parseObject(JSON.toJSONString(assetdao.getAssetById(media.getAssetid())), new TypeReference<Map<String, Object>>() {});
            tempasset.remove("uid");tempasset.remove("deleted_at");

            Map<String, Object> tempanchor = JSON.parseObject(JSON.toJSONString(anchordao.getAnchorById(media.getAnchorid())), new TypeReference<Map<String, Object>>() {});
            
            Map<String, Object> temppos = new HashMap<String, Object>();
            temppos.put("pos_x", tempanchor.get("pos_x"));temppos.put("pos_y", tempanchor.get("pos_y"));temppos.put("pos_z", tempanchor.get("pos_z"));
            tempanchor.remove("pos_x");tempanchor.remove("pos_y");tempanchor.remove("pos_z");
            tempanchor.put("pos", temppos);
            Map<String, Object> tempeuler = new HashMap<String, Object>();
            tempeuler.put("euler_x", tempanchor.get("euler_x"));tempeuler.put("euler_y", tempanchor.get("euler_y"));tempeuler.put("euler_z", tempanchor.get("euler_z"));
            tempanchor.remove("euler_x");tempanchor.remove("euler_y");tempanchor.remove("euler_z");
            tempanchor.put("euler", tempeuler);
            tempanchor.remove("cid");
            Map<String,Object> tempcolor = new HashMap<String,Object>();
            tempcolor.put("r", media.getColor_r());
            tempcolor.put("g", media.getColor_g());
            tempcolor.put("b", media.getColor_b());
            ac.remove("color_r");ac.remove("color_g");ac.remove("color_b");
            ac.remove("kid");ac.remove("anchorid");ac.remove("assetid");

            tempanchor.put("id", tempanchor.get("id").toString());
            tempasset.put("id", tempasset.get("id").toString());
            ac.put("asset", tempasset);
            ac.put("anchor", tempanchor);
            ac.put("color", tempcolor);
            ac.put("id", ac.get("id").toString());
            return ac;
        }else{
            return null;
        }
    }
}
