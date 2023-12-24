package com.example.warehouse;

public class Buys {
    String name;
    String date;
    String invoice;
    Float total;

    public Buys(String name, String date, String invoice, Float total) {
        this.name = name;
        this.date = date;
        this.invoice = invoice;
        this.total = total;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }

    public Float getTotal() {
        return total;
    }

    public void setTotal(Float total) {
        this.total = total;
    }
}
