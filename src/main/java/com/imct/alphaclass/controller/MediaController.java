package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.MediaService;

@RestController
public class MediaController {
    @Autowired
    private MediaService service;

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias",method = RequestMethod.GET)
    public JSONResult getAllMediasByCourse(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword)  {
        List<Map<String, Object>> result = service.getAllMediasByKeyword(owner, course, keyword);
        return JSONResult.successWithData(result);   
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias",method = RequestMethod.POST)
    public JSONResult addMediaByKeyword(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        Map<String, Object> result = service.addMediaByKeyword(owner, course,keyword, params);
        return JSONResult.successWithData(result);   
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias/trans_or_wiki",method = RequestMethod.POST)
    public JSONResult addTransOrWikiMediaByKeyword(@PathVariable String owner, @PathVariable String course,@PathVariable String keyword,@RequestBody Map<String, Object> params) {
        Map<String, Object> result = service.addMediaTranslationOrWikiByKeyword(owner, course,keyword, params);
        return JSONResult.successWithData(result);   
    }

    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.DELETE)
    public void deleteMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id) {
        service.deleteMediaById(course, user, keyword, media_id);
    }

    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.GET)
    public JSONResult getMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id) {
        return JSONResult.successWithData(service.getMediaById(course, user, keyword, media_id));
    }

    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}",method = RequestMethod.PUT)
    public JSONResult modifyMediaById(@PathVariable String course, @PathVariable String user,@PathVariable String keyword,@PathVariable int media_id,@RequestBody Map<String, Object> params) {
        return JSONResult.successWithData(service.modifyMediaById(course, user, keyword, media_id, params));
    }
}
