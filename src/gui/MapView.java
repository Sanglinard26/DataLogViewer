/*
 * Creation : 1 nov. 2020
 */
package gui;

import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import calib.MapCal;
import calib.Variable;
import net.ericaro.surfaceplotter.surface.JSurface;

public final class MapView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JList<Variable> listVariable;
    private CalTable dataTable;
    private SurfaceChart surfaceChart;
    private JSplitPane splitPane;

    public MapView(MapCal mapCal) {

        super();
        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        splitPane.setOneTouchExpandable(true);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 80;
        gbc.weighty = 100;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(splitPane, gbc);

        List<Variable> variables = mapCal.getListVariable();
        Collections.sort(variables);
        listVariable = new JList<Variable>(mapCal.getListVariable().toArray(new Variable[variables.size()]));
        listVariable.addMouseListener(new ListMouseListener());

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 20;
        gbc.weighty = 100;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(new JScrollPane(listVariable), gbc);

        listVariable.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (listVariable.getSelectedIndex() > -1 && !e.getValueIsAdjusting()) {
                    Variable variable = listVariable.getSelectedValue();

                    remove(dataTable);
                    dataTable = new CalTable(variable);
                    splitPane.setTopComponent(dataTable);
                    revalidate();
                    repaint();

                    splitPane.setDividerLocation(dataTable.getComponentHeight());

                    updateChart(variable);

                }
            }
        });

        dataTable = new CalTable(null);
        splitPane.setTopComponent(dataTable);

        surfaceChart = new SurfaceChart();
        splitPane.setBottomComponent(surfaceChart);
        surfaceChart.setVisible(false);

    }

    public final void updateChart(Variable variable) {

        JSurface jSurface = surfaceChart.getSurface();

        boolean chartVisible = false;

        switch (variable.getType()) {

        case COURBE:
            float[][] zValuesOrigin = variable.getZvalues();

            int length = zValuesOrigin[0].length;

            if (length == 0) {
                return;
            }

            float[][] zValuesNew = new float[2][length];

            zValuesNew[0] = Arrays.copyOf(zValuesOrigin[0], length);
            zValuesNew[1] = Arrays.copyOf(zValuesOrigin[0], length);

            surfaceChart.getArraySurfaceModel().setValues(variable.getXAxis(), new float[] { 0, 1 }, zValuesNew);
            jSurface.setXLabel("X");
            chartVisible = true;
            break;

        case MAP:
            surfaceChart.getArraySurfaceModel().setValues(variable.getXAxis(), variable.getYAxis(), variable.getZvalues());
            jSurface.setXLabel("X");
            jSurface.setYLabel("Y");
            chartVisible = true;

            break;

        default:
            break;
        }

        surfaceChart.setVisible(chartVisible);
    }

    private final class ListMouseListener extends MouseAdapter {

        final String ICON_EXCEL = "/icon_excel_24.png";

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger() && listVariable.getModel().getSize() > 0 && listVariable.getSelectedValue() != null) {
                final JPopupMenu menu = new JPopupMenu();
                final JMenu menuExport = new JMenu("Export Excel");
                JMenuItem menuItem;

                menuItem = new JMenuItem("Variable sélectionnée", new ImageIcon(getClass().getResource(ICON_EXCEL)));
                menuItem.addActionListener(new ExportListener());
                menuExport.add(menuItem);

                menuExport.addSeparator();
                menuItem = new JMenuItem("Toutes les variables", new ImageIcon(getClass().getResource(ICON_EXCEL)));
                menuItem.addActionListener(new ExportListener());
                menuExport.add(menuItem);

                menu.add(menuExport);

                menu.show(e.getComponent(), e.getX(), e.getY());
            }

        }
    }

    private final class ExportListener implements ActionListener {

        public ExportListener() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Enregistement du fichier");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier Excel", "xls"));
            fileChooser.setSelectedFile(new File("Export.xls"));
            final int rep = fileChooser.showSaveDialog(null);

            if (rep == JFileChooser.APPROVE_OPTION) {

                boolean result = false;

                List<Variable> listToExport = new ArrayList<Variable>();

                if (e.getActionCommand().equals("Toutes les variables")) {
                    for (int i = 0; i < listVariable.getModel().getSize(); i++) {
                        listToExport.add(listVariable.getModel().getElementAt(i));
                    }
                } else {
                    listToExport.add(listVariable.getSelectedValue());
                }

                if (!fileChooser.getSelectedFile().exists()) {

                    result = MapCal.toExcel(listToExport, fileChooser.getSelectedFile());

                } else {

                    switch (JOptionPane.showConfirmDialog(null, "Le fichier existe deja, ecraser?", null, JOptionPane.INFORMATION_MESSAGE)) {
                    case JOptionPane.OK_OPTION:

                        result = MapCal.toExcel(listToExport, fileChooser.getSelectedFile());

                        break;
                    case JOptionPane.NO_OPTION:
                        this.actionPerformed(e);
                        return;
                    default:
                        break;
                    }
                }

                if (result) {

                    final int reponse = JOptionPane.showConfirmDialog(null,
                            "Export termine !\n" + fileChooser.getSelectedFile() + "\nVoulez-vous ouvrir le fichier?", null,
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                    switch (reponse) {
                    case JOptionPane.OK_OPTION:
                        try {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().open(fileChooser.getSelectedFile());
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        break;
                    case JOptionPane.NO_OPTION:
                        break;
                    default:
                        break;
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Export abandonne !");
                }
            }
        }
    }

}
