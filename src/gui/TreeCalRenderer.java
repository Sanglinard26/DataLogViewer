/*
 * Creation : 28 mai 2021
 */
package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import calib.MapCal;
import calib.Variable;

public class TreeCalRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Font plainFont = label.getFont().deriveFont(Font.PLAIN);

        if (node.getUserObject() instanceof Variable) {
            Variable var = (Variable) node.getUserObject();

            if (var.isModified()) {
                Font italicFont = label.getFont().deriveFont(Font.ITALIC);
                label.setFont(italicFont);
                label.setForeground(Color.RED);
            } else {
                label.setFont(plainFont);
                label.setForeground(Color.BLACK);
            }
        } else {

            label.setFont(plainFont);
            label.setForeground(Color.BLACK);
        }

        if (sel) {
            label.setForeground(Color.WHITE);
        }

        if (node.getUserObject() instanceof MapCal) {
            if (((MapCal) node.getUserObject()).isUsedByFormula()) {
                label.setBorder(BorderFactory.createLineBorder(Color.BLUE));
            } else {
                label.setBorder(null);
            }
        } else {
            label.setBorder(null);
        }

        return label;
    }

}
