/*
 * Creation : 15 sept. 2021
 */
package observer;

import java.util.EventObject;

public class MapCalEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public MapCalEvent(Object source) {
        super(source);
    }

}
