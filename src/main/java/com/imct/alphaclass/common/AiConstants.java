package com.imct.alphaclass.common;

/**
 * AI 生成相关常量：任务状态、服务 id、每日限额、第三方 API 地址与 OSS 目录。
 * 各 AI Service 与定时任务共用，避免魔法字符串/数字散落。
 */
public final class AiConstants {

    private AiConstants() {
    }

    // ---------- 任务状态（模型/视频生成） ----------

    /** 模型任务初始状态（等待定时轮询） */
    public static final String TASK_GENERATING = "GENERATING";
    /** 视频任务处理中状态（历史查询时实时轮询） */
    public static final String TASK_PROCESSING = "PROCESSING";
    /** 腾讯云侧任务失败态 */
    public static final String TASK_FAIL = "FAIL";
    /** 腾讯云侧任务完成态 */
    public static final String TASK_DONE = "DONE";
    /** 本地落库失败态 */
    public static final String TASK_FAILED = "FAILED";
    /** 本地落库完成态（产物已上传 OSS） */
    public static final String TASK_FINISHED = "FINISHED";

    // ---------- 生成类型（写入 type 字段） ----------

    public static final String TYPE_TEXT_TO_MODEL = "textToModel";
    public static final String TYPE_IMAGE_TO_MODEL = "imageToModel";
    public static final String TYPE_TEXT_TO_VIDEO = "TextToVideo";
    public static final String TYPE_IMAGE_TO_VIDEO = "ImageToVideo";

    /** 模型默认输出格式 */
    public static final String RESULT_FORMAT_GLB = "GLB";

    // ---------- 服务与限额 ----------

    /** service_usage.service_id：文生图 */
    public static final int SERVICE_ID_TEXT_TO_IMAGE = 1;
    /** 当日生成次数上限（模型/视频共用） */
    public static final int DAILY_GENERATION_LIMIT = 10;
    /** 超限拒绝提示 */
    public static final String EXCEED_LIMIT_MESSAGE = "You have exceeded generation limit per day.";

    // ---------- 视频参数 ----------

    public static final String VIDEO_MODEL = "cogvideox-3";
    public static final String VIDEO_QUALITY_QUALITY = "quality";
    public static final String VIDEO_QUALITY_SPEED = "speed";
    public static final int VIDEO_FPS_HIGH = 60;
    public static final int VIDEO_FPS_DEFAULT = 30;

    // ---------- 文生图参数 ----------

    /** 百度 SD-XL 出图尺寸 */
    public static final String TEXT_TO_IMAGE_SIZE = "1024x576";

    // ---------- 第三方 API 地址 ----------

    public static final String BAIDU_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    public static final String BAIDU_SDXL_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/text2image/sd_xl";
    public static final String YOUDAO_API_URL = "https://openapi.youdao.com/api";
    public static final String YOUDAO_WEB_RESULT_URL = "https://www.youdao.com/result";
    public static final String YOUDAO_MOBILE_SINGLEDICT_URL = "https://mobile.youdao.com/singledict";
    public static final String BAIKE_SEARCH_URL = "http://baike.baidu.com/search/word";
    public static final String BAIKE_PAGE_PREFIX = "https://baike.baidu.com";
    public static final String TENCENT_AI3D_ENDPOINT = "ai3d.tencentcloudapi.com";
    public static final String TENCENT_AI3D_REGION = "ap-guangzhou";

    /** 视频产物链接转发前缀（智谱直链 https:// 替换为目标前缀） */
    public static final String HTTPS_PREFIX = "https://";

    // ---------- OSS 对象目录 ----------

    public static final String OSS_IMAGE_DIR = "assets/aigc_images/";
    public static final String OSS_MODEL_DIR = "assets/aigc_models/models/";
    public static final String OSS_MODEL_THUMBNAIL_DIR = "assets/aigc_models/thumbnails/";

    /** 文生图超时/解码失败的标记值 */
    public static final String TIMEOUT_MARK = "timeout";
}
