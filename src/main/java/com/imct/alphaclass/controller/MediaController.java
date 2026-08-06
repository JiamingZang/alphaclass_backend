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
@Tag(name = "媒体", description = "关键词下媒体增删改查（image/model/translation/wiki/assistant）；写操作需登录且仅课程创建者可操作")
public class MediaController {
    private final MediaService service;
    private final TokenUtils tokenUtils;

    public MediaController(MediaService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias",method = RequestMethod.GET)
    @Operation(summary = "关键词媒体列表", description = "返回媒体数组；每项含 asset/anchor/color 嵌套与 type 专属信息(model/translation/wiki)，不含 kid/anchorid/assetid")
    public JSONResult getAllMediasByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword)  {
        List<Map<String, Object>> result = service.getAllMediasByKeyword(owner, course, keyword);
        return JSONResult.successWithData(result);   
    }

    /** 新增媒体（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias",method = RequestMethod.POST)
    @Operation(summary = "新增媒体", description = "需登录且仅创建者；body: {name, type, style, color{r,g,b}, asset_id?, anchor_id?, media_model?}，返回新增媒体")
    public JSONResult addMediaByKeyword(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        Map<String, Object> result = service.addMediaByKeyword(owner, course,keyword, params);
        return JSONResult.successWithData(result);   
    }

    /** 新增翻译/百科媒体（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias/trans_or_wiki",method = RequestMethod.POST)
    @Operation(summary = "新增翻译/百科媒体", description = "需登录且仅创建者；type=translation 时传 media_translation{word,translation_english,phonetic_UK,phonetic_US,sentence_CN,sentence_EN}，type=wiki 时传 media_wiki{word,title,description,url}")
    public JSONResult addTransOrWikiMediaByKeyword(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        Map<String, Object> result = service.addMediaTranslationOrWikiByKeyword(owner, course,keyword, params);
        return JSONResult.successWithData(result);   
    }

    /** 删除媒体（需登录，仅课程创建者可操作；媒体不存在或不属于该关键词时 404） */
    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.DELETE)
    @Operation(summary = "删除媒体", description = "需登录且仅创建者；成功返回 204，媒体不存在返回 404")
    public JSONResult deleteMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id) {
        if (tokenUtils.requireOwner(user) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        if (!service.deleteMediaById(course, user, keyword, media_id)) {
            return JSONResult.failWithMsg(Constants.CODE_404, "媒体不存在");
        }
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.GET)
    @Operation(summary = "媒体详情", description = "返回媒体（含 asset/anchor/color 嵌套），不存在返回 404")
    public JSONResult getMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id) {
        Map<String, Object> result = service.getMediaById(course, user, keyword, media_id);
        if (result == null) {
            return JSONResult.failWithMsg(Constants.CODE_404, "媒体不存在");
        }
        return JSONResult.successWithData(result);
    }

    /** 修改媒体（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.PUT)
    @Operation(summary = "修改媒体", description = "需登录且仅创建者；部分更新语义（未传字段沿用旧值），media_model/media_translation/media_wiki 有则更新无则新增")
    public JSONResult modifyMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(user) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        Map<String, Object> result = service.modifyMediaById(course, user, keyword, media_id, params);
        if (result == null) {
            return JSONResult.failWithMsg(Constants.CODE_404, "媒体不存在");
        }
        return JSONResult.successWithData(result);
    }
}
