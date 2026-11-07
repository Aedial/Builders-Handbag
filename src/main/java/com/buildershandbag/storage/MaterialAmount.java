package com.buildershandbag.storage;

import java.util.Locale;


/**
 * Exact material quantities for configuration balances and output costs.
 */
public final class MaterialAmount {

    public static final MaterialAmount BLOCK = new MaterialAmount(1, 1);

    private final long numerator;
    private final int denominator;

    public MaterialAmount(long numerator, int denominator) {
        if (numerator < 0 || denominator <= 0) throw new IllegalArgumentException("Invalid material amount");

        int divisor = greatestCommonDivisor(numerator, denominator);
        this.numerator = numerator / divisor;
        this.denominator = denominator / divisor;
    }

    public int getWholeCount() {
        return (int) Math.min(Integer.MAX_VALUE, numerator / denominator);
    }

    public float getCount() {
        return (float) numerator / denominator;
    }

    public int getRemainder() {
        return (int) (numerator % denominator);
    }

    public int getDenominator() {
        return denominator;
    }

    public int getFreeSpace(int capacity) {
        return Math.max(0, capacity - getWholeCount());
    }

    public MaterialAmount add(long count) {
        return new MaterialAmount(numerator + count * denominator, denominator);
    }

    public int getOutputCount(MaterialAmount cost) {
        long count = numerator * cost.denominator / (denominator * cost.numerator);
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    public long getRequiredMaterial(MaterialAmount cost, int outputs) {
        long missing = cost.numerator * outputs * denominator - numerator * cost.denominator;
        if (missing <= 0) return 0;

        long divisor = (long) denominator * cost.denominator;
        return (missing + divisor - 1) / divisor;
    }

    public MaterialAmount consume(MaterialAmount cost, int outputs) {
        int divisor = greatestCommonDivisor(denominator, cost.denominator);
        int multiplier = cost.denominator / divisor;
        long remaining = numerator * multiplier - cost.numerator * outputs * (denominator / divisor);
        return new MaterialAmount(remaining, denominator * multiplier);
    }

    public String getRemainderText() {
        return format((float) getRemainder() / denominator);
    }

    @Override
    public String toString() {
        return format(getCount());
    }

    private static String format(float count) {
        return String.format(Locale.ROOT, "%.2f", count).replaceFirst("\\.?0+$", "");
    }

    private static int greatestCommonDivisor(long numerator, int denominator) {
        long divisor = denominator;
        while (divisor != 0) {
            long remainder = numerator % divisor;
            numerator = divisor;
            divisor = remainder;
        }

        return (int) numerator;
    }
}
