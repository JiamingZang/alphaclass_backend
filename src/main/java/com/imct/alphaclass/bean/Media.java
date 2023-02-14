package com.imct.alphaclass.bean;

import java.io.Serializable;

public class Media implements Serializable {
    private int id;
    private int anchorid;
    private int assetid;
    private int kid;
    private String name;
    private String type;
    private String style;
    private float color_r;
    private float color_g;
    private float color_b;
    public String getStyle() {
        return style;
    }
    public void setStyle(String style) {
        this.style = style;
    }
    public float getColor_r() {
        return color_r;
    }
    public void setColor_r(float color_r) {
        this.color_r = color_r;
    }
    public float getColor_g() {
        return color_g;
    }
    public void setColor_g(float color_g) {
        this.color_g = color_g;
    }
    public float getColor_b() {
        return color_b;
    }
    public void setColor_b(float color_b) {
        this.color_b = color_b;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getAnchorid() {
        return anchorid;
    }
    public void setAnchorid(int anchorid) {
        this.anchorid = anchorid;
    }
    public int getAssetid() {
        return assetid;
    }
    public void setAssetid(int assetid) {
        this.assetid = assetid;
    }
    public int getKid() {
        return kid;
    }
    public void setKid(int kid) {
        this.kid = kid;
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

}
