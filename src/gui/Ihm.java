package gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
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

    List<JDesktopPane> desktopPanes;
    JTabbedPane tabbedPane;
    DefaultListModel<Measure> listModel;
    JList<Measure> listVoie;
    NumberAxis timeAxis;

    private static Log log;

    public Ihm() {
        super("PcsLogViewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        createGUI();

        pack();
        setMinimumSize(new Dimension(getWidth(), getHeight()));

        setVisible(true);
    }

    private final void createGUI() {

        final GridBagConstraints gbc = new GridBagConstraints();

        listModel = new DefaultListModel<Measure>();

        final Container root = getContentPane();

        root.setLayout(new GridBagLayout());

        final JButton btOpen = new JButton("Ouvrir Log");
        btOpen.setToolTipText("Ouvrir fichiers AIF");
        btOpen.addActionListener(new OpenLog());
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(btOpen, gbc);

        final JButton btAddWindow = new JButton("Ajouter fenêtre");
        btAddWindow.addActionListener(new AddWindow());
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(btAddWindow, gbc);

        final JButton btArrangeWindow = new JButton("Arranger fenêtre");
        btArrangeWindow.addActionListener(new AddWindow());
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(btArrangeWindow, gbc);

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
            tabbedPane.addTab("Fenêtre n°" + tabbedPane.getTabCount(), desktopPane);
            desktopPanes.add(desktopPane);
        }

    }

    private final class MeasureSelection implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {

            if (timeAxis == null) {
                timeAxis = new NumberAxis("Time");
            }

            ChartView chartView = new ChartView(timeAxis, log.getTime(), listVoie.getSelectedValue());

            int idxWindow = tabbedPane.getSelectedIndex();
            if (idxWindow > 0) {
                desktopPanes.get(idxWindow - 1).add(chartView);
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
