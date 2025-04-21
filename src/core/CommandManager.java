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
     * Führt mehrere Commands als eine einzige Operation aus.
     * Hilfreich für Massenoperationen wie bei einer Selektion.
     * 
     * @param commands Die auszuführenden Commands
     */
    public void executeCommands(Command[] commands) {
        if (commands == null || commands.length == 0) {
            return;
        }

        if (commands.length == 1) {
            executeCommand(commands[0]);
            return;
        }

        CompositeCommand compositeCommand = new CompositeCommand();
        for (Command command : commands) {
            compositeCommand.addCommand(command);
        }

        executeCommand(compositeCommand);
    }

    /**
     * Ein Command, das mehrere andere Commands als eine einzelne Aktion gruppiert.
     * Nützlich für Massenoperationen auf einer Selektion von Zellen.
     */
    public static class CompositeCommand implements Command {
        private final Stack<Command> commands = new Stack<>();

        /**
         * Fügt ein Command zur Gruppe hinzu
         * 
         * @param command Das hinzuzufügende Command
         */
        public void addCommand(Command command) {
            if (command != null) {
                commands.push(command);
            }
        }

        /**
         * Prüft, ob die Gruppe leer ist
         * 
         * @return true, wenn keine Commands in der Gruppe sind
         */
        public boolean isEmpty() {
            return commands.isEmpty();
        }

        /**
         * Gibt die Anzahl der Commands in der Gruppe zurück
         * 
         * @return Anzahl der Commands
         */
        public int size() {
            return commands.size();
        }

        @Override
        public void execute() {
            for (Command command : commands) {
                command.execute();
            }
        }

        @Override
        public void undo() {
            // Commands in umgekehrter Reihenfolge rückgängig machen
            for (int i = commands.size() - 1; i >= 0; i--) {
                commands.get(i).undo();
            }
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
