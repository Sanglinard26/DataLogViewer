package utils;

import java.io.File;

import javax.swing.JFileChooser;

public abstract class Utilitaire {

    public final static String XML = "xml";
    public final static String LAB = "lab";
    public final static String A2L = "a2l";
    public final static String DCM = "dcm";
    public final static String M = "m";
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

    public static final Object getStorageObject(Object o) {

        Double doubleValue;

        try {
            doubleValue = Double.parseDouble(o.toString().replace(",", "."));
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
        } catch (Exception e) {
            return o;
        }
    }

    public static final Number getNumberObject(Object o) {

        Double doubleValue;

        try {
            doubleValue = Double.parseDouble(o.toString().replace(",", "."));
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
        } catch (Exception e) {
            return Double.NaN;
        }
    }

}
