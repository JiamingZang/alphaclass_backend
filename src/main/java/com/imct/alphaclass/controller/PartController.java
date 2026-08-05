package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.PartService;
import com.imct.alphaclass.utils.TokenUtils;

@RestController
public class PartController {
    @Autowired
    private PartService service;

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias/{mediaId}/parts", method = RequestMethod.GET)
    public JSONResult getAllPartsByMedia_ID(@PathVariable String owner, @PathVariable String course,
            @PathVariable String keyword, @PathVariable int media_id) {
        List<Map<String, Object>> result = service.getAllPartsByMediaID(owner, course, keyword, media_id);
        service.deletePartsByMediaID(course, owner, keyword, media_id);
        return JSONResult.successWithData(result);
    }

    @RequestMapping(value = "/courses/{owner}/{course}/{keyword}/medias/{mediaid}/parts", method = RequestMethod.POST)
    public JSONResult addPartByMedia_ID(@PathVariable String owner, @PathVariable String course,
            @PathVariable String keyword, @PathVariable String mediaid, @RequestBody Map<String, Object> params) {
        Map<String, Object> result = service.addPartByMediaModel(owner, course, keyword, mediaid, params);
        return JSONResult.successWithData(result);
    }

    @RequestMapping(value = "/courses/{user}/{course}/{keyword}/medias/{media_id}/parts", method = RequestMethod.DELETE)
    public void deletePartByMediaId(@PathVariable String course, @PathVariable String user,
            @PathVariable String keyword,
            @PathVariable int media_id) {
        service.deletePartsByMediaID(course, user, keyword, media_id);
    }
    /*
     * @RequestMapping(value =
     * "/courses/{user}/{course}/{keyword}/medias/{media_id}", method =
     * RequestMethod.GET)
     * public JSONResult getMediaById(@PathVariable String course, @PathVariable
     * String user, @PathVariable String keyword,
     * 
     * @PathVariable int media_id) {
     * return JSONResult.successWithData(service.getMediaById(course, user, keyword,
     * media_id));
     * }
     * 
     * @RequestMapping(value =
     * "/courses/{user}/{course}/{keyword}/medias/{media_id}", method =
     * RequestMethod.PUT)
     * public JSONResult modifyMediaById(@PathVariable String course, @PathVariable
     * String user,
     * 
     * @PathVariable String keyword, @PathVariable int media_id, @RequestBody
     * Map<String, Object> params) {
     * return JSONResult.successWithData(service.modifyMediaById(course, user,
     * keyword, media_id, params));
     * }
     */
}