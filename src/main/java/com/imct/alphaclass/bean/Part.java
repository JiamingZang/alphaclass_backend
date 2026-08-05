package com.imct.alphaclass.bean;

import java.io.Serializable;

public class Part implements Serializable {
    private int id;
    private int media_id;
    private String part_name;
    private int part_index;
    private int part_order;
    private float originpos_x;
    private float originpos_y;
    private float originpos_z;
    private float origineuler_x;
    private float origineuler_y;
    private float origineuler_z;

    private float targetpos_x;
    private float targetpos_y;
    private float targetpos_z;
    private float targeteuler_x;
    private float targeteuler_y;
    private float targeteuler_z;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMediaid() {
        return media_id;
    }

    public void setMediaid(int media_id) {
        this.media_id = media_id;
    }

    public String getPartName() {
        return part_name;
    }

    public void setPartName(String part_name) {
        this.part_name = part_name;
    }

    public int getPart_index() {
        return part_index;
    }

    public void setPart_index(int part_index) {
        this.part_index = part_index;
    }

    public int getPart_order() {
        return part_order;
    }

    public void setPart_order(int part_order) {
        this.part_order = part_order;
    }

    public float getOriginPos_x() {
        return originpos_x;
    }

    public void setOriginPos_x(float originpos_x) {
        this.originpos_x = originpos_x;
    }

    public float getOriginPos_y() {
        return originpos_y;
    }

    public void setOriginPos_y(float originpos_y) {
        this.originpos_y = originpos_y;
    }

    public float getOriginPos_z() {
        return originpos_z;
    }

    public void setOriginPos_z(float originpos_z) {
        this.originpos_z = originpos_z;
    }

    public float getOriginEuler_x() {
        return origineuler_x;
    }

    public void setOriginEuler_x(float origineuler_x) {
        this.origineuler_x = origineuler_x;
    }

    public float getOriginEuler_y() {
        return origineuler_y;
    }

    public void setOriginEuler_y(float origineuler_y) {
        this.origineuler_y = origineuler_y;
    }

    public float getOriginEuler_z() {
        return origineuler_z;
    }

    public void setOriginEuler_z(float origineuler_z) {
        this.origineuler_z = origineuler_z;
    }

    public float getTargetPos_x() {
        return targetpos_x;
    }

    public void setTargetPos_x(float targetpos_x) {
        this.targetpos_x = targetpos_x;
    }

    public float getTargetPos_y() {
        return targetpos_y;
    }

    public void setTargetPos_y(float targetpos_y) {
        this.targetpos_y = targetpos_y;
    }

    public float getTargetPos_z() {
        return targetpos_z;
    }

    public void setTargetPos_z(float targetpos_z) {
        this.targetpos_z = targetpos_z;
    }

    public float getTargetEuler_x() {
        return targeteuler_x;
    }

    public void setTargetEuler_x(float targeteuler_x) {
        this.targeteuler_x = targeteuler_x;
    }

    public float getTargetEuler_y() {
        return targeteuler_y;
    }

    public void setTargetEuler_y(float targeteuler_y) {
        this.targeteuler_y = targeteuler_y;
    }

    public float getTargetEuler_z() {
        return targeteuler_z;
    }

    public void setTargetEuler_z(float targeteuler_z) {
        this.targeteuler_z = targeteuler_z;
    }
}