/*
 * Creation : 28 nov. 2022
 */
package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RectangleHighlight {

    private boolean visible;
    private transient Paint paint;
    private transient Stroke stroke;
    private transient PropertyChangeSupport pcs;

    public RectangleHighlight() {
        this(Color.BLACK, new BasicStroke(1.0F));
    }

    public RectangleHighlight(Paint paint, Stroke stroke) {
        this.visible = true;
        this.paint = paint;
        this.stroke = stroke;
        this.pcs = new PropertyChangeSupport(this);
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        boolean old = this.visible;
        this.visible = visible;
        this.pcs.firePropertyChange("visible", old, visible);
    }

    public Paint getPaint() {
        return this.paint;
    }

    public void setPaint(Paint paint) {
        Paint old = this.paint;
        this.paint = paint;
        this.pcs.firePropertyChange("paint", old, paint);
    }

    public Stroke getStroke() {
        return this.stroke;
    }

    public void setStroke(Stroke stroke) {
        Stroke old = this.stroke;
        this.stroke = stroke;
        this.pcs.firePropertyChange("stroke", old, stroke);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        this.pcs.removePropertyChangeListener(l);
    }

}
