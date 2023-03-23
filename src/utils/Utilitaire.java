package utils;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public abstract class Utilitaire {

    public final static String XML = "xml";
    public final static String A2L = "a2l";
    public final static String CDFX = "cdfx";

    /*
     * Get the extension of a file.
     */
    public static final String getExtension(File f) {

        final String s = f.getName();

        final int i = s.lastIndexOf('.');

        return (i > 0 && i < s.length() - 1) ? s.substring(i + 1).toLowerCase() : "";
    }

    public static final String getFileNameWithoutExtension(File f) {

        final String fileNameWithExtension = f.getName();

        final int i = fileNameWithExtension.lastIndexOf('.');

        return (i > 0 && i < fileNameWithExtension.length() - 1) ? fileNameWithExtension.substring(0, i) : "";
    }

    public static final String getFolder(String title, String defautPath) {
        final JFileChooser fileChooser = new JFileChooser(defautPath);
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        final int reponse = fileChooser.showDialog(null, "Select");
        if (reponse == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getPath();
        }
        return defautPath;
    }

    public static final int countChar(String text, String aChar) {
        return text.length() - text.replace(aChar, "").length();
    }

    public static final Object getStorageObject(Object o) {

        Double doubleValue;

        String objectString = o.toString();

        final int idx = objectString.indexOf(',');

        try {
            if (idx == -1) {
                doubleValue = Double.parseDouble(objectString);
            } else {
                doubleValue = Double.parseDouble(objectString.replace(",", "."));
            }

            int i = doubleValue.intValue();
            if (doubleValue - i != 0) {
                return doubleValue;
            } else if (i <= Byte.MAX_VALUE && i >= Byte.MIN_VALUE) {
                return (byte) i;
            } else if (i <= Short.MAX_VALUE && i >= Short.MIN_VALUE) {
                return (short) i;
            } else {
                return i;
            }
        } catch (NumberFormatException e) {
            return o;
        }
    }

    public static final double getDoubleFromString(String number) {
        final int idx = number.indexOf(',');
        try {
            if (idx == -1) {
                return Double.parseDouble(number);
            }
            return Double.parseDouble(number.replace(',', '.'));
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    public static final float getFloatFromString(String number) {
        final int idx = number.indexOf(',');
        try {
            if (idx == -1) {
                return Float.parseFloat(number);
            }
            return Float.parseFloat(number.replace(',', '.'));
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    public static final Number getNumberObject(String number) {

        Double doubleValue;

        // String number = o.toString();

        final int idx = number.indexOf(',');

        try {
            if (idx == -1) {
                doubleValue = Double.parseDouble(number);
            } else {
                doubleValue = Double.parseDouble(number.replace(',', '.'));
            }

            int i = doubleValue.intValue();
            if (doubleValue - i != 0) {
                return doubleValue;
            } else if (i <= Byte.MAX_VALUE && i >= Byte.MIN_VALUE) {
                return (byte) i;
            } else if (i <= Short.MAX_VALUE && i >= Short.MIN_VALUE) {
                return (short) i;
            } else {
                return i;
            }
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    public static Color parseRGBColor(String stringColor, int alpha) {
        Pattern pattern = Pattern.compile("[0-9]{1,3}");

        List<String> listWord = new ArrayList<>();

        final Matcher regexMatcher = pattern.matcher(stringColor);

        while (regexMatcher.find()) {
            listWord.add(regexMatcher.group());
        }

        if (listWord.size() == 3) {
            int r = Integer.parseInt(listWord.get(0));
            int g = Integer.parseInt(listWord.get(1));
            int b = Integer.parseInt(listWord.get(2));

            return new Color(r, g, b, alpha);
        }

        return Color.BLACK;
    }

    public final static void adjustTableCells(JTable table) {

        final TableColumnModel columnModel = table.getColumnModel();
        final int nbCol = columnModel.getColumnCount();
        final int nbRow = table.getRowCount();
        int maxWidth = 10;
        TableCellRenderer cellRenderer;
        Object value;
        Component component;
        Component componentHeader;
        TableColumn column;

        for (short col = 0; col < nbCol; col++) {
            maxWidth = 0;
            for (short row = 0; row < nbRow; row++) {
                cellRenderer = table.getCellRenderer(row, col);
                value = table.getValueAt(row, col);

                if (value == null || value.toString().isEmpty()) {
                    value = 99999; // Une valeur par défaut pour ne pas avoir une case trop petite
                }

                component = cellRenderer.getTableCellRendererComponent(table, value, false, false, row, col);
                ((JLabel) component).setHorizontalAlignment(SwingConstants.CENTER);
                maxWidth = Math.max(((JLabel) component).getPreferredSize().width, maxWidth);

                if (table.getTableHeader() != null) {
                    componentHeader = table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table,
                            columnModel.getColumn(col).getHeaderValue(), false, false, 0, col);
                    maxWidth = Math.max(((JLabel) componentHeader).getPreferredSize().width, maxWidth);
                }
            }
        }

        for (short col = 0; col < nbCol; col++) {
            column = columnModel.getColumn(col);
            column.setPreferredWidth(maxWidth + 15);
        }

    }

    public final static void adjustTableCells(JTable tableRef, JTable tableChild) {

        final TableColumnModel columnModel = tableRef.getColumnModel();
        final TableColumnModel columnModelChild = tableChild.getColumnModel();
        final int nbCol = columnModel.getColumnCount();
        final int nbRow = tableRef.getRowCount();
        int maxWidth = 10;
        TableCellRenderer cellRenderer;
        Object value;
        Component component;
        Component componentHeader;
        TableColumn column;
        TableColumn columnChild;

        for (short col = 0; col < nbCol; col++) {
            maxWidth = 0;
            for (short row = 0; row < nbRow; row++) {

                cellRenderer = tableRef.getCellRenderer(row, col);
                value = tableRef.getValueAt(row, col);
                component = cellRenderer.getTableCellRendererComponent(tableRef, value, false, false, row, col);
                ((JLabel) component).setHorizontalAlignment(SwingConstants.CENTER);

                cellRenderer = tableChild.getCellRenderer(row, col);
                value = tableChild.getValueAt(row, col);
                component = cellRenderer.getTableCellRendererComponent(tableChild, value, false, false, row, col);
                ((JLabel) component).setHorizontalAlignment(SwingConstants.CENTER);

                maxWidth = Math.max(((JLabel) component).getPreferredSize().width, maxWidth);

                if (tableRef.getTableHeader() != null) {
                    componentHeader = tableRef.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(tableRef,
                            columnModel.getColumn(col).getHeaderValue(), false, false, 0, col);
                    maxWidth = Math.max(((JLabel) componentHeader).getPreferredSize().width, maxWidth);
                }
            }
        }

        maxWidth = Math.max(maxWidth, 30);

        for (short col = 0; col < nbCol; col++) {
            column = columnModel.getColumn(col);
            columnChild = columnModelChild.getColumn(col);
            column.setPreferredWidth(maxWidth + 15);
            columnChild.setPreferredWidth(maxWidth + 15);
        }

    }

}
