package com.imct.alphaclass.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private StudentCourseService service;

    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.GET)
    public JSONResult getAllStudentsByCourse(@PathVariable String owner, @PathVariable String course) {
        return JSONResult.successWithData(service.getAllStudentsByCourse(owner, course));
    }

    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.POST)
    public void addStudentsByUsername(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        service.addStudentsByUsername((List<String>)params.get("students"), owner, course);
    }

    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.DELETE)
    public void deleteStudentsByUsername(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        service.deleteStudentsByUsername((List<String>)params.get("students"), owner, course);
    }

    @RequestMapping(value = "/user/register-courses",method = RequestMethod.GET)
    public JSONResult getLoginUserCourses() {
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        return JSONResult.successWithData(service.getLoginUserCourses(user.getId()));
    }
}
