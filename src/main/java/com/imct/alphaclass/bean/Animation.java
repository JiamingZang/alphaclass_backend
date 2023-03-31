package com.imct.alphaclass.bean;


import java.io.Serializable;

public class Animation implements Serializable{
    private int id;
    private String name;
    private int mid;
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
    public int getMid() {
        return mid;
    }
    public void setMid(int mid) {
        this.mid = mid;
    }
    
}
