/*
 * Creation : 30 oct. 2020
 */
package calib;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class MapCal {

    private final String name;
    private final List<Variable> listVariable;
    private MdbData mdbData;

    public MapCal(File mapFile) {
        this.name = mapFile.getName().replace(".map", "");
        this.listVariable = new ArrayList<Variable>();
        mdbData = new MdbData(new File(mapFile.getAbsolutePath().replace(".map", ".mdb")));
        parseFile(mapFile);
    }

    private final void parseFile(File mapFile) {

        final char crochet = '[';

        try {
            List<String> fileToList = Files.readAllLines(mapFile.toPath(), Charset.forName("ISO-8859-1"));
            String line;

            for (int nLigne = 0; nLigne < fileToList.size(); nLigne++) {
                line = fileToList.get(nLigne);

                if (line.charAt(0) == crochet) {
                    final int begin = nLigne;

                    do {
                        nLigne++;
                        if (nLigne < fileToList.size() && !fileToList.get(nLigne).isEmpty() && fileToList.get(nLigne).charAt(0) == crochet) {
                            break;
                        }

                    } while (nLigne < fileToList.size());

                    listVariable.add(new Variable(fileToList.subList(begin, nLigne), mdbData));
                    nLigne--;

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getName() {
        return name;
    }

    public List<Variable> getListVariable() {
        return listVariable;
    }

}
