package gui;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ui.Drawable;

/**
 * An implementation of the {@link Drawable} interface, to illustrate the use
 * of the {@link org.jfree.chart.annotations.XYDrawableAnnotation} class. Used
 * by MarkerDemo1.java.
 */
public final class CircleDrawer implements org.jfree.chart.ui.Drawable {

    /** The outline paint. */
    private Paint outlinePaint;

    /** The outline stroke. */
    private Stroke outlineStroke;

    /** The fill paint. */
    private Paint fillPaint;

    /**
     * Creates a new instance.
     *
     * @param outlinePaint the outline paint.
     * @param outlineStroke the outline stroke.
     * @param fillPaint the fill paint.
     */
    public CircleDrawer(Paint outlinePaint, Stroke outlineStroke, Paint fillPaint) {
        this.outlinePaint = outlinePaint;
        this.outlineStroke = outlineStroke;
        this.fillPaint = fillPaint;
    }

    /**
     * Draws the circle.
     *
     * @param g2 the graphics device.
     * @param area the area in which to draw.
     */
    public void draw(Graphics2D g2, Rectangle2D area) {
        Ellipse2D ellipse = new Ellipse2D.Double(area.getX(), area.getY(), area.getWidth(), area.getHeight());
        if (this.fillPaint != null) {
            g2.setPaint(this.fillPaint);
            g2.fill(ellipse);
        }
        if (this.outlinePaint != null && this.outlineStroke != null) {
            g2.setPaint(this.outlinePaint);
            g2.setStroke(this.outlineStroke);
            g2.draw(ellipse);
        }
    }
}