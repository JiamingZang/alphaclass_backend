package com.imct.alphaclass.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Asset;
import com.imct.alphaclass.bean.Modelinfo;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.dao.ModelinfoDAO;

@Service
public class AssetService {

    @Resource
    private AssetDAO dao;

    @Resource
    private UserDAO userdao;

    @Resource
    private ModelinfoDAO modelinfodao;
    
    public List<Map<String, Object>> getAllByUser(String username,int page,int perpage,String type){
        User user = userdao.getByUsername(username);
        // System.out.println(type);
        int m = (page-1)*perpage;
        int n = perpage;
        List<Map<String, Object>> assets;
        if (type !=null) {
            assets= dao.getAllAssetsByUidAndPageAndType(user.getId(),m,n,type);
        }else{
            assets= dao.getAllAssetsByUid(user.getId());
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        DateTimeFormatter simple = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Map<String,Object> asset : assets) {
            asset.put("id", asset.get("id").toString());
            asset.remove("uid");
        
            asset.put("created_at", simple.format((LocalDateTime)asset.get("created_at")));
            asset.put("updated_at", simple.format((LocalDateTime)asset.get("updated_at")));
            if (asset.get("deleted_at")==null) {
                asset.remove("deleted_at");
                
                // 如果为model类型则添加modelinfo字段
                if (asset.get("type").toString().equals("model")) {
                    Modelinfo modelinfo = modelinfodao.getModelinfoById(Integer.valueOf(asset.get("id").toString()));
                    // "model_info": {
                    //     "anime_to_play": "\"take 001\"",
                    //     "scale": {
                    //       "scale_x": 1,
                    //       "scale_y": 1,
                    //       "scale_z": 1
                    //     }
                    //   }
                    if (modelinfo!=null) {
                        
                        Map<String,Object> scale = new HashMap<String,Object>();
                        scale.put("scale_x", modelinfo.getScale_x());
                        scale.put("scale_y", modelinfo.getScale_y());
                        scale.put("scale_z", modelinfo.getScale_z());
                        Map<String,Object> model_info = new HashMap<String,Object>();
                        model_info.put("anime_to_play", modelinfo.getAnime_to_play());
                        model_info.put("scale", scale);
                        // 加入asset中
                        asset.put("model_info", model_info);
                    }
                }
                result.add(asset);
            }
        }
        return result;
    }

    public Map<String, Object> addAsset(String username,Map<String, Object> params){
        Modelinfo modelinfo = new Modelinfo();
        Asset asset = new Asset();
        asset.setName(params.get("name").toString());
        asset.setType(params.get("type").toString());
        asset.setUrl(params.get("url").toString());
        asset.setSize((int)params.get("size"));
        asset.setThumbnail_url(params.get("thumbnail_url").toString());
        // 如果没有model_info就直接转为asset对象
        if (params.get("model_info")!=null) {
            // 试一下能不能转成
            // asset = JSON.parseObject(params,new TypeReference<Asset>() {});
            Map<String,Object> model_info = (Map<String, Object>)params.get("model_info");
            modelinfo.setAnime_to_play(model_info.get("anime_to_play").toString());
            Map<String,Object> scale = (Map<String, Object>)model_info.get("scale");
            modelinfo.setScale_x(Float.valueOf(scale.get("scale_x").toString()));
            modelinfo.setScale_y(Float.valueOf(scale.get("scale_y").toString()));
            modelinfo.setScale_z(Float.valueOf(scale.get("scale_z").toString()));
        }

        User user = userdao.getByUsername(username);
        asset.setUid(user.getId());
        asset.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        asset.setUpdated_at(new Timestamp(System.currentTimeMillis()).toString());
        dao.addAsset(asset);
        Asset assetResult = dao.getAssetById(asset.getId());
        // 添加modelinfo
        if (params.get("model_info")!=null) {   
            modelinfo.setId(assetResult.getId());
            modelinfodao.addModelinfo(modelinfo);
        }
        Map<String, Object> result = JSON.parseObject(JSON.toJSONString(assetResult), new TypeReference<Map<String, Object>>() {});
        result.remove("uid");
        result.put("id", result.get("id").toString());
        // 如果为model类型则添加modelinfo字段
        if (result.get("type").toString().equals("model")) {
            modelinfo = modelinfodao.getModelinfoById(Integer.valueOf(result.get("id").toString()));
            // "model_info": {
            //     "anime_to_play": "\"take 001\"",
            //     "scale": {
            //       "scale_x": 1,
            //       "scale_y": 1,
            //       "scale_z": 1
            //     }
            //   }
            if (modelinfo!=null) {
                
                Map<String,Object> scale = new HashMap<String,Object>();
                scale.put("scale_x", modelinfo.getScale_x());
                scale.put("scale_y", modelinfo.getScale_y());
                scale.put("scale_z", modelinfo.getScale_z());
                Map<String,Object> model_info = new HashMap<String,Object>();
                model_info.put("anime_to_play", modelinfo.getAnime_to_play());
                model_info.put("scale", scale);
                // 加入asset中
                result.put("model_info", model_info);
            }
        }
        return result;
    }

    public void deleteById(int id){
        dao.deleteAssetById(new Timestamp(System.currentTimeMillis()).toString(), id);
    }

    public Map<String, Object> modifyById(int id, Map<String, Object> params){
        if(params.get("name")!=null){
            dao.updateAssetById(params.get("name").toString(), new Timestamp(System.currentTimeMillis()).toString(), id);
        }
        if (params.get("model_info")!=null){
            Modelinfo modelinfo = new Modelinfo();
            Map<String,Object> model_info = (Map<String, Object>)params.get("model_info");
            Map<String,Object> scale = (Map<String, Object>)model_info.get("scale");
            modelinfodao.updateModelinfoById(
                model_info.get("anime_to_play").toString(), 
                Float.valueOf(scale.get("scale_x").toString()), 
                Float.valueOf(scale.get("scale_y").toString()), 
                Float.valueOf(scale.get("scale_z").toString()), 
                id);
        }


        Asset assetResult = dao.getAssetById(id);
        Map<String, Object> result = JSON.parseObject(JSON.toJSONString(assetResult), new TypeReference<Map<String, Object>>() {});
        result.remove("uid");
        result.put("id", result.get("id").toString());
        // 如果为model类型则添加modelinfo字段
        if (result.get("type").toString().equals("model")) {
            Modelinfo modelinfo = modelinfodao.getModelinfoById(Integer.valueOf(result.get("id").toString()));
            // "model_info": {
            //     "anime_to_play": "\"take 001\"",
            //     "scale": {
            //       "scale_x": 1,
            //       "scale_y": 1,
            //       "scale_z": 1
            //     }
            //   }
            if (modelinfo!=null) {
                
                Map<String,Object> scale = new HashMap<String,Object>();
                scale.put("scale_x", modelinfo.getScale_x());
                scale.put("scale_y", modelinfo.getScale_y());
                scale.put("scale_z", modelinfo.getScale_z());
                Map<String,Object> model_info = new HashMap<String,Object>();
                model_info.put("anime_to_play", modelinfo.getAnime_to_play());
                model_info.put("scale", scale);
                // 加入asset中
                result.put("model_info", model_info);
            }
        }
        return result;
        
    }
}