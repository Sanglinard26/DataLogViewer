/*
 * Creation : 10 janv. 2017
 */
package utils;

import java.util.prefs.Preferences;

public abstract class Preference {

    private static final String DEF_PATH_LOG = null;
    private static final String DEF_PATH_CONFIG = null;
    private static final String DEF_LF = "Windows";
    private static final String DEF_DISPO = "Onglet";

    public static final String KEY_LOG = "logPath";
    public static final String KEY_CONFIG = "configPath";
    public static final String KEY_CAL = "calPath";
    public static final String KEY_LF = "nomLF";
    public static final String KEY_DISPO = "dispoApp";

    private static final Preferences preferences = Preferences.userRoot().node("datalogviewer");

    public static final String getPreference(String key) {
        String defValue;
        switch (key) {
        case KEY_LOG:
            defValue = DEF_PATH_LOG;
            break;
        case KEY_CONFIG:
            defValue = DEF_PATH_CONFIG;
            break;
        case KEY_CAL:
            defValue = DEF_PATH_CONFIG;
            break;
        case KEY_LF:
            defValue = DEF_LF;
            break;
        case KEY_DISPO:
            defValue = DEF_DISPO;
            break;
        default:
            defValue = "";
            break;
        }
        return preferences.get(key, defValue);
    }

    public static final void setPreference(String key, String value) {
        preferences.put(key, value);
    }

}
