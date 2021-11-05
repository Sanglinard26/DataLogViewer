package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import calib.MapCal;
import dialog.DialAddMeasure;
import dialog.DialManageFormula;
import dialog.DialManual;
import dialog.DialNewChart;
import dialog.DialNewFormula;
import dialog.DialNews;
import dialog.DialogProperties;
import log.Formula;
import log.Log;
import log.Measure;
import observer.MapCalEvent;
import observer.MapCalListener;
import utils.ExportUtils;
import utils.Preference;
import utils.Utilitaire;

public final class Ihm extends JFrame implements MapCalListener, ActionListener {

    private static final long serialVersionUID = 1L;

    private final static String VERSION = "v1.52";
    private final String DEMO = "demo";

    private final static String LOG_PANEL = "LOG";
    private final static String MAP_PANEL = "MAP";

    private final String ICON_MAP_TAB = "/icon_mapFile_24.png";
    private final String ICON_LOG_TAB = "/icon_log_24.png";

    private final JTabbedPane mainTabbedPane;
    private JTabbedPane chartTabbedPane;
    private FilteredListModel listModel;
    private FilteredListMeasure listLogMeasure;
    private JScrollPane scrollTableCursorValues;
    private TableCursorValue tableCursorValues;
    private PanelCondition panelCondition;

    private JLabel labelFnr;
    private JLabel labelLogName;

    private Log log;
    private Set<Measure> listFormula = new HashSet<Measure>();
    private boolean axisSync = false;

    boolean activeThread = true;

    private MapView mapView;

    private final Map<Integer, List<IntervalMarker>> listZone = new HashMap<Integer, List<IntervalMarker>>();

    public Ihm() {
        super("DataLogViewer " + VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setJMenuBar(createMenu());

        mainTabbedPane = new JTabbedPane();

        mainTabbedPane.addTab(LOG_PANEL, new ImageIcon(getClass().getResource(ICON_LOG_TAB)), createLogPanel());

        mapView = new MapView();
        mapView.addMapCalListener(this);

        mainTabbedPane.addTab(MAP_PANEL, new ImageIcon(getClass().getResource(ICON_MAP_TAB)), mapView);

        mainTabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (mainTabbedPane.getSelectedIndex() == 0) {
                    if (mapView.getCalForFormula() != null) {
                        refresh();
                    }
                }
            }
        });

        Container root = getContentPane();
        root.setLayout(new BorderLayout());
        root.add(createToolBar(), BorderLayout.NORTH);
        root.add(mainTabbedPane, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(getWidth(), getHeight()));

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    private final JMenuBar createMenu() {

        final String ICON_OPEN_LOG = "/icon_openLog_16.png";
        final String ICON_SAVE_CONFIG = "/icon_saveConfig_16.png";
        final String ICON_OPEN_CONFIG = "/icon_openConfig_16.png";
        final String ICON_ADD_WINDOW = "/icon_addWindow_16.png";
        final String ICON_CLOSE_WINDOW = "/icon_closeWindow_16.png";
        final String ICON_EXIT = "/icon_exit_16.png";
        final String ICON_NEW = "/new_icon_16.png";
        final String ICON_NOTICE = "/icon_manual_16.png";
        final String ICON_FORMULA = "/icon_formula_16.png";
        final String ICON_MANAGE_FORMULA = "/icon_manageFormula_16.png";

        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Fichier");
        menuBar.add(menu);

        JMenuItem menuItem = new JMenuItem("Ouvrir log", new ImageIcon(getClass().getResource(ICON_OPEN_LOG)));
        menuItem.setMnemonic(KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new OpenLog());
        menu.add(menuItem);

        menuItem = new JMenuItem("Ouvrir configuration", new ImageIcon(getClass().getResource(ICON_OPEN_CONFIG)));
        menuItem.setMnemonic(KeyEvent.VK_I);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fc = new JFileChooser(Preference.getPreference(Preference.KEY_CONFIG));
                fc.setMultiSelectionEnabled(false);
                fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
                fc.setFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "Fichier de configuration graphique (*.cfg, *.xml)";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        return f.getName().toLowerCase().endsWith("cfg") || f.getName().toLowerCase().endsWith("xml");
                    }
                });
                final int reponse = fc.showOpenDialog(Ihm.this);
                if (reponse == JFileChooser.APPROVE_OPTION) {

                    File config = fc.getSelectedFile();

                    if (DEMO.equals(config.getName().toLowerCase())) {

                        try {
                            File tmp = File.createTempFile("config", null);
                            tmp.deleteOnExit();
                            Files.copy(getClass().getResourceAsStream("/config.cfg"), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            openConfig(tmp);
                            mainTabbedPane.setSelectedIndex(0);
                            return;
                        } catch (IOException e1) {
                        }
                    }

                    openConfig(config);
                    mainTabbedPane.setSelectedIndex(0);
                }

            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Enregistrer configuration", new ImageIcon(getClass().getResource(ICON_SAVE_CONFIG)));
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser(Preference.getPreference(Preference.KEY_CONFIG));
                fileChooser.setDialogTitle("Enregistement de la configuration");
                fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier de configuration graphique (*.xml)", "xml"));
                fileChooser.setSelectedFile(new File("config.xml"));
                final int rep = fileChooser.showSaveDialog(null);

                if (rep == JFileChooser.APPROVE_OPTION) {

                    String extension = "";
                    String fileName = fileChooser.getSelectedFile().getAbsolutePath();
                    File file = fileChooser.getSelectedFile();

                    final int idxDot = fileName.lastIndexOf(".");

                    if (idxDot > -1) {
                        extension = fileName.substring(idxDot + 1);
                    }
                    if (!extension.equalsIgnoreCase("xml")) {
                        file = new File(fileName.replace("." + extension, "") + ".xml");
                    }

                    saveConfig(file);
                }

            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Quitter", new ImageIcon(getClass().getResource(ICON_EXIT)));
        menuItem.setMnemonic(KeyEvent.VK_Q);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);

            }
        });
        menu.add(menuItem);

        menu = new JMenu("Fen\u00eatre");
        menuBar.add(menu);

        menuItem = new JMenuItem("Ajouter", new ImageIcon(getClass().getResource(ICON_ADD_WINDOW)));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addChartWindow(null);
                mainTabbedPane.setSelectedIndex(0);
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Tout fermer", new ImageIcon(getClass().getResource(ICON_CLOSE_WINDOW)));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                closeWindows();
            }
        });
        menu.add(menuItem);

        menu = new JMenu("Formules");
        menuBar.add(menu);

        menuItem = new JMenuItem("Nouvelle", new ImageIcon(getClass().getResource(ICON_FORMULA)));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new DialNewFormula(Ihm.this);
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Gestionnaire", new ImageIcon(getClass().getResource(ICON_MANAGE_FORMULA)));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new DialManageFormula(Ihm.this);
            }
        });
        menu.add(menuItem);

        menu = new JMenu("Pr\u00e9f\u00e9rences");
        menuBar.add(menu);

        JMenu subMenu = new JMenu("Log");
        menuItem = new JMenuItem(new AbstractAction("Chemin d'import") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                final String pathFolder = Utilitaire.getFolder("Choix du chemin", Preference.getPreference(Preference.KEY_LOG));
                if (!Preference.KEY_LOG.equals(pathFolder)) {
                    Preference.setPreference(Preference.KEY_LOG, pathFolder);
                    ((JMenuItem) e.getSource()).setToolTipText(pathFolder);
                }
            }
        });
        menuItem.setToolTipText(Preference.getPreference(Preference.KEY_LOG));
        subMenu.add(menuItem);
        menu.add(subMenu);

        subMenu = new JMenu("Configuration");
        menuItem = new JMenuItem(new AbstractAction("Chemin d'import") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                final String pathFolder = Utilitaire.getFolder("Choix du chemin", Preference.getPreference(Preference.KEY_CONFIG));
                if (!Preference.KEY_CONFIG.equals(pathFolder)) {
                    Preference.setPreference(Preference.KEY_CONFIG, pathFolder);
                    ((JMenuItem) e.getSource()).setToolTipText(pathFolder);
                }
            }
        });
        menuItem.setToolTipText(Preference.getPreference(Preference.KEY_CONFIG));
        subMenu.add(menuItem);
        menu.add(subMenu);

        subMenu = new JMenu("Calibration");
        menuItem = new JMenuItem(new AbstractAction("Chemin d'import/export") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                final String pathFolder = Utilitaire.getFolder("Choix du chemin", Preference.getPreference(Preference.KEY_CAL));
                if (!Preference.KEY_CAL.equals(pathFolder)) {
                    Preference.setPreference(Preference.KEY_CAL, pathFolder);
                    ((JMenuItem) e.getSource()).setToolTipText(pathFolder);
                }
            }
        });
        menuItem.setToolTipText(Preference.getPreference(Preference.KEY_CAL));
        subMenu.add(menuItem);
        menu.add(subMenu);

        menu = new JMenu("?");
        menuBar.add(menu);

        menuItem = new JMenuItem("ChangeLog", new ImageIcon(getClass().getResource(ICON_NEW)));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new DialNews(Ihm.this);

            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Aide", new ImageIcon(getClass().getResource(ICON_NOTICE)));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new DialManual(Ihm.this);
            }
        });
        menu.add(menuItem);

        return menuBar;
    }

    private final JToolBar createToolBar() {
        final String ICON_OPEN_LOG = "/icon_openLog_32.png";
        final String ICON_OPEN_CONFIG = "/icon_openConfig_32.png";
        final String ICON_OPEN_MAP = "/icon_mapFile_32.png";
        final String ICON_ADD_WINDOW = "/icon_addWindow_32.png";
        final String ICON_NEW_PLOT = "/icon_newPlot_32.png";
        final String ICON_SHARE_AXIS = "/icon_shareAxis_32.png";
        final String ICON_NEW_TABLE = "/icon_table_32.png";

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createEtchedBorder());

        JButton btOpenLog = new JButton(null, new ImageIcon(getClass().getResource(ICON_OPEN_LOG)));
        btOpenLog.setToolTipText("Ouvrir log");
        btOpenLog.addActionListener(new OpenLog());
        bar.add(btOpenLog);

        JButton btOpenConfig = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource(ICON_OPEN_CONFIG))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final JFileChooser fc = new JFileChooser(Preference.getPreference(Preference.KEY_CONFIG));
                fc.setMultiSelectionEnabled(false);
                fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
                fc.setFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "Fichier de configuration graphique (*.cfg, *.xml)";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        return f.getName().toLowerCase().endsWith("cfg") || f.getName().toLowerCase().endsWith("xml");
                    }
                });
                final int reponse = fc.showOpenDialog(Ihm.this);
                if (reponse == JFileChooser.APPROVE_OPTION) {
                    File config = fc.getSelectedFile();

                    if (DEMO.equals(config.getName().toLowerCase())) {

                        try {
                            File tmp = File.createTempFile("config", null);
                            tmp.deleteOnExit();
                            Files.copy(getClass().getResourceAsStream("/config.cfg"), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            openConfig(tmp);
                            mainTabbedPane.setSelectedIndex(0);
                            return;
                        } catch (IOException e) {
                        }
                    }

                    openConfig(config);
                    mainTabbedPane.setSelectedIndex(0);
                }

            }

        });
        btOpenConfig.setToolTipText("Ouvrir configuration");
        bar.add(btOpenConfig);

        JButton btOpenMap = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource(ICON_OPEN_MAP))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {

                final JFileChooser fc = new JFileChooser(Preference.getPreference(Preference.KEY_CAL));
                fc.setMultiSelectionEnabled(false);
                fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
                fc.setFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "Fichier de calibration (*.map)";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        return f.getName().toLowerCase().endsWith("map");
                    }
                });
                final int reponse = fc.showOpenDialog(Ihm.this);
                if (reponse == JFileChooser.APPROVE_OPTION) {
                    addMapFile(new MapCal(fc.getSelectedFile()));
                }

            }
        });
        btOpenMap.setEnabled(true);
        btOpenMap.setToolTipText("Ouvrir un fichier de calibration");
        bar.add(btOpenMap);

        bar.addSeparator(new Dimension(20, 32));

        JButton btAddWindow = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource(ICON_ADD_WINDOW))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {

                addChartWindow(null);
                mainTabbedPane.setSelectedIndex(0);
            }
        });
        btAddWindow.setEnabled(true);
        btAddWindow.setToolTipText("Ajouter fen\u00eatre");
        bar.add(btAddWindow);

        JButton btNewPlot = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource(ICON_NEW_PLOT))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {

                new DialNewChart(Ihm.this);

            }
        });
        btNewPlot.setEnabled(true);
        btNewPlot.setToolTipText("Nouveau graphique");
        bar.add(btNewPlot);

        JButton btNewTable = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource(ICON_NEW_TABLE))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {

                addTableWindow();

            }
        });
        btNewTable.setEnabled(true);
        btNewTable.setToolTipText("Pas encore impl\u00e9ment\u00e9");
        btNewTable.setEnabled(false);
        bar.add(btNewTable);

        final JToggleButton btSynchro = new JToggleButton(new ImageIcon(getClass().getResource(ICON_SHARE_AXIS)));
        btSynchro.setToolTipText("Synchroniser les axes des abscisses");
        btSynchro.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                axisSync = btSynchro.isSelected();

                final int idxWindow = chartTabbedPane.getSelectedIndex();

                if (idxWindow < 0 || log == null) {
                    btSynchro.setSelected(false);
                    return;
                }

                if (axisSync) {

                    ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idxWindow);
                    if (chartView.getDatasetType() > 1) {
                        btSynchro.setSelected(false);
                        JOptionPane.showMessageDialog(null, "Fonctionne pour les graphiques=f(temps).", "INFO", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    ValueAxis domainAxis = chartView.getPlot().getDomainAxis();
                    ChartView otherChart;
                    for (int i = 0; i < chartTabbedPane.getTabCount(); i++) {
                        if (!(chartTabbedPane.getComponentAt(i) instanceof ChartView)) {
                            continue;
                        }
                        otherChart = (ChartView) chartTabbedPane.getComponentAt(i);
                        if (i == idxWindow || otherChart.getDatasetType() > 1) {
                            continue;
                        }
                        otherChart.getPlot().setDomainAxis(domainAxis);
                    }
                } else {
                    ChartView chartView;
                    Measure time = log.getTime();
                    Range xAxisRange;
                    for (int i = 0; i < chartTabbedPane.getTabCount(); i++) {
                        if (!(chartTabbedPane.getComponentAt(i) instanceof ChartView)) {
                            continue;
                        }
                        chartView = (ChartView) chartTabbedPane.getComponentAt(i);
                        if (chartView.getDatasetType() > 1) {
                            continue;
                        }
                        xAxisRange = chartView.getPlot().getDomainAxis().getRange();
                        chartView.getPlot().setDomainAxis(new NumberAxis(time.getName()));
                        chartView.getPlot().getDomainAxis().setRange(xAxisRange);
                    }
                }

            }
        });
        bar.add(btSynchro);

        return bar;
    }

    private final JPanel createLogPanel() {

        final GridBagConstraints gbc = new GridBagConstraints();

        listModel = new FilteredListModel();

        final JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        listLogMeasure = new FilteredListMeasure(listModel);
        listLogMeasure.getListMeasure().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    directToPlot();
                }
            }
        });
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 15;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(listLogMeasure, gbc);

        chartTabbedPane = new JTabbedPane();
        chartTabbedPane.setBorder(BorderFactory.createEtchedBorder());
        chartTabbedPane.setPreferredSize(new Dimension(800, 600));
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 70;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(chartTabbedPane, gbc);

        chartTabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int idx = chartTabbedPane.getSelectedIndex();
                if (idx > -1) {

                    ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idx);
                    tableCursorValues.getModel().changeList(chartView.getMeasuresColors());

                    if (chartView.getDatasetType() < 2) {
                        chartView.updateTableValue();
                        chartView.applyCondition(listZone.get(panelCondition.getTableCondition().getActiveCondition()));
                    }
                } else {
                    tableCursorValues.getModel().clearList();
                }
            }
        });

        tableCursorValues = new TableCursorValue();
        tableCursorValues.getTableHeader().setReorderingAllowed(false);
        tableCursorValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tableCursorValues.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                activeThread = true;

                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {

                    activeThread = false;

                    Object signalName = tableCursorValues.getValueAt(tableCursorValues.getSelectedRow(), 1);

                    int idx = chartTabbedPane.getSelectedIndex();
                    if (idx > -1) {
                        if (chartTabbedPane.getComponentAt(idx) instanceof ChartView) {
                            ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idx);

                            XYPlot xyPlot = chartView.getPlot(signalName.toString());

                            if (xyPlot != null) {
                                DialogProperties propertiesPanel = new DialogProperties(xyPlot);
                                int res = JOptionPane.showConfirmDialog(Ihm.this, propertiesPanel, "Propri\u00e9t\u00e9s", 2, -1);
                                if (res == JOptionPane.OK_OPTION) {
                                    propertiesPanel.updatePlot(chartView, xyPlot);
                                    chartView.getChart().fireChartChanged();
                                    ;

                                }
                            }
                        }
                    }
                }

                if (e.getClickCount() == 1) {

                    Thread t = new Thread(new Runnable() {

                        @Override
                        public void run() {

                            int row = tableCursorValues.getSelectedRow();

                            if (row == -1) {
                                return;
                            }

                            String signalName = tableCursorValues.getValueAt(row, 1).toString();

                            ChartView chartView = (ChartView) chartTabbedPane.getSelectedComponent();

                            if (chartView == null) {
                                return;
                            }

                            XYPlot plot = chartView.getPlot(signalName);

                            if (plot == null) {
                                return;
                            }

                            XYItemRenderer renderer = null;
                            int serieIdx = -1;

                            for (int nDataset = 0; nDataset < plot.getDatasetCount(); nDataset++) {
                                serieIdx = ((XYSeriesCollection) plot.getDataset(nDataset)).getSeriesIndex(signalName);
                                if (serieIdx > -1) {
                                    renderer = plot.getRenderer(nDataset);
                                    break;
                                }
                            }

                            float widthLine = ((BasicStroke) renderer.getSeriesStroke(serieIdx)).getLineWidth();

                            for (int i = 0; i < 7; i++) {

                                if (!activeThread) {
                                    renderer.setSeriesStroke(serieIdx, new BasicStroke(widthLine));
                                    return;
                                }

                                renderer.setSeriesStroke(serieIdx, new BasicStroke(widthLine + 3 * (i % 2)));

                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e1) {
                                    renderer.setSeriesStroke(serieIdx, new BasicStroke(widthLine));
                                }
                            }

                        }
                    }, "highlightCurve");

                    t.start();

                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {

                    int row = tableCursorValues.rowAtPoint(e.getPoint());

                    if (!tableCursorValues.isRowSelected(row))
                        tableCursorValues.changeSelection(row, 1, false, false);

                    if (log != null) {
                        createTableMenu().show(e.getComponent(), e.getX(), e.getY());
                    }

                }
            }
        });

        tableCursorValues.setPreferredScrollableViewportSize(tableCursorValues.getPreferredSize());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 15;
        gbc.weighty = 60;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        scrollTableCursorValues = new JScrollPane(tableCursorValues);
        panel.add(scrollTableCursorValues, gbc);

        panelCondition = new PanelCondition();
        panelCondition.getTableCondition().getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {

                    final int row = e.getFirstRow();
                    final Condition condition = (Condition) panelCondition.getTableCondition().getModel().getValueAt(row, 1);

                    int idx = chartTabbedPane.getSelectedIndex();
                    if (idx > -1) {
                        if (chartTabbedPane.getComponentAt(idx) instanceof ChartView) {
                            final ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idx);

                            if (log != null && chartView.getDatasetType() < 2) {

                                Thread thread = new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        BitSet bitCondition = condition.apply(log);

                                        if (condition.isActive()) {
                                            listZone.put(row, chartView.applyCondition(condition.isActive(), bitCondition, condition.getColor()));
                                            panelCondition.setListBoxAnnotation(listZone.get(row));
                                        } else {
                                            listZone.remove(row);
                                            chartView.removeCondition();
                                        }
                                    }
                                });
                                thread.start();
                            }
                        }
                    }
                }

            }
        });

        panelCondition.getTableListCondition().getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                int idx = panelCondition.getTableListCondition().getSelectedRow();

                if (idx < 0) {
                    return;
                }

                double duration = (double) panelCondition.getTableListCondition().getValueAt(idx, 2)
                        - (double) panelCondition.getTableListCondition().getValueAt(idx, 1);

                double t1 = (double) panelCondition.getTableListCondition().getValueAt(idx, 1) - (duration * 0.2);
                double t2 = (double) panelCondition.getTableListCondition().getValueAt(idx, 2) + (duration * 0.2);

                int idxWin = chartTabbedPane.getSelectedIndex();
                if (idxWin > -1) {
                    if (chartTabbedPane.getComponentAt(idxWin) instanceof ChartView) {
                        ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idxWin);

                        CombinedDomainXYPlot combinedDomainXYPlot = chartView.getPlot();

                        Range newRange = new Range(t1, t2);
                        combinedDomainXYPlot.getDomainAxis().setRange(newRange);

                    }
                }
            }
        });

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 15;
        gbc.weighty = 40;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(panelCondition, gbc);

        labelFnr = new JLabel("Fournisseur du log : ");
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(labelFnr, gbc);

        labelLogName = new JLabel("Nom de l'acquisition : ");
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(labelLogName, gbc);

        return panel;

    }

    private final JPopupMenu createTableMenu() {

        final String ICON_MINVALUE = "/icon_minValue_16.png";
        final String ICON_MAXVALUE = "/icon_maxValue_16.png";

        final JPopupMenu popUp = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Trouver la valeur min", new ImageIcon(getClass().getResource(ICON_MINVALUE)));
        menuItem.setActionCommand("MIN_VALUE");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        menuItem = new JMenuItem("Trouver la valeur max", new ImageIcon(getClass().getResource(ICON_MAXVALUE)));
        menuItem.setActionCommand("MAX_VALUE");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        return popUp;
    }

    private final class OpenLog implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = new JFileChooser(Preference.getPreference(Preference.KEY_LOG));
            fc.setMultiSelectionEnabled(false);
            fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
            fc.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {
                    return "Fichier log (*.txt, *.msl)";
                }

                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    return f.getName().toLowerCase().endsWith("txt") || f.getName().toLowerCase().endsWith("msl");
                }
            });
            final int reponse = fc.showOpenDialog(Ihm.this);

            if (reponse == JFileChooser.APPROVE_OPTION) {

                log = new Log(fc.getSelectedFile());

                if (listModel.getSize() > 0) {
                    listModel.clear();
                }

                for (Measure measure : log.getMeasures()) {
                    listModel.addElement(measure);
                }

                for (Measure formule : listFormula) {
                    log.getMeasures().add(formule);
                    // ((Formula) formule).setOutdated();
                    // ((Formula) formule).calculate(log, getSelectedCal());
                    // listModel.addElement(formule);
                }

                for (Measure formule : listFormula) {
                    // log.getMeasures().add(formule);
                    ((Formula) formule).setOutdated();
                    ((Formula) formule).calculate(log, getSelectedCal());
                    listModel.addElement(formule);
                }

                labelFnr.setText("<html>Fournisseur du log : " + "<b>" + log.getFnr());
                labelLogName.setText("<html>Nom de l'acquisition : " + "<b>" + log.getName());

                // load data in chart
                reloadLogData(log);

                mapView.setLog(log);

                mainTabbedPane.setSelectedIndex(0);
            }

        }
    }

    private final void addMapFile(MapCal mapCal) {

        if (mapCal.getMdbData().getInfos().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fichier '" + mapCal.getMdbData().getName() + ".mdb' non trouv\u00e9."
                    + "\nCertaines fonctionnalit\u00e9s seront impact\u00e9es.");
        }

        mapView.addCalToTree(mapCal);
        mainTabbedPane.setSelectedComponent(mapView);
    }

    private final CalTable addTableWindow() {
        String defaultName = "Fenetre_" + chartTabbedPane.getTabCount();
        String windowName = JOptionPane.showInputDialog(Ihm.this, "Nom de la fenetre :", defaultName);
        if (windowName == null) {
            return null;
        }
        if ("".equals(windowName)) {
            windowName = defaultName;
        }
        CalTable table = new CalTable(null, null);
        chartTabbedPane.addTab(windowName, table);
        chartTabbedPane.setTabComponentAt(chartTabbedPane.getTabCount() - 1, new ButtonTabComponent(chartTabbedPane));
        chartTabbedPane.setSelectedIndex(chartTabbedPane.getTabCount() - 1);

        return table;
    }

    private final ChartView addChartWindow(String nameWindow) {

        ChartView chartView = new ChartView();
        chartView.addObservateur(tableCursorValues);
        chartView.setTransferHandler(new MeasureHandler());
        if (nameWindow == null) {
            String defaultName = "Fenetre_" + chartTabbedPane.getTabCount();
            String windowName = JOptionPane.showInputDialog(Ihm.this, "Nom de la fenetre :", defaultName);
            if (windowName == null) {
                return null;
            }
            if ("".equals(windowName)) {
                windowName = defaultName;
            }
            chartTabbedPane.addTab(windowName, chartView);
        } else {
            chartTabbedPane.addTab(nameWindow, chartView);
        }

        chartTabbedPane.setTabComponentAt(chartTabbedPane.getTabCount() - 1, new ButtonTabComponent(chartTabbedPane));
        chartTabbedPane.setSelectedIndex(chartTabbedPane.getTabCount() - 1);

        if (axisSync && chartTabbedPane.getTabCount() > 1) {
            ChartView refChartView = (ChartView) chartTabbedPane.getComponentAt(chartTabbedPane.getTabCount() - 2);
            ValueAxis domainAxis = refChartView.getPlot().getDomainAxis();
            chartView.getPlot().setDomainAxis(domainAxis);
        }

        return chartView;
    }

    private final void closeWindows() {
        for (int i = chartTabbedPane.getTabCount() - 1; i >= 0; i--) {
            chartTabbedPane.removeTabAt(i);
        }
        tableCursorValues.getModel().clearList();
    }

    private final class MeasureHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport support) {
            boolean doImport = support.isDataFlavorSupported(DataFlavor.stringFlavor);

            if (doImport) {
                ChartView chartView = (ChartView) support.getComponent();
                if (chartView.getDatasetType() > 1) {
                    return false;
                }
                chartView.highlightPlot(support.getDropLocation());
            }

            return doImport;
        }

        @Override
        public boolean importData(TransferSupport support) {

            try {
                final String measureName = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                int idxMeasure = listModel.indexOf(new Measure(measureName));
                int idxWindow = chartTabbedPane.getSelectedIndex();
                if (idxMeasure < 0 || idxWindow < 0) {
                    return false;
                }

                final ChartView chartView = (ChartView) support.getComponent();
                final CombinedDomainXYPlot combinedDomainXYPlot = chartView.getPlot();
                final XYPlot plot = combinedDomainXYPlot.findSubplot(chartView.getChartRenderingInfo().getPlotInfo(),
                        support.getDropLocation().getDropPoint());

                DialAddMeasure dialAddMeasure = new DialAddMeasure(plot, measureName);

                int res = JOptionPane.showConfirmDialog(Ihm.this, dialAddMeasure, "Ajout mesure", 2, -1);

                if (res == JOptionPane.OK_OPTION) {
                    chartView.addMeasure(plot, log.getTime(), listModel.getElementAt(idxMeasure), dialAddMeasure.getAxisName());

                    chartView.getChart().addProgressListener(new ChartProgressListener() {

                        @Override
                        public void chartProgress(ChartProgressEvent var1) {
                            if (var1.getType() == ChartProgressEvent.DRAWING_FINISHED) {
                                tableCursorValues.getModel().addElement(measureName, chartView.getMeasureColor(plot, measureName));
                                chartView.getChart().removeProgressListener(this);
                            }

                        }
                    });

                    return true;
                }
            } catch (UnsupportedFlavorException e) {
            } catch (IOException e) {
            }

            return false;
        }

    }

    public final Log getLog() {
        return log;
    }

    public final MapView getMapView() {
        return mapView;
    }

    public final MapCal getSelectedCal() {
        return mapView != null ? mapView.getCalForFormula() : null;
    }

    public final void addMeasure(Measure newMeasure) {
        if (!listModel.contains(newMeasure)) {
            listModel.addElement(newMeasure);
            listFormula.add(newMeasure);
            if (log != null) {
                log.getMeasures().add(newMeasure);
            }
        }
    }

    public final void deleteMeasure(Measure measure) {
        listModel.removeElement(measure);
        listFormula.remove(measure);
        log.getMeasures().remove(measure);

        for (int i = 0; i < chartTabbedPane.getTabCount(); i++) {
            ((ChartView) chartTabbedPane.getComponentAt(i)).removeMeasure(measure.getName());
        }

    }

    private final void directToPlot() {
        final int idxWindow = chartTabbedPane.getSelectedIndex();
        final ChartView chartView;

        if (idxWindow < 0) {
            chartView = addChartWindow(null);
            if (chartView == null) {
                return;
            }
        } else {
            chartView = (ChartView) chartTabbedPane.getComponentAt(idxWindow);
        }

        @SuppressWarnings("unchecked")
        List<XYPlot> subPlots = chartView.getPlot().getSubplots();
        if (!subPlots.isEmpty()) {
            String domainAxisName = chartView.getPlot().getDomainAxis().getLabel();
            if (!log.getTime().getName().equals(domainAxisName)) {
                return;
            }
        }
        final XYPlot plot = chartView.addPlot(log.getTime(), listLogMeasure.getSelectedValue());

        chartView.getChart().addProgressListener(new ChartProgressListener() {

            @Override
            public void chartProgress(ChartProgressEvent var1) {
                if (var1.getType() == ChartProgressEvent.DRAWING_FINISHED) {
                    final Color colorMeasure = chartView.getMeasureColor(plot, listLogMeasure.getSelectedValue().getName());
                    tableCursorValues.getModel().addElement(listLogMeasure.getSelectedValue().getName(), colorMeasure);
                    chartView.getChart().removeProgressListener(this);
                }

            }
        });

    }

    public final void plotFromDialog(String xLabel, String yLabel, String zLabel) {
        if (log == null) {
            JOptionPane.showMessageDialog(this, "Il faut d'abord ouvrir un log!", "INFO", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final int idxWindow = chartTabbedPane.getSelectedIndex();
        ChartView chartView;

        if (idxWindow > -1) {
            chartView = (ChartView) chartTabbedPane.getComponentAt(idxWindow);
            if (chartView.getPlot().getSubplots().size() > 0) {
                chartView = addChartWindow(null);
            }
        } else {
            chartView = addChartWindow(null);
        }

        if (chartView != null) {
            Measure x = pickMeasureFromList(xLabel);
            Measure y = pickMeasureFromList(yLabel);
            Measure z = pickMeasureFromList(zLabel);

            if (z.getData().isEmpty()) {
                chartView.add2DScatterPlot(x, y);
            } else {
                chartView.add3DScatterPlot(x, y, z);
            }
        }

        mainTabbedPane.setSelectedIndex(0);
    }

    public final Set<Measure> getListFormula() {
        return listFormula;
    }

    private final Measure pickMeasureFromList(String name) {
        final Measure measure = new Measure(name);
        final int idx = listModel.indexOf(measure);

        return idx > -1 ? listModel.getElementAt(idx) : measure;
    }

    public final void refresh() {

        if (log != null) {
            for (Measure form : getListFormula()) {
                if (!((Formula) form).isUpToDate() || ((Formula) form).isMapCalBased()) {
                    ((Formula) form).calculate(log, getSelectedCal());
                    reloadFormulaData(log, form.getName());
                }
            }
        }
    }

    private final void reloadLogData(Log log) {

        final int nbTab = chartTabbedPane.getTabCount();
        ChartView chartView;
        XYPlot xyPlot;
        XYSeries serie;
        Comparable<?> key;
        Measure measure = null;

        if (log == null) {
            return;
        }

        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

        final List<Number> temps = log.getTime().getData();
        final int nbPoint = temps.size();

        for (int n = 0; n < nbTab; n++) {
            if (chartTabbedPane.getComponentAt(n) instanceof ChartView) {
                chartView = (ChartView) chartTabbedPane.getComponentAt(n);
                for (Object plot : chartView.getPlot().getSubplots()) {
                    xyPlot = (XYPlot) plot;

                    for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {

                        int nbSerie = xyPlot.getDataset(nDataset).getSeriesCount();

                        for (int nSerie = 0; nSerie < nbSerie; nSerie++) {

                            if (xyPlot.getDataset() instanceof XYSeriesCollection) {

                                serie = ((XYSeriesCollection) xyPlot.getDataset(nDataset)).getSeries(nSerie);

                                serie.clear();

                                key = serie.getKey();

                                measure = pickMeasureFromList(key.toString());

                                final int sizeData = measure.getData().size();

                                for (int n1 = 0; n1 < nbPoint; n1++) {

                                    if (n1 < sizeData) {
                                        serie.add(temps.get(n1), measure.getData().get(n1), false);
                                    }
                                }

                                serie.fireSeriesChanged();

                            } else if (xyPlot.getDataset() instanceof DefaultXYZDataset) {
                                Comparable<?> serieKey = ((DefaultXYZDataset) xyPlot.getDataset()).getSeriesKey(nSerie);

                                String xLabel = xyPlot.getDomainAxis().getLabel();
                                String yLabel = xyPlot.getRangeAxis().getLabel();
                                String zLabel = ((PaintScaleLegend) chartView.getChart().getSubtitle(0)).getAxis().getLabel();

                                Measure xMeasure = pickMeasureFromList(xLabel);
                                Measure yMeasure = pickMeasureFromList(yLabel);
                                Measure zMeasure = pickMeasureFromList(zLabel);

                                ((DefaultXYZDataset) xyPlot.getDataset()).addSeries(serieKey,
                                        new double[][] { xMeasure.getDoubleValue(), yMeasure.getDoubleValue(), zMeasure.getDoubleValue() });

                            } else {
                                Comparable<?> serieKey = ((DefaultXYDataset) xyPlot.getDataset()).getSeriesKey(nSerie);

                                String xLabel = xyPlot.getDomainAxis().getLabel();
                                String yLabel = xyPlot.getRangeAxis().getLabel();

                                Measure xMeasure = pickMeasureFromList(xLabel);
                                Measure yMeasure = pickMeasureFromList(yLabel);

                                ((DefaultXYDataset) xyPlot.getDataset()).addSeries(serieKey,
                                        new double[][] { xMeasure.getDoubleValue(), yMeasure.getDoubleValue() });

                            }

                        }
                    }

                }

                chartView.getPlot().getDomainAxis().setAutoRange(true);
                chartView.getPlot().configureDomainAxes();
            }
        }

        this.setCursor(Cursor.getDefaultCursor());
    }

    private final void reloadFormulaData(Log log, String formulaName) {

        final int nbTab = chartTabbedPane.getTabCount();
        ChartView chartView;
        XYPlot xyPlot;
        XYSeries serie;
        Comparable<?> key;
        Measure measure = null;

        if (log == null) {
            return;
        }

        final List<Number> temps = log.getTime().getData();
        final int nbPoint = temps.size();

        for (int n = 0; n < nbTab; n++) {
            chartView = (ChartView) chartTabbedPane.getComponentAt(n);

            xyPlot = chartView.getPlot(formulaName);

            if (xyPlot != null) {
                for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {

                    if (xyPlot.getDataset() instanceof XYSeriesCollection) {

                        int idxSerie = xyPlot.getDataset(nDataset).indexOf(formulaName);

                        if (idxSerie < 0) {
                            return;
                        }

                        serie = ((XYSeriesCollection) xyPlot.getDataset(nDataset)).getSeries(idxSerie);

                        serie.clear();

                        key = serie.getKey();

                        measure = pickMeasureFromList(key.toString());

                        final int sizeData = measure.getData().size();

                        for (int n1 = 0; n1 < nbPoint; n1++) {

                            if (n1 < sizeData) {
                                serie.add(temps.get(n1), measure.getData().get(n1), false);
                            }
                        }

                        serie.fireSeriesChanged();

                    } else if (xyPlot.getDataset() instanceof DefaultXYZDataset) {
                        Comparable<?> serieKey = ((DefaultXYZDataset) xyPlot.getDataset()).getSeriesKey(0);

                        String xLabel = xyPlot.getDomainAxis().getLabel();
                        String yLabel = xyPlot.getRangeAxis().getLabel();
                        String zLabel = ((PaintScaleLegend) chartView.getChart().getSubtitle(0)).getAxis().getLabel();

                        Measure xMeasure = pickMeasureFromList(xLabel);
                        Measure yMeasure = pickMeasureFromList(yLabel);
                        Measure zMeasure = pickMeasureFromList(zLabel);

                        ((DefaultXYZDataset) xyPlot.getDataset()).addSeries(serieKey,
                                new double[][] { xMeasure.getDoubleValue(), yMeasure.getDoubleValue(), zMeasure.getDoubleValue() });

                    } else {
                        Comparable<?> serieKey = ((DefaultXYDataset) xyPlot.getDataset()).getSeriesKey(0);

                        String xLabel = xyPlot.getDomainAxis().getLabel();
                        String yLabel = xyPlot.getRangeAxis().getLabel();

                        Measure xMeasure = pickMeasureFromList(xLabel);
                        Measure yMeasure = pickMeasureFromList(yLabel);

                        ((DefaultXYDataset) xyPlot.getDataset()).addSeries(serieKey,
                                new double[][] { xMeasure.getDoubleValue(), yMeasure.getDoubleValue() });

                    }
                }
            }
        }

    }

    private final void saveConfig(File file) {

        int nbTab = chartTabbedPane.getTabCount();
        Map<String, JFreeChart> listChart = new LinkedHashMap<String, JFreeChart>();

        for (int i = 0; i < nbTab; i++) {
            if (chartTabbedPane.getComponentAt(i) instanceof ChartView) {
                JFreeChart chart = ((ChartView) chartTabbedPane.getComponentAt(i)).getChart();
                listChart.put(chartTabbedPane.getTitleAt(i), chart);
            }
        }

        List<Condition> conditions = this.panelCondition.getTableCondition().getModel().getConditions();

        boolean result = ExportUtils.ConfigToXml(file, listChart, listFormula, conditions);

        if (result) {
            JOptionPane.showMessageDialog(this, "Configuration sauvegard\u00e9e !");
        } else {
            JOptionPane.showMessageDialog(this, "Erreur pendant la sauvegarde...");
        }
    }

    private final void openConfig(File file) {

        if (file.getName().endsWith("cfg") || file.getName().endsWith("tmp")) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                @SuppressWarnings("unchecked")
                Map<String, JFreeChart> charts = (LinkedHashMap<String, JFreeChart>) ois.readObject();

                for (Entry<String, JFreeChart> entry : charts.entrySet()) {
                    ChartView chartView = new ChartView(entry.getValue());
                    chartView.addObservateur(tableCursorValues);
                    chartView.setTransferHandler(new MeasureHandler());
                    chartTabbedPane.addTab(entry.getKey(), chartView);
                    chartTabbedPane.setTabComponentAt(chartTabbedPane.getTabCount() - 1, new ButtonTabComponent(chartTabbedPane));
                    chartTabbedPane.setSelectedIndex(chartTabbedPane.getTabCount() - 1);
                }

                @SuppressWarnings("unchecked")
                Set<Measure> list = (Set<Measure>) ois.readObject();

                if (log != null) {
                    for (Measure formule : list) {
                        log.getMeasures().add(formule);
                        int idx = listModel.indexOf(formule);
                        if (idx < 0) {
                            listModel.addElement(formule);
                        }
                    }
                }

                for (Measure formule : list) {
                    ((Formula) formule).deserialize();
                    ((Formula) formule).setOutdated();
                    ((Formula) formule).calculate(log, getSelectedCal());
                    listFormula.add(formule);
                }

                @SuppressWarnings("unchecked")
                List<Condition> conditions = (List<Condition>) ois.readObject();
                panelCondition.getTableCondition().getModel().setConditions(conditions);

            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            } catch (ClassNotFoundException e) {
            } finally {
                reloadLogData(log);
            }
        } else {

            DocumentBuilder builder;
            Document document = null;
            DocumentBuilderFactory factory;
            factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setValidating(false);

            try {
                builder = factory.newDocumentBuilder();
                document = builder.parse(new File(file.toURI()));

                final Element racine = document.getDocumentElement();

                if (racine.getNodeName().equals("Configuration")) {

                    NodeList listWindow = racine.getElementsByTagName("Window");
                    int nbWindow = listWindow.getLength();

                    for (int i = 0; i < nbWindow; i++) {
                        Element window = (Element) listWindow.item(i);
                        String nameWindow = window.getElementsByTagName("Name").item(0).getTextContent();
                        int typeWindow = Integer.parseInt(window.getElementsByTagName("Type").item(0).getTextContent());
                        ChartView chartView = addChartWindow(nameWindow);

                        NodeList listPlot = window.getElementsByTagName("Plot");
                        int nbPlot = listPlot.getLength();

                        for (int j = 0; j < nbPlot; j++) {

                            Element plot = (Element) listPlot.item(j);

                            String bckGrndColor = plot.getElementsByTagName("Background").item(0).getTextContent();

                            switch (typeWindow) {
                            case 1:

                                String timeBase = plot.getElementsByTagName("TimeBase").item(0).getTextContent();

                                chartView.addPlot(new Measure(timeBase), bckGrndColor);

                                NodeList listAxis = plot.getElementsByTagName("Axis");

                                for (int k = 0; k < listAxis.getLength(); k++) {
                                    Element axisNode = (Element) listAxis.item(k);
                                    String nameAxis = axisNode.getElementsByTagName("Name").item(0).getTextContent();

                                    String rangeText = axisNode.getElementsByTagName("Range").item(0).getTextContent();

                                    if (rangeText.indexOf(',') > -1) {
                                        rangeText = rangeText.replace(',', '.');
                                    }

                                    String[] splitRange = rangeText.split(";");

                                    NodeList listSeries = axisNode.getElementsByTagName("Serie");

                                    for (int l = 0; l < listSeries.getLength(); l++) {

                                        Element serieNode = (Element) listSeries.item(l);
                                        String nameSerie = serieNode.getElementsByTagName("Name").item(0).getTextContent();
                                        String colorSerie = serieNode.getElementsByTagName("Color").item(0).getTextContent();
                                        String widthSerie = serieNode.getElementsByTagName("Width").item(0).getTextContent();
                                        chartView.addMeasure((XYPlot) chartView.getPlot().getSubplots().get(j), new Measure("Time_ms"),
                                                new Measure(nameSerie), colorSerie, widthSerie, nameAxis);
                                    }

                                    if (splitRange.length == 2) {
                                        try {
                                            ValueAxis axis = ((XYPlot) chartView.getPlot().getSubplots().get(j)).getRangeAxis(k);
                                            double lowerBound = Double.parseDouble(splitRange[0]);
                                            double upperBound = Double.parseDouble(splitRange[1]);
                                            Range newRange = new Range(lowerBound, upperBound);
                                            axis.setRange(newRange);

                                        } catch (NumberFormatException e) {
                                        }
                                    }
                                }

                                break;
                            case 2:

                                Element serie2D = (Element) plot.getElementsByTagName("Serie").item(0);

                                String x = serie2D.getElementsByTagName("X").item(0).getTextContent();
                                String y = serie2D.getElementsByTagName("Y").item(0).getTextContent();
                                String colorSerie = serie2D.getElementsByTagName("Color").item(0).getTextContent();
                                String shapeSize2D = serie2D.getElementsByTagName("Shape_size").item(0).getTextContent();

                                chartView.add2DScatterPlot(new Measure(x), new Measure(y), bckGrndColor, shapeSize2D, colorSerie);

                                break;
                            case 3:

                                Element serie3D = (Element) plot.getElementsByTagName("Serie").item(0);

                                String x3D = serie3D.getElementsByTagName("X").item(0).getTextContent();
                                String y3D = serie3D.getElementsByTagName("Y").item(0).getTextContent();
                                String z3D = serie3D.getElementsByTagName("Z").item(0).getTextContent();

                                String shapeSize3D = serie3D.getElementsByTagName("Shape_size").item(0).getTextContent();
                                String zRange = plot.getElementsByTagName("Z_Range").item(0).getTextContent();

                                chartView.add3DScatterPlot(new Measure(x3D), new Measure(y3D), new Measure(z3D), bckGrndColor, shapeSize3D, zRange);

                                break;
                            }
                        }

                    }

                    NodeList listFormulas = racine.getElementsByTagName("Formula");
                    int nbFormula = listFormulas.getLength();

                    for (int i = 0; i < nbFormula; i++) {
                        Element formula = (Element) listFormulas.item(i);
                        String nameFormula = formula.getElementsByTagName("Name").item(0).getTextContent();
                        String unitFormula = formula.getElementsByTagName("Unit").item(0).getTextContent();
                        String expressionFormula = formula.getElementsByTagName("Expression").item(0).getTextContent();

                        listFormula.add(new Formula(nameFormula, unitFormula, expressionFormula, log, getSelectedCal()));
                    }

                    if (log != null) {
                        for (Measure formule : listFormula) {
                            log.getMeasures().add(formule);
                            int idx = listModel.indexOf(formule);
                            if (idx < 0) {
                                listModel.addElement(formule);
                            }
                        }
                    }

                    NodeList listConditons = racine.getElementsByTagName("Condition");
                    int nbCondition = listConditons.getLength();

                    List<Condition> conditions = new ArrayList<>(nbCondition);

                    for (int i = 0; i < nbCondition; i++) {
                        Element condition = (Element) listConditons.item(i);
                        String nameCondition = condition.getElementsByTagName("Name").item(0).getTextContent();
                        String expressionCondition = condition.getElementsByTagName("Expression").item(0).getTextContent();
                        String colorCondition = condition.getElementsByTagName("Color").item(0).getTextContent();

                        conditions.add(new Condition(nameCondition, expressionCondition, Utilitaire.parseRGBColor(colorCondition, 70)));
                    }

                    panelCondition.getTableCondition().getModel().setConditions(conditions);

                    int idx = chartTabbedPane.getSelectedIndex();
                    if (idx > -1) {
                        if (chartTabbedPane.getComponentAt(idx) instanceof ChartView) {
                            ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idx);
                            tableCursorValues.getModel().changeList(chartView.getMeasuresColors());
                        }
                    }

                    document = null;

                } else {

                    return;
                }

            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            } finally {
                reloadLogData(log);
            }

        }

    }

    public static void main(String[] args) {

        // File[] fileToConvert = new File[] { new File("c:\\user\\U354706\\Perso\\Clio\\soft\\applicatif_04102021.inc"),
        // new File("c:\\user\\U354706\\Perso\\Clio\\soft\\sfr167.inc") };

        // Conversion.AppIncToA2l(fileToConvert);

        try {

            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (InstantiationException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (UnsupportedLookAndFeelException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Ihm();
            }
        });

    }

    @Override
    public void MapCalChanged(MapCalEvent arg) {
        refresh();
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        int row = tableCursorValues.getSelectedRow();

        if (row == -1) {
            return;
        }

        String signalName = tableCursorValues.getValueAt(row, 1).toString();

        ChartView chartView = (ChartView) chartTabbedPane.getSelectedComponent();

        XYPlot plot = chartView.getPlot(signalName);

        if (plot == null) {
            return;
        }

        XYSeries serie = null;

        for (int nDataset = 0; nDataset < plot.getDatasetCount(); nDataset++) {
            serie = ((XYSeriesCollection) plot.getDataset(nDataset)).getSeries(signalName);
            if (serie != null) {
                break;
            }
        }

        Measure time = log.getTime();

        switch (command) {
        case "MIN_VALUE":

            double min = serie.getMinY();
            for (int i = 0; i < serie.getItemCount(); i++) {
                if (serie.getY(i).doubleValue() == min) {
                    chartView.moveMarker(time.getData().get(i).doubleValue());
                    return;
                }
            }
            break;
        case "MAX_VALUE":

            double max = serie.getMaxY();
            for (int i = 0; i < serie.getItemCount(); i++) {
                if (serie.getY(i).doubleValue() == max) {
                    chartView.moveMarker(time.getData().get(i).doubleValue());
                    return;
                }
            }
            break;
        }
    }

}
