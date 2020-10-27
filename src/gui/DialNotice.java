/*
 * Creation : 9 mars 2017
 */
package gui;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public final class DialNotice extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String FENETRE_ICON = "/icon_manual_16.png";
    private static final String NOTICE = "/notice.txt";

    private final JEditorPane txtNotice = new JEditorPane();

    public DialNotice(JFrame ihm) {
        super(ihm, true);
        this.setTitle("Notice");
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(FENETRE_ICON)));

        StringBuilder content = new StringBuilder();

        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(getClass().getResource(NOTICE).getFile()))) {
            while ((line = br.readLine()) != null) {
                content.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        txtNotice.setEditable(false);
        txtNotice.setText(content.toString());

        add(new JScrollPane(txtNotice));

        this.setSize(800, 400);
        this.setLocationRelativeTo(ihm);
        this.setVisible(true);
    }
}
