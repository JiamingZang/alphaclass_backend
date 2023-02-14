package com.imct.alphaclass.bean;

import java.io.Serializable;

import com.alibaba.fastjson.annotation.JSONField;

public class Asset implements Serializable{
    private int id;
    private int uid;
    private String name;
    private String type;
    private String url;
    private String thumbnail_url;
    private String created_at;
    private String updated_at;
    private String deleted_at;
    private int size;
    
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
    public int getUid() {
        return uid;
    }
    public void setUid(int uid) {
        this.uid = uid;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
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
    public String getDeleted_at() {
        return deleted_at;
    }
    public void setDeleted_at(String deleted_at) {
        this.deleted_at = deleted_at;
    }
    
}
