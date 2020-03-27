/*
 * Creation : 29 août 2018
 */
package observer;

public interface Observable {

    public void addObservateur(Observateur obs);

    public void updateObservateur(String type, Object value);

    public void delObservateur();

}
