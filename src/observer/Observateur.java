/*
 * Creation : 29 août 2018
 */
package observer;

import java.util.HashMap;

public interface Observateur {

    public void update(HashMap<String, Double> tableValue);

}
