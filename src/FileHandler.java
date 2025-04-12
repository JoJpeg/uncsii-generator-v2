import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class FileHandler {

    public static String workingDir;

    public static File lastSelection = null;

    public FileHandler(ProcessingCore core) {
    }

    public static File loadFile(String message) {
        // get a file the Java way using the system's native file chooser
        // mode = 0 for save, 1 for load
        return getFile(message, FileDialog.LOAD);
    }

    public static File saveFile(String message) {
        // get a file the Java way using the system's native file chooser
        // mode = 0 for save, 1 for load
        return getFile(message, FileDialog.SAVE);
    }

    // get a file the Java way using the system's native file chooser
    public static File getFile(String message, int mode) {
        // mode = 0 for save, 1 for load
        // Use Frame as parent, can be null
        Frame parentFrame = null;
        FileDialog fileDialog = new FileDialog(parentFrame, message, mode);
        // For macOS, set system property to use native file dialog
        // System.setProperty("apple.awt.fileDialogForDirectories", "true"); // If
        // needed for directories
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

    public static void setLastAsWorkingDirectory() {
        if (lastSelection != null) {
            workingDir = lastSelection.getAbsolutePath();
            Logger.println("Working directory set to: " + workingDir);
        } else {
            Logger.println("No file selected, working directory not set.");
        }

    }

    public static void saveResult(String filePath, ProcessingCore p) {
        filePath = filePath + "2";
        if (p.resultGrid == null) {
            Logger.println("No result to save.");
            return;
        }
        if (p.gridWidth <= 0 || p.gridHeight <= 0) {
            Logger.println("Invalid grid dimensions, cannot save.");
            return;
        }

        Logger.println("Saving result to " + filePath + " (filtering control chars)...");

        try (FileOutputStream fos = new FileOutputStream(filePath);
                OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                PrintWriter writer = new PrintWriter(osw)) {

            writer.println("TYPE=USCII_ART_V3_SEPARATED");
            writer.println("WIDTH=" + p.gridWidth);
            writer.println("HEIGHT=" + p.gridHeight);
            writer.println("COLORS=xterm256");
            writer.println("DATA_FORMAT=CHARS_GRID; FG_BG_GRID");
            writer.println();

            writer.println("CHARS");
            for (int y = 0; y < p.gridHeight; y++) {
                StringBuilder charLine = new StringBuilder(p.gridWidth);
                for (int x = 0; x < p.gridWidth; x++) {
                    ResultGlyph g = p.resultGrid[y][x];
                    if (g != null) {
                        int cp = g.codePoint;

                        if (cp == 0) {
                            charLine.append(' ');
                        } else if (Character.isISOControl(cp) && !Character.isWhitespace(cp)) {
                            charLine.append('.');
                        } else {
                            charLine.append(Character.toString(cp));
                        }
                    } else {
                        charLine.append('?');
                    }
                }
                writer.println(charLine.toString());
            }
            writer.println();

            writer.println("COLORS");
            for (int y = 0; y < p.gridHeight; y++) {
                StringBuilder colorLine = new StringBuilder(p.gridWidth * 8);
                for (int x = 0; x < p.gridWidth; x++) {
                    ResultGlyph g = p.resultGrid[y][x];
                    if (g != null) {
                        colorLine.append(g.fgIndex).append(" ").append(g.bgIndex);
                    } else {
                        colorLine.append("0 0");
                    }
                    if (x < p.gridWidth - 1) {
                        colorLine.append(" ");
                    }
                }
                writer.println(colorLine.toString());
            }

            writer.flush();
            Logger.println("Result saved successfully in V3 format (control chars filtered).");

        } catch (Exception e) {
            Logger.println("Error saving result file: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
