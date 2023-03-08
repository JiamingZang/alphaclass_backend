package com.imct.alphaclass.bean;

import java.io.Serializable;

public class Modelinfo implements Serializable {
    private int id;
    private float scale_x;
    private float scale_y;
    private float scale_z;
    private String anime_to_play;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public float getScale_x() {
        return scale_x;
    }
    public void setScale_x(float scale_x) {
        this.scale_x = scale_x;
    }
    public float getScale_y() {
        return scale_y;
    }
    public void setScale_y(float scale_y) {
        this.scale_y = scale_y;
    }
    public float getScale_z() {
        return scale_z;
    }
    public void setScale_z(float scale_z) {
        this.scale_z = scale_z;
    }
    public String getAnime_to_play() {
        return anime_to_play;
    }
    public void setAnime_to_play(String anime_to_play) {
        this.anime_to_play = anime_to_play;
    }

    
    
}