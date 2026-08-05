package com.imct.alphaclass.bean;

import java.io.Serializable;

public class MediaWiki implements Serializable {
    private int id;
    public String word;
    private String wiki;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getWiki() {
        return wiki;
    }
    public void setWiki(String wiki) {
        this.wiki = wiki;
    }
    public String getWord() {
        return word;
    }
    public void setWord(String word) {
        this.word = word;
    }

    
    
}