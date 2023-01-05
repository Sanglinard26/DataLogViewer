package gui;

import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.HashUtils;
import org.jfree.chart.renderer.PaintScale;

public final class ColorPaintScale implements PaintScale {

    private double lowerBound;
    private double upperBound;
    private int alpha;

    public ColorPaintScale() {
        this(0.0D, 1.0D);
    }

    public ColorPaintScale(double lowerBound, double upperBound) {
        this(lowerBound, upperBound, 255);
    }

    public ColorPaintScale(double lowerBound, double upperBound, int alpha) {
        if (lowerBound >= upperBound) {
            throw new IllegalArgumentException("Requires lowerBound < upperBound.");
        }

        if ((alpha < 0) || (alpha > 255)) {
            throw new IllegalArgumentException("Requires alpha in the range 0 to 255.");
        }

        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.alpha = alpha;
    }

    public double getLowerBound() {
        return this.lowerBound;
    }

    public double getUpperBound() {
        return this.upperBound;
    }

    public void setBounds(double lowerValue, double upperValue) {
        this.lowerBound = lowerValue;
        this.upperBound = upperValue;
    }

    public int getAlpha() {
        return this.alpha;
    }

    public Paint getPaint(double value) {
        double v = Math.max(value, this.lowerBound);
        v = Math.min(v, this.upperBound);
        float g = (float) ((v - this.lowerBound) / (this.upperBound - this.lowerBound));

        return Color.getHSBColor(norm(1 - g), 1, 1);
    }

    private float norm(float g) {
        return (0 + g * (0.66f));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ColorPaintScale)) {
            return false;
        }
        ColorPaintScale that = (ColorPaintScale) obj;
        if (this.lowerBound != that.lowerBound) {
            return false;
        }
        if (this.upperBound != that.upperBound) {
            return false;
        }

        return (this.alpha == that.alpha);
    }

    public int hashCode() {
        int hash = 7;
        hash = HashUtils.hashCode(hash, this.lowerBound);
        hash = HashUtils.hashCode(hash, this.upperBound);
        hash = 43 * hash + this.alpha;
        return hash;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return this.lowerBound + ";" + this.upperBound;
    }
}
