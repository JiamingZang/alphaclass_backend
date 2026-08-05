package com.imct.alphaclass.bean;

/**
 * AI 服务调用记录（service_usage 表）。
 */
public class ServiceUsage {
    private int id;
    private int user_id;
    private int service_id;
    private String created_at;
    private int is_successful;

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

    public int getService_id() {
        return service_id;
    }

    public void setService_id(int service_id) {
        this.service_id = service_id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public int getIs_successful() {
        return is_successful;
    }

    public void setIs_successful(int is_successful) {
        this.is_successful = is_successful;
    }
}
