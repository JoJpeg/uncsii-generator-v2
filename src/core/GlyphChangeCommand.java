package core;
import logger.Logger;

/**
 * Command-Klasse für das Ersetzen einer Glyphe (Zeichen und Farben)
 */
public class GlyphChangeCommand implements Command {
    private ProcessingCore processingCore;
    private int gridX;
    private int gridY;
    private ResultGlyph newGlyph;
    private ResultGlyph oldGlyph;

    /**
     * Erzeugt ein neues GlyphChangeCommand
     * 
     * @param processingCore Die ProcessingCore-Instanz
     * @param gridX          X-Koordinate der zu ändernden Glyphe
     * @param gridY          Y-Koordinate der zu ändernden Glyphe
     * @param newGlyph       Die neue Glyphe
     */
    public GlyphChangeCommand(ProcessingCore processingCore, int gridX, int gridY, ResultGlyph newGlyph) {
        this.processingCore = processingCore;
        this.gridX = gridX;
        this.gridY = gridY;
        this.newGlyph = new ResultGlyph(newGlyph.codePoint, newGlyph.fgIndex, newGlyph.bgIndex);

        // Alte Glyphe sichern, falls vorhanden
        if (processingCore.resultGrid != null &&
                gridY >= 0 && gridY < processingCore.resultGrid.length &&
                gridX >= 0 && gridX < processingCore.resultGrid[gridY].length) {
            ResultGlyph currentGlyph = processingCore.resultGrid[gridY][gridX];
            if (currentGlyph != null) {
                this.oldGlyph = new ResultGlyph(currentGlyph.codePoint, currentGlyph.fgIndex, currentGlyph.bgIndex);
            }
        }
    }

    @Override
    public void execute() {
        if (processingCore.resultGrid != null &&
                gridY >= 0 && gridY < processingCore.resultGrid.length &&
                gridX >= 0 && gridX < processingCore.resultGrid[gridY].length) {

            // Neue Glyphe einsetzen
            processingCore.resultGrid[gridY][gridX] = new ResultGlyph(
                    newGlyph.codePoint, newGlyph.fgIndex, newGlyph.bgIndex);

            // Aktualisiere die angeklickte Glyphe, falls die gleiche Position betroffen ist
            if (processingCore.clickedGridX == gridX && processingCore.clickedGridY == gridY) {
                processingCore.clickedGlyph = processingCore.resultGrid[gridY][gridX];

                // Control Panel aktualisieren
                if (processingCore.controlPanel != null) {
                    processingCore.controlPanel.updateClickedInfo(gridX, gridY, processingCore.clickedGlyph,
                            ColorPalette.getColors(), processingCore.asciiPatterns);
                }
            }
            Logger.println("Glyphe bei (" + gridX + "," + gridY + ") geändert.");
        }
    }

    @Override
    public void undo() {
        if (oldGlyph == null) {
            Logger.println("Keine alte Glyphe zum Wiederherstellen vorhanden.");
            return;
        }

        if (processingCore.resultGrid != null &&
                gridY >= 0 && gridY < processingCore.resultGrid.length &&
                gridX >= 0 && gridX < processingCore.resultGrid[gridY].length) {

            // Alte Glyphe wiederherstellen
            processingCore.resultGrid[gridY][gridX] = new ResultGlyph(
                    oldGlyph.codePoint, oldGlyph.fgIndex, oldGlyph.bgIndex);

            // Aktualisiere die angeklickte Glyphe, falls die gleiche Position betroffen ist
            if (processingCore.clickedGridX == gridX && processingCore.clickedGridY == gridY) {
                processingCore.clickedGlyph = processingCore.resultGrid[gridY][gridX];

                // Control Panel aktualisieren
                if (processingCore.controlPanel != null) {
                    processingCore.controlPanel.updateClickedInfo(gridX, gridY, processingCore.clickedGlyph,
                            ColorPalette.getColors(), processingCore.asciiPatterns);
                }
            }
            Logger.println("Glyphenänderung bei (" + gridX + "," + gridY + ") rückgängig gemacht.");
        }
    }
}