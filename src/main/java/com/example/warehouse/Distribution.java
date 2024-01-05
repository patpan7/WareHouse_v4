package com.example.warehouse;

public class Distribution {
    int code;
    int item_code;
    String name;
    float quantity;
    String date;
    String unit;
    String department;


    public Distribution(int code, int item_code, String name, float quantity, String date, String unit, String department) {
        this.code = code;
        this.item_code = item_code;
        this.name = name;
        this.quantity = quantity;
        this.date = date;
        this.unit = unit;
        this.department = department;
    }

    public Distribution(String department, String date) {
        this.department = department;
        this.date = date;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getItem_code() {
        return item_code;
    }

    public void setItem_code(int item_code) {
        this.item_code = item_code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getQuantity() {
        return quantity;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
