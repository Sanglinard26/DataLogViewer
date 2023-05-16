/*
 * Creation : 14 févr. 2020
 */
package log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import utils.Utilitaire;

public final class Log {

    private String fnr;
    private String name;
    private List<Measure> datas;
    private int nbPoints = 0;
    private String timeName = "";
    private float te;

    public Log(File file) {

        // long start = System.currentTimeMillis();

        if (file != null) {

            this.name = file.getName().substring(0, file.getName().length() - 4);
            String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length());

            switch (extension) {
            case "txt":
                parseTxt(file);
                break;
            case "csv":
                parseCsv(file);
                break;
            case "msl":
                parseMsl(file);
                break;
            default:
                break;
            }

            Collections.sort(datas);
        }
        // System.out.println("Nb points = " + nbPoints);

        // System.out.println("log opened in : " + (System.currentTimeMillis() - start) + "ms");
    }

    private final void parseMsl(File file) {

        final String TAB = "\t";

        int nbLines = 0;

        Stream<String> lines;
        try {
            lines = Files.lines(file.toPath(), StandardCharsets.ISO_8859_1);
            nbLines = (int) (lines.count() - 4);
            lines.close();
        } catch (IOException e1) {
        }

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

                    for (int idxCol = 0; idxCol < splitTab.length; idxCol++) {
                        if (idxCol == 0) {
                            this.timeName = splitTab[idxCol];
                        }
                        this.datas.add(new Measure(splitTab[idxCol], nbLines));
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
                                this.datas.get(idxCol).addPoint(Double.NaN);
                            }
                        }
                    }
                    break;
                }

                cntLine++;
            }

            for (Measure measure : this.datas) {
                this.nbPoints = Math.max(this.nbPoints, measure.getDataLength());
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

    public int getNbPoints() {
        return nbPoints;
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

    private final void parseTxt(File file) {

        final String tabDelimiter = "\t";
        final String commaDelimiter = ",";
        String delimiter = "";

        int nbLines = 0;

        Stream<String> lines;
        try {
            lines = Files.lines(file.toPath(), StandardCharsets.ISO_8859_1);
            nbLines = (int) (lines.count() - 2);
            lines.close();
        } catch (IOException e1) {
        }

        // System.out.println("Nb lines = " + nbLines);

        try (final BufferedReader bf = new BufferedReader(new FileReader(file))) {

            String line;
            String[] splitTab;
            double value = 0;
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
                        this.datas.add(new Measure(splitTab[idxCol], nbLines));
                    }

                    break;
                default:
                    if (splitTab.length == this.datas.size()) {
                        for (int idxCol = 0; idxCol < this.datas.size(); idxCol++) {
                            value = Utilitaire.getDoubleFromString(splitTab[idxCol]);
                            this.datas.get(idxCol).addPoint(value);
                        }
                        this.nbPoints++;
                    } else {
                        // System.out.println("erreur ligne " + this.nbPoints);
                    }

                    break;
                }
                cntLine++;
            }

            /*
             * final Measure time = this.getTime();
             * te = 0;
             * 
             * for (int t = 0; t < nbPoints; t++) {
             * if (t > 0) {
             * double dT = time.get(t) - time.get(t - 1);
             * te += dT;
             * }
             * }
             * 
             * te /= nbPoints;
             * System.out.println("te = " + te + "s");
             * 
             * System.out.println(time.get(nbPoints - 1) / nbPoints);
             */

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final void parseCsv(File file) {

        final String tabDelimiter = "\t";
        final String commaDelimiter = ",";
        String delimiter = "";

        int nbLines = 0;

        Stream<String> lines;
        try {
            lines = Files.lines(file.toPath(), StandardCharsets.ISO_8859_1);
            nbLines = (int) (lines.count() - 1);
            lines.close();
        } catch (IOException e1) {
        }

        // System.out.println("Nb lines = " + nbLines);

        try (final BufferedReader bf = new BufferedReader(new FileReader(file))) {

            String line;
            String[] splitTab;
            double value = 0;
            int cntLine = 0;

            while ((line = bf.readLine()) != null) {

                if (cntLine == 0) {
                    if (line.indexOf(commaDelimiter) > -1) {
                        delimiter = commaDelimiter;
                    } else {
                        delimiter = tabDelimiter;
                    }
                }

                splitTab = line.split(delimiter);

                switch (cntLine) {
                case 0:
                    this.datas = new ArrayList<Measure>(splitTab.length);

                    for (int idxCol = 0; idxCol < splitTab.length; idxCol++) {
                        if (idxCol == 0) {
                            this.timeName = splitTab[idxCol];
                        }
                        this.datas.add(new Measure(splitTab[idxCol], nbLines));
                    }

                    break;
                default:
                    if (splitTab.length == this.datas.size()) {
                        for (int idxCol = 0; idxCol < this.datas.size(); idxCol++) {
                            value = Utilitaire.getDoubleFromString(splitTab[idxCol]);
                            this.datas.get(idxCol).addPoint(value);
                        }
                        this.nbPoints++;
                    } else {
                        // System.out.println("erreur ligne " + this.nbPoints);
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
}
