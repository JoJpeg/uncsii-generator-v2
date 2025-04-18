package core;

import java.io.File;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import logger.Logger;
import processing.core.PApplet;

/**
 * Handles file input/output operations
 */
public class FileIOHandler {
    private PApplet app;
    private String defaultOutputPath = "output.usc";
    private static String saveFileWorkPath;
    private static String loadFileWorkPath;

    public FileIOHandler(PApplet app) {
        this.app = app;
    }

    /**
     * Show a file chooser dialog for loading a file
     */
    public File chooseLoadFile(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image Files", "jpg", "jpeg", "png", "gif", "bmp");
        chooser.setFileFilter(filter);

        // Set initial directory if we have a saved path
        if (loadFileWorkPath != null) {
            File dir = new File(loadFileWorkPath).getParentFile();
            if (dir != null && dir.exists()) {
                chooser.setCurrentDirectory(dir);
            }
        }

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            // Save the working path for next time
            loadFileWorkPath = selected.getAbsolutePath();
            Logger.println("Load working path updated to: " + selected.getParent());
            return selected;
        }
        return null;
    }

    /**
     * Show a file chooser dialog for saving a file
     */
    public File chooseSaveFile(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "UNSCII Files", "usc");
        chooser.setFileFilter(filter);

        // Set initial directory if we have a saved path
        if (saveFileWorkPath != null) {
            File dir = new File(saveFileWorkPath).getParentFile();
            if (dir != null && dir.exists()) {
                chooser.setCurrentDirectory(dir);
            }
        }

        int result = chooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            String path = selected.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".usc")) {
                selected = new File(path + ".usc");
            }
            // Save the working path for next time
            saveFileWorkPath = selected.getAbsolutePath();
            Logger.println("Save working path updated to: " + selected.getParent());
            return selected;
        }
        return null;
    }

    /**
     * Save ASCII art result to a file
     */
    public boolean saveResult(String filePath, AsciiArtResult result) {
        // Save the working path
        saveFileWorkPath = filePath;

        if (result == null || result.isEmpty()) {
            Logger.println("No result to save.");
            return false;
        }

        Logger.println("Saving result to " + filePath + " ...");
        try (PrintWriter writer = app.createWriter(filePath)) {
            writer.println("TYPE=USCII_ART_V3_ALPHA");
            writer.println("WIDTH=" + result.getWidth());
            writer.println("HEIGHT=" + result.getHeight());
            writer.println("COLORS=xterm256");
            writer.println("DATA_FORMAT=CODEPOINT FG_INDEX BG_INDEX ALPHA");
            writer.println("DATA");

            // Write character data
            for (int y = 0; y < result.getHeight(); y++) {
                StringBuilder line = new StringBuilder();
                for (int x = 0; x < result.getWidth(); x++) {
                    ResultGlyph g = result.get(x, y);
                    line.append(g.codePoint).append(" ")
                            .append(g.fgIndex).append(" ")
                            .append(g.bgIndex).append(" ")
                            .append(g.alpha < 127 ? -1 : g.alpha); // -1 bedeutet transparent (Alpha < 127)

                    if (x < result.getWidth() - 1) {
                        line.append(" ");
                    }
                }
                writer.println(line.toString());
            }

            writer.flush();
            Logger.println("Result saved successfully to " + filePath);
            return true;
        } catch (Exception e) {
            Logger.println("Error saving result file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Set the default output path
     */
    public void setDefaultOutputPath(String path) {
        this.defaultOutputPath = path;
    }

    /**
     * Get the default output path
     */
    public String getDefaultOutputPath() {
        return defaultOutputPath;
    }
}