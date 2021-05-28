/*
 * Creation : 10 janv. 2017
 */
package utils;

import java.util.prefs.Preferences;

public abstract class Preference {

    private static final String DEF_PATH_LOG = null;
    private static final String DEF_PATH_CONFIG = null;

    public static final String KEY_LOG = "logPath";
    public static final String KEY_CONFIG = "configPath";
    public static final String KEY_CAL = "calPath";

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
