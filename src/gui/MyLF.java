/*
 * Creation : 1 juin 2022
 */
package gui;

import java.awt.Color;

import javax.swing.UIDefaults;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public final class MyLF extends NimbusLookAndFeel {

    private static final long serialVersionUID = 1L;

    public MyLF() {
        super();

        UIDefaults ui = getDefaults();
        ui.put("nimbusBase", Color.GRAY);
        ui.put("control", Color.GRAY);
        ui.put("controlText", Color.GRAY);
        ui.put("nimbusFocus", Color.ORANGE);
        ui.put("info", Color.ORANGE);
        ui.put("textForeground", Color.BLACK);
        ui.put("nimbusBorder", Color.BLACK);
        ui.put("nimbusSelectionBackground", Color.YELLOW.brighter());
        ui.put("textBackground", Color.YELLOW.brighter());
        ui.put("nimbusSelection", Color.YELLOW.brighter());
        ui.put("nimbusLightBackground", Color.LIGHT_GRAY);
        ui.put("MenuItem[MouseOver].textForeground", Color.BLACK);
        ui.put("ToggleButton[Disabled].textForeground", Color.RED);

    }

    @Override
    public String getName() {
        return "MyLF";
    }

    @Override
    public String getDescription() {
        return "Custom L&F based on Nimbus";
    }

}
