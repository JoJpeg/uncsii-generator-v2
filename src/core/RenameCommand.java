package core;

import ui.BatchManager;

public class RenameCommand implements Command {
    private BatchManager batchManager;
    private int index;
    private String oldName;
    private String newName;

    public RenameCommand(BatchManager batchManager, int index, String oldName, String newName) {
        this.batchManager = batchManager;
        this.index = index;
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public void execute() {
        batchManager.renameImage(index, newName);
    }

    @Override
    public void undo() {
        batchManager.renameImage(index, oldName);
    }
    


}
