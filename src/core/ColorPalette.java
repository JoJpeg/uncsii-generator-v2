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
     * 100% xterm standard compliant
     */
    private void setupXterm256Palette() {
        // Die xterm-256 Farbpalette hat folgende Struktur:
        // 0-7: Standard-Farben (schwarz, rot, grün, gelb, blau, magenta, cyan, weiß)
        // 8-15: Helle Varianten der Standard-Farben
        // 16-231: 6x6x6 RGB-Farbwürfel
        // 232-255: Graustufen

        // Standard-Farben (0-7)
        colors[0] = app.color(0, 0, 0); // 0: Schwarz
        colors[1] = app.color(128, 0, 0); // 1: Rot
        colors[2] = app.color(0, 128, 0); // 2: Grün
        colors[3] = app.color(128, 128, 0); // 3: Gelb
        colors[4] = app.color(0, 0, 128); // 4: Blau
        colors[5] = app.color(128, 0, 128); // 5: Magenta
        colors[6] = app.color(0, 128, 128); // 6: Cyan
        colors[7] = app.color(192, 192, 192); // 7: Weiß/Hellgrau

        // Helle Varianten (8-15)
        colors[8] = app.color(128, 128, 128); // 8: Dunkelgrau/helles Schwarz
        colors[9] = app.color(255, 0, 0); // 9: Helles Rot
        colors[10] = app.color(0, 255, 0); // 10: Helles Grün
        colors[11] = app.color(255, 255, 0); // 11: Helles Gelb
        colors[12] = app.color(0, 0, 255); // 12: Helles Blau
        colors[13] = app.color(255, 0, 255); // 13: Helles Magenta
        colors[14] = app.color(0, 255, 255); // 14: Helles Cyan
        colors[15] = app.color(255, 255, 255); // 15: Helles Weiß

        // Farbwürfel (16-231): 6x6x6 RGB
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

        // Graustufen (232-255)
        for (int i = 0; i < 24; i++) {
            int value = i * 10 + 8;
            colors[index++] = app.color(value, value, value);
        }

        Logger.println("Palette initialisiert mit Standard Xterm-256 Farbwerten");
    }

    /**
     * Find the nearest palette color index for an RGB color
     * Properly handles RGB distance without mixing alpha concerns
     */
    public int findNearestColorIndex(int rgbColor) {
        // Wir berücksichtigen nur die RGB-Komponenten, nicht Alpha
        float r1 = app.red(rgbColor);
        float g1 = app.green(rgbColor);
        float b1 = app.blue(rgbColor);

        // Spezialfall für schwarze Pixel (für bessere Kompatibilität)
        if (r1 <= 5 && g1 <= 5 && b1 <= 5) {
            return 0; // Schwarzer Index in xterm
        }

        int bestIndex = 0;
        double minDistSq = Double.MAX_VALUE;

        // Durchsuche die gesamte Palette (0-255)
        for (int i = 0; i < colors.length; i++) {
            int palColor = colors[i];
            float r2 = app.red(palColor);
            float g2 = app.green(palColor);
            float b2 = app.blue(palColor);

            // Berechne die RGB-Distanz (ohne Alpha-Komponente)
            double distSq = (r1 - r2) * (r1 - r2) +
                    (g1 - g2) * (g1 - g2) +
                    (b1 - b2) * (b1 - b2);

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

    /**
     * Find the two most dominant palette colors in a block of pixels
     * With proper alpha handling - returns correct colors regardless of
     * transparency
     */
    public int[] findDominantColors(int[] blockPixels) {
        int[] counts = new int[PALETTE_SIZE];
        int totalPixels = 0;

        // Count occurrences of each palette color, ignoring fully transparent pixels
        for (int pixel : blockPixels) {
            int alpha = (pixel >> 24) & 0xFF;

            if (alpha > 0) { // Nur nicht vollständig transparente Pixel berücksichtigen
                int nearestIndex = findNearestColorIndex(pixel);
                counts[nearestIndex]++;
                totalPixels++;
            }
        }

        // Falls alle Pixel transparent sind, verwende Schwarz als Standardfarbe
        if (totalPixels == 0) {
            return new int[] { 0, 15 }; // Schwarz und Weiß als Standardfarben
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
            bestIndex1 = 0; // Default to black
        }

        if (bestIndex2 == -1 || bestIndex2 == bestIndex1) {
            // Wähle einen guten Kontrast zur ersten Farbe
            bestIndex2 = (bestIndex1 == 0) ? 15 : 0; // Wenn die erste Farbe Schwarz ist, wähle Weiß, sonst Schwarz
        }

        // Logging für Debugging
        Logger.println("Dominant colors: " + bestIndex1 + " (count: " + counts[bestIndex1] +
                "), " + bestIndex2 + " (count: " + counts[bestIndex2] + ")");

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