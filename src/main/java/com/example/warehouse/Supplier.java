package com.example.warehouse;

public class Supplier {
    int code;
    String name;
    String phone;
    float turnover;
    int enable;

    public Supplier() {
    }

    public Supplier(int code, String name, String phone, float turnover) {
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.turnover = turnover;
    }


    public Supplier(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public Supplier(int code, String name, String phone, float turnover, int enable) {
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.turnover = turnover;
        this.enable = enable;
    }

    public Supplier(int code, String name, String phone, int enable) {
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.enable = enable;
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

    public int getEnable() {
        return enable;
    }

    public void setEnable(int enable) {
        this.enable = enable;
    }
}
