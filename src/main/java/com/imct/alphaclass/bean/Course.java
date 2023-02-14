package com.imct.alphaclass.bean;

import java.io.Serializable;

public class Course implements Serializable{
    private int id;
    private int uid;
    private String name;
    private String description;
    private String cover_url;
    private String created_at;
    private String updated_at;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getUid() {
        return uid;
    }
    public void setUid(int uid) {
        this.uid = uid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCover_url() {
        return cover_url;
    }
    public void setCover_url(String cover_url) {
        this.cover_url = cover_url;
    }
    public String getCreated_at() {
        return created_at;
    }
    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
    public String getUpdated_at() {
        return updated_at;
    }
    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }
    
}
