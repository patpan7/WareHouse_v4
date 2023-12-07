package com.example.warehouse;

public class item {
    int code;
    String name;
    float quantity;
    String unit;
    float price;

    public item() {
    }

    public item(int code, String name, float quantity, String unit, float price) {
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
    }

    public item(int code, String name, String unit, float price) {
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.price = price;
    }

    public item(int code, String name, float quantity, String unit) {
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public item(String name, String unit, Float price) {
        this.name = name;
        this.unit = unit;
        this.price = price;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public float getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public float getPrice() {
        return price;
    }


    public void setCode(int code) {
        this.code = code;
    }

    public void setName(String item) {
        this.name = name;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setPrice(float price) {
        this.price = price;
    }

}
