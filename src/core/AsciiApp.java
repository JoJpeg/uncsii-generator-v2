package core;

import java.io.File;

import logger.Logger;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.MouseEvent;
import ui.ControlPanel;

/**
 * Main UNSCII Generator application class that coordinates all components
 */
public class AsciiApp extends PApplet {
    // Constants
    public static final int DEFAULT_WINDOW_WIDTH = 800;
    public static final int DEFAULT_WINDOW_HEIGHT = 480;
    public static final String DEFAULT_FONT_PATH = "resources/unscii-8.ttf";

    // Core components
    private ColorPalette colorPalette;
    private GlyphManager glyphManager;
    private ImageProcessor imageProcessor;
    private AsciiArtGenerator asciiGenerator;
    private DisplayManager displayManager;
    private FileIOHandler fileHandler;

    // Data
    private AsciiArtResult result;
    private PFont unsciiFont;
    private String fontPath = DEFAULT_FONT_PATH;

    // UI
    private ControlPanel controlPanel;
    private ProcessingCore processingCoreAdapter; // Adapter for legacy ControlPanel

    @Override
    public void settings() {
        Logger.sysOut = true;
        size(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
    }

    @Override
    public void setup() {

        // Initialize components
        initializeComponents();

        // Initialize control panel
        initializeControlPanel();

        // Setup welcome screen
        text("Unscii Generator", width / 2, height / 2);
    }

    /**
     * Initialize all main components
     */
    private void initializeComponents() {
        // Initialize font
        unsciiFont = createFont(fontPath, 8, true);
        background(0);
        textFont(unsciiFont);
        textSize(16);
        textAlign(CENTER, CENTER);
        noSmooth(); // Important for pixel-perfect display

        // Create adapter for backwards compatibility
        processingCoreAdapter = new ProcessingCore();
        // processingCoreAdapter.setup(); // Initialize the adapter properly

        // Create core components
        colorPalette = new ColorPalette(this);
        glyphManager = new GlyphManager(this, fontPath, 8);
        imageProcessor = new ImageProcessor(this);

        // Create dependent components
        asciiGenerator = new AsciiArtGenerator(glyphManager, colorPalette, imageProcessor);
        displayManager = new DisplayManager(this, glyphManager, colorPalette);
        fileHandler = new FileIOHandler(this);

        // Generate font patterns
        glyphManager.generatePatterns();
    }

    /**
     * Initialize the control panel
     */
    private void initializeControlPanel() {
        controlPanel = new ControlPanel(processingCoreAdapter);
        controlPanel.setVisible(true);
        controlPanel.setState(ControlPanel.PanelState.SETUP);
        displayManager.setControlPanel(controlPanel);

        // Position the control panel next to the main window
        try {
            int x = getJFrame().getX() + getJFrame().getWidth();
            int y = getJFrame().getY();
            controlPanel.setLocation(x, y);
        } catch (Exception e) {
            Logger.println("Could not position control panel: " + e.getMessage());
        }
    }

    @Override
    public void draw() {
        background(30);

        if (displayManager.isShowingSourceImage() && imageProcessor.getImage() != null) {
            displayManager.drawSourceImage(imageProcessor.getImage(),
                    imageProcessor.getGridWidth(),
                    imageProcessor.getGridHeight());
        } else if (result != null && !result.isEmpty()) {
            displayManager.drawResult(result);
        } else {
            displayManager.drawWelcomeScreen();
        }
    }

    @Override
    public void mousePressed() {
        if (result != null) {
            displayManager.handleMousePressed(result, mouseX, mouseY);
        }
    }

    @Override
    public void mouseDragged() {
        displayManager.handleMouseDragged(mouseX, mouseY, pmouseX, pmouseY, mouseButton);
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        if (result != null) {
            float e = event.getCount();
            int currentScale = displayManager.getDisplayScale();
            int targetScale = currentScale + (e < 0 ? 1 : -1); // zoom in on up, out on down
            displayManager.setDisplayScale(targetScale, result.getWidth(), result.getHeight());
        }
    }

    @Override
    public void keyPressed() {
        handleKeyPress(key);
    }

    /**
     * Handle key press events
     */
    public void handleKeyPress(char k) {
        // Update the ProcessingCore adapter to maintain compatibility with ControlPanel
        if (processingCoreAdapter != null) {
            processingCoreAdapter.keyPressed(k);
        }

        switch (k) {
            case 's':
            case 'S':
                displayManager.toggleSourceImage();
                Logger.println("Toggled view: " +
                        (displayManager.isShowingSourceImage() ? "Source Image" : "ASCII Art"));
                break;

            case 'p':
            case 'P':
                File outputFile = fileHandler.chooseSaveFile("Save ASCII Art");
                if (outputFile != null) {
                    String path = outputFile.getAbsolutePath();
                    fileHandler.setDefaultOutputPath(path);
                    fileHandler.saveResult(path, result);
                }
                break;

            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
                if (result != null) {
                    int targetScale = k - '0';
                    displayManager.setDisplayScale(targetScale, result.getWidth(), result.getHeight());
                }
                break;

            case 'r':
            case 'R':
                if (imageProcessor.getImagePath() != null) {
                    reloadCurrentImage();
                }
                break;

            case 'l':
            case 'L':
                loadImage();
                break;
        }
    }

    /**
     * Load a new image and process it
     */
    public void loadImage() {
        File imageFile = fileHandler.chooseLoadFile("Select Image");
        if (imageFile == null) {
            Logger.println("No image file selected.");
            controlPanel.setState(ControlPanel.PanelState.SETUP);
            return;
        }

        String path = imageFile.getAbsolutePath();
        if (imageProcessor.loadFromFile(path)) {
            // Generate ASCII art
            result = asciiGenerator.generateAsciiArt(true);

            // Update ProcessingCore adapter with result for compatibility with ControlPanel
            if (processingCoreAdapter != null && result != null) {
                updateProcessingCoreAdapter();
            }

            if (result != null) {
                controlPanel.setState(ControlPanel.PanelState.EDIT);
                Logger.println("ASCII art generation complete. Press 's' to toggle view.");
            } else {
                Logger.println("Failed to generate ASCII art.");
                controlPanel.setState(ControlPanel.PanelState.SETUP);
            }
        } else {
            Logger.println("Failed to load image.");
            controlPanel.setState(ControlPanel.PanelState.SETUP);
        }
    }

    /**
     * Update the ProcessingCore adapter with the current result
     */
    private void updateProcessingCoreAdapter() {
        // Only update if we have a valid result and adapter
        if (processingCoreAdapter == null || result == null) {
            return;
        }

        // Set public fields directly
        processingCoreAdapter.resultGrid = convertResultToOldFormat(result);
        processingCoreAdapter.gridWidth = result.getWidth();
        processingCoreAdapter.gridHeight = result.getHeight();
    }

    /**
     * Convert our new AsciiArtResult to the old format used by ProcessingCore
     */
    private ResultGlyph[][] convertResultToOldFormat(AsciiArtResult result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        ResultGlyph[][] oldFormat = new ResultGlyph[result.getHeight()][result.getWidth()];
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                oldFormat[y][x] = result.get(x, y);
            }
        }
        return oldFormat;
    }

    /**
     * Reload the current image
     */
    private void reloadCurrentImage() {
        String path = imageProcessor.getImagePath();
        if (path != null && !path.isEmpty()) {
            fill(128);
            textSize(32);
            textAlign(CENTER, CENTER);
            text("Reloading...", width / 2, height / 2);

            if (imageProcessor.loadFromFile(path)) {
                result = asciiGenerator.generateAsciiArt(true);

                // Update ProcessingCore adapter with result
                if (processingCoreAdapter != null && result != null) {
                    updateProcessingCoreAdapter();
                }

                controlPanel.setState(ControlPanel.PanelState.EDIT);
            }
        } else {
            Logger.println("No image path set to reload.");
        }
    }

    /**
     * Get the Java Frame object for this Processing sketch
     */
    private java.awt.Frame getJFrame() {
        try {
            return (java.awt.Frame) ((processing.awt.PSurfaceAWT.SmoothCanvas) surface.getNative()).getFrame();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the result of ASCII art generation
     */
    public AsciiArtResult getResult() {
        return result;
    }

    @Override
    public void exit() {
        if (controlPanel != null) {
            controlPanel.dispose();
        }
        super.exit();
    }

    /**
     * Main method to start the application
     */
    public static void main(String[] args) {
        try {
            PApplet.main(new String[] { "core.AsciiApp" });
        } catch (Exception e) {
            Logger.println("Error starting the application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}