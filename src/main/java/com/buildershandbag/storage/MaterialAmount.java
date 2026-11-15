package com.buildershandbag.storage;

import java.util.Locale;


/**
 * Exact material quantities for configuration balances and output costs.
 */
public final class MaterialAmount {

    public static final MaterialAmount BLOCK = new MaterialAmount(1, 1);
    public static final MaterialAmount ZERO = new MaterialAmount(0, 1);

    private final long numerator;
    private final int denominator;

    public MaterialAmount(long numerator, int denominator) {
        if (numerator < 0 || denominator <= 0) throw new IllegalArgumentException("Invalid material amount");

        this.numerator = numerator;
        this.denominator = denominator;
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

    public boolean isEmpty() {
        return numerator == 0;
    }

    public int getFreeSpace(int capacity) {
        return Math.max(0, capacity - getWholeCount());
    }

    public MaterialAmount roundDown(int newDenominator) {
        long whole = numerator / denominator;
        long remainder = (long) getRemainder() * newDenominator / denominator;
        return new MaterialAmount(whole * newDenominator + remainder, newDenominator);
    }

    public MaterialAmount add(long count) {
        return new MaterialAmount(numerator + count * denominator, denominator);
    }

    public MaterialAmount add(MaterialAmount amount, int count) {
        int units = Math.max(denominator, amount.denominator);
        long total = numeratorIn(units) + amount.numeratorIn(units) * count;
        return new MaterialAmount(total, units);
    }

    public int getInsertCount(MaterialAmount amount, int capacity) {
        if (amount.numerator == 0) return Integer.MAX_VALUE;

        long space = ((long) (capacity + 1) * denominator - numerator) * amount.denominator;
        if (space <= 0) return 0;

        long divisor = denominator * amount.numerator;
        return (int) Math.min(Integer.MAX_VALUE, (space - 1) / divisor);
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
        int units = Math.max(denominator, cost.denominator);
        long remaining = numeratorIn(units) - cost.numeratorIn(units) * outputs;
        return new MaterialAmount(remaining, units);
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

    private long numeratorIn(int units) {
        long scaled = numerator * units;
        if (units <= 0 || scaled % denominator != 0) {
            throw new IllegalArgumentException("Incompatible material denominator");
        }

        return scaled / denominator;
    }
}
