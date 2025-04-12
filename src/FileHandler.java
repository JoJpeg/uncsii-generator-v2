import java.io.File;
import java.awt.FileDialog;
import java.awt.Frame;

public class FileHandler {

    public static File lastSelection = null;
    static ProcessingCore p = new ProcessingCore();

    public FileHandler(ProcessingCore core) {
        p = core;
    }

    // is called by ProcessingCore.fileSelected() since all the File things
    // are provided by the Processing class
    public static void handle(File file) {
        lastSelection = file;
    }

    public static File getFileP(String message) {
        p.selectInput(message, "fileSelected");
        return lastSelection;
    }

    // get a file the Java way using the system's native file chooser
    public static File getFileJ(String message) {
        // Use Frame as parent, can be null
        Frame parentFrame = null;
        FileDialog fileDialog = new FileDialog(parentFrame, message, FileDialog.LOAD);
        // For macOS, set system property to use native file dialog
        // System.setProperty("apple.awt.fileDialogForDirectories", "true"); // If needed for directories
        fileDialog.setVisible(true);

        String directory = fileDialog.getDirectory();
        String filename = fileDialog.getFile();

        if (directory != null && filename != null) {
            lastSelection = new File(directory, filename);
            return lastSelection;
        } else {
            lastSelection = null;
            return null;
        }
        // For macOS, unset the property if you set it earlier
        // System.setProperty("apple.awt.fileDialogForDirectories", "false");
    }

}
