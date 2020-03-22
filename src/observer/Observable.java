/*
 * Creation : 29 août 2018
 */
package observer;

import java.util.HashMap;

public interface Observable {

    public void addObservateur(Observateur obs);

    public void updateObservateur(HashMap<String, Double> tableValue);

    public void delObservateur();

}
