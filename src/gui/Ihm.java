package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PlotEntity;
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
import calib.MapCal.ComparResult;
import calib.Variable;
import dialog.DialAddMeasure;
import dialog.DialDiscretisation;
import dialog.DialManageFormula;
import dialog.DialManual;
import dialog.DialMoveWindow;
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

    private final static String VERSION = "v1.64";
    private static String APPLICATION_TITLE = "DataLogViewer " + VERSION;

    private final static String LOG_PANEL = "LOG";
    private final static String MAP_PANEL = "MAP";

    private final String ICON_MAP_TAB = "/icon_mapFile_24.png";
    private final String ICON_LOG_TAB = "/icon_log_24.png";
    private final String ICON_EDIT = "/icon_edit_16.png";
    private final String ICON_DELETE = "/icon_removePlot_16.png";

    private JTabbedPane mainTabbedPane;
    private JTabbedPane chartTabbedPane;
    private FilteredListModel listModel;
    private FilteredListMeasure listLogMeasure;
    private JScrollPane scrollTableCursorValues;
    private TableCursorValue tableCursorValues;
    private PanelCondition panelCondition;
    private JToggleButton btSynchro;
    private JToggleButton btAncre;

    // Test
    private JSplitPane splitLogMap;
    private MyLogDisplay logDisplay;
    public static Logger logger;
    //

    private int selectedIndexTab = -1;

    private Log log;
    private Set<Formula> listFormula = new HashSet<Formula>();

    boolean activeThread = true;

    private MapView mapView;

    private final Map<Integer, List<IntervalMarker>> listZone = new HashMap<Integer, List<IntervalMarker>>();

    public Ihm() {
        super(APPLICATION_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setJMenuBar(createMenu());

        Container root = getContentPane();
        root.setLayout(new BorderLayout());
        root.add(createToolBar(), BorderLayout.NORTH);

        mapView = new MapView();
        mapView.addMapCalListener(this);

        if ("Onglet".equals(Preference.getPreference(Preference.KEY_DISPO))) {
            mainTabbedPane = new JTabbedPane();

            mainTabbedPane.addTab(LOG_PANEL, new ImageIcon(getClass().getResource(ICON_LOG_TAB)), createLogPanel());
            mainTabbedPane.addTab(MAP_PANEL, new ImageIcon(getClass().getResource(ICON_MAP_TAB)), mapView);

            mainTabbedPane.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    if (mainTabbedPane.getSelectedIndex() == 0) {
                        if (mapView.getCalForFormula() != null) {
                            refresh(null);
                        }
                    }
                }
            });

            root.add(mainTabbedPane, BorderLayout.CENTER);
        } else {
            splitLogMap = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createLogPanel(), mapView);
            splitLogMap.setOneTouchExpandable(true);
            root.add(splitLogMap, BorderLayout.CENTER);
        }

        logDisplay = new MyLogDisplay();
        JScrollPane pane = new JScrollPane(logDisplay);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        root.add(pane, BorderLayout.SOUTH);

        logger = Logger.getLogger(getClass().getName());
        logger.addHandler(logDisplay.getHandler());

        pack();
        setMinimumSize(new Dimension(getWidth(), getHeight()));

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);

        logger.log(Level.INFO, "Application démarrée");
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
        final String ICON_COMPAR = "/icon_compar_16.png";
        final String ICON_MOVE_WINDOW = "/icon_moveWindow_16.png";

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
                        return "Fichier de configuration graphique (*.xml)";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        return f.getName().toLowerCase().endsWith("xml");
                    }
                });
                final int reponse = fc.showOpenDialog(Ihm.this);
                if (reponse == JFileChooser.APPROVE_OPTION) {

                    final File config = fc.getSelectedFile();
                    openConfig(config);

                    if (mainTabbedPane != null) {
                        mainTabbedPane.setSelectedIndex(0);
                    }
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
                if (mainTabbedPane != null) {
                    mainTabbedPane.setSelectedIndex(0);
                }
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

        menuItem = new JMenuItem("Organiser", new ImageIcon(getClass().getResource(ICON_MOVE_WINDOW)));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (chartTabbedPane.getTabCount() > 1) {
                    new DialMoveWindow(Ihm.this, chartTabbedPane);
                }
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

        menu = new JMenu("Divers");
        menuBar.add(menu);

        menuItem = new JMenuItem(new AbstractAction("Comparaison calibration", new ImageIcon(getClass().getResource(ICON_COMPAR))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

                final JFileChooser fc = new JFileChooser(Preference.getPreference(Preference.KEY_CAL));
                fc.setMultiSelectionEnabled(true);
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

                    if (fc.getSelectedFiles().length == 2) {
                        List<MapCal> mapCals = new ArrayList<MapCal>();

                        for (File f : fc.getSelectedFiles()) {
                            mapCals.add(new MapCal(f));
                        }

                        JPanel panel = new JPanel();
                        ButtonGroup bg = new ButtonGroup();
                        JRadioButton rb1 = new JRadioButton(mapCals.get(0).getName());
                        panel.add(rb1);
                        JRadioButton rb2 = new JRadioButton(mapCals.get(1).getName());
                        panel.add(rb2);
                        bg.add(rb1);
                        bg.add(rb2);

                        rb1.setSelected(true);

                        JOptionPane.showMessageDialog(null, panel, "Choix du fichier de r\u00e9f\u00e9rence", JOptionPane.QUESTION_MESSAGE);

                        final JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Enregistement du fichier");
                        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier Html", "html"));
                        fileChooser.setSelectedFile(new File("Comparaison.html"));
                        final int rep = fileChooser.showSaveDialog(null);

                        if (rep == JFileChooser.APPROVE_OPTION) {

                            ComparResult result = ComparResult.ERROR;

                            if (!fileChooser.getSelectedFile().exists()) {

                                if (rb1.isSelected()) {
                                    result = MapCal.compare(mapCals.get(0), mapCals.get(1), fileChooser.getSelectedFile());
                                } else {
                                    result = MapCal.compare(mapCals.get(1), mapCals.get(0), fileChooser.getSelectedFile());
                                }
                            } else {
                                switch (JOptionPane.showConfirmDialog(null, "Le fichier existe d\u00e9ja, \u00e9craser?", null,
                                        JOptionPane.INFORMATION_MESSAGE)) {
                                case JOptionPane.OK_OPTION:

                                    if (rb1.isSelected()) {
                                        result = MapCal.compare(mapCals.get(0), mapCals.get(1), fileChooser.getSelectedFile());
                                    } else {
                                        result = MapCal.compare(mapCals.get(1), mapCals.get(0), fileChooser.getSelectedFile());
                                    }

                                    break;
                                case JOptionPane.NO_OPTION:
                                    this.actionPerformed(e);
                                    return;
                                default:
                                    break;
                                }
                            }

                            mapCals.clear();

                            switch (result) {
                            case RESULT_OK:
                                final int reponse2 = JOptionPane.showConfirmDialog(null,
                                        "Comparaison termin\u00e9 !\n" + fileChooser.getSelectedFile() + "\nVoulez-vous ouvrir le fichier?", null,
                                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                                switch (reponse2) {
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
                                break;
                            case NO_DIFFERENCE:
                                JOptionPane.showMessageDialog(null, "Aucune diff\u00e9rence entre les deux fichiers !");
                                break;
                            case ERROR:
                                JOptionPane.showMessageDialog(null, "Export abandonn\u00e9 suite à une erreur...");
                                break;
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Il faut choisir deux fichiers.");
                    }
                }
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

        subMenu = new JMenu("Themes");
        ButtonGroup groupBis = new ButtonGroup();

        JRadioButtonMenuItem radioMenuItem = new JRadioButtonMenuItem("Windows");
        radioMenuItem.addActionListener(new ChangeLookAndFeel());
        groupBis.add(radioMenuItem);
        subMenu.add(radioMenuItem);

        radioMenuItem = new JRadioButtonMenuItem("Nimbus");
        radioMenuItem.addActionListener(new ChangeLookAndFeel());
        groupBis.add(radioMenuItem);
        subMenu.add(radioMenuItem);

        radioMenuItem = new JRadioButtonMenuItem("Metal");
        radioMenuItem.addActionListener(new ChangeLookAndFeel());
        groupBis.add(radioMenuItem);
        subMenu.add(radioMenuItem);

        final Enumeration<AbstractButton> enumAbBis = groupBis.getElements();
        AbstractButton abBis;
        while (enumAbBis.hasMoreElements()) {
            abBis = enumAbBis.nextElement();
            if (abBis.getActionCommand().equals(Preference.getPreference(Preference.KEY_LF))) {
                abBis.setSelected(true);
                break;
            }
        }
        menu.add(subMenu);

        // Menu pour changement de disposition
        subMenu = new JMenu("Disposition");
        ButtonGroup groupTer = new ButtonGroup();

        radioMenuItem = new JRadioButtonMenuItem("Onglet");
        radioMenuItem.addActionListener(new ChangeDisposition());
        groupTer.add(radioMenuItem);
        subMenu.add(radioMenuItem);

        radioMenuItem = new JRadioButtonMenuItem("Partag\u00e9");
        radioMenuItem.addActionListener(new ChangeDisposition());
        groupTer.add(radioMenuItem);
        subMenu.add(radioMenuItem);

        final Enumeration<AbstractButton> enumAbTer = groupTer.getElements();
        AbstractButton abTer;
        while (enumAbTer.hasMoreElements()) {
            abTer = enumAbTer.nextElement();
            if (abTer.getActionCommand().equals(Preference.getPreference(Preference.KEY_DISPO))) {
                abTer.setSelected(true);
                break;
            }
        }
        menu.add(subMenu);
        //

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
        final String ICON_ANCRE = "/icon_ancre_32.png";

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
                        return "Fichier de configuration graphique (*.xml)";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        return f.getName().toLowerCase().endsWith("xml");
                    }
                });
                final int reponse = fc.showOpenDialog(Ihm.this);
                if (reponse == JFileChooser.APPROVE_OPTION) {
                    final File config = fc.getSelectedFile();

                    int nbWindow = chartTabbedPane.getTabCount();

                    if (nbWindow > 0) {
                        int res = JOptionPane.showConfirmDialog(null,
                                "Voulez-vous ajouter la configuration aux fenetres existantes? \nDans le cas d'une réponse négative les fenetres déjà présentes seront supprimées.",
                                "", JOptionPane.YES_NO_OPTION);
                        if (res == JOptionPane.NO_OPTION) {
                            closeWindows();
                        }
                    }

                    new OpenConfigWorker(config).execute();
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
                    logger.log(Level.INFO, "Ouverture du fichier map: " + fc.getSelectedFile());
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
                if (mainTabbedPane != null) {
                    mainTabbedPane.setSelectedIndex(0);
                }
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
                new DialDiscretisation(Ihm.this);
            }
        });
        btNewTable.setEnabled(true);
        btNewTable.setToolTipText("En cours de d\u00e9veloppement");
        btNewTable.setEnabled(true);
        btNewTable.setVisible(true);
        bar.add(btNewTable);

        btSynchro = new JToggleButton(new ImageIcon(getClass().getResource(ICON_SHARE_AXIS)));
        btSynchro.setToolTipText("Synchroniser les axes des abscisses");
        btSynchro.setEnabled(false);
        btSynchro.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                final int idxWindow = chartTabbedPane.getSelectedIndex();

                if (btSynchro.isSelected()) {

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
                        otherChart.getPlot().getDomainAxis().removeChangeListener(otherChart);
                        otherChart.getPlot().setDomainAxis(domainAxis);
                        otherChart.getPlot().getDomainAxis().addChangeListener(otherChart);
                        otherChart.setScrollBarProperties(chartView.getScrollbar());
                    }
                } else {
                    ChartView chartView;
                    Measure time = log.getTime();

                    for (int i = 0; i < chartTabbedPane.getTabCount(); i++) {
                        if (!(chartTabbedPane.getComponentAt(i) instanceof ChartView)) {
                            continue;
                        }
                        chartView = (ChartView) chartTabbedPane.getComponentAt(i);
                        if (chartView.getDatasetType() > 1) {
                            continue;
                        }

                        chartView.removeAxisSynchro(time);
                    }
                }

            }
        });
        bar.add(btSynchro);

        btAncre = new JToggleButton(new ImageIcon(getClass().getResource(ICON_ANCRE)));
        btAncre.setToolTipText("Ancre le curseur sur une position de l'écran");
        btAncre.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                ChartView chartView;
                for (int i = 0; i < chartTabbedPane.getTabCount(); i++) {
                    if (!(chartTabbedPane.getComponentAt(i) instanceof ChartView)) {
                        continue;
                    }
                    chartView = (ChartView) chartTabbedPane.getComponentAt(i);
                    if (chartView.getDatasetType() > 1) {
                        continue;
                    }
                    chartView.setCursorBehaviour(btAncre.isSelected());
                }
            }
        });
        bar.add(btAncre);

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
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    directToPlot();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int idx = listLogMeasure.getListMeasure().locationToIndex(e.getPoint());

                if (idx > -1) {
                    listLogMeasure.getListMeasure().setSelectedIndex(idx);

                    Measure measure = listLogMeasure.getListMeasure().getSelectedValue();

                    if (e.isPopupTrigger() && measure instanceof Formula) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem item = new JMenuItem(new AbstractAction("Editer", new ImageIcon(getClass().getResource(ICON_EDIT))) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                new DialNewFormula(Ihm.this, ((Formula) measure), true);
                            }
                        });
                        menu.add(item);
                        item = new JMenuItem(new AbstractAction("Supprimer", new ImageIcon(getClass().getResource(ICON_DELETE))) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                deleteMeasure(listLogMeasure.getListMeasure().getSelectedValue());
                            }
                        });
                        menu.add(item);
                        menu.show(listLogMeasure.getListMeasure(), e.getX(), e.getY());
                    }
                }

            }

        });
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
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
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 70;
        gbc.weighty = 95;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(chartTabbedPane, gbc);

        chartTabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {

                if (chartTabbedPane.getTabCount() > 1) {
                    btSynchro.setEnabled(true);
                } else {
                    btSynchro.setEnabled(false);
                }

                int idx = chartTabbedPane.getSelectedIndex();

                if (idx > -1) {

                    if (selectedIndexTab > -1 && selectedIndexTab < chartTabbedPane.getTabCount()) {
                        if (chartTabbedPane.getTabComponentAt(selectedIndexTab) != null) {
                            ((ButtonTabComponent) chartTabbedPane.getTabComponentAt(selectedIndexTab)).stopEditing();
                        }
                    }
                    selectedIndexTab = idx;

                    ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idx);
                    tableCursorValues.getModel().changeList(chartView.getMeasuresColors());

                    if (chartView.getDatasetType() < 2) {
                        chartView.updateTableValue();
                        if (!listZone.isEmpty()) {
                            chartView.applyCondition(listZone.get(panelCondition.getTableCondition().getNumActiveCondition()));
                        } else {
                            int row = panelCondition.getTableCondition().getNumActiveCondition();

                            if (row < 0) {
                                return;
                            }

                            final Condition condition = (Condition) panelCondition.getTableCondition().getModel().getValueAt(row, 1);
                            BitSet bitCondition = condition.applyCondition(log);
                            if (condition.isActive()) {
                                listZone.put(row, chartView.applyCondition(condition.isActive(), bitCondition, condition.getColor()));
                                panelCondition.setListBoxAnnotation(listZone.get(row));
                            } else {
                                listZone.remove(row);
                                chartView.removeCondition();
                                panelCondition.setListBoxAnnotation(Collections.<IntervalMarker> emptyList());
                            }
                        }

                    } else {
                        Condition condition = panelCondition.getTableCondition().getActiveCondition();
                        if (condition != null) {
                            BitSet bitCondition = condition.applyCondition(log);
                            chartView.applyCondition(condition.isActive(), bitCondition, log);
                        } else {
                            chartView.applyCondition(false, null, log);
                        }
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
                                DialogProperties propertiesPanel = new DialogProperties(chartView, xyPlot);
                                int res = JOptionPane.showConfirmDialog(Ihm.this, propertiesPanel, "Propri\u00e9t\u00e9s", 2, -1);
                                if (res == JOptionPane.OK_OPTION) {
                                    propertiesPanel.updatePlot(chartView, xyPlot);
                                    chartView.getChart().fireChartChanged();
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

                            for (int i = 0; i < 5; i++) {

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
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 15;
        gbc.weighty = 50;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        scrollTableCursorValues = new JScrollPane(tableCursorValues);
        panel.add(scrollTableCursorValues, gbc);

        panelCondition = new PanelCondition();
        panelCondition.getTableCondition().getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {

                    final int row = panelCondition.getTableCondition().getSelectedRow();

                    if (row < 0) {
                        return;
                    }

                    final Condition condition = (Condition) panelCondition.getTableCondition().getModel().getValueAt(row, 1);

                    int idx = chartTabbedPane.getSelectedIndex();
                    if (idx > -1) {
                        if (log != null && chartTabbedPane.getComponentAt(idx) instanceof ChartView) {
                            final ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idx);

                            BitSet bitCondition = condition.applyCondition(log);

                            if (chartView.getDatasetType() < 2) {

                                Thread thread = new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        if (condition.isActive()) {
                                            listZone.put(row, chartView.applyCondition(condition.isActive(), bitCondition, condition.getColor()));
                                            panelCondition.setListBoxAnnotation(listZone.get(row));
                                        } else {
                                            listZone.remove(row);
                                            chartView.removeCondition();
                                            panelCondition.setListBoxAnnotation(Collections.<IntervalMarker> emptyList());
                                        }
                                    }
                                });
                                thread.start();
                            } else {

                                Thread thread = new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        chartView.applyCondition(condition.isActive(), bitCondition, log);
                                    }
                                });
                                thread.start();

                            }
                        } else {
                            if (condition.isActive()) {
                                condition.setActive(false);
                            }
                        }
                    }
                }

            }
        });

        panelCondition.getCbZones().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                int idxWin = chartTabbedPane.getSelectedIndex();
                if (idxWin > -1) {
                    if (chartTabbedPane.getComponentAt(idxWin) instanceof ChartView) {
                        ChartView chartView = (ChartView) chartTabbedPane.getComponentAt(idxWin);

                        if (chartView.getDatasetType() < 2) {
                            CombinedDomainXYPlot combinedDomainXYPlot = chartView.getPlot();

                            double t1 = panelCondition.getStartZone();
                            double t2 = panelCondition.getEndZone();

                            if (!Double.isNaN(t1) && !Double.isNaN(t2)) {
                                double duration = panelCondition.getDurationZone();

                                t1 -= (duration * 0.2);
                                t2 += (duration * 0.2);

                                Range newRange = new Range(t1, t2);
                                combinedDomainXYPlot.getDomainAxis().setRange(newRange);
                            }
                        }

                    }
                }

            }
        });

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 15;
        gbc.weighty = 50;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JScrollPane(panelCondition), gbc);

        return panel;

    }

    private final JPopupMenu createTableMenu() {

        final String ICON_MINVALUE = "/icon_minValue_16.png";
        final String ICON_MAXVALUE = "/icon_maxValue_16.png";

        final JPopupMenu popUp = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Aller à valeur min", new ImageIcon(getClass().getResource(ICON_MINVALUE)));
        menuItem.setActionCommand("MIN_VALUE");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        menuItem = new JMenuItem("Aller à la valeur max", new ImageIcon(getClass().getResource(ICON_MAXVALUE)));
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
                    return "Fichier log (*.txt, *.msl, *.csv)";
                }

                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    return f.getName().toLowerCase().endsWith("txt") || f.getName().toLowerCase().endsWith("msl")
                            || f.getName().toLowerCase().endsWith("csv");
                }
            });
            final int reponse = fc.showOpenDialog(Ihm.this);

            if (reponse == JFileChooser.APPROVE_OPTION) {

                logger.log(Level.INFO, "Ouverture de " + fc.getSelectedFile().getName());

                if (listModel.getSize() > 0) {
                    listModel.clear();
                }

                log = new Log(fc.getSelectedFile());

                for (Measure measure : log.getMeasures()) {
                    listModel.addElement(measure);
                }

                for (Measure formule : listFormula) {
                    log.getMeasures().add(formule);
                }

                for (Measure formule : listFormula) {
                    ((Formula) formule).setOutdated();
                    ((Formula) formule).calculate(log, getSelectedCal());
                    listModel.addElement(formule);
                }

                Ihm.this.setTitle(APPLICATION_TITLE + " - " + log.getName());

                // load data in chart
                if (chartTabbedPane.getTabCount() > 0) {

                    btSynchro.setEnabled(true);

                    reloadLogData(log);
                }

                mapView.setLog(log);

                if (mainTabbedPane != null) {
                    mainTabbedPane.setSelectedIndex(0);
                }
            }

        }
    }

    private final void addMapFile(MapCal mapCal) {

        if (mapCal.getMdbData().getInfos().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fichier '" + mapCal.getMdbData().getName() + ".mdb' non trouv\u00e9."
                    + "\nCertaines fonctionnalit\u00e9s seront impact\u00e9es.");
        }

        mapView.addCalToTree(mapCal);
        if (mainTabbedPane != null) {
            mainTabbedPane.setSelectedComponent(mapView);
        }

    }

    private final ChartView addChartWindow(String nameWindow) {

        ChartView chartView = new ChartView();
        chartView.addObservateur(tableCursorValues);

        // Test
        chartView.addCursorObservateur(mapView);
        //

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
        // schartTabbedPane.setBackgroundAt(chartTabbedPane.getTabCount() - 1, Color.CYAN);

        if (btSynchro.isSelected() && chartTabbedPane.getTabCount() > 1) {
            ChartView refChartView = (ChartView) chartTabbedPane.getComponentAt(chartTabbedPane.getTabCount() - 2);
            ValueAxis domainAxis = refChartView.getPlot().getDomainAxis();
            chartView.getPlot().setDomainAxis(domainAxis);
            chartView.setScrollBarProperties(refChartView.getScrollBarModel());
        }

        return chartView;
    }

    private final void closeWindows() {

        if (btSynchro.isSelected()) {
            btSynchro.doClick();
        }
        btSynchro.setEnabled(false);

        for (int i = chartTabbedPane.getTabCount() - 1; i >= 0; i--) {
            ((ChartView) chartTabbedPane.getComponentAt(i)).delObservateur();
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
                if (support.getComponent() instanceof ChartView) {
                    ChartView chartView = (ChartView) support.getComponent();
                    ChartEntity chartEntity = chartView.getChartPanel().getEntityForPoint(support.getDropLocation().getDropPoint().x,
                            support.getDropLocation().getDropPoint().y);
                    if (chartView.getDatasetType() > 1 || !(chartEntity instanceof PlotEntity)) {
                        return false;
                    }
                }
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

                if (support.getComponent() instanceof ChartView) {
                    final ChartView chartView = (ChartView) support.getComponent();
                    final CombinedDomainXYPlot combinedDomainXYPlot = chartView.getPlot();
                    final XYPlot plot = combinedDomainXYPlot.findSubplot(chartView.getChartPanel().getChartRenderingInfo().getPlotInfo(),
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

    public final void addMeasure(Formula newMeasure) {
        if (!listModel.contains(newMeasure)) {
            listModel.addElement(newMeasure);
            listFormula.add(newMeasure);
            if (log != null) {
                log.getMeasures().add(newMeasure);
            }
            logger.log(Level.INFO, "'" + newMeasure.getName() + "'" + " a été ajouté à la liste de variable.");
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

            if (z.isEmpty()) {
                chartView.add2DScatterPlot(x, y);
            } else {
                chartView.add3DScatterPlot(x, y, z);
            }
        }

        if (mainTabbedPane != null) {
            mainTabbedPane.setSelectedIndex(0);
        }
    }

    public final List<String> getListStringMeasure() {
        return this.listModel.getStringList();
    }

    public final Set<Formula> getListFormula() {
        return listFormula;
    }

    private final Measure pickMeasureFromList(String name) {
        final Measure measure = new Measure(name);
        final int idx = listModel.indexOf(measure);

        return idx > -1 ? listModel.getElementAt(idx) : measure;
    }

    public final void refresh(Variable var) {
        if (log != null) {
            if (var == null) {
                for (Formula form : getListFormula()) {
                    if (form.needUpdate() || form.isMapCalBased()) {
                        form.calculate(log, getSelectedCal());
                        reloadFormulaData(log, form.getName());
                    }
                }
            } else {
                for (Formula form : getListFormula()) {
                    if (form.needUpdate() || form.isMapCalBased()) {
                        if (form.getExpression().contains(var.getName())) {
                            form.calculate(log, getSelectedCal());
                            reloadFormulaData(log, form.getName());
                        }
                    }
                }
            }

        }
    }

    private final void reloadLogData(Log log) {

        final int nbTab = chartTabbedPane.getTabCount();

        if (log == null) {
            return;
        }

        Ihm.this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

        final int nbProcs = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

        ChartView chartView;

        ExecutorService executor = Executors.newFixedThreadPool(nbProcs);

        for (int n = 0; n < nbTab; n++) {
            if (chartTabbedPane.getComponentAt(n) instanceof ChartView) {
                chartView = (ChartView) chartTabbedPane.getComponentAt(n);
                executor.execute(new UpdateDataset(chartView));
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (!executor.isTerminated()) {
        }

        if (btSynchro.isSelected()) {
            for (int n = 0; n < nbTab; n++) {
                if (chartTabbedPane.getComponentAt(n) instanceof ChartView) {
                    chartView = (ChartView) chartTabbedPane.getComponentAt(n);
                    chartView.getPlot().getDomainAxis().setAutoRange(true);
                    chartView.getPlot().configureDomainAxes();
                    chartView.configureScrollbar();
                    break;
                }
            }
        }

        Ihm.this.setCursor(Cursor.getDefaultCursor());
    }

    private final class UpdateDataset implements Runnable {
        private ChartView chartView;

        public UpdateDataset(ChartView chartView) {
            this.chartView = chartView;
        }

        @Override
        public void run() {

            final double[] temps = log.getTime().getData();
            final int nbPoint = temps.length;

            XYPlot xyPlot;
            XYSeries serie;
            Comparable<?> key;
            Measure measure = null;

            chartView.getPlot().setNotify(false);

            for (Object plot : chartView.getPlot().getSubplots()) {
                xyPlot = (XYPlot) plot;

                for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {

                    int nbSerie = xyPlot.getDataset(nDataset).getSeriesCount();

                    for (int nSerie = 0; nSerie < nbSerie; nSerie++) {

                        if (xyPlot.getDataset(nDataset) instanceof XYSeriesCollection) {

                            serie = ((XYSeriesCollection) xyPlot.getDataset(nDataset)).getSeries(nSerie);

                            if (serie != null) {
                                serie.clear();

                                key = serie.getKey();

                                measure = pickMeasureFromList(key.toString());

                                final int sizeData = measure.getDataLength();

                                for (int n1 = 0; n1 < nbPoint; n1++) {

                                    if (n1 < sizeData) {
                                        serie.add(temps[n1], measure.get(n1), false);
                                    }
                                }

                                serie.fireSeriesChanged();
                            }

                        } else if (xyPlot.getDataset() instanceof DefaultXYZDataset) {
                            Comparable<?> serieKey = ((DefaultXYZDataset) xyPlot.getDataset()).getSeriesKey(nSerie);

                            String xLabel = xyPlot.getDomainAxis().getLabel();
                            String yLabel = xyPlot.getRangeAxis().getLabel();
                            String zLabel = ((PaintScaleLegend) chartView.getChart().getSubtitle(0)).getAxis().getLabel();

                            Measure xMeasure = pickMeasureFromList(xLabel);
                            Measure yMeasure = pickMeasureFromList(yLabel);
                            Measure zMeasure = pickMeasureFromList(zLabel);

                            if ((xMeasure.getDataLength() == yMeasure.getDataLength()) && (yMeasure.getDataLength() == zMeasure.getDataLength())) {
                                ((DefaultXYZDataset) xyPlot.getDataset()).addSeries(serieKey,
                                        new double[][] { xMeasure.getData(), yMeasure.getData(), zMeasure.getData() });
                            }

                        } else {
                            Comparable<?> serieKey = ((DefaultXYDataset) xyPlot.getDataset()).getSeriesKey(nSerie);

                            if (xyPlot.getDatasetCount() > 1) {
                                xyPlot.clearAnnotations();
                            }

                            String xLabel = xyPlot.getDomainAxis().getLabel();
                            String yLabel = xyPlot.getRangeAxis().getLabel();

                            Measure xMeasure = pickMeasureFromList(xLabel);
                            Measure yMeasure = pickMeasureFromList(yLabel);

                            ((DefaultXYDataset) xyPlot.getDataset()).addSeries(serieKey, new double[][] { xMeasure.getData(), yMeasure.getData() });

                        }
                    }
                }

            }

            if (!btSynchro.isSelected()) {
                chartView.getPlot().getDomainAxis().setAutoRange(true);
                chartView.getPlot().configureDomainAxes();
                chartView.configureScrollbar();
            }

            for (Object plot : chartView.getPlot().getSubplots()) { // Obligé de configuer l'axe Y une fois que l'axe X est sur la pleine échelle
                xyPlot = (XYPlot) plot;

                if (xyPlot.getRangeAxis().isAutoRange()) {
                    xyPlot.configureRangeAxes();
                }
            }

            chartView.getPlot().setNotify(true);

        }

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

        final Measure temps = log.getTime();
        final int nbPoint = temps.getDataLength();

        for (int n = 0; n < nbTab; n++) {
            chartView = (ChartView) chartTabbedPane.getComponentAt(n);

            xyPlot = chartView.getPlot(formulaName);

            if (xyPlot != null) {
                for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {

                    if (xyPlot.getDataset() instanceof XYSeriesCollection) {

                        int idxSerie = xyPlot.getDataset(nDataset).indexOf(formulaName);

                        if (idxSerie == -1) {
                            continue;
                        }

                        serie = ((XYSeriesCollection) xyPlot.getDataset(nDataset)).getSeries(idxSerie);
                        serie.clear();
                        key = serie.getKey();

                        measure = pickMeasureFromList(key.toString());

                        final int sizeData = measure.getDataLength();

                        for (int n1 = 0; n1 < nbPoint; n1++) {

                            if (n1 < sizeData) {
                                serie.add(temps.get(n1), measure.get(n1), false);
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
                                new double[][] { xMeasure.getData(), yMeasure.getData(), zMeasure.getData() });

                    } else {
                        Comparable<?> serieKey = ((DefaultXYDataset) xyPlot.getDataset()).getSeriesKey(0);

                        String xLabel = xyPlot.getDomainAxis().getLabel();
                        String yLabel = xyPlot.getRangeAxis().getLabel();

                        Measure xMeasure = pickMeasureFromList(xLabel);
                        Measure yMeasure = pickMeasureFromList(yLabel);

                        ((DefaultXYDataset) xyPlot.getDataset()).addSeries(serieKey, new double[][] { xMeasure.getData(), yMeasure.getData() });

                    }
                }
            }
        }

    }

    private final class ChangeLookAndFeel implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent action) {

            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (action.getActionCommand().equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                        SwingUtilities.updateComponentTreeUI(Ihm.this);
                        Preference.setPreference(Preference.KEY_LF, action.getActionCommand());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private final class ChangeDisposition implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent action) {

            if (action.getActionCommand().equals(Preference.getPreference(Preference.KEY_DISPO))) {
                return;
            }

            Preference.setPreference(Preference.KEY_DISPO, action.getActionCommand());

            Container root = Ihm.this.getContentPane();

            if ("Onglet".equals(action.getActionCommand())) {
                mainTabbedPane = new JTabbedPane();

                mainTabbedPane.addTab(LOG_PANEL, new ImageIcon(getClass().getResource(ICON_LOG_TAB)), splitLogMap.getTopComponent());
                mainTabbedPane.addTab(MAP_PANEL, new ImageIcon(getClass().getResource(ICON_MAP_TAB)), mapView);

                mainTabbedPane.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        if (mainTabbedPane.getSelectedIndex() == 0) {
                            if (mapView.getCalForFormula() != null) {
                                refresh(null);
                            }
                        }
                    }
                });

                root.add(mainTabbedPane, BorderLayout.CENTER);
                root.remove(splitLogMap);
                splitLogMap = null;
            } else {
                splitLogMap = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainTabbedPane.getComponentAt(0), mapView);
                splitLogMap.setOneTouchExpandable(true);
                root.add(splitLogMap, BorderLayout.CENTER);
                root.remove(mainTabbedPane);
                ;
                mainTabbedPane = null;
            }
            root.invalidate();
            root.revalidate();
            root.repaint();
        }
    }

    private final void saveConfig(File file) {

        int nbTab = chartTabbedPane.getTabCount();
        Map<String, ChartView> listChart = new LinkedHashMap<String, ChartView>();

        for (int i = 0; i < nbTab; i++) {
            if (chartTabbedPane.getComponentAt(i) instanceof ChartView) {
                listChart.put(chartTabbedPane.getTitleAt(i), (ChartView) chartTabbedPane.getComponentAt(i));
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

        DocumentBuilder builder;
        Document document = null;
        DocumentBuilderFactory factory;
        factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setValidating(false);

        logger.log(Level.INFO, "Ouverture de la configuration " + file.getName());
        long start = 0;

        try {

            start = System.currentTimeMillis();

            builder = factory.newDocumentBuilder();
            document = builder.parse(new File(file.toURI()));

            final Element racine = document.getDocumentElement();
            ChartView chartView;

            if ("Configuration".equals(racine.getNodeName())) {

                NodeList listWindow = racine.getElementsByTagName("Window");
                int nbWindow = listWindow.getLength();

                for (int i = 0; i < nbWindow; i++) {
                    Element window = (Element) listWindow.item(i);
                    String nameWindow = window.getElementsByTagName("Name").item(0).getTextContent();
                    int typeWindow = Integer.parseInt(window.getElementsByTagName("Type").item(0).getTextContent());
                    chartView = addChartWindow(nameWindow);

                    Element cursor = (Element) window.getElementsByTagName("Cursor").item(0);
                    String cursorColor;
                    float cursorWidth = 2;
                    float[] dashArray = new float[] { 2, 2 };
                    BasicStroke cursorStroke;
                    if (cursor != null) {
                        cursorColor = cursor.getElementsByTagName("Color").item(0).getTextContent();
                        cursorWidth = Float.parseFloat(cursor.getElementsByTagName("Width").item(0).getTextContent());

                        String txtDashArray = cursor.getElementsByTagName("Style").item(0).getTextContent();
                        if (!"continue".equals(txtDashArray)) {
                            String[] sDashArray = cursor.getElementsByTagName("Style").item(0).getTextContent().split(";");
                            dashArray[0] = Float.parseFloat(sDashArray[0]);
                            dashArray[1] = Float.parseFloat(sDashArray[1]);
                        }

                        cursorStroke = new BasicStroke(cursorWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 4.0f, dashArray, 0.0f);
                    } else {
                        cursorColor = "[r=0,g=0,b=255]";
                        cursorStroke = new BasicStroke(2);
                    }

                    chartView.getChart().setNotify(false);

                    NodeList listPlot = window.getElementsByTagName("Plot");
                    int nbPlot = listPlot.getLength();

                    for (int j = 0; j < nbPlot; j++) {

                        Element plot = (Element) listPlot.item(j);

                        String bckGrndColor = plot.getElementsByTagName("Background").item(0).getTextContent();

                        switch (typeWindow) {
                        case 1:

                            String timeBase = plot.getElementsByTagName("TimeBase").item(0).getTextContent();

                            chartView.addPlot(new Measure(timeBase), bckGrndColor, cursorColor, cursorStroke);
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
                                    chartView.addMeasure((XYPlot) chartView.getPlot().getSubplots().get(j), new Measure(timeBase),
                                            new Measure(nameSerie), colorSerie, widthSerie, nameAxis);
                                }

                                ValueAxis axis = ((XYPlot) chartView.getPlot().getSubplots().get(j)).getRangeAxis(k);

                                if (splitRange.length == 2) {
                                    try {
                                        double lowerBound = Double.parseDouble(splitRange[0]);
                                        double upperBound = Double.parseDouble(splitRange[1]);
                                        Range newRange = new Range(lowerBound, upperBound);
                                        axis.setRange(newRange);

                                    } catch (NumberFormatException e) {
                                    }
                                } else {
                                    axis.setAutoRange(true);
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

                    chartView.getChart().setNotify(true);

                }

                NodeList listFormulas = racine.getElementsByTagName("Formula");
                int nbFormula = listFormulas.getLength();

                for (int i = 0; i < nbFormula; i++) {
                    Element formula = (Element) listFormulas.item(i);
                    String nameFormula = formula.getElementsByTagName("Name").item(0).getTextContent();
                    String unitFormula = formula.getElementsByTagName("Unit").item(0).getTextContent();
                    String expressionFormula = formula.getElementsByTagName("Expression").item(0).getTextContent();

                    // Tâche longue, 90% de la fonction openConfig : voir pour optimiser
                    if (log != null) {
                        listFormula.add(new Formula(nameFormula, unitFormula, expressionFormula, log, getSelectedCal()));
                    } else {
                        listFormula.add(new Formula(nameFormula, unitFormula, expressionFormula));
                    }
                    // *****
                }

                if (log != null) {

                    btSynchro.setEnabled(true);

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
                        chartView = (ChartView) chartTabbedPane.getComponentAt(idx);
                        tableCursorValues.getModel().changeList(chartView.getMeasuresColors());
                    }
                }

                document = null;

            } else {
                return;
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.log(Level.WARNING, "Erreur sur l'ouverture de la configuration " + file.getName());
        } finally {
            reloadLogData(log);

            System.out.println("Config : " + (System.currentTimeMillis() - start) + "ms");
        }

    }

    public static void main(String[] args) {

        // File[] fileToConvert = new File[] { new File("c:\\user\\U354706\\Perso\\Clio\\soft\\applicatif_04102021_Cor.inc"),
        // new File("c:\\user\\U354706\\Perso\\Clio\\soft\\regf276e.def"),
        // new File("c:\\user\\U354706\\Perso\\Clio\\Calib\\RS2_pnp_PH1_V2_001\\RS2_pnp_PH1_V2_001.mdb") };

        // Conversion.AppIncToA2l(fileToConvert);

        try {

            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (Preference.getPreference(Preference.KEY_LF).equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                    } catch (ClassNotFoundException e1) {
                    } catch (InstantiationException e1) {
                    } catch (IllegalAccessException e1) {
                    } catch (UnsupportedLookAndFeelException e1) {
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
        Variable var = ((MapView) arg.getSource()).getSelectedVariable();
        refresh(var);
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
            int idx = ((XYSeriesCollection) plot.getDataset(nDataset)).getSeriesIndex(signalName);
            if (idx > -1) {
                serie = ((XYSeriesCollection) plot.getDataset(nDataset)).getSeries(signalName);
                break;
            }
        }

        Measure time = log.getTime();

        switch (command) {
        case "MIN_VALUE":

            double min = serie.getMinY();
            for (int i = 0; i < serie.getItemCount(); i++) {
                if (serie.getY(i).doubleValue() == min) {
                    chartView.moveMarker(time.get(i));
                    return;
                }
            }
            break;
        case "MAX_VALUE":

            double max = serie.getMaxY();
            for (int i = 0; i < serie.getItemCount(); i++) {
                if (serie.getY(i).doubleValue() == max) {
                    chartView.moveMarker(time.get(i));
                    return;
                }
            }
            break;
        }
    }

    private class OpenConfigWorker extends SwingWorker<Void, String> {
        File cfg;
        long start;
        ChartView chartView;
        List<Condition> conditions;

        public OpenConfigWorker(File config) {
            this.cfg = config;
        }

        @Override
        protected Void doInBackground() throws Exception {

            logger.log(Level.INFO, "Ouverture de la configuration " + this.cfg.toURI());

            DocumentBuilder builder;
            Document document = null;
            DocumentBuilderFactory factory;
            factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setValidating(false);

            try {

                start = System.currentTimeMillis();

                builder = factory.newDocumentBuilder();
                document = builder.parse(new File(this.cfg.toURI()));

                final Element racine = document.getDocumentElement();

                if ("Configuration".equals(racine.getNodeName())) {

                    NodeList listWindow = racine.getElementsByTagName("Window");
                    int nbWindow = listWindow.getLength();

                    for (int i = 0; i < nbWindow; i++) {
                        Element window = (Element) listWindow.item(i);
                        String nameWindow = window.getElementsByTagName("Name").item(0).getTextContent();
                        int typeWindow = Integer.parseInt(window.getElementsByTagName("Type").item(0).getTextContent());
                        chartView = addChartWindow(nameWindow);

                        Element cursor = (Element) window.getElementsByTagName("Cursor").item(0);
                        String cursorColor;
                        float cursorWidth = 2;
                        float[] dashArray = new float[] { 2, 2 };
                        BasicStroke cursorStroke;
                        if (cursor != null) {
                            cursorColor = cursor.getElementsByTagName("Color").item(0).getTextContent();
                            cursorWidth = Float.parseFloat(cursor.getElementsByTagName("Width").item(0).getTextContent());

                            String txtDashArray = cursor.getElementsByTagName("Style").item(0).getTextContent();
                            if (!"continue".equals(txtDashArray)) {
                                String[] sDashArray = cursor.getElementsByTagName("Style").item(0).getTextContent().split(";");
                                dashArray[0] = Float.parseFloat(sDashArray[0]);
                                dashArray[1] = Float.parseFloat(sDashArray[1]);
                            }

                            cursorStroke = new BasicStroke(cursorWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 4.0f, dashArray, 0.0f);
                        } else {
                            cursorColor = "[r=0,g=0,b=255]";
                            cursorStroke = new BasicStroke(2);
                        }

                        chartView.getChart().setNotify(false);

                        NodeList listPlot = window.getElementsByTagName("Plot");
                        int nbPlot = listPlot.getLength();

                        for (int j = 0; j < nbPlot; j++) {

                            Element plot = (Element) listPlot.item(j);

                            String bckGrndColor = plot.getElementsByTagName("Background").item(0).getTextContent();

                            switch (typeWindow) {
                            case 1:

                                String timeBase = plot.getElementsByTagName("TimeBase").item(0).getTextContent();

                                chartView.addPlot(new Measure(timeBase), bckGrndColor, cursorColor, cursorStroke);
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
                                        chartView.addMeasure((XYPlot) chartView.getPlot().getSubplots().get(j), new Measure(timeBase),
                                                new Measure(nameSerie), colorSerie, widthSerie, nameAxis);
                                    }

                                    ValueAxis axis = ((XYPlot) chartView.getPlot().getSubplots().get(j)).getRangeAxis(k);

                                    if (splitRange.length == 2) {
                                        try {
                                            double lowerBound = Double.parseDouble(splitRange[0]);
                                            double upperBound = Double.parseDouble(splitRange[1]);
                                            Range newRange = new Range(lowerBound, upperBound);
                                            axis.setRange(newRange);

                                        } catch (NumberFormatException e) {
                                        }
                                    } else {
                                        axis.setAutoRange(true);
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

                        chartView.getChart().setNotify(true);

                    }

                    NodeList listFormulas = racine.getElementsByTagName("Formula");
                    int nbFormula = listFormulas.getLength();

                    for (int i = 0; i < nbFormula; i++) {
                        Element formula = (Element) listFormulas.item(i);
                        String nameFormula = formula.getElementsByTagName("Name").item(0).getTextContent();
                        String unitFormula = formula.getElementsByTagName("Unit").item(0).getTextContent();
                        String expressionFormula = formula.getElementsByTagName("Expression").item(0).getTextContent();

                        // Tâche longue, 90% de la fonction openConfig : voir pour optimiser
                        if (log != null) {
                            listFormula.add(new Formula(nameFormula, unitFormula, expressionFormula, log, getSelectedCal()));
                        } else {
                            listFormula.add(new Formula(nameFormula, unitFormula, expressionFormula));
                        }
                        // *****
                    }

                    NodeList listConditons = racine.getElementsByTagName("Condition");
                    int nbCondition = listConditons.getLength();

                    conditions = new ArrayList<>(nbCondition);

                    for (int i = 0; i < nbCondition; i++) {
                        Element condition = (Element) listConditons.item(i);
                        String nameCondition = condition.getElementsByTagName("Name").item(0).getTextContent();
                        String expressionCondition = condition.getElementsByTagName("Expression").item(0).getTextContent();
                        String colorCondition = condition.getElementsByTagName("Color").item(0).getTextContent();

                        conditions.add(new Condition(nameCondition, expressionCondition, Utilitaire.parseRGBColor(colorCondition, 70)));
                    }

                    document = null;

                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                logger.log(Level.WARNING, "Erreur sur l'ouverture de la configuration " + this.cfg.toURI());
            } finally {
                // reloadLogData(log);
            }

            return null;
        }

        @Override
        protected void done() {

            if (log != null) {

                btSynchro.setEnabled(true);

                for (Measure formule : listFormula) {
                    log.getMeasures().add(formule);
                    int idx = listModel.indexOf(formule);
                    if (idx < 0) {
                        listModel.addElement(formule);
                    }
                }
            }

            panelCondition.getTableCondition().getModel().setConditions(conditions);

            int idx = chartTabbedPane.getSelectedIndex();
            if (idx > -1) {
                if (chartTabbedPane.getComponentAt(idx) instanceof ChartView) {
                    chartView = (ChartView) chartTabbedPane.getComponentAt(idx);
                    tableCursorValues.getModel().changeList(chartView.getMeasuresColors());
                }
            }

            reloadLogData(log);

            System.out.println("Config worker : " + (System.currentTimeMillis() - start) + "ms");
        }

    }

}
