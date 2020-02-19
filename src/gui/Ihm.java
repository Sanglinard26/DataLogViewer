package gui;

import java.awt.Color;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JDesktopPane;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.jfree.chart.axis.NumberAxis;

import log.Log;
import log.Measure;

/*
 * Creation : 3 mai 2018
 */

public final class Ihm extends JFrame {

    private static final long serialVersionUID = 1L;

    private List<JDesktopPane> desktopPanes;
    private JTabbedPane tabbedPane;
    private DefaultListModel<Measure> listModel;
    private JList<Measure> listVoie;
    private NumberAxis timeAxis;

    private static Log log;

    public Ihm() {
        super("PcsLogViewer");
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

        menuItem = new JMenuItem("Réorganiser");
        menuItem.addActionListener(new ArrangeWindow());
        menu.add(menuItem);

        return menuBar;
    }

    private final void createGUI() {

        final GridBagConstraints gbc = new GridBagConstraints();

        listModel = new DefaultListModel<Measure>();

        final Container root = getContentPane();

        root.setLayout(new GridBagLayout());

        listVoie = new JList<Measure>();
        listVoie.addListSelectionListener(new MeasureSelection());
        listVoie.setModel(listModel);
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
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
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(tabbedPane, gbc);

        tabbedPane.addTab("Introduction", new JLabel("<html>Double clicker sur un label pour le tracer"));

        desktopPanes = new ArrayList<JDesktopPane>();
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
            JDesktopPane desktopPane = new JDesktopPane();
            desktopPane.setBackground(Color.LIGHT_GRAY);
            tabbedPane.addTab("Fenêtre n°" + tabbedPane.getTabCount(), desktopPane);
            desktopPanes.add(desktopPane);
        }

    }

    private final class ArrangeWindow implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int idxSelTab = tabbedPane.getSelectedIndex();
            if (idxSelTab <= 0) {
                return;
            }
            JDesktopPane desktopPane = (JDesktopPane) tabbedPane.getComponentAt(idxSelTab);
            int nbWin = desktopPane.getComponentCount();

            int winWidth = desktopPane.getWidth();
            int winHeight = desktopPane.getHeight() / 4;

            ChartView chartView;
            for (int n = 0; n < nbWin; n++) {
                chartView = (ChartView) desktopPane.getComponent(n);
                chartView.setSize(winWidth, winHeight);
                chartView.setLocation(0, (n - (n / nbWin) * nbWin) * chartView.getHeight());
            }
        }

    }

    private final class MeasureHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
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
                chartView.addMeasure(listModel.get(idxMeasure));

                return true;
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }
    }

    private final class MeasureSelection implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {

            if (timeAxis == null) {
                timeAxis = new NumberAxis("Time");
            }

            int idxWindow = tabbedPane.getSelectedIndex();
            if (idxWindow > 0) {
                JDesktopPane desktopPane = desktopPanes.get(idxWindow - 1);

                int winWidth = desktopPane.getWidth() / 2;
                int winHeight = desktopPane.getHeight() / 2;

                int nbWin = desktopPane.getComponentCount();
                if (nbWin > 3) {
                    JOptionPane.showMessageDialog(Ihm.this, "Quatre graphiques maxi par feuille");
                    return;
                }
                Dimension dim = new Dimension(winWidth, winHeight);
                ChartView chartView = new ChartView(Integer.toString(nbWin + 1), dim, timeAxis, log.getTime(), listVoie.getSelectedValue());
                chartView.setTransferHandler(new MeasureHandler());
                chartView.setLocation((nbWin - (nbWin / 2) * 2) * chartView.getWidth(), (nbWin / 2) * chartView.getHeight());
                desktopPane.add(chartView);
            }

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
