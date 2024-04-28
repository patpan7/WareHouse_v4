package com.example.warehouse;

import java.math.BigDecimal;

public class Buys {
    int code;
    String name;
    String date;
    String invoice;
    BigDecimal total;
    int suppliercode;
    int totalinvoices;

    public Buys(int code, String name, String date, String invoice, BigDecimal total, int suppliercode) {
        this.code = code;
        this.name = name;
        this.date = date;
        this.invoice = invoice;
        this.total = total;
        this.suppliercode = suppliercode;
    }

    public Buys(int code, String name, int totalInvoices, BigDecimal total) {
        this.suppliercode = code;
        this.name = name;
        this.totalinvoices = totalInvoices;
        this.total = total;
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

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public int getSuppliercode() {
        return suppliercode;
    }

    public void setSuppliercode(int suppliercode) {
        this.suppliercode = suppliercode;
    }

    public int getTotalinvoices() {
        return totalinvoices;
    }

    public void setTotalinvoices(int totalinvoices) {
        this.totalinvoices = totalinvoices;
    }
}


