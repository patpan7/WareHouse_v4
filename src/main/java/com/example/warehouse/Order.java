package com.example.warehouse;

public class Order {
    String date;
    int totalProducts;

    public Order(){

    }

    public Order(String date, int totalProducts){
        this.date = date;
        this.totalProducts = totalProducts;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTotalProducts(int totalProducts) {
        this.totalProducts = totalProducts;
    }

    public String getDate() {
        return date;
    }

    public int getTotalProducts() {
        return totalProducts;
    }
}
