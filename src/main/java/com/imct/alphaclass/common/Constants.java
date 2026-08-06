package com.imct.alphaclass.common;

public interface Constants {
    String CODE_200="200";
    String CODE_204="204";
    String CODE_400="400";//参数错误
    String CODE_401="401";
    String CODE_403="403";//权限不足/超出限额
    String CODE_404="404";
    String CODE_405="405";//请求方法不支持
    String CODE_415="415";//Content-Type 不支持
    String CODE_500="500";
    String CODE_503="503";//服务不可用（第三方生成失败）
    String CODE_600="600";//业务异常

    // 通用错误文案（Controller 共用，避免散落裸字符串；与契约快照/测试断言绑定，改动需同步更新）
    String MSG_NO_TOKEN = "无token";
    String MSG_OWNER_ONLY_MODIFY = "仅课程创建者可修改";
    String MSG_OWNER_ONLY_DELETE = "仅课程创建者可删除";
    String MSG_COURSE_NOT_FOUND = "课程不存在";
    String MSG_ANCHOR_NOT_FOUND = "锚点不存在";
    String MSG_MEDIA_NOT_FOUND = "媒体不存在";
    String MSG_ASSET_NOT_FOUND = "资源不存在";
    String MSG_USERNAME_TAKEN = "用户名已被注册";
    String MSG_AUTH_FAILED = "认证失败";
    String MSG_VERIFY_FAILED = "验证失败";
    String MSG_PAGE_INVALID = "分页参数不合法";
    String MSG_PROMPT_REQUIRED = "缺少 prompt 参数";
    String MSG_PROMPT_SIZE_REQUIRED = "缺少 prompt/size 参数";
    String MSG_PROMPT_SIZE_IMAGE_REQUIRED = "缺少 prompt/size/image_url 参数";
    String MSG_STUDENTS_INVALID = "students 参数不合法";
}
