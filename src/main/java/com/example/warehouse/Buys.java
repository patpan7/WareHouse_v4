package com.example.warehouse;

public class Buys {
    int code;
    String name;
    String date;
    String invoice;
    Float total;
    int suppliercode;

    public Buys(int code, String name, String date, String invoice, Float total, int suppliercode) {
        this.code = code;
        this.name = name;
        this.date = date;
        this.invoice = invoice;
        this.total = total;
        this.suppliercode = suppliercode;
    }

    public int getCode() {return code;}

    public void setCode(int code) {this.code = code;}

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

    public int getSuppliercode() {
        return suppliercode;
    }

    public void setSuppliercode(int suppliercode) {
        this.suppliercode = suppliercode;
    }
}


