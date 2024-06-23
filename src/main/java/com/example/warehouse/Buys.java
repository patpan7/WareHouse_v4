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
    String type;
    BigDecimal fpa;
    BigDecimal totalValue;

    public Buys(int code, String name, String date, String invoice, BigDecimal total, int suppliercode, BigDecimal fpa, BigDecimal totalValue, String type) {
        this.code = code;
        this.name = name;
        this.date = date;
        this.invoice = invoice;
        this.total = total;
        this.suppliercode = suppliercode;
        this.fpa = fpa;
        this.totalValue = totalValue;
        this.type = type;
    }

    public Buys(int code, String name, int totalInvoices, BigDecimal total) {
        this.suppliercode = code;
        this.name = name;
        this.totalinvoices = totalInvoices;
        this.total = total;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
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

    public BigDecimal getFpa() {
        return fpa;
    }

    public void setFpa(BigDecimal fpa) {
        this.fpa = fpa;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getType() {return type;}

    public void setType(String type) {this.type = type;}
}