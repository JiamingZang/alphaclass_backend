package com.imct.alphaclass.bean;

import java.io.Serializable;

public class Keyword implements Serializable {
    private int id;
    private int cid;
    private String keyword;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getCid() {
        return cid;
    }
    public void setCid(int cid) {
        this.cid = cid;
    }
    public String getKeyword() {
        return keyword;
    }
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
