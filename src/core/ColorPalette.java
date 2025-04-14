package core;
import logger.Logger;
import processing.core.PApplet;

/**
 * Manages color palette generation and color matching operations
 */
public class ColorPalette {
    private static final int PALETTE_SIZE = 256;
    private int[] colors;
    private PApplet app;

    public ColorPalette(PApplet app) {
        this.app = app;
        this.colors = new int[PALETTE_SIZE];
        setupXterm256Palette();
    }

    /**
     * Sets up the xterm-256 color palette
     */
    private void setupXterm256Palette() {
        // Standard 16 colors
        int[] basic16 = {
                app.color(0, 0, 0), app.color(128, 0, 0), app.color(0, 128, 0), app.color(128, 128, 0),
                app.color(0, 0, 128), app.color(128, 0, 128), app.color(0, 128, 128), app.color(192, 192, 192),
                app.color(128, 128, 128), app.color(255, 0, 0), app.color(0, 255, 0), app.color(255, 255, 0),
                app.color(0, 0, 255), app.color(255, 0, 255), app.color(0, 255, 255), app.color(255, 255, 255)
        };

        // Copy basic 16 colors
        System.arraycopy(basic16, 0, colors, 0, basic16.length);

        // Generate the 216 color cube (6x6x6)
        int index = 16;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    int red = r > 0 ? (r * 40 + 55) : 0;
                    int green = g > 0 ? (g * 40 + 55) : 0;
                    int blue = b > 0 ? (b * 40 + 55) : 0;
                    colors[index++] = app.color(red, green, blue);
                }
            }
        }

        // Generate the grayscale ramp (24 colors)
        for (int i = 0; i < 24; i++) {
            int value = i * 10 + 8;
            colors[index++] = app.color(value, value, value);
        }

        if (index != PALETTE_SIZE) {
            Logger.println("WARNING: Color Palette size is not " + PALETTE_SIZE + "!");
        }
    }

    /**
     * Find the nearest palette color index for an RGB color
     */
    public int findNearestColorIndex(int rgbColor) {
        int bestIndex = 0;
        double minDistSq = Double.MAX_VALUE;

        float r1 = app.red(rgbColor);
        float g1 = app.green(rgbColor);
        float b1 = app.blue(rgbColor);

        for (int i = 0; i < colors.length; i++) {
            int palColor = colors[i];
            float r2 = app.red(palColor);
            float g2 = app.green(palColor);
            float b2 = app.blue(palColor);

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

    /**
     * Find the two most dominant palette colors in a block of pixels
     */
    public int[] findDominantColors(int[] blockPixels) {
        int[] counts = new int[PALETTE_SIZE];

        // Count occurrences of each palette color
        for (int pixel : blockPixels) {
            int nearestIndex = findNearestColorIndex(pixel);
            counts[nearestIndex]++;
        }

        // Find the two most frequent colors
        int bestIndex1 = -1, bestIndex2 = -1;
        int maxCount1 = -1, maxCount2 = -1;

        for (int i = 0; i < PALETTE_SIZE; i++) {
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
            for (int i = 0; i < PALETTE_SIZE; i++) {
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
     * Get the RGB color value for a palette index
     */
    public int getColor(int index) {
        if (index >= 0 && index < PALETTE_SIZE) {
            return colors[index];
        }
        return 0;
    }

    /**
     * Get the entire color palette
     */
    public int[] getColors() {
        return colors;
    }

    public int[] getPalette() {
        return colors;
    }

    /**
     * Get the size of the color palette
     */
    public int size() {
        return PALETTE_SIZE;
    }
}