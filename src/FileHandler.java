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

        // make the filename consistent
        String withoutFileEnding = filePath.split("\\.")[0];
        String fileEnding = "usc2";
        filePath = withoutFileEnding + "." + fileEnding;
        if (p.resultGrid == null) {
            Logger.println("No result to save.");
            return;
        }
        // Use full grid dimensions initially
        int fullGridWidth = p.gridWidth;
        int fullGridHeight = p.gridHeight;

        if (fullGridWidth <= 0 || fullGridHeight <= 0) {
            Logger.println("Invalid grid dimensions, cannot save.");
            return;
        }

        // Determine export boundaries based on selection in ProcessingCore
        int exportX = 0;
        int exportY = 0;
        int exportWidth = fullGridWidth;
        int exportHeight = fullGridHeight;

        // Access selection state directly from ProcessingCore instance p
        if (p.hasSelection && p.selectionStartX != -1 && p.selectionStartY != -1 && p.selectionEndX != -1
                && p.selectionEndY != -1) {
            exportX = Math.min(p.selectionStartX, p.selectionEndX);
            exportY = Math.min(p.selectionStartY, p.selectionEndY);
            int maxX = Math.max(p.selectionStartX, p.selectionEndX);
            int maxY = Math.max(p.selectionStartY, p.selectionEndY);
            exportWidth = maxX - exportX + 1;
            exportHeight = maxY - exportY + 1;
            Logger.println("Exporting selected area: (" + exportX + "," + exportY + ") Width=" + exportWidth
                    + " Height=" + exportHeight);
        } else {
            Logger.println("Exporting full grid.");
        }

        Logger.println("Saving result to " + filePath + " (filtering control chars)...");

        try (FileOutputStream fos = new FileOutputStream(filePath);
                OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                PrintWriter writer = new PrintWriter(osw)) {

            writer.println("TYPE=USCII_ART_V3_SEPARATED");
            // Write the dimensions of the exported area
            writer.println("WIDTH=" + exportWidth);
            writer.println("HEIGHT=" + exportHeight);
            writer.println("COLORS=xterm256");
            writer.println("DATA_FORMAT=CHARS_GRID; FG_BG_GRID");
            writer.println();

            writer.println("CHARS");
            // Loop over the export area dimensions
            for (int y = 0; y < exportHeight; y++) {
                StringBuilder charLine = new StringBuilder(exportWidth);
                for (int x = 0; x < exportWidth; x++) {
                    // Calculate grid coordinates using offset
                    int currentGridY = exportY + y;
                    int currentGridX = exportX + x;

                    // Boundary check before accessing resultGrid
                    if (currentGridY >= 0 && currentGridY < fullGridHeight && currentGridX >= 0
                            && currentGridX < fullGridWidth) {
                        ResultGlyph g = p.resultGrid[currentGridY][currentGridX];
                        if (g != null) {
                            int cp = g.codePoint;

                            if (cp == 0) {
                                charLine.append(' ');
                            } else if (Character.isISOControl(cp) && !Character.isWhitespace(cp)) {
                                charLine.append('.'); // Replace control chars (except whitespace) with '.'
                            } else {
                                charLine.append(Character.toChars(cp)); // Use toChars for broader Unicode support
                            }
                        } else {
                            charLine.append('?'); // Placeholder for null glyphs
                        }
                    } else {
                        charLine.append('!'); // Indicate out-of-bounds access attempt (should not happen)
                        Logger.println("Warning: Attempted to save out-of-bounds cell at (" + currentGridX + ","
                                + currentGridY + ")");
                    }
                }
                writer.println(charLine.toString());
            }
            writer.println();

            writer.println("COLORS");
            // Loop over the export area dimensions
            for (int y = 0; y < exportHeight; y++) {
                StringBuilder colorLine = new StringBuilder(exportWidth * 8); // Adjusted capacity estimate
                for (int x = 0; x < exportWidth; x++) {
                    // Calculate grid coordinates using offset
                    int currentGridY = exportY + y;
                    int currentGridX = exportX + x;

                    // Boundary check before accessing resultGrid
                    if (currentGridY >= 0 && currentGridY < fullGridHeight && currentGridX >= 0
                            && currentGridX < fullGridWidth) {
                        ResultGlyph g = p.resultGrid[currentGridY][currentGridX];
                        if (g != null) {
                            colorLine.append(g.fgIndex).append(" ").append(g.bgIndex);
                        } else {
                            colorLine.append("0 0"); // Default colors for null glyphs
                        }
                    } else {
                        colorLine.append("0 0"); // Default colors for out-of-bounds
                    }

                    if (x < exportWidth - 1) {
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
