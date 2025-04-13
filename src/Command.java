/**
 * Interface für alle Commands, die undo/redo unterstützen
 */
public interface Command {
    /**
     * Führt das Command aus
     */
    void execute();

    /**
     * Macht die Ausführung des Commands rückgängig
     */
    void undo();
}
