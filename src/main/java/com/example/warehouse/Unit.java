package com.example.warehouse;

public class Unit {
    int code;
    String unit;

    public Unit(int code, String unit) {
        this.code = code;
        this.unit = unit;
    }

    public Unit(String unit) {
        this.unit = unit;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return unit;
    }
}
