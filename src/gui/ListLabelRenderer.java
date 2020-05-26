package gui;

import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import log.Formula;
import log.Measure;

public final class ListLabelRenderer extends DefaultListCellRenderer {

	private final ImageIcon icon = new ImageIcon(getClass().getResource("/icon_label_16.png"));
	private final ImageIcon icon_fx = new ImageIcon(getClass().getResource("/icon_formule_16.png"));
	private NumberFormat formatter = NumberFormat.getInstance();

	private static final long serialVersionUID = 1L;

	@Override
	public Component getListCellRendererComponent(JList<? extends Object> paramJList, Object paramObject, int paramInt,
			boolean paramBoolean1, boolean paramBoolean2) {

		JLabel label = (JLabel) super.getListCellRendererComponent(paramJList, paramObject, paramInt, paramBoolean1, paramBoolean2);

		Measure measure = (Measure) paramObject;
		label.setText("<html><b>" + measure.getName() 
		+ "</b><br><i>Min=" + this.formatter.format(measure.getMin()) + " | Max=" + this.formatter.format(measure.getMax()));

		if(!(paramObject instanceof Formula))
		{
			label.setIcon(icon);
		}else{
			label.setIcon(icon_fx);
		}

		label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

		return label;
	}

}
