package core;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import logger.Logger;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import ui.ControlPanel;

/**
 * Represents a single character in the resulting ASCII art
 */
// public class ResultGlyph {
//     int codePoint; // Unicode Codepoint
//     int fgIndex; // Foreground color index (0-255)
//     int bgIndex; // Background color index (0-255)

//     ResultGlyph(int cp, int fg, int bg) {
//         this.codePoint = cp;
//         this.fgIndex = fg;
//         this.bgIndex = bg;
//     }
// }

/**
 * Main Processing application for UNSCII Generator
 * Handles font pattern generation, image conversion, and display
 */
public class ProcessingCore extends PApplet {

    // ========== CONSTANTS ==========

    // Font and glyph dimensions
    public static final int GLYPH_WIDTH = 8;
    public static final int GLYPH_HEIGHT = 8;
    public static final int PIXEL_COUNT = GLYPH_WIDTH * GLYPH_HEIGHT;

    // Display constants
    public static final int DEFAULT_WINDOW_WIDTH = 800;
    public static final int DEFAULT_WINDOW_HEIGHT = 480;
    public static final int DEFAULT_DISPLAY_AREA_WIDTH = 400;
    public static final int DEFAULT_DISPLAY_AREA_HEIGHT = 240;
    public static final int DEFAULT_DISPLAY_SCALE = 2;

    // File paths
    public static final String DEFAULT_FONT_PATH = "/Users/jonaseschner/IdeaProjects/unscii-generator/src/resources/unscii-8.ttf";
    public static final String DEFAULT_OUTPUT_PATH = "output.usc";

    // Image conversion states
    public enum ImageLoadingState {
        NONE,
        LOADING,
        LOADED,
        ERROR
    }

    // ========== INSTANCE VARIABLES ==========

    // --- Command Manager für Undo/Redo ---
    private CommandManager commandManager = new CommandManager();

    // --- Display Variables ---
    private int displayAreaWidth = DEFAULT_DISPLAY_AREA_WIDTH;
    private int displayAreaHeight = DEFAULT_DISPLAY_AREA_HEIGHT;
    private int DISPLAY_SCALE = DEFAULT_DISPLAY_SCALE;
    private boolean showSourceImage = false;
    private int drawW, drawH;
    private int drawX, drawY;

    // --- Font Pattern Variables ---
    private GlyphPatternGenerator patternGenerator;
    public Map<Integer, Long> asciiPatterns;
    private String fontPath = DEFAULT_FONT_PATH;
    private PFont unscii;

    // --- Image & Conversion Variables ---
    private PImage inputImage;
    // Make these public for FileHandler access
    public ResultGlyph[][] resultGrid;
    public int gridWidth;
    public int gridHeight;
    private String imagePath;
    private String outputPath = DEFAULT_OUTPUT_PATH;
    public int[] colorPalette; // Color palette (256 colors)
    private ImageLoadingState imageLoadingState = ImageLoadingState.NONE;

    // Synchronization flag to avoid race conditions
    private volatile boolean isImageProcessing = false;

    // --- Selection Variables ---
    // Hover selection
    private int mouseGridX = -1;
    private int mouseGridY = -1;
    private ResultGlyph selectedGlyph = null;

    // Clicked selection (single cell)
    public int clickedGridX = -1;
    public int clickedGridY = -1;
    public ResultGlyph clickedGlyph = null;

    // --- Control Panel ---
    public ControlPanel controlPanel;

    // --- Interaction State ---
    public boolean isSpacebarDown = false; // Track spacebar state
    public boolean isSelecting = false; // Is a selection drag currently active?
    public boolean hasSelection = false; // Does a valid selection exist?
    public int selectionStartX = -1; // Grid coordinates for selection start
    public int selectionStartY = -1;
    public int selectionEndX = -1; // Grid coordinates for selection end
    public int selectionEndY = -1;
    public boolean startedDragging = false; // Track if dragging started (for panning or selection)
    public long clickStart = 0; // Track click time

    // Variablen zum Speichern der Auswahl zwischen Bildern
    private boolean savedSelectionActive = false;
    private int savedSelectionStartX = -1;
    private int savedSelectionStartY = -1;
    private int savedSelectionEndX = -1;
    private int savedSelectionEndY = -1;

    // ========== PROCESSING LIFECYCLE METHODS ==========

    @Override
    public void settings() {
        Logger.sysOut = true;
        size(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
    }

    @Override
    public void setup() {
        // Initialize display variables
        drawX = (width - drawW) / 2;
        drawY = (height - drawH) / 2;
        // Initialize font
        unscii = createFont(fontPath, 8, true);
        background(0);
        textFont(unscii);
        textSize(16);
        textAlign(CENTER, CENTER);
        text("Unscii Generator", width / 2, height / 2);
        noSmooth(); // Important for pixel-perfect display

        // Initialize control panel
        initControlPanel();

        // Setup application
        setupPalette();
        initializeDefaultPaths();
        generateFontPatterns();
    }

    @Override
    public void draw() {
        background(30);

        // When image is processing, show a loading indicator
        if (isImageProcessing) {
            fill(128);
            textSize(24);
            textAlign(CENTER, CENTER);
            text("Please wait...", width / 2, height / 2);
            // return; // Stop drawing until loading is complete
        }

        if (showSourceImage && inputImage != null) {
            drawSourceImage();
        } else if (resultGrid != null) {
            drawResult();
        } else {
            drawWelcomeScreen();
        }
    }

    // ========== INITIALIZATION METHODS ==========

    /**
     * Initialize the control panel
     */
    private void initControlPanel() {
        controlPanel = new ControlPanel(this);
        controlPanel.setVisible(true);
        controlPanel.setState(ControlPanel.PanelState.SETUP);

        // Position the control panel next to the main window
        try {
            int x = getJFrame().getX() + getJFrame().getWidth();
            int y = getJFrame().getY();
            controlPanel.setLocation(x, y);
        } catch (Exception e) {
            Logger.println("Could not position control panel: " + e.getMessage());
        }
    }

    /**
     * Initialize default file paths
     */
    private void initializeDefaultPaths() {
        // Example image paths - would be better to make this configurable
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/Ambient Mean/Xorm/Hypno Flute/renders/visual ready/16_9/hypno in flower.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/Ambient Mean/Xorm/Hypno Flute/renders/visual ready/16_9/crow spottet.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/Ambient Mean/Xorm/Hypno Flute/renders/visual ready/16_9/xorm logo.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/JoJpeg/Fun Stuff/Dirigent/Dirigent Xorm Intro 3.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/JoJpeg/Fun Stuff/Dirigent/Dirigent Font Test.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/JoJpeg/Fun Stuff/Dirigent/Dirigent Xorm Screen 1.png";
        outputPath = "Dirigent Xorm Screen 1.usc";
    }

    /**
     * Generate font patterns for ASCII art conversion
     */
    private void generateFontPatterns() {
        float fontSize = 8;

        Logger.println("Generating glyph patterns...");
        try {
            patternGenerator = new GlyphPatternGenerator(this, fontPath, fontSize);
            asciiPatterns = patternGenerator.generatePatterns();
            Logger.println("Generated " + asciiPatterns.size() + " patterns.");

            if (asciiPatterns.isEmpty()) {
                throw new RuntimeException("Pattern generation resulted in an empty map.");
            }

            // Print a test pattern for 'A'
            int testCodePoint = 'A';
            Logger.println("\nPattern for U+" + String.format("%04X", testCodePoint) + " ('A'):");
            if (asciiPatterns.containsKey(testCodePoint)) {
                GlyphPatternGenerator.printPattern(asciiPatterns.get(testCodePoint));
            } else {
                Logger.println("Pattern not found.");
            }
        } catch (Exception e) {
            Logger.println("Failed to initialize patterns: " + e.getMessage());
            e.printStackTrace();
            exitActual();
        }

        Logger.println("\n--- Conversion Setup ---");
        Logger.println("Image loaded: " + (inputImage != null ? inputImage.width + "x" + inputImage.height : "Failed"));
        Logger.println("Grid dimensions: " + gridWidth + "x" + gridHeight);
        Logger.println("Total blocks: " + (gridWidth * gridHeight));
        Logger.println("Press 's' to switch between Source Image and ASCII Art.");
        Logger.println("Press 'p' to save the result to 'output.usc' file.");
        Logger.println("Press '1'-'8' to change display scale.");
    }

    /**
     * Setup the xterm-256 color palette
     */
    private void setupPalette() {
        colorPalette = new int[256];

        // Reserved color index for transparency (fully transparent black)
        // Using 0 alpha to make it fully transparent
        colorPalette[0] = color(0, 0, 0, 0);

        // Standard 16 colors (now starting at index 1)
        int[] basic16 = {
                color(128, 0, 0), color(0, 128, 0), color(128, 128, 0),
                color(0, 0, 128), color(128, 0, 128), color(0, 128, 128), color(192, 192, 192),
                color(128, 128, 128), color(255, 0, 0), color(0, 255, 0), color(255, 255, 0),
                color(0, 0, 255), color(255, 0, 255), color(0, 255, 255), color(255, 255, 255)
        };

        // Copy basic 16 colors (starting at index 1)
        System.arraycopy(basic16, 0, colorPalette, 1, basic16.length);

        // Generate the 216 color cube (6x6x6)
        int index = 16 + 1; // +1 to account for the reserved transparent color
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    int red = r > 0 ? (r * 40 + 55) : 0;
                    int green = g > 0 ? (g * 40 + 55) : 0;
                    int blue = b > 0 ? (b * 40 + 55) : 0;
                    colorPalette[index++] = color(red, green, blue);
                }
            }
        }

        // Generate the grayscale ramp (24 colors)
        for (int i = 0; i < 24; i++) {
            int value = i * 10 + 8;
            if (index < 256) {
                colorPalette[index] = color(value, value, value);
            }
            index++;
        }

        // Add an additional semi-transparent black (80% transparency)
        // This gives some options between fully transparent and opaque
        if (index < 256) {
            colorPalette[index] = color(0, 0, 0, 51); // ~20% opacity
            index++;
        }

        if (colorPalette.length != 256) {
            Logger.println("WARNING: Color Palette size is not 256!");
        }

        Logger.println("Palette initialized with transparency support. Index 0 = transparent.");
    }

    // ========== IMAGE LOADING AND PROCESSING ==========

    /**
     * Loads an image from the file system
     */
    public void loadImage() {
        saveCurrentSelection(); // Save the current selection before loading a new image

        File imageFile = FileHandler.loadFile("Select Image");
        if (imageFile == null) {
            Logger.println("File selection canceled.");

            // Check if we already had an image loaded before this operation
            if (resultGrid != null && imageLoadingState == ImageLoadingState.LOADED) {
                // Keep the EDIT state since we already have an image loaded
                controlPanel.setState(ControlPanel.PanelState.EDIT);
                Logger.println("Returning to edit mode with previously loaded image.");
            } else {
                controlPanel.setState(ControlPanel.PanelState.SETUP);
            }
            return;
        }

        imagePath = imageFile.getAbsolutePath();
        loadAndProcessImage(imagePath);

        if (imageLoadingState == ImageLoadingState.ERROR) {
            Logger.println("Error loading image. Exiting.");
            controlPanel.setState(ControlPanel.PanelState.SETUP);
            return;
        }

        restoreSavedSelection(); // Restore the saved selection after loading the new image

        controlPanel.setState(ControlPanel.PanelState.EDIT);
    }

    /**
     * Load and process an image for conversion
     */
    void loadAndProcessImage(String path) {
        try {
            // Early exit if path is null or empty
            if (path == null || path.isEmpty()) {
                Logger.println("Invalid image path: null or empty");
                imageLoadingState = ImageLoadingState.ERROR;
                return;
            }

            // Activate image processing flag to manage render state
            isImageProcessing = true;

            // Show loading status
            fill(128);
            textSize(24);
            textAlign(CENTER, CENTER);
            background(30);
            text("Loading image...", width / 2, height / 2);

            // Update the canvas to give immediate feedback
            redraw();

            inputImage = loadImage(path);
            if (inputImage == null) {
                Logger.println("Error loading image: " + path);
                imageLoadingState = ImageLoadingState.ERROR;
                isImageProcessing = false; // Important: Reset flag on error
                return;
            }

            // Prüfen, ob Bild Alpha-Kanal hat und sicherstellen, dass Schwarze Pixel nicht
            // transparent sind
            boolean hasAlphaChannel = false;
            inputImage.loadPixels();

            // Einfacher Test: Suche nach Pixeln mit nicht-255 Alpha
            for (int i = 0; i < inputImage.pixels.length; i++) {
                int alpha = (inputImage.pixels[i] >> 24) & 0xFF;
                if (alpha != 255) {
                    hasAlphaChannel = true;
                    break;
                }
            }

            Logger.println("Image loaded: " + path);
            Logger.println("Image has alpha channel: " + (hasAlphaChannel ? "yes" : "no"));

            // Wenn kein Alpha-Kanal erkannt wurde, stelle sicher, dass alle Pixel volle
            // Deckkraft haben
            if (!hasAlphaChannel) {
                Logger.println("Ensuring all pixels have full opacity");
                for (int i = 0; i < inputImage.pixels.length; i++) {
                    // Setze Alpha auf voll opak (255)
                    inputImage.pixels[i] = inputImage.pixels[i] | 0xFF000000;
                }
                inputImage.updatePixels();
            }

            // Resize image to fit display area while maintaining aspect ratio
            resizeImageForDisplay();

            // Ensure image dimensions are multiples of the glyph size
            cropImageToFitGrid();

            if (gridWidth == 0 || gridHeight == 0) {
                Logger.println("Image too small after resizing/cropping for an 8x8 grid.");
                imageLoadingState = ImageLoadingState.ERROR;
                isImageProcessing = false; // Important: Reset flag on error
                return;
            }

            // Create a new result grid array
            resultGrid = new ResultGlyph[gridHeight][gridWidth];

            Logger.println("Starting ASCII conversion...");
            long startTime = System.currentTimeMillis();

            generateAsciiArtExact();

            long endTime = System.currentTimeMillis();
            Logger.println("Conversion finished in " + (endTime - startTime) + " ms.");
            imageLoadingState = ImageLoadingState.LOADED;

            // Reset selection states
            clickedGridX = -1;
            clickedGridY = -1;
            clickedGlyph = null;
            mouseGridX = -1;
            mouseGridY = -1;
            selectedGlyph = null;
            hasSelection = false;
            selectionStartX = selectionStartY = selectionEndX = selectionEndY = -1;

            if (controlPanel != null) {
                controlPanel.updateClickedInfo(-1, -1, null, colorPalette, asciiPatterns);
                controlPanel.updateSelectionInfo(-1, -1, null);
            }

            // CRITICAL FIX: Always reset the processing flag when done
            isImageProcessing = false;
        } catch (Exception e) {
            Logger.println("Error during image processing: " + e.getMessage());
            e.printStackTrace();
            isImageProcessing = false; // Important: Reset flag on error
            imageLoadingState = ImageLoadingState.ERROR;
            resultGrid = null;
        } finally {
            // Ensure the processing flag is always reset, even if we missed some case
            isImageProcessing = false;
        }
    }

    /**
     * Resize image to fit display area while maintaining aspect ratio
     */
    private void resizeImageForDisplay() {
        float imgAspect = (float) inputImage.width / inputImage.height;
        float targetAspect = (float) displayAreaWidth / displayAreaHeight;
        int targetWidth, targetHeight;

        if (imgAspect > targetAspect) {
            targetWidth = displayAreaWidth;
            targetHeight = (int) (displayAreaWidth / imgAspect);
        } else {
            targetHeight = displayAreaHeight;
            targetWidth = (int) (displayAreaHeight * imgAspect);
        }

        inputImage.resize(targetWidth, targetHeight);
        inputImage.loadPixels();

        gridWidth = inputImage.width / GLYPH_WIDTH;
        gridHeight = inputImage.height / GLYPH_HEIGHT;
    }

    /**
     * Crop image to ensure dimensions are multiples of the glyph size
     */
    private void cropImageToFitGrid() {
        int croppedWidth = gridWidth * GLYPH_WIDTH;
        int croppedHeight = gridHeight * GLYPH_HEIGHT;

        if (croppedWidth != inputImage.width || croppedHeight != inputImage.height) {
            Logger.println("Cropping image from " + inputImage.width + "x" + inputImage.height +
                    " to " + croppedWidth + "x" + croppedHeight + " to fit grid.");
            inputImage = inputImage.get(0, 0, croppedWidth, croppedHeight);
            inputImage.loadPixels();
        }
    }

    // ========== ASCII ART GENERATION ==========

    /**
     * Generate ASCII art using exact matching when possible, with fallback to
     * approximation
     */
    void generateAsciiArtExact() {
        for (int gridY = 0; gridY < gridHeight; gridY++) {
            for (int gridX = 0; gridX < gridWidth; gridX++) {
                int[] blockPixels = extractBlockPixels(gridX, gridY);

                // Try to find an exact match first
                ResultGlyph exactMatchResult = findExactMatch(blockPixels);
                if (exactMatchResult != null) {
                    resultGrid[gridY][gridX] = exactMatchResult;
                    continue;
                }

                // Fall back to approximation if no exact match
                resultGrid[gridY][gridX] = findApproximateMatch(blockPixels);
            }

            // Show progress
            if ((gridY + 1) % 10 == 0 || gridY == gridHeight - 1) {
                Logger.println("Processed row " + (gridY + 1) + "/" + gridHeight);
            }
        }
    }

    /**
     * Extract block pixels from the source image
     */
    private int[] extractBlockPixels(int gridX, int gridY) {
        int[] blockPixels = new int[PIXEL_COUNT];
        int startX = gridX * GLYPH_WIDTH;
        int startY = gridY * GLYPH_HEIGHT;

        for (int y = 0; y < GLYPH_HEIGHT; y++) {
            for (int x = 0; x < GLYPH_WIDTH; x++) {
                int imgX = startX + x;
                int imgY = startY + y;
                int color = inputImage.pixels[imgY * inputImage.width + imgX];
                blockPixels[y * GLYPH_WIDTH + x] = color;
                // float r = red(color);
                // float g = green(color);
                // float b = blue(color);
                // int alpha = (color >> 24) & 0xFF;
                // if (r == 0 && b == 0 && g == 0 && alpha > 0) {
                // float falpha = alpha(color);
                // // Logger.println("black with non 0 alpha :) " + falpha + " : " + alpha);
                // }
            }
        }
        return blockPixels;
    }

    /**
     * Find an approximate match for a block using dominant colors
     */
    private ResultGlyph findApproximateMatch(int[] blockPixels) {
        // Extrahiere den durchschnittlichen Alpha-Wert für Debug-Zwecke
        int totalAlpha = 0;
        for (int pixel : blockPixels) {
            totalAlpha += (pixel >> 24) & 0xFF;
        }
        int avgAlpha = totalAlpha / blockPixels.length;
        
        // Find two dominant colors
        int[] dominantIndices = findDominantPaletteColors(blockPixels);
        int color1Index = dominantIndices[0];
        int color2Index = dominantIndices[1];

        // Find best character and color combination
        int bestCodePoint = 0;
        int bestFgIndex = color1Index;
        int bestBgIndex = color2Index;
        double minError = Double.MAX_VALUE;

        // Iterate through all glyph patterns
        for (Map.Entry<Integer, Long> entry : asciiPatterns.entrySet()) {
            int currentCodePoint = entry.getKey();
            long currentPattern = entry.getValue();

            // Calculate error for both color assignments
            double errorA = calculateMatchError(currentPattern, color1Index, color2Index, blockPixels);
            double errorB = calculateMatchError(currentPattern, color2Index, color1Index, blockPixels);

            // Update best match
            if (errorA < minError) {
                minError = errorA;
                bestCodePoint = currentCodePoint;
                bestFgIndex = color1Index;
                bestBgIndex = color2Index;
            }

            if (errorB < minError) {
                minError = errorB;
                bestCodePoint = currentCodePoint;
                bestFgIndex = color2Index;
                bestBgIndex = color1Index;
            }
        }

        // Erstelle ResultGlyph mit Alpha-Wert
        return new ResultGlyph(bestCodePoint, bestFgIndex, bestBgIndex, avgAlpha);
    }

    /**
     * Generate ASCII art using approximation only (faster)
     */
    void generateAsciiArtApproxOnly() {
        for (int gridY = 0; gridY < gridHeight; gridY++) {
            for (int gridX = 0; gridX < gridWidth; gridX++) {
                int[] blockPixels = extractBlockPixels(gridX, gridY);
                resultGrid[gridY][gridX] = findApproximateMatch(blockPixels);
            }

            // Show progress
            if ((gridY + 1) % 10 == 0 || gridY == gridHeight - 1) {
                Logger.println("Processed row " + (gridY + 1) + "/" + gridHeight);
            }
        }
    }

    /**
     * Find the two most dominant palette colors in a block of pixels
     */
    int[] findDominantPaletteColors(int[] blockPixels) {
        int[] counts = new int[256];

        // Count occurrences of each palette color
        for (int i = 0; i < PIXEL_COUNT; i++) {
            int nearestIndex = findNearestPaletteIndex(blockPixels[i]);
            counts[nearestIndex]++;
        }

        // Find the two most frequent colors
        int bestIndex1 = -1, bestIndex2 = -1;
        int maxCount1 = -1, maxCount2 = -1;

        for (int i = 0; i < 256; i++) {
            if (counts[i] > maxCount1) {
                maxCount2 = maxCount1;
                bestIndex2 = bestIndex1;
                maxCount1 = counts[i];
                bestIndex1 = i;
            } else if (counts[i] > maxCount2) {
                maxCount2 = counts[i];
                bestIndex2 = i;
            }
        }

        // Handle edge cases
        if (bestIndex1 == -1) {
            bestIndex1 = 0;
        }

        if (bestIndex2 == -1 || bestIndex2 == bestIndex1) {
            bestIndex2 = (bestIndex1 == 0) ? 15 : 0; // Default to white or black

            // Try to find a third color if the first two are the same
            int thirdMaxCount = -1;
            int thirdBestIndex = -1;
            for (int i = 0; i < 256; i++) {
                if (i != bestIndex1 && counts[i] > thirdMaxCount) {
                    thirdMaxCount = counts[i];
                    thirdBestIndex = i;
                }
            }

            if (thirdBestIndex != -1) {
                bestIndex2 = thirdBestIndex;
            }
        }

        return new int[] { bestIndex1, bestIndex2 };
    }

    /**
     * Calculate the error between a pattern with given colors and original pixels
     */
    double calculateMatchError(long pattern, int fgIndex, int bgIndex, int[] originalBlockPixels) {
        double totalError = 0;
        int fgColor = colorPalette[fgIndex];
        int bgColor = colorPalette[bgIndex];

        // Extract RGB components
        float fgR = red(fgColor);
        float fgG = green(fgColor);
        float fgB = blue(fgColor);
        float bgR = red(bgColor);
        float bgG = green(bgColor);
        float bgB = blue(bgColor);

        // Calculate error for each pixel
        for (int i = 0; i < PIXEL_COUNT; i++) {
            boolean pixelOn = ((pattern >> i) & 1L) == 1L;

            // Use foreground or background color based on pattern
            float simR = pixelOn ? fgR : bgR;
            float simG = pixelOn ? fgG : bgG;
            float simB = pixelOn ? fgB : bgB;

            // Get original pixel color
            int originalColor = originalBlockPixels[i];
            float origR = red(originalColor);
            float origG = green(originalColor);
            float origB = blue(originalColor);

            // Sum of squared differences per channel
            totalError += (origR - simR) * (origR - simR) +
                    (origG - simG) * (origG - simG) +
                    (origB - simB) * (origB - simB);
        }

        return totalError;
    }

    /**
     * Simulate a block of pixels using a pattern and colors
     */
    int[] simulateBlock(long pattern, int fgColor, int bgColor) {
        int[] pixels = new int[PIXEL_COUNT];
        for (int i = 0; i < PIXEL_COUNT; i++) {
            boolean pixelOn = ((pattern >> i) & 1L) == 1L;
            pixels[i] = pixelOn ? fgColor : bgColor;
        }
        return pixels;
    }

    /**
     * Compare two blocks of pixels for exact equality
     */
    boolean compareBlocksExactly(int[] blockA, int[] blockB) {
        for (int i = 0; i < PIXEL_COUNT; i++) {
            if (blockA[i] != blockB[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Try to find an exact match for a block of pixels
     * With improved support for single-color blocks and alpha channel
     */
    ResultGlyph findExactMatch(int[] blockPixels) {
        // Extrahiere den durchschnittlichen Alpha-Wert für Debug-Zwecke
        int totalAlpha = 0;
        for (int pixel : blockPixels) {
            totalAlpha += (pixel >> 24) & 0xFF;
        }
        int avgAlpha = totalAlpha / blockPixels.length;
        
        Set<Integer> uniqueIndices = new HashSet<>();
        int[] quantizedIndices = new int[PIXEL_COUNT];

        // Quantize pixels to palette indices
        for (int i = 0; i < PIXEL_COUNT; i++) {
            int nearestIndex = findNearestPaletteIndex(blockPixels[i]);
            uniqueIndices.add(nearestIndex);
            quantizedIndices[i] = nearestIndex;
        }

        // Special case: Single-color block
        if (uniqueIndices.size() == 1) {
            int singleIndex = uniqueIndices.iterator().next();
            // Find a solid block pattern (either all on or all off)
            Long solidPattern = null;
            
            // Try to find either a completely filled or completely empty glyph
            for (Map.Entry<Integer, Long> entry : asciiPatterns.entrySet()) {
                long pattern = entry.getValue();
                boolean isAllOn = pattern == -1L; // All bits set (all pixels on)
                boolean isAllOff = pattern == 0L; // All bits clear (all pixels off)
                
                if (isAllOn || isAllOff) {
                    solidPattern = pattern;
                    int codePoint = entry.getKey();
                    // For solid blocks, we'll make both FG and BG the same color
                    // Speichere Alpha-Wert für besseres Debugging
                    return new ResultGlyph(codePoint, singleIndex, singleIndex, avgAlpha);
                }
            }
            
            // If no solid pattern found, continue to normal matching process
            // by finding a second color to use with the block
        }

        // Exact match only possible with exactly 2 colors (or one color with special handling)
        if (uniqueIndices.size() != 2) {
            return null;
        }

        // Get the two unique colors
        Integer[] indices = uniqueIndices.toArray(new Integer[0]);
        int indexA = indices[0];
        int indexB = indices[1];
        int colorA = colorPalette[indexA];
        int colorB = colorPalette[indexB];

        // Try each glyph pattern
        for (Map.Entry<Integer, Long> entry : asciiPatterns.entrySet()) {
            int currentCodePoint = entry.getKey();
            long currentPattern = entry.getValue();

            // Try color combination A
            boolean matchA = true;
            for (int i = 0; i < PIXEL_COUNT; i++) {
                boolean pixelOn = ((currentPattern >> i) & 1L) == 1L;
                int simulatedQuantizedIndex = pixelOn ? indexA : indexB;
                if (simulatedQuantizedIndex != quantizedIndices[i]) {
                    matchA = false;
                    break;
                }
            }

            if (matchA) {
                int[] simulatedExactA = simulateBlock(currentPattern, colorA, colorB);
                if (compareBlocksExactly(blockPixels, simulatedExactA)) {
                    // Alpha-Wert mit speichern
                    return new ResultGlyph(currentCodePoint, indexA, indexB, avgAlpha);
                }
            }

            // Try color combination B
            boolean matchB = true;
            for (int i = 0; i < PIXEL_COUNT; i++) {
                boolean pixelOn = ((currentPattern >> i) & 1L) == 1L;
                int simulatedQuantizedIndex = pixelOn ? indexB : indexA;
                if (simulatedQuantizedIndex != quantizedIndices[i]) {
                    matchB = false;
                    break;
                }
            }

            if (matchB) {
                int[] simulatedExactB = simulateBlock(currentPattern, colorB, colorA);
                if (compareBlocksExactly(blockPixels, simulatedExactB)) {
                    // Alpha-Wert mit speichern
                    return new ResultGlyph(currentCodePoint, indexB, indexA, avgAlpha);
                }
            }
        }

        return null; // No exact match found
    }

    /**
     * Find the nearest palette color index for an RGB color
     * Mit verbesserter Alpha-Kanal-Unterstützung und speziellem Handling für Schwarz
     */
    int findNearestPaletteIndex(int rgbColor) {
        // Extract alpha channel value
        int alpha = (rgbColor >> 24) & 0xFF;

        // Nur vollständig transparente Pixel (alpha = 0) als transparent behandeln
        if (alpha == 0) {
            return 0; // Index 0 ist für vollständige Transparenz reserviert
        }

        // Wenn der Pixel schwarz oder nahezu schwarz und opak ist,
        // direkt opakes Schwarz zurückgeben (Index 1)
        float r = red(rgbColor);
        float g = green(rgbColor);
        float b = blue(rgbColor);
        if (r <= 5 && g <= 5 && b <= 5 && alpha > 200) {
            return 1; // Index 1 ist opakes Schwarz
        }

        // Für alle anderen Pixel (einschließlich schwarzer Pixel mit Alpha > 0)
        // finde den nächsten Farbindex in der Palette
        int bestIndex = 1; // Starte bei 1, da 0 für Transparenz reserviert ist
        double minDistSq = Double.MAX_VALUE;

        // Berücksichtige alle Nicht-Transparenz-Indizes (ab Index 1)
        for (int i = 1; i < colorPalette.length; i++) {
            int palColor = colorPalette[i];
            float r2 = red(palColor);
            float g2 = green(palColor);
            float b2 = blue(palColor);

            // Berechne die RGB-Distanz (ohne Alpha-Komponente)
            double distSq = (r - r2) * (r - r2) +
                    (g - g2) * (g - g2) +
                    (b - b2) * (b - b2);

            if (distSq < minDistSq) {
                minDistSq = distSq;
                bestIndex = i;
            }

            if (minDistSq == 0) {
                break; // Exakter Match gefunden
            }
        }

        return bestIndex;
    }

    // ========== DRAWING METHODS ==========

    /**
     * Draw the welcome screen
     */
    private void drawWelcomeScreen() {
        background(0);
        textFont(unscii);
        textSize(16);
        textAlign(CENTER, CENTER);
        text("Unscii Generator", width / 2, height / 2);
    }

    /**
     * Draw the source image
     */
    private void drawSourceImage() {
        if (inputImage == null)
            return;

        int cellWidth = GLYPH_WIDTH * DISPLAY_SCALE;
        int cellHeight = GLYPH_HEIGHT * DISPLAY_SCALE;
        int totalGridWidthPixels = gridWidth * cellWidth;
        int totalGridHeightPixels = gridHeight * cellHeight;

        // Berechne die Position des Grids genau wie in drawResult()
        int gridOriginX = (width - totalGridWidthPixels) / 2 + drawX - width / 2;
        int gridOriginY = (height - totalGridHeightPixels) / 2 + drawY - height / 2;

        // Zeichne das Bild genau so groß wie das Grid, damit es deckungsgleich ist
        image(inputImage, gridOriginX, gridOriginY, totalGridWidthPixels, totalGridHeightPixels);

        // Speichere die berechneten Dimensionen für andere Methoden
        drawW = totalGridWidthPixels;
        drawH = totalGridHeightPixels;

        // Clear selection info when showing source image
        if (controlPanel != null) {
            controlPanel.updateSelectionInfo(-1, -1, null);
        }

        mouseGridX = -1;
        mouseGridY = -1;
        selectedGlyph = null;
    }

    /**
     * Draw the ASCII art result
     */
    void drawResult() {
        if (resultGrid == null) {
            return;
        }

        int cellWidth = GLYPH_WIDTH * DISPLAY_SCALE;
        int cellHeight = GLYPH_HEIGHT * DISPLAY_SCALE;

        int totalGridWidthPixels = gridWidth * cellWidth;
        int totalGridHeightPixels = gridHeight * cellHeight;
        int gridOriginX = (width - totalGridWidthPixels) / 2 + drawX - width / 2;
        int gridOriginY = (height - totalGridHeightPixels) / 2 + drawY - height / 2;

        // Draw all glyphs
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                // check for bounds
                if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) {
                    return;
                }
                ResultGlyph glyphInfo = null;
                try {
                    glyphInfo = resultGrid[y][x];
                } catch (Exception e) {
                    Logger.println("ArrayIndexOutOfBoundsException: " + e.getMessage());
                    return;
                }
                if (glyphInfo == null) {
                    return;
                }

                long pattern = asciiPatterns.getOrDefault(glyphInfo.codePoint, 0L);
                int fgColor = colorPalette[glyphInfo.fgIndex];
                int bgColor = colorPalette[glyphInfo.bgIndex];
                int screenX = gridOriginX + x * cellWidth;
                int screenY = gridOriginY + y * cellHeight;

                // Verwende den im ResultGlyph gespeicherten Alpha-Wert
                displayScaledGlyph(pattern, screenX, screenY, DISPLAY_SCALE, bgColor, fgColor, glyphInfo.alpha);
            }
        }

        // Handle hover highlight
        handleHoverHighlight(gridOriginX, gridOriginY, cellWidth, cellHeight, totalGridWidthPixels,
                totalGridHeightPixels);

        // Handle clicked highlight
        handleClickedHighlight(gridOriginX, gridOriginY, cellWidth, cellHeight);

        // Draw Selection Rectangle
        drawSelectionRectangle(gridOriginX, gridOriginY, cellWidth, cellHeight);
    }

    /**
     * Handle hover highlight in result view
     */
    private void handleHoverHighlight(int gridOriginX, int gridOriginY, int cellWidth, int cellHeight,
            int totalGridWidthPixels, int totalGridHeightPixels) {
        int mouseRelativeX = mouseX - gridOriginX;
        int mouseRelativeY = mouseY - gridOriginY;
        int currentMouseGridX = mouseRelativeX / cellWidth;
        int currentMouseGridY = mouseRelativeY / cellHeight;

        boolean hoverValid = mouseRelativeX >= 0 && mouseRelativeX < totalGridWidthPixels &&
                mouseRelativeY >= 0 && mouseRelativeY < totalGridHeightPixels &&
                currentMouseGridX >= 0 && currentMouseGridX < gridWidth &&
                currentMouseGridY >= 0 && currentMouseGridY < gridHeight;

        if (hoverValid) {
            // Update hover state
            this.mouseGridX = currentMouseGridX;
            this.mouseGridY = currentMouseGridY;
            this.selectedGlyph = resultGrid[this.mouseGridY][this.mouseGridX];

            // Update Control Panel (Hover Info)
            if (controlPanel != null) {
                controlPanel.updateSelectionInfo(this.mouseGridX, this.mouseGridY, this.selectedGlyph);
            }

            // Draw Hover Highlight Rectangle
            int highlightX = gridOriginX + this.mouseGridX * cellWidth;
            int highlightY = gridOriginY + this.mouseGridY * cellHeight;
            noFill();
            stroke(255, 255, 0); // Yellow outline for hover
            strokeWeight(1);
            rect(highlightX, highlightY, cellWidth, cellHeight);
            noStroke();
        } else {
            // Reset hover state if mouse is outside
            this.mouseGridX = -1;
            this.mouseGridY = -1;
            this.selectedGlyph = null;

            // Update Control Panel (Hover Info)
            if (controlPanel != null) {
                controlPanel.updateSelectionInfo(-1, -1, null);
            }
        }
    }

    /**
     * Handle clicked highlight in result view
     */
    private void handleClickedHighlight(int gridOriginX, int gridOriginY, int cellWidth, int cellHeight) {
        if (clickedGridX >= 0 && clickedGridY >= 0) {
            int clickedHighlightX = gridOriginX + clickedGridX * cellWidth;
            int clickedHighlightY = gridOriginY + clickedGridY * cellHeight;
            noFill();
            stroke(0, 255, 255); // Cyan outline for clicked
            strokeWeight(2); // Make it slightly thicker
            rect(clickedHighlightX + 1, clickedHighlightY + 1, cellWidth - 2, cellHeight - 2); // Inset slightly
            noStroke();
        }
    }

    /**
     * Draws the selection rectangle if one is active or exists.
     */
    private void drawSelectionRectangle(int gridOriginX, int gridOriginY, int cellWidth, int cellHeight) {
        if (isSelecting || hasSelection) {
            // Normalize coordinates to find top-left and bottom-right
            int minX = min(selectionStartX, selectionEndX);
            int minY = min(selectionStartY, selectionEndY);
            int maxX = max(selectionStartX, selectionEndX);
            int maxY = max(selectionStartY, selectionEndY);

            if (minX == -1 || minY == -1)
                return; // Invalid selection state

            float rectX = gridOriginX + minX * cellWidth;
            float rectY = gridOriginY + minY * cellHeight;
            float rectW = (maxX - minX + 1) * cellWidth;
            float rectH = (maxY - minY + 1) * cellHeight;

            noFill();
            stroke(255, 255, 255, 200); // White, slightly transparent outline
            strokeWeight(1.5f);
            rect(rectX, rectY, rectW, rectH);
            noStroke();
        }
    }

    /**
     * Display a scaled glyph with the given pattern and colors
     * Now with improved transparency support using alpha values
     */
    void displayScaledGlyph(long pattern, int screenX, int screenY, int pixelSize, int bgCol, int fgCol,
            int alphaValue) {
        noStroke();

        // Determine transparency based on the stored alpha value
        boolean isTransparentBg = bgCol == colorPalette[0] || alphaValue < 5;

        for (int y = 0; y < GLYPH_HEIGHT; y++) {
            for (int x = 0; x < GLYPH_WIDTH; x++) {
                int bitIndex = y * GLYPH_WIDTH + x;
                boolean pixelOn = ((pattern >> bitIndex) & 1L) == 1L;

                // Set the fill color
                int pixelColor = pixelOn ? fgCol : bgCol;

                // Apply the alpha value to non-transparent pixels for debugging visibility
                if (pixelColor != colorPalette[0]) {
                    // Only modify the alpha if it's not already transparent
                    int alpha = (pixelColor >> 24) & 0xFF;
                    if (alpha > 0) {
                        pixelColor = (pixelColor & 0x00FFFFFF) | (alphaValue << 24);
                    }
                }

                // Only draw pixel if it's not transparent background
                if (!isTransparentBg || pixelOn) {
                    fill(pixelColor);
                    rect(screenX + x * pixelSize, screenY + y * pixelSize, pixelSize, pixelSize);
                }
            }
        }
    }

    // ========== USER INTERACTION ==========

    @Override
    public void mousePressed() {
        clickStart = System.currentTimeMillis();
        startedDragging = false; // Reset dragging flag on new press
        isSelecting = false; // Reset selection drag flag

        if (!showSourceImage && resultGrid != null) {
            // Calculate grid cell dimensions and origin
            int cellWidth = GLYPH_WIDTH * DISPLAY_SCALE;
            int cellHeight = GLYPH_HEIGHT * DISPLAY_SCALE;
            int totalGridWidthPixels = gridWidth * cellWidth;
            int totalGridHeightPixels = gridHeight * cellHeight;
            int gridOriginX = (width - totalGridWidthPixels) / 2 + drawX - width / 2;
            int gridOriginY = (height - totalGridHeightPixels) / 2 + drawY - height / 2;

            int mouseRelativeX = mouseX - gridOriginX;
            int mouseRelativeY = mouseY - gridOriginY;
            int gridClickX = mouseRelativeX / cellWidth;
            int gridClickY = mouseRelativeY / cellHeight;

            // Check if the click is within the valid grid boundaries
            boolean clickInGrid = mouseRelativeX >= 0 && mouseRelativeX < totalGridWidthPixels &&
                    mouseRelativeY >= 0 && mouseRelativeY < totalGridHeightPixels &&
                    gridClickX >= 0 && gridClickX < gridWidth &&
                    gridClickY >= 0 && gridClickY < gridHeight &&
                    mouseButton == LEFT;

            if (clickInGrid) {
                // Start selection drag
                isSelecting = true;
                // Speichere den alten Zustand, bevor wir ihn ändern
                boolean oldHasSelection = hasSelection;
                int oldStartX = selectionStartX;
                int oldStartY = selectionStartY;
                int oldEndX = selectionEndX;
                int oldEndY = selectionEndY;

                hasSelection = false; // Clear previous final selection
                selectionStartX = gridClickX;
                selectionStartY = gridClickY;
                selectionEndX = gridClickX; // Init end to start
                selectionEndY = gridClickY;

                // Erstelle ein SelectionCommand für die Änderung der Selektion
                if (oldHasSelection) {
                    // Nur wenn vorher eine Selektion aktiv war, erstellen wir ein Command
                    setSelection(false, -1, -1, -1, -1);
                }

                // Clear single-cell click selection when starting drag selection
                clickedGridX = -1;
                clickedGridY = -1;
                clickedGlyph = null;
                if (controlPanel != null) {
                    controlPanel.updateClickedInfo(-1, -1, null, colorPalette, asciiPatterns);
                }
                Logger.println("Selection started at: (" + selectionStartX + "," + selectionStartY + ")");

            } else {
                // Click outside grid: Clear single-cell click AND selection area

                // Wenn eine Selektion aktiv war, speichere sie für Undo
                if (hasSelection) {
                    int oldStartX = selectionStartX;
                    int oldStartY = selectionStartY;
                    int oldEndX = selectionEndX;
                    int oldEndY = selectionEndY;

                    // Erstelle ein SelectionCommand, um das Aufheben der Selektion rückgängig
                    // machen zu können
                    setSelection(false, -1, -1, -1, -1);
                } else {
                    // Bei keiner aktiven Selektion einfach zurücksetzen ohne Command
                    hasSelection = false;
                    selectionStartX = selectionStartY = selectionEndX = selectionEndY = -1;
                }

                clickedGridX = -1;
                clickedGridY = -1;
                clickedGlyph = null;
                if (controlPanel != null) {
                    controlPanel.updateClickedInfo(-1, -1, null, colorPalette, asciiPatterns);
                }
                Logger.println("Clicked outside grid, selection cleared.");
            }
        } else {
            // Source image shown or no grid: Clear single-cell click AND selection area

            // Wenn eine Selektion aktiv war, speichere sie für Undo
            if (hasSelection) {
                int oldStartX = selectionStartX;
                int oldStartY = selectionStartY;
                int oldEndX = selectionEndX;
                int oldEndY = selectionEndY;

                // Erstelle ein SelectionCommand, um das Aufheben der Selektion rückgängig
                // machen zu können
                setSelection(false, -1, -1, -1, -1);
            } else {
                // Bei keiner aktiven Selektion einfach zurücksetzen ohne Command
                hasSelection = false;
                selectionStartX = selectionStartY = selectionEndX = selectionEndY = -1;
            }

            clickedGridX = -1;
            clickedGridY = -1;
            clickedGlyph = null;
            if (controlPanel != null) {
                controlPanel.updateClickedInfo(-1, -1, null, colorPalette, asciiPatterns);
            }
        }
    }

    @Override
    public void mouseDragged() {

        int deltaX = mouseX - pmouseX;
        int deltaY = mouseY - pmouseY;
        int totalDelta = abs(deltaX) + abs(deltaY);

        boolean draggedSignificantly = totalDelta > 3; // Small threshold to differentiate click from drag
        if (!startedDragging && draggedSignificantly) {
            startedDragging = true; // Mark that a drag has actually started
        }

        if (mouseButton == RIGHT) {

            // --- Panning Logic ---
            isSelecting = false; // Stop selection if space is pressed mid-drag
            // Pan only if dragging has started
            if (startedDragging) {
                drawX += deltaX;
                drawY += deltaY;
            }
        }

        if (isSelecting) {
            // --- Selection Logic ---
            // Calculate grid cell dimensions and origin (needed to find current grid cell)
            int cellWidth = GLYPH_WIDTH * DISPLAY_SCALE;
            int cellHeight = GLYPH_HEIGHT * DISPLAY_SCALE;
            int totalGridWidthPixels = gridWidth * cellWidth;
            int totalGridHeightPixels = gridHeight * cellHeight;
            int gridOriginX = (width - totalGridWidthPixels) / 2 + drawX - width / 2;
            int gridOriginY = (height - totalGridHeightPixels) / 2 + drawY - height / 2;

            // Calculate current grid coordinates under mouse, clamped to grid bounds
            int mouseRelativeX = mouseX - gridOriginX;
            int mouseRelativeY = mouseY - gridOriginY;
            int currentGridX = constrain(mouseRelativeX / cellWidth, 0, gridWidth - 1);
            int currentGridY = constrain(mouseRelativeY / cellHeight, 0, gridHeight - 1);

            // Update selection end coordinates only if dragging has started
            if (startedDragging) {
                selectionEndX = currentGridX;
                selectionEndY = currentGridY;
                hasSelection = false; // Selection is not final until mouse release
            }
        }

    }

    @Override
    public void mouseReleased() {
        Logger.print("Mouse released. ");
        if (isSelecting) {
            isSelecting = false; // Selection drag finished
            // Check if selection is valid (start coords are valid)
            if (selectionStartX != -1 && selectionStartY != -1) {
                // If end coords are still -1 (no drag occurred), make selection 1x1
                if (selectionEndX == -1)
                    selectionEndX = selectionStartX;
                if (selectionEndY == -1)
                    selectionEndY = selectionStartY;

                // Check if the selection is just a single cell (no actual drag)
                if (selectionStartX == selectionStartX && selectionStartY == selectionEndY && !startedDragging) {
                    // Es war nur ein Klick, keine richtige Selektion
                    selectionStartX = selectionStartY = selectionEndX = selectionEndY = -1;
                    hasSelection = false;
                } else {
                    // Erstelle ein SelectionCommand, um die Selektion zu setzen

                    setSelection(true, selectionStartX, selectionStartY, selectionEndX, selectionEndY);

                    // Normalize coordinates for logging
                    int minX = min(selectionStartX, selectionEndX);
                    int minY = min(selectionStartY, selectionEndY);
                    int maxX = max(selectionStartX, selectionEndX);
                    int maxY = max(selectionStartY, selectionEndY);
                    Logger.println("Selection finished: (" + minX + "," + minY + ") to (" + maxX + "," + maxY + ")");
                }
            } else {
                hasSelection = false; // Invalid selection start state
                Logger.println("Selection finished but was invalid.");
            }
        }

        // --- Handle single cell click ---
        // Occurs if:
        // - Not panning (spacebar wasn't down during press/drag/release)
        // - Not a finalized selection drag (either !hasSelection or it was reset above
        // because no drag occurred)
        // - Dragging didn't actually start (or was negligible)
        if (!hasSelection && !startedDragging && mouseButton == LEFT) {
            // Calculate grid cell dimensions and origin
            int cellWidth = GLYPH_WIDTH * DISPLAY_SCALE;
            int cellHeight = GLYPH_HEIGHT * DISPLAY_SCALE;
            int totalGridWidthPixels = gridWidth * cellWidth;
            int totalGridHeightPixels = gridHeight * cellHeight;
            int gridOriginX = (width - totalGridWidthPixels) / 2 + drawX - width / 2;
            int gridOriginY = (height - totalGridHeightPixels) / 2 + drawY - height / 2;

            int mouseRelativeX = mouseX - gridOriginX;
            int mouseRelativeY = mouseY - gridOriginY;
            int gridClickX = mouseRelativeX / cellWidth;
            int gridClickY = mouseRelativeY / cellHeight;

            boolean clickInGrid = mouseRelativeX >= 0 && mouseRelativeX < totalGridWidthPixels &&
                    mouseRelativeY >= 0 && mouseRelativeY < totalGridHeightPixels &&
                    gridClickX >= 0 && gridClickX < gridWidth &&
                    gridClickY >= 0 && gridClickY < gridHeight;

            if (clickInGrid) {
                clickedGridX = gridClickX;
                clickedGridY = gridClickY;
                clickedGlyph = resultGrid[clickedGridY][gridClickX];
                if (controlPanel != null) {
                    controlPanel.updateClickedInfo(clickedGridX, clickedGridY, clickedGlyph, colorPalette,
                            asciiPatterns);
                }
                Logger.println("Single cell clicked: X=" + clickedGridX + ", Y=" + clickedGridY);
                // Ensure selection variables are reset if it was just a click
                selectionStartX = selectionStartY = selectionEndX = selectionEndY = -1;
            }
        }

        // Reset flags for next interaction
        startedDragging = false;
        isSelecting = false; // Ensure this is false after release
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        float e = event.getCount();
        int currentScale = DISPLAY_SCALE;

        // Determine target scale based on scroll direction
        int targetScale = currentScale;
        if (e < 0) { // Scroll Up / Zoom In
            targetScale++;
        } else if (e > 0) { // Scroll Down / Zoom Out
            targetScale--;
        }

        // Clamp scale between 1 and 8
        targetScale = constrain(targetScale, 1, 8);

        // Apply the new scale if it changed
        if (targetScale != currentScale) {
            setDisplayScale(targetScale);
        }
    }

    @Override
    public void keyPressed() {
        // Track spacebar down state
        if (key == ' ') {
            isSpacebarDown = true;
        }

        // Pass non-coded keys (and not space) to the char handler
        if (key != CODED && key != ' ') {
            handleCharacterKey(key);
        }
    }

    @Override
    public void keyReleased() {
        // Track spacebar up state
        if (key == ' ') {
            isSpacebarDown = false;
        }
    }

    public void keyPressed(char k) {
        handleCharacterKey(k);
    }

    @Override
    public void keyPressed(KeyEvent event) {
        // Check if controlPanel exists and is in EDIT state
        if (controlPanel == null || controlPanel.getPanelState() != ControlPanel.PanelState.EDIT) {
            return;
        }

        char keyChar = event.getKey();
        boolean isMeta = event.isMetaDown(); // Cmd on macOS
        boolean isCtrl = event.isControlDown(); // Ctrl on Win/Linux
        boolean isShift = event.isShiftDown();
        boolean isAlt = event.isAltDown();

        // Use isMetaDown for macOS Cmd key, isControlDown otherwise
        boolean primaryModifier = (System.getProperty("os.name").toLowerCase().contains("mac")) ? isMeta : isCtrl;

        boolean handled = false; // Flag to check if we handled the shortcut

        // --- Handle Modifier Shortcuts ---
        if (primaryModifier && !isAlt) { // Cmd/Ctrl pressed (without Alt)
            char lowerKeyChar = Character.toLowerCase(keyChar);

            if (lowerKeyChar == 'z') {
                // Cmd/Ctrl + Z -> Undo
                Logger.println("ProcessingCore: Shortcut Cmd/Ctrl+Z detected.");
                undo();
                handled = true;
            } else if (lowerKeyChar == 'y' || (lowerKeyChar == 'z' && isShift)) {
                // Cmd/Ctrl + Y or Cmd/Ctrl + Shift + Z -> Redo
                Logger.println("ProcessingCore: Shortcut " +
                        (lowerKeyChar == 'y' ? "Cmd/Ctrl+Y" : "Cmd/Ctrl+Shift+Z") + " detected.");
                redo();
                handled = true;
            } else if (lowerKeyChar == 'c') {
                if (isShift) {
                    // Cmd/Ctrl + Shift + C -> Copy Colors
                    Logger.println("ProcessingCore: Shortcut Cmd/Ctrl+Shift+C detected.");
                    controlPanel.copyInternalColors();
                    handled = true;
                } else {
                    // Cmd/Ctrl + C -> Copy Glyph (Internal + External Char)
                    Logger.println("ProcessingCore: Shortcut Cmd/Ctrl+C detected.");
                    controlPanel.copyInternalGlyphAndExternalChar();
                    handled = true;
                }
            } else if (lowerKeyChar == 'v') {
                if (isShift) {
                    // Cmd/Ctrl + Shift + V -> Paste Colors
                    Logger.println("ProcessingCore: Shortcut Cmd/Ctrl+Shift+V detected.");
                    controlPanel.pasteInternalColors();
                    handled = true;
                } else {
                    // Cmd/Ctrl + V -> Paste Char (External)
                    Logger.println("ProcessingCore: Shortcut Cmd/Ctrl+V detected.");
                    controlPanel.pasteCharacterFromClipboard();
                    handled = true;
                }
            } else if (lowerKeyChar == 'f') {
                if (!isShift) {
                    // Cmd/Ctrl + F -> Flip Colors
                    Logger.println("ProcessingCore: Shortcut Cmd/Ctrl+F detected.");
                    flipClickedGlyphColors();
                    handled = true;
                }
            }
        } else if (primaryModifier && isAlt && !isShift) { // Cmd/Ctrl + Alt pressed (without Shift)
            char lowerKeyChar = Character.toLowerCase(keyChar);
            if (lowerKeyChar == 'v') {
                // Cmd/Ctrl + Alt + V -> Paste Glyph (Internal)
                Logger.println("ProcessingCore: Shortcut Cmd/Ctrl+Alt+V detected.");
                controlPanel.pasteInternalGlyph();
                handled = true;
            }
        }
    }

    /**
     * Undo-Methode für die UI-Taste
     * Wird vom ControlPanel aufgerufen
     */
    public void undoAction() {
        undo();
    }

    /**
     * Redo-Methode für die UI-Taste
     * Wird vom ControlPanel aufgerufen
     */
    public void redoAction() {
        redo();
    }

    /**
     * Helper method to handle simple character key presses (s, p, 1-8, r, l).
     * Called directly from the simple `keyPressed()` method.
     * 
     * @param k The character pressed.
     */
    private void handleCharacterKey(char k) {
        // Convert to lowercase for case-insensitive comparison for letters
        char lowerK = Character.toLowerCase(k);

        if (lowerK == 's') {
            showSourceImage = !showSourceImage;
            Logger.println("Toggled view: " + (showSourceImage ? "Source Image" : "ASCII Art"));
        } else if (lowerK == 'p') {
            File outputFile = FileHandler.saveFile("Save Result");
            if (outputFile == null) {
                Logger.println("No file selected for saving.");
                return;
            }
            outputPath = outputFile.getAbsolutePath();
            FileHandler.saveResult(outputPath, this);
        } else if (k >= '1' && k <= '8') {
            int targetScale = k - '0';
            setDisplayScale(targetScale);
        } else if (lowerK == 'r') {
            reloadCurrentImage();
        } else if (lowerK == 'l') {
            loadImage();
        }
    }

    /**
     * Swaps the foreground and background color indices of the currently selected
     * glyph.
     * Called from the ControlPanel or via shortcut (Cmd/Ctrl+F).
     */
    public void flipClickedGlyphColors() {
        if (clickedGlyph != null && clickedGridX >= 0 && clickedGridY >= 0) {
            // Erstelle eine neue Glyphe mit vertauschten Farben
            ResultGlyph newGlyph = new ResultGlyph(
                    clickedGlyph.codePoint,
                    clickedGlyph.bgIndex, // Tausche fg und bg
                    clickedGlyph.fgIndex);

            // Erstelle und führe das Command aus
            GlyphChangeCommand cmd = new GlyphChangeCommand(this, clickedGridX, clickedGridY, newGlyph);
            commandManager.executeCommand(cmd);

            Logger.println("Flipped colors at (" + clickedGridX + "," + clickedGridY + ")");
        } else {
            Logger.println("No glyph selected to flip colors.");
        }
    }

    /**
     * Replaces the character (codepoint) of the currently selected glyph.
     * Called potentially from the ControlPanel.
     * 
     * @param newChar The new character to use.
     */
    public void replaceClickedGlyph(char newChar) {
        if (clickedGlyph != null && clickedGridX >= 0 && clickedGridY >= 0) {
            int newCodePoint = (int) newChar;

            // Check if the new codepoint has a pattern available
            if (asciiPatterns.containsKey(newCodePoint)) {
                // Erstelle eine neue Glyphe mit geändertem codePoint
                ResultGlyph newGlyph = new ResultGlyph(
                        newCodePoint,
                        clickedGlyph.fgIndex,
                        clickedGlyph.bgIndex);

                // Erstelle und führe das Command aus
                GlyphChangeCommand cmd = new GlyphChangeCommand(this, clickedGridX, clickedGridY, newGlyph);
                commandManager.executeCommand(cmd);

                Logger.println("Replaced glyph at (" + clickedGridX + "," + clickedGridY + ") with char '" + newChar
                        + "' (Codepoint: " + newCodePoint + ")");
            } else {
                Logger.println("Error: Character '" + newChar + "' (Codepoint: " + newCodePoint
                        + ") not found in font patterns. Cannot replace.");
            }
        } else {
            Logger.println("No glyph selected to replace.");
        }
    }

    /**
     * Replaces only the foreground and background colors of the currently selected
     * glyph.
     * Called from the ControlPanel (e.g., Paste Colors).
     * 
     * @param fgIndex The new foreground color index (0-255).
     * @param bgIndex The new background color index (0-255).
     */
    public void replaceClickedGlyphColors(int fgIndex, int bgIndex) {
        if (clickedGlyph != null && clickedGridX >= 0 && clickedGridY >= 0) {
            // Validate indices (optional but recommended)
            if (fgIndex < 0 || fgIndex >= colorPalette.length || bgIndex < 0 || bgIndex >= colorPalette.length) {
                Logger.println("Error: Invalid color indices provided (FG: " + fgIndex + ", BG: " + bgIndex
                        + "). Must be between 0 and 255.");
                return;
            }

            // Erstelle eine neue Glyphe mit neuen Farben
            ResultGlyph newGlyph = new ResultGlyph(
                    clickedGlyph.codePoint,
                    fgIndex,
                    bgIndex);

            // Erstelle und führe das Command aus
            GlyphChangeCommand cmd = new GlyphChangeCommand(this, clickedGridX, clickedGridY, newGlyph);
            commandManager.executeCommand(cmd);

            Logger.println("Replaced colors at (" + clickedGridX + "," + clickedGridY + ") with FG=" + fgIndex + ", BG="
                    + bgIndex);
        } else {
            Logger.println("No glyph selected to replace colors.");
        }
    }

    /**
     * Replaces the entire data (codepoint, fgIndex, bgIndex) of the currently
     * selected glyph
     * with the data from the provided glyph object.
     * Called from the ControlPanel (e.g., Paste Internal Glyph).
     * 
     * @param sourceGlyph The ResultGlyph object containing the new data.
     */
    public void replaceClickedGlyphWithGlyph(ResultGlyph sourceGlyph) {
        if (clickedGlyph != null && clickedGridX >= 0 && clickedGridY >= 0 && sourceGlyph != null) {
            // Validate source glyph data (optional but recommended)
            if (!asciiPatterns.containsKey(sourceGlyph.codePoint)) {
                Logger.println("Error: Source glyph codepoint " + sourceGlyph.codePoint
                        + " not found in font patterns. Cannot replace.");
                return;
            }
            if (sourceGlyph.fgIndex < 0 || sourceGlyph.fgIndex >= colorPalette.length || sourceGlyph.bgIndex < 0
                    || sourceGlyph.bgIndex >= colorPalette.length) {
                Logger.println("Error: Invalid color indices in source glyph (FG: " + sourceGlyph.fgIndex + ", BG: "
                        + sourceGlyph.bgIndex + ").");
                return;
            }

            // Erstelle eine neue Glyphe basierend auf der Quellglyphe
            ResultGlyph newGlyph = new ResultGlyph(
                    sourceGlyph.codePoint,
                    sourceGlyph.fgIndex,
                    sourceGlyph.bgIndex);

            // Erstelle und führe das Command aus
            GlyphChangeCommand cmd = new GlyphChangeCommand(this, clickedGridX, clickedGridY, newGlyph);
            commandManager.executeCommand(cmd);

            Logger.println("Replaced glyph at (" + clickedGridX + "," + clickedGridY + ") with internal glyph data.");
        } else {
            if (clickedGlyph == null)
                Logger.println("No glyph selected to replace.");
            if (sourceGlyph == null)
                Logger.println("Source glyph data is null.");
        }
    }

    /**
     * Setzt den Hintergrund der aktuell ausgewählten Glyphe auf transparent (Index
     * 0)
     * Wird vom ControlPanel aufgerufen.
     */
    public void setTransparentBackground() {
        if (clickedGlyph != null && clickedGridX >= 0 && clickedGridY >= 0) {
            // Erstelle eine neue Glyphe mit transparentem Hintergrund
            ResultGlyph newGlyph = new ResultGlyph(
                    clickedGlyph.codePoint,
                    clickedGlyph.fgIndex,
                    0); // 0 ist der Index für Transparenz, den wir in setupPalette definiert haben

            // Erstelle und führe das Command aus
            GlyphChangeCommand cmd = new GlyphChangeCommand(this, clickedGridX, clickedGridY, newGlyph);
            commandManager.executeCommand(cmd);

            Logger.println("Hintergrund bei (" + clickedGridX + "," + clickedGridY + ") auf transparent gesetzt");
        } else {
            Logger.println("Keine Glyphe ausgewählt, um den Hintergrund transparent zu setzen.");
        }
    }

    /**
     * Setze eine Selektion und speichert die Änderung über das Command-Pattern
     * 
     * @param hasSelection Gibt an, ob die Selektion aktiv ist
     * @param startX       X-Startkoordinate der Selektion
     * @param startY       Y-Startkoordinate der Selektion
     * @param endX         X-Endkoordinate der Selektion
     * @param endY         Y-Endkoordinate der Selektion
     */
    public void setSelection(boolean hasSelection, int startX, int startY, int endX, int endY) {
        SelectionCommand cmd = new SelectionCommand(this, hasSelection, startX, startY, endX, endY);
        commandManager.executeCommand(cmd);
    }

    /**
     * Speichert die aktuelle Auswahl, damit sie nach dem Laden eines neuen Bildes
     * wiederhergestellt werden kann
     */
    private void saveCurrentSelection() {
        if (hasSelection && selectionStartX >= 0 && selectionStartY >= 0 &&
                selectionEndX >= 0 && selectionEndY >= 0) {
            savedSelectionActive = true;
            savedSelectionStartX = selectionStartX;
            savedSelectionStartY = selectionStartY;
            savedSelectionEndX = selectionEndX;
            savedSelectionEndY = selectionEndY;
            Logger.println("Selection saved: (" + savedSelectionStartX + "," + savedSelectionStartY +
                    ") to (" + savedSelectionEndX + "," + savedSelectionEndY + ")");
        } else {
            savedSelectionActive = false;
            savedSelectionStartX = savedSelectionStartY = savedSelectionEndX = savedSelectionEndY = -1;
        }
    }

    /**
     * Versucht, die gespeicherte Auswahl wiederherzustellen und passt sie an die
     * neuen Bilddimensionen an
     */
    private void restoreSavedSelection() {
        if (!savedSelectionActive) {
            return; // Keine gespeicherte Auswahl vorhanden
        }

        // Sicherheitsüberprüfung: Ist die Auswahl noch im gültigen Bereich des neuen
        // Bildes?
        int newStartX = Math.min(savedSelectionStartX, gridWidth - 1);
        int newStartY = Math.min(savedSelectionStartY, gridHeight - 1);
        int newEndX = Math.min(savedSelectionEndX, gridWidth - 1);
        int newEndY = Math.min(savedSelectionEndY, gridHeight - 1);

        // Nur wiederherstellen, wenn die Auswahl noch gültig ist
        if (newStartX >= 0 && newStartY >= 0 && newEndX >= 0 && newEndY >= 0) {
            setSelection(true, newStartX, newStartY, newEndX, newEndY);
            Logger.println("Selection restored: (" + newStartX + "," + newStartY +
                    ") to (" + newEndX + "," + newEndY + ")");

            // Informiere das ControlPanel über die aktualisierte Auswahl
            if (controlPanel != null) {
                controlPanel.updateSelectionAreaInfo(true, newStartX, newStartY, newEndX, newEndY);
            }
        } else {
            Logger.println("Could not restore selection because it would be outside the new image boundaries.");
        }
    }

    /**
     * Macht die letzte Aktion rückgängig
     */
    public void undo() {
        if (commandManager.canUndo()) {
            commandManager.undo();
            Logger.println("Aktion rückgängig gemacht");
        } else {
            Logger.println("Nichts zum Rückgängig machen");
        }
    }

    /**
     * Wiederholt die zuletzt rückgängig gemachte Aktion
     */
    public void redo() {
        if (commandManager.canRedo()) {
            commandManager.redo();
            Logger.println("Nichts zum Wiederherstellen");
        } else {
            Logger.println("Nichts zum Wiederherstellen");
        }
    }

    /**
     * Reload the current image
     */
    private void reloadCurrentImage() {
        if (imagePath != null && !imagePath.isEmpty()) {
            fill(128);
            textSize(32);
            textAlign(CENTER, CENTER);
            text("Reloading...", width / 2, height / 2);
            loadAndProcessImage(imagePath);
        } else {
            Logger.println("No image path set to reload.");
        }
    }

    /**
     * Set the display scale for the ASCII art
     */
    private void setDisplayScale(int newScale) {
        newScale = constrain(newScale, 1, 8);

        if (newScale == DISPLAY_SCALE || resultGrid == null) {
            if (controlPanel != null) {
                controlPanel.updateScaleSelector(newScale);
            }
            DISPLAY_SCALE = newScale;
            return;
        }

        int oldScale = DISPLAY_SCALE;

        // Calculate current grid and mouse positions
        float oldCellW = GLYPH_WIDTH * oldScale;
        float oldCellH = GLYPH_HEIGHT * oldScale;
        float oldTotalW = gridWidth * oldCellW;
        float oldTotalH = gridHeight * oldCellH;
        float oldGridOriginX = (width - oldTotalW) / 2f + drawX - width / 2f;
        float oldGridOriginY = (height - oldTotalH) / 2f + drawY - height / 2f;

        // Convert mouse position to world coordinates
        float worldX = (mouseX - oldGridOriginX) / oldScale;
        float worldY = (mouseY - oldGridOriginY) / oldScale;

        // Apply new scale
        DISPLAY_SCALE = newScale;
        Logger.println("Set display scale to: " + DISPLAY_SCALE);

        // Adjust drawing position to keep the point under the mouse unchanged
        float newCellW = GLYPH_WIDTH * newScale;
        float newCellH = GLYPH_HEIGHT * newScale;
        float newTotalW = gridWidth * newCellW;
        float newTotalH = gridHeight * newCellH;

        float newDrawX = mouseX - worldX * newScale - (width - newTotalW) / 2f + width / 2f;
        float newDrawY = mouseY - worldY * newScale - (height - newTotalH) / 2f + height / 2f;

        drawX = round(newDrawX);
        drawY = round(newDrawY);

        // Update the control panel if it exists
        if (controlPanel != null) {
            controlPanel.updateScaleSelector(DISPLAY_SCALE);
        }
    }

    /**
     * Zentriert das aktuelle Bild im Anzeigefenster
     */
    public void centerImage() {
        drawX = (width - drawW) / 2;
        drawY = (height - drawH) / 2;
        Logger.println("Image centered in view");
    }

    // ========== UTILITY METHODS ==========

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
     * Get the currently clicked glyph
     */
    public ResultGlyph getClickedGlyph() {
        return this.clickedGlyph;
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
            PApplet.main(new String[] { "ProcessingCore" });
        } catch (Exception e) {
            Logger.println("Error starting the application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Findet einen alternativen, oft invertierten Glyph-Match für den aktuell
     * ausgewählten Block.
     * Der Algorithmus versucht den am besten passenden Glyph zu finden, wobei der
     * aktuell
     * verwendete Glyph ausgeschlossen wird.
     * Wird vom ControlPanel aufgerufen.
     */
    public void invertGlyphPattern() {
        if (clickedGlyph == null || clickedGridX < 0 || clickedGridY < 0) {
            Logger.println("Kein Glyph ausgewählt zum Neuvergleichen.");
            return;
        }

        // 1. Extrahiere den Original-Pixelblock aus dem Bild
        int[] blockPixels = extractBlockPixels(clickedGridX, clickedGridY);
        if (blockPixels == null) {
            Logger.println("Fehler beim Extrahieren der Pixel für die Neuberechnung.");
            return;
        }

        // 2. Erstelle eine Liste von Codepoints, die wir ausschließen wollen (aktueller
        // Codepoint)
        Set<Integer> excludedCodepoints = new HashSet<>();
        excludedCodepoints.add(clickedGlyph.codePoint);

        // 3. Führe einen optimierten Match durch, unter Ausschluss des aktuellen
        // Glyphen
        ResultGlyph newMatch = findBestMatchExcluding(blockPixels, excludedCodepoints);

        // 4. Wenn ein neuer Match gefunden wurde, wende ihn an
        if (newMatch != null) {
            // Erstelle und führe das Command aus
            GlyphChangeCommand cmd = new GlyphChangeCommand(this, clickedGridX, clickedGridY, newMatch);
            commandManager.executeCommand(cmd);

            Logger.println("Alternative Glyphe bei (" + clickedGridX + "," + clickedGridY +
                    ") gefunden: Codepoint=" + newMatch.codePoint +
                    " (von " + clickedGlyph.codePoint + ")");
        } else {
            Logger.println("Keine bessere Alternative gefunden.");
        }
    }

    /**
     * Findet den besten Glyph-Match für einen Block, unter Ausschluss bestimmter
     * Codepoints.
     * 
     * @param blockPixels        Die Pixel des zu matchenden Blocks
     * @param excludedCodepoints Eine Menge von Codepoints, die ausgeschlossen
     *                           werden sollen
     * @return Der beste gefundene Match als ResultGlyph, oder null wenn keiner
     *         gefunden wurde
     */
    private ResultGlyph findBestMatchExcluding(int[] blockPixels, Set<Integer> excludedCodepoints) {
        // 1. Versuche zuerst einen exakten Match zu finden
        ResultGlyph exactMatch = findExactMatchExcluding(blockPixels, excludedCodepoints);
        if (exactMatch != null) {
            return exactMatch;
        }

        // 2. Sonst finde eine Approximation
        return findApproximateMatchExcluding(blockPixels, excludedCodepoints);
    }

    /**
     * Findet einen exakten Match für einen Block unter Ausschluss bestimmter
     * Codepoints.
     * Diese Methode ist eine Kopie von findExactMatch, aber mit Ausschluss-Logik.
     */
    private ResultGlyph findExactMatchExcluding(int[] blockPixels, Set<Integer> excludedCodepoints) {
        Set<Integer> uniqueIndices = new HashSet<>();
        int[] quantizedIndices = new int[PIXEL_COUNT];

        // Quantize pixels to palette indices
        for (int i = 0; i < PIXEL_COUNT; i++) {
            int nearestIndex = findNearestPaletteIndex(blockPixels[i]);
            uniqueIndices.add(nearestIndex);
            quantizedIndices[i] = nearestIndex;
        }

        // Exact match only possible with exactly 2 colors
        if (uniqueIndices.size() != 2) {
            return null;
        }

        // Get the two unique colors
        Integer[] indices = uniqueIndices.toArray(new Integer[0]);
        int indexA = indices[0];
        int indexB = indices[1];
        int colorA = colorPalette[indexA];
        int colorB = colorPalette[indexB];

        // Beste Übereinstimmung und Fehler speichern
        int bestCodePoint = -1;
        boolean bestIsCombinationA = true;
        double bestError = Double.MAX_VALUE;

        // Try each glyph pattern
        for (Map.Entry<Integer, Long> entry : asciiPatterns.entrySet()) {
            int currentCodePoint = entry.getKey();

            // Überspringen, wenn dieser Codepoint ausgeschlossen werden soll
            if (excludedCodepoints.contains(currentCodePoint)) {
                continue;
            }

            long currentPattern = entry.getValue();

            // Try color combination A
            boolean matchA = true;
            for (int i = 0; i < PIXEL_COUNT; i++) {
                boolean pixelOn = ((currentPattern >> i) & 1L) == 1L;
                int simulatedQuantizedIndex = pixelOn ? indexA : indexB;
                if (simulatedQuantizedIndex != quantizedIndices[i]) {
                    matchA = false;
                    break;
                }
            }

            double errorA = Double.MAX_VALUE;
            if (matchA) {
                int[] simulatedExactA = simulateBlock(currentPattern, colorA, colorB);
                // Berechne den exakten Fehler für eine genauere Sortierung
                errorA = calculateColorDistance(blockPixels, simulatedExactA);

                // Ist dies der beste Match bisher?
                if (errorA < bestError) {
                    bestError = errorA;
                    bestCodePoint = currentCodePoint;
                    bestIsCombinationA = true;
                }
            }

            // Try color combination B
            boolean matchB = true;
            for (int i = 0; i < PIXEL_COUNT; i++) {
                boolean pixelOn = ((currentPattern >> i) & 1L) == 1L;
                int simulatedQuantizedIndex = pixelOn ? indexB : indexA;
                if (simulatedQuantizedIndex != quantizedIndices[i]) {
                    matchB = false;
                    break;
                }
            }

            double errorB = Double.MAX_VALUE;
            if (matchB) {
                int[] simulatedExactB = simulateBlock(currentPattern, colorB, colorA);
                // Berechne den exakten Fehler für eine genauere Sortierung
                errorB = calculateColorDistance(blockPixels, simulatedExactB);

                // Ist dies der beste Match bisher?
                if (errorB < bestError) {
                    bestError = errorB;
                    bestCodePoint = currentCodePoint;
                    bestIsCombinationA = false;
                }
            }
        }

        // Wenn wir einen Match gefunden haben, erstelle einen ResultGlyph
        if (bestCodePoint != -1) {
            return bestIsCombinationA
                    ? new ResultGlyph(bestCodePoint, indexA, indexB)
                    : new ResultGlyph(bestCodePoint, indexB, indexA);
        }

        return null; // No exact match found
    }

    /**
     * Findet einen ungefähren Match für einen Block unter Ausschluss bestimmter
     * Codepoints.
     * Diese Methode ist eine modifizierte Version von findApproximateMatch.
     */
    private ResultGlyph findApproximateMatchExcluding(int[] blockPixels, Set<Integer> excludedCodepoints) {
        // Find two dominant colors
        int[] dominantIndices = findDominantPaletteColors(blockPixels);
        int color1Index = dominantIndices[0];
        int color2Index = dominantIndices[1];

        // Find best character and color combination
        int bestCodePoint = 0;
        int bestFgIndex = color1Index;
        int bestBgIndex = color2Index;
        double minError = Double.MAX_VALUE;

        // Iterate through all glyph patterns
        for (Map.Entry<Integer, Long> entry : asciiPatterns.entrySet()) {
            int currentCodePoint = entry.getKey();

            // Überspringe ausgeschlossene Codepoints
            if (excludedCodepoints.contains(currentCodePoint)) {
                continue;
            }

            long currentPattern = entry.getValue();

            // Calculate error for both color assignments
            double errorA = calculateMatchError(currentPattern, color1Index, color2Index, blockPixels);
            double errorB = calculateMatchError(currentPattern, color2Index, color1Index, blockPixels);

            // Update best match
            if (errorA < minError) {
                minError = errorA;
                bestCodePoint = currentCodePoint;
                bestFgIndex = color1Index;
                bestBgIndex = color2Index;
            }

            if (errorB < minError) {
                minError = errorB;
                bestCodePoint = currentCodePoint;
                bestFgIndex = color2Index;
                bestBgIndex = color1Index;
            }
        }

        // Wenn kein Match gefunden wurde (alle Codepoints ausgeschlossen), gib null
        // zurück
        if (excludedCodepoints.contains(bestCodePoint)) {
            return null;
        }

        return new ResultGlyph(bestCodePoint, bestFgIndex, bestBgIndex);
    }

    /**
     * Berechnet den genauen Farbabstand zwischen zwei Pixelblöcken
     * 
     * @param blockA Der erste Pixelblock
     * @param blockB Der zweite Pixelblock
     * @return Die Summe der quadrierten Farbdifferenzen
     */
    private double calculateColorDistance(int[] blockA, int[] blockB) {
        if (blockA.length != blockB.length) {
            return Double.MAX_VALUE;
        }

        double totalError = 0;

        for (int i = 0; i < blockA.length; i++) {
            int colorA = blockA[i];
            int colorB = blockB[i];

            // Extrahiere die RGB-Komponenten
            float r1 = red(colorA);
            float g1 = green(colorA);
            float b1 = blue(colorA);

            float r2 = red(colorB);
            float g2 = green(colorB);
            float b2 = blue(colorB);

            // Berechne die quadrierte Distanz
            totalError += (r1 - r2) * (r1 - r2) +
                    (g1 - g2) * (g1 - g2) +
                    (b1 - b2) * (b1 - b2);
        }

        return totalError;
    }
}