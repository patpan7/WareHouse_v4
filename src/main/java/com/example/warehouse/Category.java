package com.example.warehouse;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return code == category.code && Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name);
    }


}
