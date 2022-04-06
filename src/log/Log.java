/*
 * Creation : 14 févr. 2020
 */
package log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import utils.Utilitaire;

public final class Log {

    private String fnr;
    private String name;
    private List<Measure> datas;
    private int nbPoints = 0;
    private String timeName = "";

    public Log(File file) {
        if (file != null) {

            this.name = file.getName().substring(0, file.getName().length() - 4);
            String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length());

            switch (extension) {
            case "txt":
                timeName = "Time_ms";
                long start = System.currentTimeMillis();
                parseTxt(file);
                System.out.println("log : " + (System.currentTimeMillis() - start) + "ms");
                break;
            case "msl":
                timeName = "Time";
                parseMsl(file);
                break;
            default:
                break;
            }

            Collections.sort(datas);
        }
    }

    private final void parseTxt(File file) {

        final String tabDelimiter = "\t";
        final String commaDelimiter = ",";
        String delimiter = "";

        try (final BufferedReader bf = new BufferedReader(new FileReader(file))) {

            String line;
            String parsedValue;
            String[] splitTab;
            Number value;

            int cntLine = 0;

            while ((line = bf.readLine()) != null) {

                if (cntLine == 1) {
                    if (line.indexOf(commaDelimiter) > -1) {
                        delimiter = commaDelimiter;
                    } else {
                        delimiter = tabDelimiter;
                    }
                }

                splitTab = line.split(delimiter);

                switch (cntLine) {
                case 0:
                    this.fnr = line.trim();
                    break;
                case 1:
                    this.datas = new ArrayList<Measure>(splitTab.length);

                    for (int idxCol = 0; idxCol < splitTab.length; idxCol++) {
                        if (idxCol == 0) {
                            this.timeName = splitTab[idxCol];
                        }
                        this.datas.add(new Measure(splitTab[idxCol]));
                    }

                    break;
                default:
                    if (splitTab.length == this.datas.size()) {
                        for (int idxCol = 0; idxCol < this.datas.size(); idxCol++) {

                            parsedValue = splitTab[idxCol];

                            value = Utilitaire.getNumberObject(parsedValue.trim());

                            this.datas.get(idxCol).addPoint(value);
                        }

                        this.nbPoints++;
                    }
                    break;
                }

                cntLine++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final void parseMsl(File file) {

        final String TAB = "\t";

        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {

            String line;
            String parsedValue;
            String[] splitTab;

            int cntLine = 0;

            while ((line = bf.readLine()) != null) {

                splitTab = line.split(TAB);

                switch (cntLine) {
                case 0:
                    if (splitTab.length > 0) {
                        this.fnr = splitTab[0].replaceAll("\"", "");
                    }
                    break;
                case 2:
                    this.datas = new ArrayList<Measure>(splitTab.length);

                    for (String nameMeasure : splitTab) {
                        this.datas.add(new Measure(nameMeasure));
                    }
                    break;
                case 3:
                    if (splitTab.length == datas.size()) {
                        for (int idxCol = 0; idxCol < splitTab.length; idxCol++) {
                            this.datas.get(idxCol).setUnit(splitTab[idxCol]);
                        }
                    }
                    break;
                default:
                    if (cntLine > 3 && splitTab.length == this.datas.size()) {

                        for (int idxCol = 0; idxCol < this.datas.size(); idxCol++) {

                            parsedValue = splitTab[idxCol];

                            if (parsedValue.indexOf(",") > -1) {
                                parsedValue = splitTab[idxCol].replace(',', '.');
                            }

                            try {
                                double value = Double.parseDouble(parsedValue.trim());
                                this.datas.get(idxCol).addPoint(value);
                            } catch (NumberFormatException e) {
                                this.datas.get(idxCol).getData().add(Double.NaN);
                            }
                        }
                    }
                    break;
                }

                cntLine++;
            }

            for (Measure measure : this.datas) {
                this.nbPoints = Math.max(this.nbPoints, measure.getData().size());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final List<Measure> getMeasures() {
        return this.datas;
    }

    public final String getName() {
        return name;
    }

    public final String getFnr() {
        return this.fnr;
    }

    public final Measure getTime() {
        Measure time = new Measure(timeName);
        int idx = datas.indexOf(time);
        return idx > -1 ? datas.get(idx) : time;
    }

    public final Measure getMeasure(String name) {
        final Measure measure = new Measure(name);
        final int idx = this.datas.indexOf(measure);

        return idx > -1 ? this.datas.get(idx) : measure;
    }

    public final Measure getMeasureWoutUnit(String nameWoutUnit) {

        String measureWoutUnit;

        for (Measure measure : datas) {
            measureWoutUnit = measure.getName();
            int idx = measureWoutUnit.indexOf('(');
            if (idx > -1) {
                measureWoutUnit = measureWoutUnit.substring(0, idx);
            }

            if (measureWoutUnit.equals(nameWoutUnit)) {
                return getMeasure(measure.getName());
            }
        }

        return new Measure(nameWoutUnit);

    }
}
