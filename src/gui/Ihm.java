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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
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
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import log.Log;
import log.Measure;
import utils.Preference;
import utils.Utilitaire;

/*
 * Creation : 3 mai 2018
 */

public final class Ihm extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTabbedPane tabbedPane;
    private DefaultListModel<Measure> listModel;
    private JList<Measure> listVoie;
    private TableCursorValue tableCursorValues;

    private JLabel labelFnr;
    private JLabel labelLogName;

    private static Log log;

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
        final String ICON_EXIT = "/icon_exit_16.png";
        final String ICON_NEW = "/new_icon_16.png";

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
                        return "Fichier de configuration graphique (*.cfg)";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        return f.getName().toLowerCase().endsWith("cfg");
                    }
                });
                final int reponse = fc.showOpenDialog(Ihm.this);
                if (reponse == JFileChooser.APPROVE_OPTION) {
                    openConfig(fc.getSelectedFile());
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
                fileChooser.setFileFilter(new FileNameExtensionFilter("Fichier de configuration graphique (*.cfg)", "cfg"));
                fileChooser.setSelectedFile(new File("config.cfg"));
                final int rep = fileChooser.showSaveDialog(null);

                if (rep == JFileChooser.APPROVE_OPTION) {

                    String extension = "";
                    String fileName = fileChooser.getSelectedFile().getAbsolutePath();
                    File file = fileChooser.getSelectedFile();

                    final int idxDot = fileName.lastIndexOf(".");

                    if (idxDot > -1) {
                        extension = fileName.substring(idxDot + 1);
                    }
                    if (!extension.equalsIgnoreCase("cfg")) {
                        file = new File(fileName.replace("." + extension, "") + ".cfg");
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
        menuItem.addActionListener(new AddWindow());
        menu.add(menuItem);
        
        menu = new JMenu("Pr\u00e9f\u00e9rences");
        menuBar.add(menu);
        
        JMenu subMenu = new JMenu("Log");
        menuItem = new JMenuItem(new AbstractAction("Chemin d'import") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                final String pathFolder = Utilitaire.getFolder("Choix du chemin",
                        Preference.getPreference(Preference.KEY_LOG));
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
                final String pathFolder = Utilitaire.getFolder("Choix du chemin",
                        Preference.getPreference(Preference.KEY_CONFIG));
                if (!Preference.KEY_CONFIG.equals(pathFolder)) {
                    Preference.setPreference(Preference.KEY_CONFIG, pathFolder);
                    ((JMenuItem) e.getSource()).setToolTipText(pathFolder);
                }
            }
        });
        menuItem.setToolTipText(Preference.getPreference(Preference.KEY_CONFIG));
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

        return menuBar;
    }
    
    private final JToolBar createToolBar()
    {
    	JToolBar bar = new JToolBar();
    	bar.setFloatable(false);
    	
    	JButton btNewPlot = new JButton(new AbstractAction("Nouveau graphique") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				ChartView chartView = new ChartView();
	            chartView.addObservateur(tableCursorValues);
	            chartView.setTransferHandler(new MeasureHandler());
	            String defaultName = "Fenetre_" + tabbedPane.getTabCount();
	            String windowName = JOptionPane.showInputDialog(Ihm.this, "Nom de la fenetre :", defaultName);
	            if (windowName == null || "".equals(windowName)) {
	                windowName = defaultName;
	            }
	            tabbedPane.addTab(windowName, chartView);
	            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ButtonTabComponent(tabbedPane));
	            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
	            
	            int idxX = log.getMeasures().indexOf(new Measure("RPM(tr/min)"));
	            int idxY = log.getMeasures().indexOf(new Measure("Intake_manifold_P(mbar)"));
	            int idxZ = log.getMeasures().indexOf(new Measure("Ign_Avance_app(°V)"));
				
		        chartView.addScatterPlot(log.getMeasures().get(idxX), log.getMeasures().get(idxY), log.getMeasures().get(idxZ));
				
			}
		});
    	btNewPlot.setEnabled(false);
    	bar.add(btNewPlot);
    	
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
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(createToolBar(), gbc);

        listVoie = new JList<Measure>();
        listVoie.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    measureSelection();
                }
            }
        });
        listVoie.setModel(listModel);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 5;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        listVoie.setDragEnabled(true);
        root.add(new JScrollPane(listVoie), gbc);

        tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(800, 600));
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 90;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(tabbedPane, gbc);

        tabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int idx = tabbedPane.getSelectedIndex();
                if (idx > -1) {
                    ChartView chartView = (ChartView) tabbedPane.getComponentAt(idx);
                    ((DataValueModel) tableCursorValues.getModel()).changeList(chartView.getMeasures());
                    chartView.updateTableValue();
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
        gbc.weightx = 5;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(new JScrollPane(tableCursorValues), gbc);

        labelFnr = new JLabel("Fournisseur du log : ");
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(labelFnr, gbc);

        labelLogName = new JLabel("Nom de l'acquisition : ");
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 3;
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

                labelFnr.setText("<html>Fournisseur du log : " + "<b>" + log.getFnr());
                labelLogName.setText("<html>Nom de l'acquisition : " + "<b>" + log.getName());

                // load data in chart
                reloadLogData(log);
            }

        }
    }

    private final class AddWindow implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            ChartView chartView = new ChartView();
            chartView.addObservateur(tableCursorValues);
            chartView.setTransferHandler(new MeasureHandler());
            String defaultName = "Fenetre_" + tabbedPane.getTabCount();
            String windowName = JOptionPane.showInputDialog(Ihm.this, "Nom de la fenetre :", defaultName);
            if (windowName == null || "".equals(windowName)) {
                windowName = defaultName;
            }
            tabbedPane.addTab(windowName, chartView);
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ButtonTabComponent(tabbedPane));
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        }

    }

    private final class MeasureHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport support) {
            boolean doImport = support.isDataFlavorSupported(DataFlavor.stringFlavor);

            if (doImport) {
                ChartView chartView = (ChartView) support.getComponent();
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
                chartView.addMeasure(support.getDropLocation().getDropPoint(), listModel.get(idxMeasure));
                ((DataValueModel) tableCursorValues.getModel()).addElement(measureName);

                return true;
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

    }

    private final void measureSelection() {
        final int idxWindow = tabbedPane.getSelectedIndex();

        if (idxWindow < 0) {
            return;
        }

        ChartView chartView2 = (ChartView) tabbedPane.getComponentAt(idxWindow);
        chartView2.addPlot(log.getTime(), listVoie.getSelectedValue());
        ((DataValueModel) tableCursorValues.getModel()).addElement(listVoie.getSelectedValue().getName());

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
        
        final List<Double> temps = log.getTime().getData();
        final int nbPoint = temps.size();

        for (int n = 0; n < nbTab; n++) {
            chartView = (ChartView) tabbedPane.getComponentAt(n);
            for (Object plot : chartView.getPlot().getSubplots()) {
                xyPlot = (XYPlot) plot;
                int nbSerie = xyPlot.getSeriesCount();

                for (int nSerie = 0; nSerie < nbSerie; nSerie++) {
                    serie = ((XYSeriesCollection) xyPlot.getDataset()).getSeries(nSerie);
                    serie.clear();

                    key = serie.getKey();

                    int idxMeasure = log.getMeasures().indexOf(new Measure(key.toString()));

                    if (idxMeasure > -1) {

                        measure = log.getMeasures().get(idxMeasure);
                        
                        final int sizeData = measure.getData().size();

                        for (int n1 = 0; n1 < nbPoint; n1++) {

                            if (n1 < sizeData) {
                                serie.add(temps.get(n1), measure.getData().get(n1), false);
                            }
                        }
                    }
                    
                    serie.fireSeriesChanged();
                }
                
                //xyPlot.configureRangeAxes();
                
                
            }
            chartView.getPlot().configureDomainAxes();
        }
    }

    private final void saveConfig(File file) {

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            int nbTab = tabbedPane.getTabCount();
            Map<String, JFreeChart> listChart = new LinkedHashMap<String, JFreeChart>(nbTab);

            for (int i = 0; i < nbTab; i++) {
                JFreeChart chart = ((ChartView) tabbedPane.getComponentAt(i)).getChart();
                @SuppressWarnings("unchecked")
                List<XYPlot> subPlots = ((CombinedDomainXYPlot) chart.getXYPlot()).getSubplots();
                for (XYPlot subplot : subPlots) {
                    int nbSerie = subplot.getDataset().getSeriesCount();
                    for (int j = 0; j < nbSerie; j++) {
                        ((XYSeriesCollection) subplot.getDataset()).getSeries(j).clear();
                    }
                }
                listChart.put(tabbedPane.getTitleAt(i), chart);
            }

            oos.writeObject(listChart);
            oos.flush();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            reloadLogData(log);
        }

    }

    private final void openConfig(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            Map<String, JFreeChart> map = (LinkedHashMap<String, JFreeChart>) ois.readObject();

            for (Entry<String, JFreeChart> entry : map.entrySet()) {
                ChartView chartView = new ChartView(entry.getValue());
                chartView.addObservateur(tableCursorValues);
                chartView.setTransferHandler(new MeasureHandler());
                tabbedPane.addTab(entry.getKey(), chartView);
                tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ButtonTabComponent(tabbedPane));
                tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            }

            reloadLogData(log);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

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
