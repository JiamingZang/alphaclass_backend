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

import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.CourseService;
import com.imct.alphaclass.utils.TokenUtils;


@RestController
public class CourseController {
    @Autowired
    private CourseService service;

    /** 查询用户全部课程（GET 放行，无需登录） */
    @RequestMapping(value = "/users/{owner}/courses", method =RequestMethod.GET)
    public JSONResult getAllByUser(@PathVariable String owner){
        return JSONResult.successWithData(service.getAllByUser(owner));
    }

    /** 新增课程（需登录，归属当前用户） */
    @RequestMapping(value =  "/user/courses", method =RequestMethod.POST)
    public JSONResult addCourse(@RequestBody Course course){
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        return JSONResult.successWithData(service.addCourse(user.getUsername(), course));
    }

    /** 按用户名+课程名查询单个课程（GET 放行）；不存在时返回 404 */
    @RequestMapping(value = "/courses/{owner}/{course}",method = RequestMethod.GET)
    public JSONResult getByUserAndName(@PathVariable String owner, @PathVariable String course) {
        Map<String, Object> result = service.getByUserAndName(owner,course);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_404, "课程不存在");
        }
    }

    /** 按课程 id 查询（GET 放行）；不存在时返回 404 */
    @RequestMapping(value = "/courses/actions/get-project-by-id",method = RequestMethod.GET)
    public JSONResult getById(@RequestParam(value = "id",required = true) int id) {
        Map<String, Object> result = service.getById(id);
        if (result!=null) {
            return JSONResult.successWithData(result);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_404, "课程不存在");
        }
    }

    /** 修改课程（需登录，仅课程创建者可修改） */
    @RequestMapping(value = "/courses/{owner}/{course}",method = RequestMethod.PUT)
    public JSONResult modifyByUserAndName(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        User user = TokenUtils.getCurrentUser();
        if (user != null && owner.equals(user.getUsername())) {   
            Map<String, Object> result = service.modifyByUserAndName(owner, course,params);
            if (result!=null) {
                return JSONResult.successWithData(result);
            }
        }
        return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
    }

    /** 删除课程（需登录） */
    @RequestMapping(value = "/courses/{owner}/{course}",method = RequestMethod.DELETE)
    public JSONResult deleteByUserAndName(@PathVariable String owner, @PathVariable String course) {
        if (TokenUtils.getCurrentUser()!=null) {
            service.deleteByUserAndName(owner, course);
            return JSONResult.customWithStatus(Constants.CODE_204);
        }else{
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
    }
}
