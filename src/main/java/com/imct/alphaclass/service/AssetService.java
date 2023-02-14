package com.imct.alphaclass.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.bean.Asset;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.UserDAO;

@Service
public class AssetService {

    @Resource
    private AssetDAO dao;

    @Resource
    private UserDAO userdao;
    
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
            // Map<String, Object> userResult = JSON.parseObject(JSON.toJSONString(asset), new TypeReference<Map<String, Object>>() {});
            // userResult.put("url", "https://123.56.224.193/courses/"+user.getUsername());
            asset.put("id", asset.get("id").toString());
            asset.remove("uid");
        
            asset.put("created_at", simple.format((LocalDateTime)asset.get("created_at")));
            asset.put("updated_at", simple.format((LocalDateTime)asset.get("updated_at")));
            if (asset.get("deleted_at")==null) {
                asset.remove("deleted_at");
                result.add(asset);
            }
        }
        return result;
    }

    public Map<String, Object> addAsset(String username,Asset asset){
        User user = userdao.getByUsername(username);
        asset.setUid(user.getId());
        asset.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        asset.setUpdated_at(new Timestamp(System.currentTimeMillis()).toString());
        dao.addAsset(asset);
        Asset assetResult = dao.getAssetById(asset.getId());
        Map<String, Object> result = JSON.parseObject(JSON.toJSONString(assetResult), new TypeReference<Map<String, Object>>() {});
        result.remove("uid");
        result.put("id", result.get("id").toString());
        return result;
    }

    public void deleteById(int id){
        dao.deleteAssetById(new Timestamp(System.currentTimeMillis()).toString(), id);
    }

    public Map<String, Object> modifyById(int id, Map<String, Object> params){
        dao.updateAssetById(params.get("name").toString(), new Timestamp(System.currentTimeMillis()).toString(), id);
        Asset assetResult = dao.getAssetById(id);
        Map<String, Object> result = JSON.parseObject(JSON.toJSONString(assetResult), new TypeReference<Map<String, Object>>() {});
        result.remove("uid");
        result.put("id", result.get("id").toString());
        return result;
        
    }
}
