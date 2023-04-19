package com.imct.alphaclass.controller;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.Asset;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.AssetService;
import com.imct.alphaclass.utils.TokenUtils;

@RestController
// @RequestMapping("/user/assets")
public class AssetController {
    @Autowired
    private AssetService service;

    @RequestMapping(value = "/user/assets", method =RequestMethod.GET)
    public JSONResult getAllByUser(
        @RequestParam(value = "page",required = false,defaultValue = "1") int page,
        @RequestParam(value ="perpage",required = false,defaultValue = "5") int perpage,
        @RequestParam(name = "type",required = false) String tyoe){
        return JSONResult.successWithData(service.getAllByUser(TokenUtils.getCurrentUser().getUsername(),page,perpage,tyoe));
    }

    // 更改参数为map类型是为了能够解析出asset及modelinfo两种对象
    @RequestMapping(value =  "/user/assets", method =RequestMethod.POST)
    public JSONResult addCourse(@RequestBody Map<String, Object> params){
        Map<String, Object> result = service.addAsset(TokenUtils.getCurrentUser().getUsername(), params);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg("401", "");
        }
    }

    @RequestMapping(value =  "/user/assets/{id}", method =RequestMethod.DELETE)
    public void deleteCourseById(@PathVariable int id){
        service.deleteById(id);
    }
    
    @RequestMapping(value = "/user/assets/{id}",method = RequestMethod.PUT)
    public JSONResult modifyById(@PathVariable int id, @RequestBody Map<String, Object> params) {
        Map<String, Object> result = service.modifyById(id, params);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg("401", "");
        }
        
    }

    @RequestMapping(value = "/user/assets/{id}",method = RequestMethod.GET)
    public JSONResult getById(@PathVariable int id) {
        Map<String, Object> result = service.getAssetById(id);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg("401", "");
        }
        
    }
}