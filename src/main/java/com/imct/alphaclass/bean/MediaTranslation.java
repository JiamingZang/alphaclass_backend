package com.imct.alphaclass.bean;

import java.io.Serializable;

public class MediaTranslation implements Serializable {
    private int id;
    public String word;
    private String translation_english;
    private String phonetic_UK;
    private String phonetic_US;
    private String sentence_CN;
    private String sentence_EN;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getWord() {
        return word;
    }
    public void setWord(String word) {
        this.word = word;
    }
    public String getTranslation_english() {
        return translation_english;
    }
    public void setTranslation_english(String translation_english) {
        this.translation_english = translation_english;
    }
    public String getPhonetic_UK() {
        return phonetic_UK;
    }
    public void setPhonetic_UK(String phonetic_UK) {
        this.phonetic_UK = phonetic_UK;
    }
    public String getPhonetic_US() {
        return phonetic_US;
    }
    public void setPhonetic_US(String phonetic_US) {
        this.phonetic_US = phonetic_US;
    }
    public String getSentence_CN() {
        return sentence_CN;
    }
    public void setSentence_CN(String sentence_CN) {
        this.sentence_CN = sentence_CN;
    }
    public String getSentence_EN() {
        return sentence_EN;
    }
    public void setSentence_EN(String sentence_EN) {
        this.sentence_EN = sentence_EN;
    }   
}