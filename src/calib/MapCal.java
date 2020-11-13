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

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.VerticalAlignment;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

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

    public static final boolean toExcel(List<Variable> listVariable, final File file) {

        WritableWorkbook workbook = null;

        try {
            workbook = Workbook.createWorkbook(file);

            final WritableFont arial10Bold = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
            final WritableCellFormat arial10format = new WritableCellFormat(arial10Bold);

            final WritableCellFormat borderFormat = new WritableCellFormat();
            borderFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
            borderFormat.setAlignment(Alignment.CENTRE);
            borderFormat.setVerticalAlignment(VerticalAlignment.CENTRE);

            final WritableCellFormat borderBoldFormat = new WritableCellFormat(arial10Bold);
            borderBoldFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
            borderBoldFormat.setAlignment(Alignment.CENTRE);
            borderBoldFormat.setVerticalAlignment(VerticalAlignment.CENTRE);

            final WritableCellFormat axisFormat = new WritableCellFormat(arial10Bold);
            axisFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
            axisFormat.setBackground(Colour.VERY_LIGHT_YELLOW);
            axisFormat.setAlignment(Alignment.CENTRE);
            axisFormat.setVerticalAlignment(VerticalAlignment.CENTRE);

            // Entete de la feuille Valeurs
            final WritableSheet sheetValues = workbook.createSheet("Valeurs", 0);
            sheetValues.getSettings().setShowGridLines(false);
            //

            int row = 0; // Point de depart, une ligne d'espace par rapport au titre

            WritableCellFormat centerAlignYellow = new WritableCellFormat();
            centerAlignYellow.setAlignment(Alignment.CENTRE);
            centerAlignYellow.setVerticalAlignment(VerticalAlignment.CENTRE);
            centerAlignYellow.setBackground(Colour.VERY_LIGHT_YELLOW);

            String variableName;

            for (Variable var : listVariable) {

                variableName = var.getName();

                writeCell(sheetValues, 0, row, variableName, arial10format);

                switch (var.getType()) {
                case "scalaire":

                    row += 1;
                    writeCell(sheetValues, 0, row, var.getValue(0, 0), borderFormat);
                    row += 2;
                    break;

                case "courbe":

                    for (byte y = 0; y < 2; y++) {
                        int col = 0;
                        row += 1;
                        for (short x = 0; x < var.getDimX(); x++) {
                            if (y == 0) {
                                writeCell(sheetValues, col, row, var.getValue(y, x), axisFormat);
                            } else {
                                writeCell(sheetValues, col, row, var.getValue(y, x), borderFormat);
                            }
                            col += 1;
                        }
                    }
                    row += 2;
                    break;

                case "carto":

                    for (short y = 0; y < var.getDimY(); y++) {
                        int col = 0;
                        row += 1;
                        for (short x = 0; x < var.getDimX(); x++) {
                            if (y == 0 | x == 0) {
                                writeCell(sheetValues, col, row, var.getValue(y, x), axisFormat);
                            } else {
                                writeCell(sheetValues, col, row, var.getValue(y, x), borderFormat);
                            }
                            col += 1;
                        }
                    }
                    row += 2;
                    break;

                default:
                    break;
                }
            }

            workbook.write();

        } catch (IOException e) {
            return false;
        } catch (RowsExceededException rowException) {
            return false;
        } catch (WriteException e) {

        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (WriteException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return true;
    }

    private static final void writeCell(WritableSheet sht, int col, int row, Object value, WritableCellFormat format)
            throws RowsExceededException, WriteException {

        if (value instanceof Number) {
            sht.addCell(new Number(col, row, Double.parseDouble(value.toString()), format));
        } else {
            sht.addCell(new Label(col, row, value.toString(), format));
        }

    }

}
