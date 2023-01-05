/*
 * Creation : 29 août 2018
 */
package observer;

public interface CursorObservable {

    public void addCursorObservateur(CursorObservateur obs);

    public void updateCursorObservateur(int cursorIndex);

    public void delCursorObservateur();

}
