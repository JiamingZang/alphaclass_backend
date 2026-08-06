package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.MediaService;
import com.imct.alphaclass.utils.TokenUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "看点", description = "关键词下看点增删改查（image/model/translation/wiki/assistant）；写操作需登录且仅课程创建者可操作")
public class MediaController {
    private final MediaService service;
    private final TokenUtils tokenUtils;

    public MediaController(MediaService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias",method = RequestMethod.GET)
    @Operation(summary = "关键词看点列表", description = "返回看点数组；每项含 asset/anchor/color 嵌套与 type 专属信息(model/translation/wiki)，不含 kid/anchorid/assetid")
    public JSONResult getAllMediasByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword)  {
        List<Map<String, Object>> result = service.getAllMediasByKeyword(owner, course, keyword);
        return JSONResult.successWithData(result);   
    }

    /** 新增看点（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias",method = RequestMethod.POST)
    @Operation(summary = "新增看点", description = "需登录且仅创建者；body: {name, type, style, color{r,g,b}, asset_id?, anchor_id?, media_model?}，返回新增看点")
    public JSONResult addMediaByKeyword(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        Map<String, Object> result = service.addMediaByKeyword(owner, course,keyword, params);
        return JSONResult.successWithData(result);   
    }

    /** 新增翻译/百科看点（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias/trans_or_wiki",method = RequestMethod.POST)
    @Operation(summary = "新增翻译/百科看点", description = "需登录且仅创建者；type=translation 时传 media_translation{word,translation_english,phonetic_UK,phonetic_US,sentence_CN,sentence_EN}，type=wiki 时传 media_wiki{word,title,description,url}")
    public JSONResult addTransOrWikiMediaByKeyword(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        Map<String, Object> result = service.addMediaTranslationOrWikiByKeyword(owner, course,keyword, params);
        return JSONResult.successWithData(result);   
    }

    /** 删除看点（需登录，仅课程创建者可操作；看点不存在或不属于该关键词时 404） */
    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.DELETE)
    @Operation(summary = "删除看点", description = "需登录且仅创建者；成功返回 204，看点不存在返回 404")
    public JSONResult deleteMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id) {
        if (tokenUtils.requireOwner(user) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        if (!service.deleteMediaById(course, user, keyword, media_id)) {
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_MEDIA_NOT_FOUND);
        }
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.GET)
    @Operation(summary = "看点详情", description = "返回看点（含 asset/anchor/color 嵌套），不存在返回 404")
    public JSONResult getMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id) {
        Map<String, Object> result = service.getMediaById(course, user, keyword, media_id);
        if (result == null) {
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_MEDIA_NOT_FOUND);
        }
        return JSONResult.successWithData(result);
    }

    /** 修改看点（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.PUT)
    @Operation(summary = "修改看点", description = "需登录且仅创建者；部分更新语义（未传字段沿用旧值），media_model/media_translation/media_wiki 有则更新无则新增")
    public JSONResult modifyMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(user) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, Constants.MSG_OWNER_ONLY_MODIFY);
        }
        Map<String, Object> result = service.modifyMediaById(course, user, keyword, media_id, params);
        if (result == null) {
            return JSONResult.failWithMsg(Constants.CODE_404, Constants.MSG_MEDIA_NOT_FOUND);
        }
        return JSONResult.successWithData(result);
    }
}
