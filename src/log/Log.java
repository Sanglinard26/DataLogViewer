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
import java.util.List;

public final class Log {

    private String name;
    private List<Measure> datas;
    private int nbPoints = 0;

    public Log(File file) {
        if (file != null) {

            this.name = file.getName().substring(0, file.getName().length() - 4);

            parse(file);
        }
    }

    private final void parse(File file) {

        final String TAB = "\t";

        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {

            String line;
            String parsedValue;
            String[] splitTab;

            int cntLine = 0;

            while ((line = bf.readLine()) != null) {

                splitTab = line.split(TAB);

                if (cntLine > 1) {
                    if (splitTab.length == this.datas.size()) {

                        for (int idxCol = 0; idxCol < splitTab.length; idxCol++) {
                            parsedValue = splitTab[idxCol].trim().replace(',', '.');
                            try {
                                this.datas.get(idxCol).getData().add(Double.parseDouble(parsedValue));
                            } catch (NumberFormatException e) {
                                this.datas.get(idxCol).getData().add(Double.NaN);
                            }
                        }
                    }
                } else {
                    this.datas = new ArrayList<Measure>(splitTab.length);

                    for (String nameMeasure : splitTab) {
                        this.datas.add(new Measure(nameMeasure));
                    }
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

    public final Measure getTime() {
        Measure time = new Measure("Time_ms");
        int idx = datas.indexOf(time);
        return idx > -1 ? datas.get(idx) : time;
    }

}
