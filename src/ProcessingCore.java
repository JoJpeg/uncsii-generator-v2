import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.event.MouseEvent;

/**
 * Represents a single character in the resulting ASCII art
 */
class ResultGlyph {
    int codePoint; // Unicode Codepoint
    int fgIndex; // Foreground color index (0-255)
    int bgIndex; // Background color index (0-255)

    ResultGlyph(int cp, int fg, int bg) {
        this.codePoint = cp;
        this.fgIndex = fg;
        this.bgIndex = bg;
    }
}

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

    // --- Display Variables ---
    private int displayAreaWidth = DEFAULT_DISPLAY_AREA_WIDTH;
    private int displayAreaHeight = DEFAULT_DISPLAY_AREA_HEIGHT;
    private int DISPLAY_SCALE = DEFAULT_DISPLAY_SCALE;
    private boolean showSourceImage = false;
    private int drawW, drawH;
    private int drawX, drawY;

    // --- Font Pattern Variables ---
    private GlyphPatternGenerator patternGenerator;
    private Map<Integer, Long> asciiPatterns;
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
    private int[] colorPalette; // Color palette (256 colors)
    private ImageLoadingState imageLoadingState = ImageLoadingState.NONE;

    // --- Selection Variables ---
    // Hover selection
    private int mouseGridX = -1;
    private int mouseGridY = -1;
    private ResultGlyph selectedGlyph = null;

    // Clicked selection
    private int clickedGridX = -1;
    private int clickedGridY = -1;
    public ResultGlyph clickedGlyph = null;

    // --- Control Panel ---
    private ControlPanel controlPanel;

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
        controlPanel.setState(ControlPanel.State.SETUP);

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

        // Standard 16 colors
        int[] basic16 = {
                color(0, 0, 0), color(128, 0, 0), color(0, 128, 0), color(128, 128, 0),
                color(0, 0, 128), color(128, 0, 128), color(0, 128, 128), color(192, 192, 192),
                color(128, 128, 128), color(255, 0, 0), color(0, 255, 0), color(255, 255, 0),
                color(0, 0, 255), color(255, 0, 255), color(0, 255, 255), color(255, 255, 255)
        };

        // Copy basic 16 colors
        System.arraycopy(basic16, 0, colorPalette, 0, basic16.length);

        // Generate the 216 color cube (6x6x6)
        int index = 16;
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
            colorPalette[index++] = color(value, value, value);
        }

        if (colorPalette.length != 256) {
            Logger.println("WARNING: Color Palette size is not 256!");
        }
    }

    // ========== IMAGE LOADING AND PROCESSING ==========

    /**
     * Loads an image from the file system
     */
    public void loadImage() {
        File imageFile = FileHandler.loadFile("Select Image");
        if (imageFile == null) {
            Logger.println("No image file selected. Exiting.");
            controlPanel.setState(ControlPanel.State.SETUP);
            return;
        }

        imagePath = imageFile.getAbsolutePath();
        loadAndProcessImage(imagePath);

        if (imageLoadingState == ImageLoadingState.ERROR) {
            Logger.println("Error loading image. Exiting.");
            controlPanel.setState(ControlPanel.State.SETUP);
            return;
        }

        controlPanel.setState(ControlPanel.State.EDIT);
    }

    /**
     * Load and process an image for conversion
     */
    void loadAndProcessImage(String path) {
        inputImage = loadImage(path);
        if (inputImage == null) {
            Logger.println("Error loading image: " + path);
            imageLoadingState = ImageLoadingState.ERROR;
            return;
        }

        inputImage.loadPixels();

        // Resize image to fit display area while maintaining aspect ratio
        resizeImageForDisplay();

        // Ensure image dimensions are multiples of the glyph size
        cropImageToFitGrid();

        if (gridWidth == 0 || gridHeight == 0) {
            Logger.println("Image too small after resizing/cropping for an 8x8 grid.");
            imageLoadingState = ImageLoadingState.ERROR;
            return;
        }

        resultGrid = new ResultGlyph[gridHeight][gridWidth];

        Logger.println("Starting ASCII conversion...");
        long startTime = System.currentTimeMillis();

        generateAsciiArtExact();

        long endTime = System.currentTimeMillis();
        Logger.println("Conversion finished in " + (endTime - startTime) + " ms.");
        imageLoadingState = ImageLoadingState.LOADED;
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
                blockPixels[y * GLYPH_WIDTH + x] = inputImage.pixels[imgY * inputImage.width + imgX];
            }
        }

        return blockPixels;
    }

    /**
     * Find an approximate match for a block using dominant colors
     */
    private ResultGlyph findApproximateMatch(int[] blockPixels) {
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

        return new ResultGlyph(bestCodePoint, bestFgIndex, bestBgIndex);
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
     */
    ResultGlyph findExactMatch(int[] blockPixels) {
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
                    return new ResultGlyph(currentCodePoint, indexA, indexB);
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
                    return new ResultGlyph(currentCodePoint, indexB, indexA);
                }
            }
        }

        return null; // No exact match found
    }

    /**
     * Find the nearest palette color index for an RGB color
     */
    int findNearestPaletteIndex(int rgbColor) {
        int bestIndex = 0;
        double minDistSq = Double.MAX_VALUE;

        float r1 = red(rgbColor);
        float g1 = green(rgbColor);
        float b1 = blue(rgbColor);

        for (int i = 0; i < colorPalette.length; i++) {
            int palColor = colorPalette[i];
            float r2 = red(palColor);
            float g2 = green(palColor);
            float b2 = blue(palColor);

            double distSq = (r1 - r2) * (r1 - r2) +
                    (g1 - g2) * (g1 - g2) +
                    (b1 - b2) * (b1 - b2);

            if (distSq < minDistSq) {
                minDistSq = distSq;
                bestIndex = i;
            }

            if (minDistSq == 0) {
                break; // Exact match found
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
        float imgAspect = (float) inputImage.width / inputImage.height;
        float canvasAspect = (float) width / height;
        int cellWidth = GLYPH_WIDTH * DISPLAY_SCALE;
        int cellHeight = GLYPH_HEIGHT * DISPLAY_SCALE;
        int w = gridWidth * cellWidth;
        int h = gridHeight * cellHeight;

        if (imgAspect > canvasAspect) {
            drawW = w;
            drawH = (int) (w / imgAspect);
        } else {
            drawH = h;
            drawW = (int) (h * imgAspect);
        }

        image(inputImage, drawX - width / 2, drawY - height / 2, drawW, drawH);
        imageMode(CORNER);

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
                ResultGlyph glyphInfo = resultGrid[y][x];
                if (glyphInfo == null) {
                    continue;
                }

                long pattern = asciiPatterns.getOrDefault(glyphInfo.codePoint, 0L);
                int fgColor = colorPalette[glyphInfo.fgIndex];
                int bgColor = colorPalette[glyphInfo.bgIndex];
                int screenX = gridOriginX + x * cellWidth;
                int screenY = gridOriginY + y * cellHeight;

                displayScaledGlyph(pattern, screenX, screenY, DISPLAY_SCALE, bgColor, fgColor);
            }
        }

        // Handle hover highlight
        handleHoverHighlight(gridOriginX, gridOriginY, cellWidth, cellHeight, totalGridWidthPixels,
                totalGridHeightPixels);

        // Handle clicked highlight
        handleClickedHighlight(gridOriginX, gridOriginY, cellWidth, cellHeight);
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
     * Display a scaled glyph with the given pattern and colors
     */
    void displayScaledGlyph(long pattern, int screenX, int screenY, int pixelSize, int bgCol, int fgCol) {
        noStroke();
        for (int y = 0; y < GLYPH_HEIGHT; y++) {
            for (int x = 0; x < GLYPH_WIDTH; x++) {
                int bitIndex = y * GLYPH_WIDTH + x;
                boolean pixelOn = ((pattern >> bitIndex) & 1L) == 1L;
                fill(pixelOn ? fgCol : bgCol);
                rect(screenX + x * pixelSize, screenY + y * pixelSize, pixelSize, pixelSize);
            }
        }
    }

    // ========== FILE OPERATIONS ==========

    /**
     * Save the result to a file in the old format
     */
    void saveResultOldFormat(String filePath) {
        if (resultGrid == null) {
            Logger.println("No result to save.");
            return;
        }

        Logger.println("Saving result to " + filePath + " ...");
        try (PrintWriter writer = createWriter(filePath)) {
            writer.println("TYPE=USCII_ART_V2_CODEPOINT");
            writer.println("WIDTH=" + gridWidth);
            writer.println("HEIGHT=" + gridHeight);
            writer.println("COLORS=xterm256");
            writer.println("DATA_FORMAT=CODEPOINT FG_INDEX BG_INDEX");
            writer.println("DATA");

            for (int y = 0; y < gridHeight; y++) {
                StringBuilder line = new StringBuilder();
                for (int x = 0; x < gridWidth; x++) {
                    ResultGlyph g = resultGrid[y][x];
                    line.append(g.codePoint).append(" ")
                            .append(g.fgIndex).append(" ")
                            .append(g.bgIndex);

                    if (x < gridWidth - 1) {
                        line.append(" ");
                    }
                }
                writer.println(line.toString());
            }
            writer.flush();
            Logger.println("Result saved successfully.");
        } catch (Exception e) {
            Logger.println("Error saving result file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== USER INTERACTION ==========

    @Override
    public void mousePressed() {
        // Check if the click is within the result grid area when it's displayed
        clickStart = System.currentTimeMillis();
        if (!showSourceImage && resultGrid != null) {
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
            if (mouseRelativeX >= 0 && mouseRelativeX < totalGridWidthPixels &&
                    mouseRelativeY >= 0 && mouseRelativeY < totalGridHeightPixels &&
                    gridClickX >= 0 && gridClickX < gridWidth &&
                    gridClickY >= 0 && gridClickY < gridHeight) {

                // Update clicked selection state
                clickedGridX = gridClickX;
                clickedGridY = gridClickY;
                clickedGlyph = resultGrid[clickedGridY][clickedGridX];

                // Update the control panel with the clicked info, palette, and patterns
                if (controlPanel != null) {
                    controlPanel.updateClickedInfo(clickedGridX, clickedGridY, clickedGlyph, colorPalette,
                            asciiPatterns);
                }

                Logger.println("Clicked cell: X=" + clickedGridX + ", Y=" + clickedGridY);
            } else {
                // Reset clicked selection if outside and update control panel
                clickedGridX = -1;
                clickedGridY = -1;
                clickedGlyph = null;
                if (controlPanel != null) {
                    controlPanel.updateClickedInfo(-1, -1, null, colorPalette, asciiPatterns);
                }
            }
        } else {
            // Reset clicked selection if source image is shown and update control panel
            clickedGridX = -1;
            clickedGridY = -1;
            clickedGlyph = null;
            if (controlPanel != null) {
                controlPanel.updateClickedInfo(-1, -1, null, colorPalette, asciiPatterns);
            }
        }
    }

    boolean startedDragging = false;
    long clickStart = 0;

    @Override
    public void mouseDragged() {
        // Only pan if left mouse button is held
        if (mouseButton == LEFT) {
            int deltaX = mouseX - pmouseX;
            int deltaY = mouseY - pmouseY;
            int totalDelta = abs(deltaX) + abs(deltaY);
            // int threshold = 2; // Minimum drag distance to start panning
            boolean draggedFastEnough = totalDelta > 10;
            boolean draggedLongeEnough = System.currentTimeMillis() - clickStart > 100;
            if (startedDragging || draggedLongeEnough || draggedFastEnough) {
                drawX += deltaX;
                drawY += deltaY;
            }
            // if (abs(deltaX) < threshold && abs(deltaY) < threshold && !startedDragging) {
            // return; // Ignore small drags
            // }
            startedDragging = true;

        }
    }

    public void mouseReleased() {
        // Reset dragging state when mouse is released
        startedDragging = false;
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
        keyPressed(key);
    }

    public void keyPressed(char k) {
        if (k == 's' || k == 'S') {
            showSourceImage = !showSourceImage;
            Logger.println("Toggled view: " + (showSourceImage ? "Source Image" : "ASCII Art"));
        } else if (k == 'p' || k == 'P') {
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
        } else if (k == 'r' || k == 'R') {
            reloadCurrentImage();
        } else if (k == 'l' || k == 'L') {
            loadImage();
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
                resultGrid[clickedGridY][clickedGridX].codePoint = newCodePoint;
                clickedGlyph = resultGrid[clickedGridY][clickedGridX]; // Update the reference

                Logger.println("Replaced glyph at (" + clickedGridX + "," + clickedGridY + ") with char '" + newChar
                        + "' (Codepoint: " + newCodePoint + ")");

                // Update the control panel display if needed, passing updated glyph, palette,
                // and patterns
                if (controlPanel != null) {
                    controlPanel.updateClickedInfo(clickedGridX, clickedGridY, clickedGlyph, colorPalette,
                            asciiPatterns);
                }

                // Redrawing happens automatically in the draw() loop
            } else {
                Logger.println("Error: Character '" + newChar + "' (Codepoint: " + newCodePoint
                        + ") not found in font patterns. Cannot replace.");
            }
        } else {
            Logger.println("No glyph selected to replace.");
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
}