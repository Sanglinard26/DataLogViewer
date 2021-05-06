/*
 * Creation : 13 déc. 2020
 */
package calib;

public enum Type {

    SCALAIRE("VALUE"), COURBE("CURVE"), MAP("MAP"), ARRAY("VAL_BLK"), TEXT("VAL_BLK"), UNKNOWN("UNKNOWN");

    String name;

    private Type(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
