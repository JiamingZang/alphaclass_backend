package com.imct.alphaclass.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.Asset;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

@Service
public class AssetService {

    @Resource
    private AssetDAO dao;

    @Resource
    private UserDAO userdao;

    public List<Map<String, Object>> getAllByUser(String username, int page, int perpage, String type) {
        User user = requireUser(username);
        int m = (page - 1) * perpage;
        int n = perpage;
        List<Map<String, Object>> assets;
        if (type != null) {
            assets = dao.getAllAssetsByUidAndPageAndType(user.getId(), m, n, type);
        } else {
            assets = dao.getAllAssetsByUid(user.getId());
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        DateTimeFormatter simple = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Map<String, Object> asset : assets) {
            asset.put("id", asset.get("id").toString());
            asset.remove("uid");
            asset.put("created_at", simple.format((LocalDateTime) asset.get("created_at")));
            asset.put("updated_at", simple.format((LocalDateTime) asset.get("updated_at")));
            if (asset.get("deleted_at") == null) {
                asset.remove("deleted_at");
                result.add(asset);
            }
        }
        return result;
    }

    public Map<String, Object> getAssetById(int id) {
        Asset assetResult = dao.getAssetById(id);
        Map<String, Object> result = MapUtils.toMap(assetResult);
        result.remove("uid");
        result.put("id", result.get("id").toString());
        return result;
    }

    public Map<String, Object> addAsset(String username, Map<String, Object> params) {
        Asset asset = new Asset();
        asset.setName(params.get("name").toString());
        asset.setType(params.get("type").toString());
        asset.setUrl(params.get("url").toString());
        asset.setSize((int) params.get("size"));
        asset.setThumbnail_url(params.get("thumbnail_url").toString());
        if (params.get("generated") != null) {
            asset.setGenerated(params.get("generated").toString().equals("true"));
        } else {
            asset.setGenerated(false);
        }

        User user = requireUser(username);
        asset.setUid(user.getId());
        asset.setCreated_at(new Timestamp(System.currentTimeMillis()).toString());
        asset.setUpdated_at(new Timestamp(System.currentTimeMillis()).toString());
        dao.addAsset(asset);
        Asset assetResult = dao.getAssetById(asset.getId());

        Map<String, Object> result = MapUtils.toMap(assetResult);
        result.remove("uid");
        result.put("id", result.get("id").toString());
        return result;
    }

    public void deleteById(int id) {
        dao.deleteAssetById(new Timestamp(System.currentTimeMillis()).toString(), id);
    }

    public Map<String, Object> modifyById(int id, Map<String, Object> params) {
        if (params.get("name") != null) {
            dao.updateAssetById(params.get("name").toString(), new Timestamp(System.currentTimeMillis()).toString(), id);
        }

        Asset assetResult = dao.getAssetById(id);
        Map<String, Object> result = MapUtils.toMap(assetResult);
        result.remove("uid");
        result.put("id", result.get("id").toString());
        return result;
    }

    /** 用户不存在时抛 404（替代链式 NPE） */
    private User requireUser(String username) {
        User user = userdao.getByUsername(username);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        return user;
    }
}
