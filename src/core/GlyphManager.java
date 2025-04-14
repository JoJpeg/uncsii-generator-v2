package core;
import java.util.Map;

import logger.Logger;
import processing.core.PApplet;

/**
 * Manages font pattern generation and glyph operations
 */
public class GlyphManager {
    // Font dimensions
    public static final int GLYPH_WIDTH = 8;
    public static final int GLYPH_HEIGHT = 8;
    public static final int PIXEL_COUNT = GLYPH_WIDTH * GLYPH_HEIGHT;

    private Map<Integer, Long> glyphPatterns;
    private PApplet app;
    private String fontPath;
    private float fontSize;

    public GlyphManager(PApplet app, String fontPath, float fontSize) {
        this.app = app;
        this.fontPath = fontPath;
        this.fontSize = fontSize;
    }

    /**
     * Generate font patterns for all supported codepoints
     */
    public void generatePatterns() {
        Logger.println("Generating glyph patterns...");
        try {
            GlyphPatternGenerator generator = new GlyphPatternGenerator(app, fontPath, fontSize);
            glyphPatterns = generator.generatePatterns();
            Logger.println("Generated " + glyphPatterns.size() + " patterns.");

            if (glyphPatterns.isEmpty()) {
                throw new RuntimeException("Pattern generation resulted in an empty map.");
            }

            // Print a test pattern for 'A'
            int testCodePoint = 'A';
            Logger.println("\nPattern for U+" + String.format("%04X", testCodePoint) + " ('A'):");
            if (glyphPatterns.containsKey(testCodePoint)) {
                GlyphPatternGenerator.printPattern(glyphPatterns.get(testCodePoint));
            } else {
                Logger.println("Pattern not found.");
            }
        } catch (Exception e) {
            Logger.println("Failed to initialize patterns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calculate the error between a pattern with given colors and original pixels
     */
    public double calculateMatchError(long pattern, int fgIndex, int bgIndex, int[] originalBlockPixels,
            ColorPalette palette) {
        double totalError = 0;
        int fgColor = palette.getColor(fgIndex);
        int bgColor = palette.getColor(bgIndex);

        // Extract RGB components
        float fgR = app.red(fgColor);
        float fgG = app.green(fgColor);
        float fgB = app.blue(fgColor);
        float bgR = app.red(bgColor);
        float bgG = app.green(bgColor);
        float bgB = app.blue(bgColor);

        // Calculate error for each pixel
        for (int i = 0; i < PIXEL_COUNT; i++) {
            boolean pixelOn = ((pattern >> i) & 1L) == 1L;

            // Use foreground or background color based on pattern
            float simR = pixelOn ? fgR : bgR;
            float simG = pixelOn ? fgG : bgG;
            float simB = pixelOn ? fgB : bgB;

            // Get original pixel color
            int originalColor = originalBlockPixels[i];
            float origR = app.red(originalColor);
            float origG = app.green(originalColor);
            float origB = app.blue(originalColor);

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
    public int[] simulateBlock(long pattern, int fgColor, int bgColor) {
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
    public boolean compareBlocksExactly(int[] blockA, int[] blockB) {
        for (int i = 0; i < PIXEL_COUNT; i++) {
            if (blockA[i] != blockB[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a glyph pattern for a specific codepoint
     */
    public long getPattern(int codePoint) {
        return glyphPatterns.getOrDefault(codePoint, 0L);
    }

    /**
     * Check if a specific codepoint is supported
     */
    public boolean hasPattern(int codePoint) {
        return glyphPatterns.containsKey(codePoint);
    }

    /**
     * Get the number of available patterns
     */
    public int getPatternCount() {
        return glyphPatterns == null ? 0 : glyphPatterns.size();
    }

    /**
     * Get all available patterns
     */
    public Map<Integer, Long> getAllPatterns() {
        return glyphPatterns;
    }

    public Map<Integer, Long> getPatterns() {
        if (glyphPatterns == null) {
            Logger.println("No patterns available.");
            return null;
        } else {
            return glyphPatterns;
        }
    }
}