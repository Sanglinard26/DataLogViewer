package utils;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

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

        String number = o.toString();

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
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    public static final double applyResolution(double rawValue, double resol) {
        double roundValue = Math.rint(rawValue / resol);
        return roundValue * resol;
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

}
