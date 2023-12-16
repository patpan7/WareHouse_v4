package com.example.warehouse;

public class Category {
    int code;
    String name;

    public Category(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public Category(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return name;
    }

}
