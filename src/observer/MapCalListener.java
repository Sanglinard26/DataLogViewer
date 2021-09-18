/*
 * Creation : 15 sept. 2021
 */
package observer;

import java.util.EventListener;

public interface MapCalListener extends EventListener {

    void MapCalChanged(MapCalEvent arg);

}
