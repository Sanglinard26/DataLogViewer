/*
 * Creation : 9 mars 2017
 */
package dialog;

import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public final class DialNews extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String FENETRE_ICON = "/new_icon_16.png";
    private static final String CHANGELOG = "/news.html";

    private final JEditorPane txtNews = new JEditorPane();

    public DialNews(JFrame ihm) {
        super(ihm, true);
        this.setTitle("Nouveautes");
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(FENETRE_ICON)));

        URL urlNews = DialNews.class.getResource(CHANGELOG);

        txtNews.setEditable(false);

        try {
            txtNews.setPage(urlNews);
        } catch (IOException e) {
            e.printStackTrace();
        }

        add(new JScrollPane(txtNews));

        this.setSize(600, 600);
        this.setLocationRelativeTo(ihm);
        this.setVisible(true);
    }
}
