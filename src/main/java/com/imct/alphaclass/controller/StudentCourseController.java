package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.StudentCourseService;
import com.imct.alphaclass.utils.TokenUtils;

@RestController
public class StudentCourseController {
    private final StudentCourseService service;
    private final TokenUtils tokenUtils;

    public StudentCourseController(StudentCourseService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.GET)
    public JSONResult getAllStudentsByCourse(@PathVariable String owner, @PathVariable String course) {
        return JSONResult.successWithData(service.getAllStudentsByCourse(owner, course));
    }

    /** 批量添加学生（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.POST)
    public JSONResult addStudentsByUsername(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        service.addStudentsByUsername((List<String>)params.get("students"), owner, course);
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    /** 批量删除学生（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.DELETE)
    public JSONResult deleteStudentsByUsername(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        service.deleteStudentsByUsername((List<String>)params.get("students"), owner, course);
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    @RequestMapping(value = "/user/register-courses",method = RequestMethod.GET)
    public JSONResult getLoginUserCourses() {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        return JSONResult.successWithData(service.getLoginUserCourses(user.getId()));
    }
}
