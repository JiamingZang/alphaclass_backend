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

@RestController
public class MediaController {
    private final MediaService service;
    private final TokenUtils tokenUtils;

    public MediaController(MediaService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias",method = RequestMethod.GET)
    public JSONResult getAllMediasByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword)  {
        List<Map<String, Object>> result = service.getAllMediasByKeyword(owner, course, keyword);
        return JSONResult.successWithData(result);   
    }

    /** 新增媒体（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias",method = RequestMethod.POST)
    public JSONResult addMediaByKeyword(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        Map<String, Object> result = service.addMediaByKeyword(owner, course,keyword, params);
        return JSONResult.successWithData(result);   
    }

    /** 新增翻译/百科媒体（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias/trans_or_wiki",method = RequestMethod.POST)
    public JSONResult addTransOrWikiMediaByKeyword(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        Map<String, Object> result = service.addMediaTranslationOrWikiByKeyword(owner, course,keyword, params);
        return JSONResult.successWithData(result);   
    }

    /** 删除媒体（需登录，仅课程创建者可操作；媒体不存在或不属于该关键词时 404） */
    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.DELETE)
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
    public JSONResult getMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id) {
        Map<String, Object> result = service.getMediaById(course, user, keyword, media_id);
        if (result == null) {
            return JSONResult.failWithMsg(Constants.CODE_404, "媒体不存在");
        }
        return JSONResult.successWithData(result);
    }

    /** 修改媒体（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.PUT)
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
