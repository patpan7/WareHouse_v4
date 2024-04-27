package com.example.warehouse;
import java.math.BigDecimal;
import java.math.RoundingMode;
public class BigDecimalQuantity {
    private static final int SCALE = 2; // Αριθμός δεκαδικών ψηφίων

    private BigDecimal value;

    public BigDecimalQuantity(String value) {
        this.value = new BigDecimal(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimalQuantity add(BigDecimalQuantity other) {
        BigDecimal result = this.value.add(other.value);
        return new BigDecimalQuantity(result.toString());
    }

    public BigDecimalQuantity subtract(BigDecimalQuantity other) {
        BigDecimal result = this.value.subtract(other.value);
        return new BigDecimalQuantity(result.toString());
    }

    public BigDecimalQuantity multiply(BigDecimalQuantity other) {
        BigDecimal result = this.value.multiply(other.value);
        return new BigDecimalQuantity(result.toString());
    }

    public BigDecimalQuantity divide(BigDecimalQuantity divisor) {
        if (divisor.value.equals(BigDecimal.ZERO)) {
            throw new ArithmeticException("Division by zero");
        }
        BigDecimal result = this.value.divide(divisor.value, SCALE, RoundingMode.HALF_UP);
        return new BigDecimalQuantity(result.toString());
    }

    public BigDecimalQuantity negate() {
        BigDecimal result = this.value.negate();
        return new BigDecimalQuantity(result.toString());
    }

    public int compareTo(BigDecimalQuantity other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

}
