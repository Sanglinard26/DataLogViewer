/*
 * Creation : 11 sept. 2024
 */
package gui;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import javax.swing.JTextArea;

public class MyLogDisplay extends JTextArea {

    private static final long serialVersionUID = 1L;
    private TextAreaHandler handler;

    public MyLogDisplay() {
        setRows(3);
        setEditable(false);
        handler = new TextAreaHandler();
    }

    public TextAreaHandler getHandler() {
        return handler;
    }

    private class TextAreaHandler extends StreamHandler {

        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            MyLogDisplay.this.append(getFormatter().format(record));
        }

    }

}
