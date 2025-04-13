/**
 * Command-Klasse für das Ändern einer Selektion (für Undo/Redo von Selektionen)
 */
public class SelectionCommand implements Command {
    private ProcessingCore processingCore;
    private boolean oldHasSelection;
    private int oldStartX, oldStartY, oldEndX, oldEndY;
    private boolean newHasSelection;
    private int newStartX, newStartY, newEndX, newEndY;

    /**
     * Erzeugt ein neues SelectionCommand
     * 
     * @param processingCore  Die ProcessingCore-Instanz
     * @param newHasSelection Ob die neue Selektion aktiv ist
     * @param newStartX       X-Startkoordinate der neuen Selektion
     * @param newStartY       Y-Startkoordinate der neuen Selektion
     * @param newEndX         X-Endkoordinate der neuen Selektion
     * @param newEndY         Y-Endkoordinate der neuen Selektion
     */
    public SelectionCommand(ProcessingCore processingCore,
            boolean newHasSelection,
            int newStartX, int newStartY,
            int newEndX, int newEndY) {
        this.processingCore = processingCore;

        // Alte Selektion speichern
        this.oldHasSelection = processingCore.hasSelection;
        this.oldStartX = processingCore.selectionStartX;
        this.oldStartY = processingCore.selectionStartY;
        this.oldEndX = processingCore.selectionEndX;
        this.oldEndY = processingCore.selectionEndY;

        // Neue Selektion speichern
        this.newHasSelection = newHasSelection;
        this.newStartX = newStartX;
        this.newStartY = newStartY;
        this.newEndX = newEndX;
        this.newEndY = newEndY;
    }

    @Override
    public void execute() {
        // Setze die neue Selektion
        processingCore.hasSelection = newHasSelection;
        processingCore.selectionStartX = newStartX;
        processingCore.selectionStartY = newStartY;
        processingCore.selectionEndX = newEndX;
        processingCore.selectionEndY = newEndY;

        String logMessage = newHasSelection
                ? "Selektion aktiviert: (" + newStartX + "," + newStartY + ") bis (" + newEndX + "," + newEndY + ")"
                : "Selektion aufgehoben";
        Logger.println(logMessage);
    }

    @Override
    public void undo() {
        // Alte Selektion wiederherstellen
        processingCore.hasSelection = oldHasSelection;
        processingCore.selectionStartX = oldStartX;
        processingCore.selectionStartY = oldStartY;
        processingCore.selectionEndX = oldEndX;
        processingCore.selectionEndY = oldEndY;

        String logMessage = oldHasSelection
                ? "Selektion wiederhergestellt: (" + oldStartX + "," + oldStartY + ") bis (" + oldEndX + "," + oldEndY
                        + ")"
                : "Selektion aufhebung rückgängig gemacht";
        Logger.println(logMessage);
    }
}