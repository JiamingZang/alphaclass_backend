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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "选课", description = "课程学生列表/批量添加/批量移除/当前用户已选课程")
public class StudentCourseController {
    private final StudentCourseService service;
    private final TokenUtils tokenUtils;

    public StudentCourseController(StudentCourseService service, TokenUtils tokenUtils) {
        this.service = service;
        this.tokenUtils = tokenUtils;
    }

    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.GET)
    @Operation(summary = "课程学生列表", description = "返回学生用户数组（不含 password）")
    public JSONResult getAllStudentsByCourse(@PathVariable String owner, @PathVariable String course) {
        return JSONResult.successWithData(service.getAllStudentsByCourse(owner, course));
    }

    /** 批量添加学生（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.POST)
    @Operation(summary = "批量添加学生", description = "需登录且仅创建者；body: {students: [用户名...]}，成功返回 204")
    public JSONResult addStudentsByUsername(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        List<String> students = studentList(params);
        if (students == null) {
            return JSONResult.failWithMsg(Constants.CODE_400, "students 参数不合法");
        }
        service.addStudentsByUsername(students, owner, course);
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    /** 批量删除学生（需登录，仅课程创建者可操作） */
    @RequestMapping(value = "/courses/{owner}/{course}/students",method = RequestMethod.DELETE)
    @Operation(summary = "批量移除学生", description = "需登录且仅创建者；body: {students: [用户名...]}，成功返回 204")
    public JSONResult deleteStudentsByUsername(@PathVariable String owner, @PathVariable String course,@RequestBody Map<String, Object> params) {
        if (tokenUtils.requireOwner(owner) == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "仅课程创建者可修改");
        }
        List<String> students = studentList(params);
        if (students == null) {
            return JSONResult.failWithMsg(Constants.CODE_400, "students 参数不合法");
        }
        service.deleteStudentsByUsername(students, owner, course);
        return JSONResult.customWithStatus(Constants.CODE_204);
    }

    /** 解析 students 列表参数：缺失/非列表时返回 null（替代直接强转的 ClassCastException） */
    @SuppressWarnings("unchecked")
    private static List<String> studentList(Map<String, Object> params) {
        Object students = params.get("students");
        if (students instanceof List) {
            return (List<String>) students;
        }
        return null;
    }

    @RequestMapping(value = "/user/register-courses",method = RequestMethod.GET)
    @Operation(summary = "当前用户已选课程", description = "需识别用户（无 token 返回 401）；返回当前用户加入的课程列表")
    public JSONResult getLoginUserCourses() {
        User user = tokenUtils.getCurrentUser();
        if (user == null) {
            return JSONResult.failWithMsg(Constants.CODE_401, "无token");
        }
        return JSONResult.successWithData(service.getLoginUserCourses(user.getId()));
    }
}
