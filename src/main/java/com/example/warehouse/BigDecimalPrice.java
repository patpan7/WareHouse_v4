package com.example.warehouse;
import java.math.BigDecimal;
import java.math.RoundingMode;
public class BigDecimalPrice {
    private static final int SCALE = 2; // Αριθμός δεκαδικών ψηφίων

    private BigDecimal value;

    public BigDecimalPrice(String value) {
        this.value = new BigDecimal(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimalPrice add(BigDecimalPrice other) {
        BigDecimal result = this.value.add(other.value);
        return new BigDecimalPrice(result.toString());
    }

    public BigDecimalPrice subtract(BigDecimalPrice other) {
        BigDecimal result = this.value.subtract(other.value);
        return new BigDecimalPrice(result.toString());
    }

    public BigDecimalPrice multiply(BigDecimalPrice other) {
        BigDecimal result = this.value.multiply(other.value);
        return new BigDecimalPrice(result.toString());
    }

    public BigDecimalPrice divide(BigDecimalPrice divisor) {
        if (divisor.value.equals(BigDecimal.ZERO)) {
            throw new ArithmeticException("Division by zero");
        }
        BigDecimal result = this.value.divide(divisor.value, SCALE, RoundingMode.HALF_UP);
        return new BigDecimalPrice(result.toString());
    }

    public BigDecimalPrice negate() {
        BigDecimal result = this.value.negate();
        return new BigDecimalPrice(result.toString());
    }

    public int compareTo(BigDecimalPrice other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

}
