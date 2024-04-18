package com.example.warehouse;

import java.math.BigDecimal;

public class Supplier {
    int code;
    String name;
    String phone;
    float turnover;
    int enable;

    int item_code;
    double total_quantity;
    BigDecimal total_sum;
    BigDecimal average_price;
    String unit;

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

    public Supplier(String name) {
        this.name = name;
    }

    public Supplier(int itemCode, String supplierName, double totalQuantity, BigDecimal totalSum, BigDecimal averagePrice, String unit) {
        this.item_code = itemCode;
        this.name = supplierName;
        this.total_quantity = totalQuantity;
        this.total_sum = totalSum;
        this.average_price = averagePrice;
        this.unit = unit;
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

    public int getItem_code() {
        return item_code;
    }

    public void setItem_code(int item_code) {
        this.item_code = item_code;
    }

    public double getTotal_quantity() {
        return total_quantity;
    }

    public void setTotal_quantity(double total_quantity) {
        this.total_quantity = total_quantity;
    }

    public BigDecimal getTotal_sum() {
        return total_sum;
    }

    public void setTotal_sum(BigDecimal total_sum) {
        this.total_sum = total_sum;
    }

    public BigDecimal getAverage_price() {
        return average_price;
    }

    public void setAverage_price(BigDecimal average_price) {
        this.average_price = average_price;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
