package com.example.warehouse;

public class Item {
    int code;
    String name;
    float quantity;
    String unit;
    float price;
    int category_code;
    Float sum = 0.0F;
    String cat;

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

    public Item(int code, String name, float quantity, String unit) {
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public Item(int code, String name, float quantity, String unit, float price, int category_code, float sum) {
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
        this.sum = sum;
    }

    public Item(int code, String name, float quantity, String unit, float price, float sum) {
        this.code = code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.sum = sum;
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

    public float getSum() {
        return sum;
    }
    public void setCode(int code) {
        this.code = code;
    }

    public void setName(String name) {
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

    public void setSum (float sum){
        this.sum = sum;
    }
    @Override
    public String toString() {
        return name;
    }

    public void print() {
//        System.out.println("Code: " + code);
//        System.out.println("Name: " + name);
        System.out.println("Quantity: " + quantity);
//        System.out.println("Unit: " + unit);
//        System.out.println("Price: " + price);
//        System.out.println("Category Code: " + category_code);
//        System.out.println("Sum: " + sum);
//        System.out.println("Category: " + cat);
    }
}
