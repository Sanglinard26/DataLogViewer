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
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

public final class DialManual extends JDialog implements HyperlinkListener {

    private static final long serialVersionUID = 1L;
    private static final String FENETRE_ICON = "/icon_manual_16.png";
    private static final String MANUAL = "/manual.html";

    private final JEditorPane txtManual = new JEditorPane();

    public DialManual(JFrame ihm) {
        super(ihm, true);
        this.setTitle("Aide");
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setModal(false);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(FENETRE_ICON)));

        URL urlNews = DialManual.class.getResource(MANUAL);

        txtManual.setEditable(false);

        try {
            txtManual.setPage(urlNews);
        } catch (IOException e) {
            e.printStackTrace();
        }

        txtManual.addHyperlinkListener(this);

        add(new JScrollPane(txtManual));

        this.setSize(1400, 800);
        this.setLocationRelativeTo(ihm);
        this.setVisible(true);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {

        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

            if (event instanceof HTMLFrameHyperlinkEvent) {
                HTMLDocument doc = (HTMLDocument) txtManual.getDocument();
                HTMLFrameHyperlinkEvent frameEvent = (HTMLFrameHyperlinkEvent) event;
                doc.processHTMLFrameHyperlinkEvent(frameEvent);
            }

        }

    }
}
