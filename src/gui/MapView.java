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
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jfree.data.xy.XYSeries;

import calib.MapCal;
import calib.MdbWorkspace;
import calib.Variable;
import log.Log;
import net.ericaro.surfaceplotter.surface.JSurface;
import observer.CursorObservateur;
import observer.MapCalEvent;
import observer.MapCalListener;
import utils.Preference;

public final class MapView extends JPanel implements Observer, ListSelectionListener, CursorObservateur {

    private static final long serialVersionUID = 1L;

    private final List<MapCal> listCal;
    private final JTree treeVariable;
    private CalTable dataTable;
    private LineChart lineChartX;
    private LineChart lineChartY;
    private SurfaceChart surfaceChart;
    private JSplitPane splitPane;
    private Variable selectedVariable;

    private MapChartView mapChartView;

    private Log log;

    private final DefaultTreeModel treeModel;

    private final List<MapCalListener> listeners = new ArrayList<>();

    public MapView() {

        super();
        setLayout(new GridBagLayout());

        this.listCal = new ArrayList<MapCal>();

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
        treeVariable.setDragEnabled(true);

        treeVariable.setCellRenderer(new TreeCalRenderer());

        JScrollPane sp = new JScrollPane(treeVariable);
        add(sp, gbc);

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

                    dataTable.populate(selectedVariable);

                    // splitPane.setTopComponent(dataTable);
                    splitPane.revalidate();
                    splitPane.repaint();

                    splitPane.setDividerLocation(dataTable.getComponentHeight());

                    updateChart(selectedVariable);
                } else {
                    clearSelection();
                }
            }
        });

        treeVariable.addMouseListener(new TreeMouseListener());

        dataTable = new CalTable(this);
        dataTable.getTable().getSelectionModel().addListSelectionListener(MapView.this);
        dataTable.getTable().getColumnModel().getSelectionModel().addListSelectionListener(MapView.this);
        splitPane.setTopComponent(dataTable);

        mapChartView = new MapChartView();
        lineChartX.setTable(dataTable);
        lineChartY.setTable(dataTable);
        splitPane.setBottomComponent(mapChartView);
    }

    public final void setLog(Log log) {
        this.log = log;

        if (dataTable != null) {
            dataTable.setTrackFlag();
        }
    }

    public final Log getLog() {
        return log;
    }

    public final MapCal findSelectedCal() {
        TreePath path = treeVariable.getSelectionPath();

        if (path != null && path.getPath().length > 1) {
            Object calNode = path.getPath()[1];
            for (MapCal mapCal : listCal) {
                if (mapCal.getName().equals(calNode.toString())) {
                    return mapCal;
                }
            }
        }

        return null;
    }

    public Variable getSelectedVariable() {
        return selectedVariable;
    }

    public final MapCal getCalForFormula() {

        if (listCal == null || listCal.isEmpty()) {
            return null;
        }

        for (MapCal mapCal : listCal) {
            if (mapCal.isUsedByFormula()) {
                return mapCal;
            }
        }
        return null;
    }

    public void addCalToTree(MapCal cal) {

        DefaultMutableTreeNode nodeCal = new DefaultMutableTreeNode(cal);

        ((DefaultMutableTreeNode) treeVariable.getModel().getRoot()).add(nodeCal);

        DynamicUtilTreeNode.createChildren(nodeCal, cal.getMdbData().getCategory());

        Vector<DefaultMutableTreeNode> v = new Vector<>();

        @SuppressWarnings("unchecked")
        Enumeration<DefaultMutableTreeNode> en = nodeCal.children();

        while (en.hasMoreElements()) {
            v.add(en.nextElement());
        }

        Collections.sort(v, new Comparator<DefaultMutableTreeNode>() {

            @Override
            public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        });

        nodeCal.removeAllChildren();

        for (DefaultMutableTreeNode sortedNode : v) {
            nodeCal.add(sortedNode);
        }

        v.clear();

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

        this.listCal.add(cal);

        treeModel.reload();

        // fireMapCalChange();
    }

    private final void removeCalFromTree(DefaultMutableTreeNode nodeCal) {
        ((DefaultMutableTreeNode) treeVariable.getModel().getRoot()).remove(nodeCal);

        for (MapCal mapCal : listCal) {
            if (mapCal.getName().equals(nodeCal.toString())) {
                listCal.remove(mapCal);
                clearSelection();
                break;
            }
        }

        treeModel.reload();

        fireMapCalChange();
    }

    private final void clearSelection() {
        selectedVariable = null;
        dataTable.populate(null);
        // splitPane.setTopComponent(dataTable);
        lineChartX.setVisible(false);
        lineChartY.setVisible(false);
        surfaceChart.setVisible(false);
        splitPane.revalidate();
        splitPane.repaint();
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

    /**
     * Est appelé par le changement de sélection sur l'arbre des variables
     */
    public final void updateChart(Variable variable) {

        JSurface jSurface = surfaceChart.getSurface();

        BitSet chartVisible = new BitSet(3); // bit 0 : lineChartX, bit 1 : lineChartY, bit 2 : surfaceChart
        int idxPage = dataTable.getIdxCalPage();

        switch (variable.getType()) {

        case COURBE:
            float[][] zValuesOrigin = variable.getZvalues(idxPage);

            int length = zValuesOrigin[0].length;

            if (length == 0) {
                return;
            }

            chartVisible.set(1);

            this.lineChartY.changeSeries(createCurve(variable), variable.getMin(), variable.getMax());

            break;

        case MAP:
            surfaceChart.getArraySurfaceModel().setValues(variable.getXAxis(idxPage), variable.getYAxis(idxPage), variable.getZvalues(idxPage));
            jSurface.setXLabel("X");
            jSurface.setYLabel("Y");

            chartVisible.flip(0, 3);

            this.lineChartX.changeSeries(createIsoX(variable), variable.getMin(), variable.getMax());
            this.lineChartY.changeSeries(createIsoY(variable), variable.getMin(), variable.getMax());

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

        int idxPage = dataTable.getIdxCalPage();

        for (int x = 1; x < variable.getDimX(); x++) {
            series[x - 1] = new XYSeries(variable.getValue(idxPage, 0, x).toString());

            for (int y = 1; y < variable.getDimY(); y++) {
                series[x - 1].add(variable.getDoubleValue(idxPage, y, 0), variable.getDoubleValue(idxPage, y, x), false);
            }
        }

        return series;
    }

    private XYSeries[] createIsoY(Variable variable) {

        final XYSeries[] series = new XYSeries[variable.getDimY() - 1];

        int idxPage = dataTable.getIdxCalPage();

        for (int y = 1; y < variable.getDimY(); y++) {
            series[y - 1] = new XYSeries(variable.getValue(idxPage, y, 0).toString());

            for (int x = 1; x < variable.getDimX(); x++) {
                series[y - 1].add(variable.getDoubleValue(idxPage, 0, x), variable.getDoubleValue(idxPage, y, x), false);
            }
        }

        return series;
    }

    private XYSeries[] createCurve(Variable variable) {

        final XYSeries[] series = new XYSeries[] { new XYSeries("") };

        int idxPage = dataTable.getIdxCalPage();

        for (int x = 0; x < variable.getDimX(); x++) {
            series[0].add(variable.getDoubleValue(idxPage, 0, x), variable.getDoubleValue(idxPage, 1, x), false);
        }

        return series;
    }

    private final class MapChartView extends JPanel {

        private static final long serialVersionUID = 1L;

        public MapChartView() {
            super(new GridBagLayout());

            final GridBagConstraints gbc = new GridBagConstraints();

            lineChartX = new LineChart('x');
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

            lineChartY = new LineChart('y');
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

    public final void addMapCalListener(MapCalListener listener) {
        this.listeners.add(listener);
    }

    public final void removeMapCalListener(MapCalListener listener) {
        this.listeners.remove(listener);
    }

    public final void fireMapCalChange() {
        for (MapCalListener l : listeners) {
            l.MapCalChanged(new MapCalEvent(this));
        }
    }

    public final void setCalForFormula(String calName) {
        for (MapCal c : listCal) {
            if (c.getName().equals(calName)) {
                c.setUsedByFormula(true);
            } else {
                c.setUsedByFormula(false);
            }
        }

        treeVariable.revalidate();
        treeVariable.repaint();
        fireMapCalChange();
    }

    private final class TreeMouseListener extends MouseAdapter {

        final String ICON_SELECT = "/icon_selectMap_24.png";
        final String ICON_EXCEL = "/icon_excel_24.png";
        final String ICON_XML = "/icon_xml_24.png";
        final String ICON_MAP = "/icon_exportMap_24.png";
        final String ICON_DELETE = "/icon_del_24.png";
        final String ICON_WORKSPACE = "/icon_addWorkspace_24.png";

        @Override
        public void mouseReleased(MouseEvent e) {

            final TreePath treePath = treeVariable.getPathForLocation(e.getX(), e.getY());

            if (treePath == null || treePath.getPath().length < 2) {
                return;
            }

            treeVariable.setSelectionPath(treePath);

            final Object object = ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();

            if (e.isPopupTrigger() && object != null) {
                final JPopupMenu menu = new JPopupMenu();
                final JMenu menuExport = new JMenu("Export Excel");
                JMenuItem menuItem;

                if (object.toString().equals(findSelectedCal().getName())) {

                    menuItem = new JMenuItem("Associer un workspace", new ImageIcon(getClass().getResource(ICON_WORKSPACE)));
                    menuItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {

                            final JFileChooser fc = new JFileChooser(Preference.getPreference(Preference.KEY_CAL));
                            fc.setMultiSelectionEnabled(false);
                            fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
                            fc.setFileFilter(new FileFilter() {

                                @Override
                                public String getDescription() {
                                    return "Workspace (*.mdb)";
                                }

                                @Override
                                public boolean accept(File f) {
                                    if (f.isDirectory()) {
                                        return true;
                                    }
                                    return f.getName().toLowerCase().endsWith("mdb");
                                }
                            });
                            final int reponse = fc.showOpenDialog(null);

                            if (reponse == JFileChooser.APPROVE_OPTION) {
                                MdbWorkspace workspace = new MdbWorkspace(fc.getSelectedFile());
                                MapCal cal = findSelectedCal();
                                boolean res = cal.associateWorksplace(workspace.getVariablesECU());
                                if (res) {
                                    JOptionPane.showMessageDialog(null, "Workspace associ\u00e9");
                                } else {
                                    JOptionPane.showMessageDialog(null, "Association avort\u00e9e...");
                                }

                            }

                        }
                    });
                    menu.add(menuItem);

                    menuItem = new JMenuItem("Map utilis\u00e9e pour les formules", new ImageIcon(getClass().getResource(ICON_SELECT)));
                    menuItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {

                            setCalForFormula(object.toString());
                        }
                    });
                    menu.add(menuItem);

                    menuItem = new JMenuItem("Supprimer la calibration", new ImageIcon(getClass().getResource(ICON_DELETE)));
                    menuItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeCalFromTree((DefaultMutableTreeNode) treePath.getLastPathComponent());
                        }
                    });
                    menu.add(menuItem);
                }

                if (selectedVariable != null) {
                    menuItem = new JMenuItem("Variable s\u00e9lectionn\u00e9e", new ImageIcon(getClass().getResource(ICON_EXCEL)));
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

                        MapCal mapCal = findSelectedCal();

                        final JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Enregistement du fichier");
                        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier Cdfx", "cdfx"));
                        fileChooser.setSelectedFile(new File(mapCal.getName() + ".cdfx"));
                        final int rep = fileChooser.showSaveDialog(null);

                        if (rep == JFileChooser.APPROVE_OPTION) {

                            boolean result = MapCal.toCdfx(mapCal.getListVariable(), mapCal.getMdbData().getConfigEcu().getParamsEcu(),
                                    fileChooser.getSelectedFile());

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

                        MapCal mapCal = findSelectedCal();

                        final JFileChooser fileChooser = new JFileChooser(Preference.getPreference(Preference.KEY_CAL));
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

            MapCal mapCal = findSelectedCal();

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

                    switch (JOptionPane.showConfirmDialog(null, "Le fichier existe d\u00e9ja, \u00e9craser?", null,
                            JOptionPane.INFORMATION_MESSAGE)) {
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
                            "Export termin\u00e9 !\n" + fileChooser.getSelectedFile() + "\nVoulez-vous ouvrir le fichier?", null,
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
                    JOptionPane.showMessageDialog(null, "Export abandonn\u00e9 !");
                }
            }
        }
    }

    @Override
    public void update(Observable object, Object arg) {

        if (object instanceof Variable) {
            updateChart((Variable) object);

            if (treeVariable.getSelectionPath() != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeVariable.getSelectionPath().getLastPathComponent();
                ((DefaultTreeModel) treeVariable.getModel()).nodeChanged(node);
            }

        }
    }

    public final void refreshVariable(Variable var) {
        dataTable.populate(var);
        updateChart(var);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {

        if (!e.getValueIsAdjusting()) {

            int[] cols = dataTable.getTable().getSelectedColumns();
            int[] rows = dataTable.getTable().getSelectedRows();

            if (lineChartX.isVisible()) {
                lineChartX.updateAnnotation(rows, cols);
            }

            if (lineChartY.isVisible()) {
                lineChartY.updateAnnotation(rows, cols);
            }

        }

    }

    @Override
    public void updateCursorValue(int cursorIndex) {
        if (this.selectedVariable != null) {
            MapCal cal = findSelectedCal();
            if (cal != null && cal.hasWorkspaceLinked()) {
                dataTable.showCursorValues(cursorIndex);
            } else {
                // JOptionPane.showMessageDialog(null, "Il faut associer un workspace à la calibration");
            }

        }
    }
}
