/*
 * Creation : 23 mars 2020
 */
package gui;

import java.awt.BasicStroke;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;

abstract class DialogFactory {

}

final class DialogProperties extends JPanel

{
	private static final long serialVersionUID = 1L;

	private final DefaultTableModel model;

	public DialogProperties(CombinedDomainXYPlot combinedplot) {

		model = new DefaultTableModel(new String[] { "Serie", "Couleur", "Epaisseur", "Supprimer?" }, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				switch (columnIndex) {
				case 0:
					return String.class;
				case 1:
					return Color.class;
				case 2:
					return Float.class;
				case 3:
					return Boolean.class;
				default:
					return String.class;
				}
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column > 0 ? true : false;
			}
		};

		JTable table = new JTable(model);
		table.setRowSelectionAllowed(false);

		table.setDefaultRenderer(Color.class, new ColorRenderer(true));

		table.setDefaultEditor(Color.class, new ColorEditor());

		add(new JScrollPane(table));

		XYPlot xyPlot;
		Comparable<?> key;
		XYItemRenderer renderer;

		for (Object plot : combinedplot.getSubplots()) {
			xyPlot = (XYPlot) plot;
			int nbSerie = xyPlot.getSeriesCount();

			renderer = xyPlot.getRenderer();

			if(renderer instanceof XYLineAndShapeRenderer)
			{
				for (int nSerie = 0; nSerie < nbSerie; nSerie++) {

					key = xyPlot.getDataset().getSeriesKey(nSerie);

					model.addRow(new Object[] { key, (Color) renderer.getSeriesPaint(nSerie),
							((BasicStroke) renderer.getSeriesStroke(nSerie)).getLineWidth(), Boolean.FALSE });

				}
			}else{
				
				key = xyPlot.getDataset().getSeriesKey(0);
				
				model.addRow(new Object[] { key, null, null, Boolean.FALSE });
			}


		}
	}

	public final void updatePlot(ChartView chartView) {

		CombinedDomainXYPlot combinedplot = chartView.getPlot();
		XYPlot xyPlot;
		String serieName;

		for (int i = 0; i < model.getRowCount(); i++) {
			serieName = model.getValueAt(i, 0).toString();

			boolean delete = (boolean) model.getValueAt(i, 3);

			for (Object plot : combinedplot.getSubplots()) {
				xyPlot = (XYPlot) plot;
				
				if(xyPlot.getRenderer() instanceof XYShapeRenderer)
				{
					if(delete)
					{
						combinedplot.remove(xyPlot);
					}
					break;
				}

				int idxSerie = ((XYSeriesCollection) xyPlot.getDataset()).getSeriesIndex(serieName);

				if (idxSerie > -1) {
					if (delete) {
						if (((XYSeriesCollection) xyPlot.getDataset()).getSeriesCount() == 1) {
							combinedplot.remove(xyPlot);
						} else {
							((XYSeriesCollection) xyPlot.getDataset()).removeSeries(idxSerie);
						}
						chartView.updateObservateur("data", serieName);
						break;
					}
					Color color = (Color) model.getValueAt(i, 1);
					float widthLine = (float) model.getValueAt(i, 2);
					xyPlot.getRenderer().setSeriesPaint(idxSerie, color);
					xyPlot.getRenderer().setSeriesStroke(idxSerie, new BasicStroke(widthLine));
					break;
				}

			}
		}
	}

}
