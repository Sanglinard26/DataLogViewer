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
import java.util.BitSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.JTree.DynamicUtilTreeNode;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jfree.data.xy.XYSeries;

import calib.MapCal;
import calib.Variable;
import net.ericaro.surfaceplotter.surface.JSurface;

public final class MapView extends JPanel implements Observer {

    private static final long serialVersionUID = 1L;

    private final MapCal mapCal;
    private final JTree treeVariable;
    private CalTable dataTable;
    private LineChart lineChartX;
    private LineChart lineChartY;
    private SurfaceChart surfaceChart;
    private JSplitPane splitPane;
    private Variable selectedVariable;

    private MapChartView mapChartView;

    DefaultTreeModel treeModel;

    // private boolean moveInProgress = false;

    public MapView(MapCal mapCal) {

        super();
        setLayout(new GridBagLayout());

        this.mapCal = mapCal;

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

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 20;
        gbc.weighty = 100;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;

        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Fichiers"));
        treeVariable = new JTree(treeModel);
        addCalToTree(mapCal);
        add(new JScrollPane(treeVariable), gbc);

        treeVariable.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent treeEvent) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeEvent.getPath().getLastPathComponent();
                if (node != null && node.getUserObject() instanceof Variable) {

                    if (selectedVariable != null) {
                        selectedVariable.deleteObservers();
                    }

                    selectedVariable = (Variable) node.getUserObject();
                    selectedVariable.addObserver(MapView.this);

                    remove(dataTable);
                    dataTable = new CalTable(selectedVariable);

                    lineChartX.setTable(dataTable);
                    lineChartY.setTable(dataTable);

                    splitPane.setTopComponent(dataTable);
                    revalidate();
                    repaint();

                    splitPane.setDividerLocation(dataTable.getComponentHeight());

                    updateChart(selectedVariable);
                }
            }
        });

        treeVariable.addMouseListener(new TreeMouseListener());

        dataTable = new CalTable(null);
        splitPane.setTopComponent(dataTable);

        mapChartView = new MapChartView();
        splitPane.setBottomComponent(mapChartView);
    }

    public void addCalToTree(MapCal cal) {

        DefaultMutableTreeNode nodeCal = new DefaultMutableTreeNode(cal.getName());

        ((DefaultMutableTreeNode) treeVariable.getModel().getRoot()).add(nodeCal);

        DynamicUtilTreeNode.createChildren(nodeCal, cal.getMdbData().getCategory());

        String typeName;
        String sousType;
        TreePath path = null;

        for (Variable var : cal.getListVariable()) {

            if (var.getInfos() != null) {
                typeName = var.getInfos().getTypeName();
                sousType = var.getInfos().getSousType();

                path = find(nodeCal, typeName, sousType);
            }

            if (path != null) {
                DefaultMutableTreeNode node = ((DefaultMutableTreeNode) path.getLastPathComponent());
                node.setAllowsChildren(true);
                node.add(new DefaultMutableTreeNode(var));
            } else {
                nodeCal.add(new DefaultMutableTreeNode(var));
            }
        }

        treeModel.reload();
    }

    private final void removeCalFromTree(DefaultMutableTreeNode nodeCal) {
        ((DefaultMutableTreeNode) treeVariable.getModel().getRoot()).remove(nodeCal);
        treeModel.reload();
    }

    private TreePath find(DefaultMutableTreeNode root, String parent, String child) {
        @SuppressWarnings("unchecked")
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();

        DefaultMutableTreeNode node;
        DefaultMutableTreeNode nodeChild;

        while (e.hasMoreElements()) {
            node = e.nextElement();

            int nbChild = node.getChildCount();

            if (node.toString().equalsIgnoreCase(parent)) {
                if (nbChild > 0) {
                    for (int i = 0; i < nbChild; i++) {
                        nodeChild = (DefaultMutableTreeNode) node.getChildAt(i);
                        if (nodeChild.toString().equalsIgnoreCase(child)) {
                            return new TreePath(nodeChild.getPath());
                        }
                    }
                }
                return new TreePath(node.getPath());
            }
        }
        return null;
    }

    public final void updateChart(Variable variable) {

        JSurface jSurface = surfaceChart.getSurface();

        BitSet chartVisible = new BitSet(3); // bit 0 : lineChartX, bit 1 : lineChartY, bit 2 : surfaceChart
        boolean modifiedVariable = variable.isModified();

        switch (variable.getType()) {

        case COURBE:
            float[][] zValuesOrigin = variable.getZvalues(modifiedVariable);

            int length = zValuesOrigin[0].length;

            if (length == 0) {
                return;
            }

            float[][] zValuesNew = new float[2][length];

            zValuesNew[0] = Arrays.copyOf(zValuesOrigin[0], length);
            zValuesNew[1] = Arrays.copyOf(zValuesOrigin[0], length);

            surfaceChart.getArraySurfaceModel().setValues(variable.getXAxis(modifiedVariable), new float[] { 0, 1 }, zValuesNew);
            jSurface.setXLabel("X");
            chartVisible.set(1);

            this.lineChartY.changeSeries(createCurve(variable));

            break;

        case MAP:
            surfaceChart.getArraySurfaceModel().setValues(variable.getXAxis(modifiedVariable), variable.getYAxis(modifiedVariable),
                    variable.getZvalues(modifiedVariable));
            jSurface.setXLabel("X");
            jSurface.setYLabel("Y");

            chartVisible.flip(0, 3);

            this.lineChartX.changeSeries(createIsoX(variable));
            this.lineChartY.changeSeries(createIsoY(variable));

            break;

        default:
            break;
        }

        lineChartX.setVisible(chartVisible.get(0));
        lineChartY.setVisible(chartVisible.get(1));
        surfaceChart.setVisible(chartVisible.get(2));
    }

    private XYSeries[] createIsoX(Variable variable) {

        final XYSeries[] series = new XYSeries[variable.getDimX() - 1];

        for (int x = 1; x < variable.getDimX(); x++) {
            series[x - 1] = new XYSeries(variable.getValue(true, 0, x).toString());

            for (int y = 1; y < variable.getDimY(); y++) {
                series[x - 1].add(Double.parseDouble(variable.getValue(true, y, 0).toString()),
                        Double.parseDouble(variable.getValue(true, y, x).toString()), false);
            }
        }

        return series;
    }

    private XYSeries[] createIsoY(Variable variable) {

        final XYSeries[] series = new XYSeries[variable.getDimY() - 1];

        for (int y = 1; y < variable.getDimY(); y++) {
            series[y - 1] = new XYSeries(variable.getValue(true, y, 0).toString());

            for (int x = 1; x < variable.getDimX(); x++) {
                series[y - 1].add(Double.parseDouble(variable.getValue(true, 0, x).toString()),
                        Double.parseDouble(variable.getValue(true, y, x).toString()), false);
            }
        }

        return series;
    }

    private XYSeries[] createCurve(Variable variable) {

        final XYSeries[] series = new XYSeries[] { new XYSeries("") };

        for (int x = 0; x < variable.getDimX(); x++) {

            series[0].add(Double.parseDouble(variable.getValue(true, 0, x).toString()), Double.parseDouble(variable.getValue(true, 1, x).toString()),
                    false);
        }

        return series;
    }

    private final class MapChartView extends JPanel {

        private static final long serialVersionUID = 1L;

        public MapChartView() {
            super(new GridBagLayout());

            final GridBagConstraints gbc = new GridBagConstraints();

            lineChartX = new LineChart();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 50;
            gbc.weighty = 50;
            gbc.insets = new Insets(0, 0, 2, 4);
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            add(lineChartX, gbc);
            lineChartX.setVisible(false);

            lineChartY = new LineChart();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 50;
            gbc.weighty = 50;
            gbc.insets = new Insets(2, 0, 0, 4);
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            add(lineChartY, gbc);
            lineChartY.setVisible(false);

            surfaceChart = new SurfaceChart();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.gridheight = 2;
            gbc.weightx = 50;
            gbc.weighty = 100;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            add(surfaceChart, gbc);
            surfaceChart.setVisible(false);

        }
    }

    private final class TreeMouseListener extends MouseAdapter {

        final String ICON_EXCEL = "/icon_excel_24.png";
        final String ICON_XML = "/icon_xml_24.png";
        final String ICON_MAP = "/icon_exportMap_24.png";

        @Override
        public void mouseReleased(MouseEvent e) {

            final TreePath treePath = treeVariable.getPathForLocation(e.getX(), e.getY());

            if (treePath == null) {
                return;
            }

            treeVariable.setSelectionPath(treePath);

            final Object object = ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();

            if (e.isPopupTrigger() && object != null) {
                final JPopupMenu menu = new JPopupMenu();
                final JMenu menuExport = new JMenu("Export Excel");
                JMenuItem menuItem;

                if (object.toString().equals(mapCal.getName())) {
                    menuItem = new JMenuItem("Supprimer la calibration", new ImageIcon(getClass().getResource(ICON_XML)));
                    menuItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeCalFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
                        }
                    });
                    menu.add(menuItem);
                }

                if (selectedVariable != null) {
                    menuItem = new JMenuItem("Variable sélectionnée", new ImageIcon(getClass().getResource(ICON_EXCEL)));
                    menuItem.addActionListener(new ExportListener());
                    menuExport.add(menuItem);
                    menuExport.addSeparator();
                }

                menuItem = new JMenuItem("Toutes les variables", new ImageIcon(getClass().getResource(ICON_EXCEL)));
                menuItem.addActionListener(new ExportListener());
                menuExport.add(menuItem);

                menu.add(menuExport);
                menu.addSeparator();

                menuItem = new JMenuItem("Convertir en Cdfx", new ImageIcon(getClass().getResource(ICON_XML)));
                menuItem.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        final JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Enregistement du fichier");
                        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier Cdfx", "cdfx"));
                        fileChooser.setSelectedFile(new File(mapCal.getName() + ".cdfx"));
                        final int rep = fileChooser.showSaveDialog(null);

                        if (rep == JFileChooser.APPROVE_OPTION) {
                            List<Variable> listToExport = new ArrayList<Variable>();

                            for (int i = 0; i < mapCal.getListVariable().size(); i++) {
                                listToExport.add(mapCal.getListVariable().get(i));
                            }

                            boolean result = MapCal.toCdfx(listToExport, fileChooser.getSelectedFile());

                            if (result) {
                                JOptionPane.showMessageDialog(null, "Conversion termin\u00e9e !");
                            }
                        }
                    }
                });
                menu.add(menuItem);

                menuItem = new JMenuItem("Enregistrer fichier Map", new ImageIcon(getClass().getResource(ICON_MAP)));
                menuItem.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        final JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Enregistement du fichier");
                        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier Map", "map"));
                        fileChooser.setSelectedFile(new File(mapCal.getName() + "_new.map"));
                        final int rep = fileChooser.showSaveDialog(null);

                        if (rep == JFileChooser.APPROVE_OPTION) {
                            List<Variable> listToExport = new ArrayList<Variable>();

                            for (int i = 0; i < mapCal.getListVariable().size(); i++) {
                                listToExport.add(mapCal.getListVariable().get(i));
                            }

                            boolean result = MapCal.exportMap(listToExport, fileChooser.getSelectedFile());

                            if (result) {
                                JOptionPane.showMessageDialog(null, "Sauvegarde termin\u00e9e !");
                            }
                        }
                    }
                });
                menu.add(menuItem);

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
                    for (int i = 0; i < mapCal.getListVariable().size(); i++) {
                        listToExport.add(mapCal.getListVariable().get(i));
                    }
                } else {
                    listToExport.add(selectedVariable);
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

    @Override
    public void update(Observable object, Object arg) {
        if (object instanceof Variable) {
            updateChart((Variable) object);
        }
    }

}
