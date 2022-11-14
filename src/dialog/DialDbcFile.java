/*
 * Creation : 11 juin 2022
 */
package dialog;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import calib.MdbData;
import calib.MdbWorkspace;
import utils.ExportUtils;

public final class DialDbcFile extends JDialog {

    private static final long serialVersionUID = 1L;

    public DialDbcFile(Frame owner) {
        super(owner, false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        final GridBagConstraints gbc = new GridBagConstraints();

        setLayout(new GridBagLayout());

        JTextField txtMdbCal = new JTextField(60);
        txtMdbCal.setText("C:\\User\\U354706\\Perso\\Clio\\Calib\\ClioRS1_7_65_BAAClioV6_FlexFuel_InjMRS2_Richesse1.13_VVTOFF@5800rpm_220430mdb.mdb");
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        add(txtMdbCal, gbc);

        JButton btMdbCal = new JButton("Selection Mdb calibration");
        btMdbCal.addActionListener(new FileSelection(txtMdbCal));
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        add(btMdbCal, gbc);

        JTextField txtMdbWorkspace = new JTextField(60);
        txtMdbWorkspace.setText("C:\\User\\U354706\\Perso\\Clio\\Ecran_ProLog_stepper.mdb");
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        add(txtMdbWorkspace, gbc);

        JButton btMdbWorkspace = new JButton("Selection Mdb workspace");
        btMdbWorkspace.addActionListener(new FileSelection(txtMdbWorkspace));
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        add(btMdbWorkspace, gbc);

        JButton btCreateDbc = new JButton("Creation fichier Dbc");
        btCreateDbc.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                MdbData mdbData = new MdbData(new File(txtMdbCal.getText()));
                MdbWorkspace mdbWorkspace = new MdbWorkspace(new File(txtMdbWorkspace.getText()));
                ExportUtils.createDbcFile(new File("C:\\TEMP\\PcsLab_Test.dbc"), mdbData, mdbWorkspace);

            }
        });
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        add(btCreateDbc, gbc);

        pack();
        setLocationRelativeTo(owner);
        setResizable(true);
        setVisible(true);

    }

    private final class FileSelection implements ActionListener {
        private JTextField txtField;

        public FileSelection(JTextField tf) {
            this.txtField = tf;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(false);
            fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
            fc.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {
                    return "Fichier mdb (*.mdb)";
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
                this.txtField.setText(fc.getSelectedFile().getAbsolutePath());
            }

        }
    }

}
