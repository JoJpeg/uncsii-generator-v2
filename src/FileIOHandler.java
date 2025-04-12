import java.io.File;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import processing.core.PApplet;

/**
 * Handles file input/output operations
 */
public class FileIOHandler {
    private PApplet app;
    private String defaultOutputPath = "output.usc";

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

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
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

        int result = chooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            String path = selected.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".usc")) {
                selected = new File(path + ".usc");
            }
            return selected;
        }
        return null;
    }

    /**
     * Save ASCII art result to a file
     */
    public boolean saveResult(String filePath, AsciiArtResult result) {
        if (result == null || result.isEmpty()) {
            Logger.println("No result to save.");
            return false;
        }

        Logger.println("Saving result to " + filePath + " ...");
        try (PrintWriter writer = app.createWriter(filePath)) {
            writer.println("TYPE=USCII_ART_V2_CODEPOINT");
            writer.println("WIDTH=" + result.getWidth());
            writer.println("HEIGHT=" + result.getHeight());
            writer.println("COLORS=xterm256");
            writer.println("DATA_FORMAT=CODEPOINT FG_INDEX BG_INDEX");
            writer.println("DATA");

            // Write character data
            for (int y = 0; y < result.getHeight(); y++) {
                StringBuilder line = new StringBuilder();
                for (int x = 0; x < result.getWidth(); x++) {
                    ResultGlyph g = result.get(x, y);
                    line.append(g.codePoint).append(" ")
                            .append(g.fgIndex).append(" ")
                            .append(g.bgIndex);

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