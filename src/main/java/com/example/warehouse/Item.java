package com.example.warehouse;

import java.math.BigDecimal;

public class Item {
    int code;
    int item_code;
    String name;
    BigDecimal quantity;
    String unit;
    BigDecimal price;
    int category_code;
    BigDecimal sum = BigDecimal.ZERO;
    int enable;

    public Item() {
    }

    public Item(int item_code, String name, BigDecimal quantity, String unit, BigDecimal price, int category_code) {
        this.item_code = item_code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
    }

    public Item(int code, int item_code, String name, String unit, BigDecimal quantity, BigDecimal price) {
        this.code = code;
        this.item_code = item_code;
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.price = price;
    }

    public Item(String name, String unit, BigDecimal price, int category_code) {
        this.name = name;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
    }

    public Item(int item_code, String name, BigDecimal quantity, String unit) {
        this.item_code = item_code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public Item(int item_code, String name, BigDecimal quantity, String unit, BigDecimal price, int category_code, BigDecimal sum) {
        this.item_code = item_code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
        this.sum = sum;
    }

    public Item(int code, int item_code, String name, BigDecimal quantity, String unit, BigDecimal price, BigDecimal sum) {
        this.code = code;
        this.item_code = item_code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.sum = sum;
    }

    public Item(int item_code, String name, BigDecimal quantity, String unit, BigDecimal price, int category_code, int enable) {
        this.item_code = item_code;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
        this.enable = enable;
    }

    public Item(int item_code, String name, String unit, BigDecimal price, int category_code, int enable) {
        this.item_code = item_code;
        this.name = name;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
        this.enable = enable;
    }

    public Item(String name, String unit, BigDecimal price, int category_code, int enable) {
        this.name = name;
        this.unit = unit;
        this.price = price;
        this.category_code = category_code;
        this.enable = enable;
    }

    public int getEnable() {
        return enable;
    }

    public void setEnable(int enable) {
        this.enable = enable;
    }

    public int getItem_code() {
        return item_code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getCategory_code() {return category_code;}

    public BigDecimal getSum() {
        return sum;
    }
    public void setItem_code(int item_code) {
        this.item_code = item_code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setCategory_code(int category_code) {
        this.category_code = category_code;
    }

    public void setSum (BigDecimal sum){
        this.sum = sum;
    }
    @Override
    public String toString() {
        return name;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
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
