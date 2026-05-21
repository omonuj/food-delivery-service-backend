package com.food.ordering.system.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void addNormalizesToTwoDecimalPlaces() {
        Money result = new Money(new BigDecimal("10.005")).add(new Money(new BigDecimal("0")));
        // HALF_EVEN rounding of 10.005 -> 10.00
        assertEquals(new BigDecimal("10.00"), result.getAmount());
    }

    @Test
    void addSumsAmounts() {
        Money result = new Money(new BigDecimal("10.50")).add(new Money(new BigDecimal("4.50")));
        assertEquals(new BigDecimal("15.00"), result.getAmount());
    }

    @Test
    void subtractReducesAmount() {
        Money result = new Money(new BigDecimal("10.00")).subtract(new Money(new BigDecimal("3.25")));
        assertEquals(new BigDecimal("6.75"), result.getAmount());
    }

    @Test
    void multiplyScalesAmountByMultiplier() {
        Money result = new Money(new BigDecimal("2.50")).multiply(3);
        assertEquals(new BigDecimal("7.50"), result.getAmount());
    }

    @Test
    void multiplyByZeroIsZero() {
        Money result = new Money(new BigDecimal("2.50")).multiply(0);
        assertEquals(new BigDecimal("0.00"), result.getAmount());
    }

    @Test
    void isGreaterThanZeroTrueForPositive() {
        assertTrue(new Money(new BigDecimal("0.01")).isGreaterThanZero());
    }

    @Test
    void isGreaterThanZeroFalseForZero() {
        assertFalse(new Money(BigDecimal.ZERO).isGreaterThanZero());
    }

    @Test
    void isGreaterThanZeroFalseForNegative() {
        assertFalse(new Money(new BigDecimal("-1.00")).isGreaterThanZero());
    }

    @Test
    void isGreaterThanComparesAmounts() {
        Money ten = new Money(new BigDecimal("10.00"));
        Money five = new Money(new BigDecimal("5.00"));
        assertTrue(ten.isGreaterThan(five));
        assertFalse(five.isGreaterThan(ten));
        assertFalse(ten.isGreaterThan(ten));
    }

    @Test
    void equalsTrueForSameScaleAndAmount() {
        assertEquals(new Money(new BigDecimal("10.00")), new Money(new BigDecimal("10.00")));
    }

    @Test
    void arithmeticResultsAreComparableRegardlessOfInputScale() {
        // add/subtract/multiply always normalize to scale 2, so their results compare equal
        Money viaAdd = new Money(new BigDecimal("7")).add(new Money(new BigDecimal("3")));
        Money viaMultiply = new Money(new BigDecimal("5")).multiply(2);
        assertEquals(viaAdd, viaMultiply);
    }

    @Test
    void equalsIsScaleSensitiveForRawConstruction() {
        // Documents current behavior: equals() delegates to BigDecimal.equals, which is
        // scale-sensitive. 10 and 10.00 are NOT equal even though they compare numerically equal.
        // Arithmetic paths avoid this because they normalize via setScale(2); raw constructor does not.
        Money unscaled = new Money(new BigDecimal("10"));
        Money scaled = new Money(new BigDecimal("10.00"));
        assertFalse(unscaled.equals(scaled));
        assertEquals(0, unscaled.getAmount().compareTo(scaled.getAmount()));
    }
}
