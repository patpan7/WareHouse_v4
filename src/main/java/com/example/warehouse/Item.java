package com.example.warehouse;

public class Item {
    int code;
    String name;
    float quantity;
    String unit;
    float price;
    int category_code;

    public Item() {
    }

    public Item(int code, String name, float quantity, String unit, float price, int category_code) {
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
    }

    public Item(int code, String name, String unit, float price, int category_code) {
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
    }

    public Item(String name, String unit, Float price, int category_code) {
        this.name = name;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
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

    public int getCategory_code() {return category_code;}


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

    public void setCategory_code(int category_code) {
        this.category_code = category_code;
    }
}
