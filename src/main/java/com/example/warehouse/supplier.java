package com.example.warehouse;

public class supplier {
    int code;
    String name;
    String phone;
    float turnover;

    public supplier() {
    }

    public supplier(int code, String name, String phone, float turnover) {
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.turnover = turnover;
    }

    public supplier(int code, String name, String phone) {
        this.code = code;
        this.name = name;
        this.phone = phone;
    }

    public supplier(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public float getTurnover() {
        return turnover;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setTurnover(float turnover) {
        this.turnover = turnover;
    }
}
