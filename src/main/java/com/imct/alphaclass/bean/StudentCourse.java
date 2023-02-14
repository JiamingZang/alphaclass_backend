package com.imct.alphaclass.bean;

import java.io.Serializable;

public class StudentCourse implements Serializable {
    private int id;
    private int cid;
    private int sid;
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
    public int getSid() {
        return sid;
    }
    public void setSid(int sid) {
        this.sid = sid;
    }
}
