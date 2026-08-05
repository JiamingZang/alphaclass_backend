package com.imct.alphaclass.bean;

/**
 * 3D 模型生成结果记录（model_generate_result 表）。
 */
public class GenModelResult {
    private int id;
    private int user_id;
    private String job_id;
    private String request_id;
    private String type;
    private String prompt;
    private String prompt_image_url;
    private String url;
    private String thumbnail_url;
    /** 三角面数（表列为 int，保持数字类型与历史 Map 输出一致） */
    private Integer polygon_count;
    /** 模型尺寸（表列为 int，保持数字类型与历史 Map 输出一致） */
    private Integer size;
    private String created_at;
    private int is_deleted;
    private String task_status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getJob_id() {
        return job_id;
    }

    public void setJob_id(String job_id) {
        this.job_id = job_id;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt_image_url() {
        return prompt_image_url;
    }

    public void setPrompt_image_url(String prompt_image_url) {
        this.prompt_image_url = prompt_image_url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnail_url() {
        return thumbnail_url;
    }

    public void setThumbnail_url(String thumbnail_url) {
        this.thumbnail_url = thumbnail_url;
    }

    public Integer getPolygon_count() {
        return polygon_count;
    }

    public void setPolygon_count(Integer polygon_count) {
        this.polygon_count = polygon_count;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public int getIs_deleted() {
        return is_deleted;
    }

    public void setIs_deleted(int is_deleted) {
        this.is_deleted = is_deleted;
    }

    public String getTask_status() {
        return task_status;
    }

    public void setTask_status(String task_status) {
        this.task_status = task_status;
    }
}
