package com.imct.alphaclass.bean;

import java.io.Serializable;

public class Anchor implements Serializable{
    private int id;
    private int cid;
    private String name;
    private float pos_x;
    private float pos_y;
    private float pos_z;
    private float euler_x;
    private float euler_y;
    private float euler_z;
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
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public float getPos_x() {
        return pos_x;
    }
    public void setPos_x(float pos_x) {
        this.pos_x = pos_x;
    }
    public float getPos_y() {
        return pos_y;
    }
    public void setPos_y(float pos_y) {
        this.pos_y = pos_y;
    }
    public float getPos_z() {
        return pos_z;
    }
    public void setPos_z(float pos_z) {
        this.pos_z = pos_z;
    }
    public float getEuler_x() {
        return euler_x;
    }
    public void setEuler_x(float euler_x) {
        this.euler_x = euler_x;
    }
    public float getEuler_y() {
        return euler_y;
    }
    public void setEuler_y(float euler_y) {
        this.euler_y = euler_y;
    }
    public float getEuler_z() {
        return euler_z;
    }
    public void setEuler_z(float euler_z) {
        this.euler_z = euler_z;
    }
    
}
