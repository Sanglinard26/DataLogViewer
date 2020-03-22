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
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import log.Log;
import log.Measure;

/*
 * Creation : 3 mai 2018
 */

public final class Ihm extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTabbedPane tabbedPane;
    private DefaultListModel<Measure> listModel;
    private JList<Measure> listVoie;
    private TableCursorValue tableCursorValues;

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
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Fichier");
        menuBar.add(menu);

        JMenuItem menuItem = new JMenuItem("Ouvrir log");
        menuItem.setMnemonic(KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new OpenLog());
        menu.add(menuItem);

        menuItem = new JMenuItem("Ouvrir configuration");
        menuItem.setMnemonic(KeyEvent.VK_C);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(Ihm.this, "Fonction pas encore implementee");
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Enregistrer configuration");
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(Ihm.this, "Fonction pas encore implementee");
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Quitter");
        menuItem.setMnemonic(KeyEvent.VK_Q);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);

            }
        });
        menu.add(menuItem);

        menu = new JMenu("Fenêtre");
        menuBar.add(menu);

        menuItem = new JMenuItem("Ajouter");
        menuItem.addActionListener(new AddWindow());
        menu.add(menuItem);

        menuItem = new JMenuItem("Supprimer");
        menu.add(menuItem);

        menu = new JMenu("Info");
        menuBar.add(menu);

        return menuBar;
    }

    private final void createGUI() {

        final GridBagConstraints gbc = new GridBagConstraints();

        listModel = new DefaultListModel<Measure>();

        final Container root = getContentPane();

        root.setLayout(new GridBagLayout());

        listVoie = new JList<Measure>();
        // listVoie.addListSelectionListener(new MeasureSelection());
        listVoie.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    measureSelection();
                }
            }
        });
        listVoie.setModel(listModel);
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
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
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1;
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
                }
            }
        });

        tableCursorValues = new TableCursorValue();
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(new JScrollPane(tableCursorValues), gbc);

    }

    private final class OpenLog implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(true);
            fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
            fc.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {
                    return "Fichier log (*.txt)";
                }

                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    return f.getName().toLowerCase().endsWith("txt");
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

            }

        }
    }

    private final class AddWindow implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            ChartView chartView = new ChartView();
            chartView.addObservateur(tableCursorValues);
            chartView.setTransferHandler(new MeasureHandler());
            tabbedPane.addTab("Fenêtre n°" + tabbedPane.getTabCount(), chartView);
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
        // if (timeAxis == null) {
        // timeAxis = new NumberAxis("Time");
        // }

        int idxWindow = tabbedPane.getSelectedIndex();

        if (idxWindow < 0) {
            return;
        }

        ChartView chartView2 = (ChartView) tabbedPane.getComponentAt(idxWindow);
        chartView2.addPlot(log.getTime(), listVoie.getSelectedValue());
        ((DataValueModel) tableCursorValues.getModel()).addElement(listVoie.getSelectedValue().getName());

    }

    private final void saveConfig() {

    }

    private final void openConfig(File file) {

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
