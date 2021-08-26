package gui;

import java.awt.Container;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
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
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
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
import dialog.DialNewChart;
import dialog.DialNewFormula;
import dialog.DialNews;
import dialog.DialNotice;
import log.Formula;
import log.Log;
import log.Measure;
import utils.ExportUtils;
import utils.Preference;
import utils.Utilitaire;

/*
 * Creation : 3 mai 2018
 */

public final class Ihm extends JFrame {

    private static final long serialVersionUID = 1L;

    private final String DEMO = "demo";

    private JTabbedPane tabbedPane;
    private DefaultListModel<Measure> listModel;
    private JList<Measure> listVoie;
    private JScrollPane scrollListVoie;
    private JScrollPane scrollTableCursorValues;
    private TableCursorValue tableCursorValues;
    private PanelCondition panelCondition;

    private JLabel labelFnr;
    private JLabel labelLogName;

    private Log log;
    private Set<Measure> listFormula = new HashSet<Measure>();
    private boolean axisSync = false;

    private final Map<Integer, List<IntervalMarker>> listZone = new HashMap<Integer, List<IntervalMarker>>();

    public Ihm() {
        super("DataLogViewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setJMenuBar(createMenu());

        createGUI();

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
                            return;
                        } catch (IOException e1) {
                        }
                    }

                    openConfig(config);
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
        menuItem = new JMenuItem(new AbstractAction("Chemin d'import") {

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

        menuItem = new JMenuItem("Notice", new ImageIcon(getClass().getResource(ICON_NOTICE)));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new DialNotice(Ihm.this);
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
                            return;
                        } catch (IOException e) {
                        }
                    }

                    openConfig(config);
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
                    addMapWindow(new MapCal(fc.getSelectedFile()));
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

                final int idxWindow = tabbedPane.getSelectedIndex();

                if (idxWindow < 0 || log == null) {
                    btSynchro.setSelected(false);
                    return;
                }

                if (axisSync) {

                    ChartView chartView = (ChartView) tabbedPane.getComponentAt(idxWindow);
                    if (chartView.getDatasetType() > 1) {
                        btSynchro.setSelected(false);
                        JOptionPane.showMessageDialog(null, "Fonctionne pour les graphiques=f(temps).", "INFO", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    ValueAxis domainAxis = chartView.getPlot().getDomainAxis();
                    ChartView otherChart;
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        if (!(tabbedPane.getComponentAt(i) instanceof ChartView)) {
                            continue;
                        }
                        otherChart = (ChartView) tabbedPane.getComponentAt(i);
                        if (i == idxWindow || otherChart.getDatasetType() > 1) {
                            continue;
                        }
                        otherChart.getPlot().setDomainAxis(domainAxis);
                    }
                } else {
                    ChartView chartView;
                    Measure time = log.getTime();
                    Range xAxisRange;
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        if (!(tabbedPane.getComponentAt(i) instanceof ChartView)) {
                            continue;
                        }
                        chartView = (ChartView) tabbedPane.getComponentAt(i);
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

    private final void createGUI() {

        final GridBagConstraints gbc = new GridBagConstraints();

        listModel = new DefaultListModel<Measure>();

        final Container root = getContentPane();

        root.setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(createToolBar(), gbc);

        listVoie = new JList<Measure>();
        listVoie.setCellRenderer(new ListLabelRenderer());
        listVoie.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    directToPlot();
                }
            }
        });
        listVoie.setModel(listModel);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 15;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        listVoie.setDragEnabled(true);
        scrollListVoie = new JScrollPane(listVoie);
        root.add(scrollListVoie, gbc);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEtchedBorder());
        tabbedPane.setPreferredSize(new Dimension(800, 600));
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 70;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(tabbedPane, gbc);

        tabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int idx = tabbedPane.getSelectedIndex();
                if (idx > -1) {
                    if (tabbedPane.getComponentAt(idx) instanceof ChartView) {
                        scrollListVoie.setVisible(true);
                        scrollTableCursorValues.setVisible(true);
                        panelCondition.setVisible(true);
                        ChartView chartView = (ChartView) tabbedPane.getComponentAt(idx);
                        ((DataValueModel) tableCursorValues.getModel()).changeList(chartView.getMeasures());
                        chartView.updateTableValue();
                        if (chartView.getDatasetType() < 2) {
                            chartView.applyCondition(listZone.get(panelCondition.getTableCondition().getActiveCondition()));
                        }
                    } else if (tabbedPane.getComponentAt(idx) instanceof MapView) {
                        scrollListVoie.setVisible(false);
                        scrollTableCursorValues.setVisible(false);
                        panelCondition.setVisible(false);
                    }
                } else {
                    scrollListVoie.setVisible(true);
                    scrollTableCursorValues.setVisible(true);
                    panelCondition.setVisible(true);
                    ((DataValueModel) tableCursorValues.getModel()).changeList(Collections.<String> emptySet());
                }
            }
        });

        tableCursorValues = new TableCursorValue();
        tableCursorValues.setPreferredScrollableViewportSize(tableCursorValues.getPreferredSize());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 15;
        gbc.weighty = 60;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        scrollTableCursorValues = new JScrollPane(tableCursorValues);
        root.add(scrollTableCursorValues, gbc);

        panelCondition = new PanelCondition();
        panelCondition.getTableCondition().getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {

                    final int row = e.getFirstRow();
                    final Condition condition = (Condition) panelCondition.getTableCondition().getModel().getValueAt(row, 1);

                    int idx = tabbedPane.getSelectedIndex();
                    if (idx > -1) {
                        if (tabbedPane.getComponentAt(idx) instanceof ChartView) {
                            final ChartView chartView = (ChartView) tabbedPane.getComponentAt(idx);

                            if (chartView.getDatasetType() < 2) {

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

                int idxWin = tabbedPane.getSelectedIndex();
                if (idxWin > -1) {
                    if (tabbedPane.getComponentAt(idxWin) instanceof ChartView) {
                        ChartView chartView = (ChartView) tabbedPane.getComponentAt(idxWin);

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
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(panelCondition, gbc);

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
        root.add(labelFnr, gbc);

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
        root.add(labelLogName, gbc);

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

                if (!listModel.isEmpty()) {
                    listModel.clear();
                }

                for (Measure measure : log.getMeasures()) {
                    listModel.addElement(measure);
                }

                for (Measure formule : listFormula) {
                    log.getMeasures().add(formule);
                    ((Formula) formule).calculate(log);
                    listModel.addElement(formule);
                }

                labelFnr.setText("<html>Fournisseur du log : " + "<b>" + log.getFnr());
                labelLogName.setText("<html>Nom de l'acquisition : " + "<b>" + log.getName());

                // load data in chart
                reloadLogData(log);
            }

        }
    }

    private int checkPresence(Class<?> aClass) {
        int nbTab = tabbedPane.getTabCount();

        for (int i = 0; i < nbTab; i++) {
            Class<?> theClass = tabbedPane.getComponentAt(i).getClass();
            if (theClass.equals(aClass)) {
                return i;
            }
        }
        return -1;
    }

    private final void addMapWindow(MapCal mapCal) {

        if (mapCal.getMdbData().getInfos().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fichier '" + mapCal.getMdbData().getName() + ".mdb' non trouv\u00e9."
                    + "\nCertaines fonctionnalit\u00e9s seront impact\u00e9es.");
        }

        MapView mapView;

        int idxMapView = checkPresence(MapView.class);

        if (idxMapView == -1) {
            mapView = new MapView(mapCal);
            tabbedPane.addTab("Calibration", mapView);
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ButtonTabComponent(tabbedPane));
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        } else {
            mapView = (MapView) tabbedPane.getComponentAt(idxMapView);
            mapView.addCalToTree(mapCal);
        }

    }

    private final CalTable addTableWindow() {
        String defaultName = "Fenetre_" + tabbedPane.getTabCount();
        String windowName = JOptionPane.showInputDialog(Ihm.this, "Nom de la fenetre :", defaultName);
        if (windowName == null) {
            return null;
        }
        if ("".equals(windowName)) {
            windowName = defaultName;
        }
        CalTable table = new CalTable(null);
        tabbedPane.addTab(windowName, table);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ButtonTabComponent(tabbedPane));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

        return table;
    }

    private final ChartView addChartWindow(String nameWindow) {

        ChartView chartView = new ChartView();
        chartView.addObservateur(tableCursorValues);
        chartView.setTransferHandler(new MeasureHandler());
        if (nameWindow == null) {
            String defaultName = "Fenetre_" + tabbedPane.getTabCount();
            String windowName = JOptionPane.showInputDialog(Ihm.this, "Nom de la fenetre :", defaultName);
            if (windowName == null) {
                return null;
            }
            if ("".equals(windowName)) {
                windowName = defaultName;
            }
            tabbedPane.addTab(windowName, chartView);
        } else {
            tabbedPane.addTab(nameWindow, chartView);
        }

        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ButtonTabComponent(tabbedPane));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

        if (axisSync && tabbedPane.getTabCount() > 1) {
            ChartView refChartView = (ChartView) tabbedPane.getComponentAt(tabbedPane.getTabCount() - 2);
            ValueAxis domainAxis = refChartView.getPlot().getDomainAxis();
            chartView.getPlot().setDomainAxis(domainAxis);
        }

        return chartView;
    }

    private final void closeWindows() {
        for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
            tabbedPane.removeTabAt(i);
        }
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
                String measureName = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                int idxMeasure = listModel.indexOf(new Measure(measureName));
                int idxWindow = tabbedPane.getSelectedIndex();
                if (idxMeasure < 0 || idxWindow < 0) {
                    return false;
                }

                ChartView chartView = (ChartView) support.getComponent();
                final CombinedDomainXYPlot combinedDomainXYPlot = chartView.getPlot();
                final XYPlot plot = combinedDomainXYPlot.findSubplot(chartView.getChartRenderingInfo().getPlotInfo(),
                        support.getDropLocation().getDropPoint());

                DialAddMeasure dialAddMeasure = new DialAddMeasure(plot, measureName);

                int res = JOptionPane.showConfirmDialog(Ihm.this, dialAddMeasure, "Ajout mesure", 2, -1);

                if (res == JOptionPane.OK_OPTION) {
                    chartView.addMeasure(plot, log.getTime(), listModel.get(idxMeasure), dialAddMeasure.getAxisName());
                    ((DataValueModel) tableCursorValues.getModel()).addElement(measureName);
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

    public final void addMeasure(Measure newMeasure) {
        if (!listModel.contains(newMeasure)) {
            listModel.addElement(newMeasure);
            listFormula.add(newMeasure);
            if (log != null) {
                log.getMeasures().add(newMeasure);
            }
        }
    }

    public final void deleteMeasure(Measure newMeasure) {
        listModel.removeElement(newMeasure);
        listFormula.remove(newMeasure);
        log.getMeasures().remove(newMeasure);
    }

    private final void directToPlot() {
        final int idxWindow = tabbedPane.getSelectedIndex();

        if (idxWindow < 0) {
            return;
        }

        ChartView chartView = (ChartView) tabbedPane.getComponentAt(idxWindow);
        @SuppressWarnings("unchecked")
        List<XYPlot> subPlots = chartView.getPlot().getSubplots();
        if (!subPlots.isEmpty()) {
            String domainAxisName = chartView.getPlot().getDomainAxis().getLabel();
            if (!log.getTime().getName().equals(domainAxisName)) {
                return;
            }
        }
        chartView.addPlot(log.getTime(), listVoie.getSelectedValue());
        ((DataValueModel) tableCursorValues.getModel()).addElement(listVoie.getSelectedValue().getName());
    }

    public final void plotFromDialog(String xLabel, String yLabel, String zLabel) {
        if (log == null) {
            JOptionPane.showMessageDialog(this, "Il faut d'abord ouvrir un log!", "INFO", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final int idxWindow = tabbedPane.getSelectedIndex();
        ChartView chartView;

        if (idxWindow > -1) {
            chartView = (ChartView) tabbedPane.getComponentAt(idxWindow);
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
    }

    public final Set<Measure> getListFormula() {
        return listFormula;
    }

    private final Measure pickMeasureFromList(String name) {
        final Measure measure = new Measure(name);
        final int idx = listModel.indexOf(measure);

        return idx > -1 ? listModel.get(idx) : measure;
    }

    public final void refresh() {
        if (log != null) {
            for (Measure form : getListFormula()) {
                ((Formula) form).calculate(log);
            }

            reloadLogData(log);
        }
    }

    private final void reloadLogData(Log log) {

        final int nbTab = tabbedPane.getTabCount();
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
            if (tabbedPane.getComponentAt(n) instanceof ChartView) {
                chartView = (ChartView) tabbedPane.getComponentAt(n);
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
                                // xyPlot.configureRangeAxes();
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
                // chartView.getChart().fireChartChanged();
                chartView.getPlot().getDomainAxis().setAutoRange(true);
                chartView.getPlot().configureDomainAxes();
            }
        }

    }

    private final void saveConfig(File file) {

        int nbTab = tabbedPane.getTabCount();
        Map<String, JFreeChart> listChart = new LinkedHashMap<String, JFreeChart>();

        for (int i = 0; i < nbTab; i++) {
            if (tabbedPane.getComponentAt(i) instanceof ChartView) {
                JFreeChart chart = ((ChartView) tabbedPane.getComponentAt(i)).getChart();
                listChart.put(tabbedPane.getTitleAt(i), chart);
            }
        }

        List<Condition> conditions = this.panelCondition.getTableCondition().getModel().getConditions();

        ExportUtils.ConfigToXml(file, listChart, listFormula, conditions);
    }

    private final void openConfig(File file) {

        if (file.getName().endsWith("cfg")) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                @SuppressWarnings("unchecked")
                Map<String, JFreeChart> charts = (LinkedHashMap<String, JFreeChart>) ois.readObject();

                for (Entry<String, JFreeChart> entry : charts.entrySet()) {
                    ChartView chartView = new ChartView(entry.getValue());
                    chartView.addObservateur(tableCursorValues);
                    chartView.setTransferHandler(new MeasureHandler());
                    tabbedPane.addTab(entry.getKey(), chartView);
                    tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ButtonTabComponent(tabbedPane));
                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
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
                    ((Formula) formule).calculate(log);
                    listFormula.add(formule);
                }

                @SuppressWarnings("unchecked")
                List<Condition> conditions = (List<Condition>) ois.readObject();
                panelCondition.getTableCondition().getModel().setConditions(conditions);

            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                System.out.println(e);
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

                                chartView.addPlot(new Measure("Time_ms"), bckGrndColor);

                                NodeList listAxis = plot.getElementsByTagName("Axis");

                                for (int k = 0; k < listAxis.getLength(); k++) {
                                    Element axisNode = (Element) listAxis.item(k);
                                    String nameAxis = axisNode.getElementsByTagName("Name").item(0).getTextContent();

                                    String rangeText = axisNode.getElementsByTagName("Range").item(0).getTextContent();

                                    String[] splitRange = rangeText.replace(',', '.').split(";");

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
                                            ValueAxis axis = ((XYPlot) chartView.getPlot().getSubplots().get(j)).getRangeAxis(0);
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

                        listFormula.add(new Formula(nameFormula, unitFormula, expressionFormula, log));
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

        // Conversion.AppIncToA2l(new File("c:\\user\\U354706\\Perso\\Clio\\soft\\applicatif_26072021_Cor.inc"));

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

}
