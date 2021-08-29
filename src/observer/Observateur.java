/*
 * Creation : 29 août 2018
 */
package observer;

import java.util.HashMap;

public interface Observateur {

    public void updateValues(HashMap<String, Double> tableValue);

    public void updateData(String type, Object object);

}
