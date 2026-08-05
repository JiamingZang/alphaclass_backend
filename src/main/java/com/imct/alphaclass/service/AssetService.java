package com.imct.alphaclass.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.Asset;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.AssetDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetDAO dao;
    private final UserDAO userdao;

    /**
     * 查询用户资产列表：传 type 时按类型分页查询，否则查全部；
     * 统一过滤软删除行（deleted_at 非空），并格式化时间字段。
     */
    public List<Map<String, Object>> getAllByUser(String username, int page, int perpage, String type) {
        User user = requireUser(username);
        int m = (page - 1) * perpage;
        int n = perpage;
        List<Map<String, Object>> assets = type != null
                ? dao.getAllAssetsByUidAndPageAndType(user.getId(), m, n, type)
                : dao.getAllAssetsByUid(user.getId());
        DateTimeFormatter simple = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return assets.stream()
                .filter(asset -> asset.get("deleted_at") == null)
                .map(asset -> decorateAsset(asset, simple))
                .collect(Collectors.toList());
    }

    /** 复制并装饰单条 asset 行（id 转字符串/时间格式化/移除 uid 与 deleted_at），不污染 DAO 返回的列表 */
    private Map<String, Object> decorateAsset(Map<String, Object> asset, DateTimeFormatter simple) {
        Map<String, Object> result = new HashMap<String, Object>(asset);
        result.put("id", result.get("id").toString());
        result.remove("uid");
        result.put("created_at", simple.format((LocalDateTime) result.get("created_at")));
        result.put("updated_at", simple.format((LocalDateTime) result.get("updated_at")));
        result.remove("deleted_at");
        return result;
    }

    /** 查询单个资产（uid 移除、id 转字符串）；不存在时返回 null */
    public Map<String, Object> getAssetById(int id) {
        return toAssetMap(dao.getAssetById(id));
    }

    /** 新增资产：uid/时间戳由服务端填充，generated 未传时默认 false */
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

        return toAssetMap(assetResult);
    }

    /** 删除资产：软删除（deleted_at 置当前时间），不物理移除数据 */
    public void deleteById(int id) {
        dao.deleteAssetById(new Timestamp(System.currentTimeMillis()).toString(), id);
    }

    /** 修改资产名称（仅支持 name），返回更新后的资产响应 */
    public Map<String, Object> modifyById(int id, Map<String, Object> params) {
        if (params.get("name") != null) {
            dao.updateAssetById(params.get("name").toString(), new Timestamp(System.currentTimeMillis()).toString(), id);
        }

        Asset assetResult = dao.getAssetById(id);
        return toAssetMap(assetResult);
    }

    /** 用户不存在时抛 404（替代链式 NPE） */
    private User requireUser(String username) {
        User user = userdao.getByUsername(username);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        return user;
    }

    /** asset 响应公共组装（uid 移除、id 转字符串）；不存在时返回 null */
    private Map<String, Object> toAssetMap(Asset asset) {
        if (asset == null) {
            return null;
        }
        Map<String, Object> result = MapUtils.toMap(asset);
        result.remove("uid");
        result.put("id", result.get("id").toString());
        return result;
    }
}
