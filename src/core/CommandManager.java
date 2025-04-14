package core;
import java.util.Stack;

/**
 * Verwaltet die Ausführung von Commands und deren Undo/Redo-Operationen
 */
public class CommandManager {
    private Stack<Command> undoStack = new Stack<>();
    private Stack<Command> redoStack = new Stack<>();
    private static final int MAX_HISTORY = 50; // Maximale Anzahl gespeicherter Aktionen

    /**
     * Führt ein Command aus und fügt es zum Undo-Stack hinzu
     * 
     * @param command Das auszuführende Command
     */
    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Nach einer neuen Aktion wird der Redo-Stack gelöscht

        // Begrenze die Größe des Undo-Stacks
        if (undoStack.size() > MAX_HISTORY) {
            // Entferne das älteste Command
            undoStack.remove(0);
        }
    }

    /**
     * Prüft, ob Undo möglich ist
     * 
     * @return true, wenn mindestens ein Befehl rückgängig gemacht werden kann
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Prüft, ob Redo möglich ist
     * 
     * @return true, wenn mindestens ein Befehl wiederhergestellt werden kann
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Führt einen Undo-Schritt aus
     */
    public void undo() {
        if (!canUndo())
            return;

        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    /**
     * Führt einen Redo-Schritt aus
     */
    public void redo() {
        if (!canRedo())
            return;

        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }

    /**
     * Löscht die gesamte Command-Historie
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }
}
