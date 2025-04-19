package core;

import logger.Logger;
import processing.core.PApplet;

/**
 * Manages color palette generation and color matching operations
 */
public class ColorPalette {
    private static final int PALETTE_SIZE = 256;
    private static int[] colors;
    private PApplet p;

    public ColorPalette(PApplet app) {
        this.p = app;
        colors = new int[PALETTE_SIZE];
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
        colors[0] = p.color(0, 0, 0); // 0: Schwarz
        colors[1] = p.color(128, 0, 0); // 1: Rot
        colors[2] = p.color(0, 128, 0); // 2: Grün
        colors[3] = p.color(128, 128, 0); // 3: Gelb
        colors[4] = p.color(0, 0, 128); // 4: Blau
        colors[5] = p.color(128, 0, 128); // 5: Magenta
        colors[6] = p.color(0, 128, 128); // 6: Cyan
        colors[7] = p.color(192, 192, 192); // 7: Weiß/Hellgrau

        // Helle Varianten (8-15)
        colors[8] = p.color(128, 128, 128); // 8: Dunkelgrau/helles Schwarz
        colors[9] = p.color(255, 0, 0); // 9: Helles Rot
        colors[10] = p.color(0, 255, 0); // 10: Helles Grün
        colors[11] = p.color(255, 255, 0); // 11: Helles Gelb
        colors[12] = p.color(0, 0, 255); // 12: Helles Blau
        colors[13] = p.color(255, 0, 255); // 13: Helles Magenta
        colors[14] = p.color(0, 255, 255); // 14: Helles Cyan
        colors[15] = p.color(255, 255, 255); // 15: Helles Weiß

        // Farbwürfel (16-231): 6x6x6 RGB
        int index = 16;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    int red = r > 0 ? (r * 40 + 55) : 0;
                    int green = g > 0 ? (g * 40 + 55) : 0;
                    int blue = b > 0 ? (b * 40 + 55) : 0;
                    colors[index++] = p.color(red, green, blue);
                }
            }
        }

        // Graustufen (232-255)
        for (int i = 0; i < 24; i++) {
            int value = i * 10 + 8;
            colors[index++] = p.color(value, value, value);
        }

        Logger.println("Palette initialisiert mit Standard Xterm-256 Farbwerten");
    }

    public static int findNearestColorIndex(int rgbColor, PApplet p) {
        int bestIndex = 0;
        double minDistSq = Double.MAX_VALUE;

        float r1 = p.red(rgbColor);
        float g1 = p.green(rgbColor);
        float b1 = p.blue(rgbColor);

        for (int i = 0; i < colors.length; i++) {
            int palColor = colors[i];
            float r2 = p.red(palColor);
            float g2 = p.green(palColor);
            float b2 = p.blue(palColor);

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
     * Berechnet den genauen Farbabstand zwischen zwei Pixelblöcken
     * 
     * @param blockA Der erste Pixelblock
     * @param blockB Der zweite Pixelblock
     * @return Die Summe der quadrierten Farbdifferenzen
     */
    public static double calculateColorDistance(int[] blockA, int[] blockB, PApplet p) {
        if (blockA.length != blockB.length) {
            return Double.MAX_VALUE;
        }

        double totalError = 0;

        for (int i = 0; i < blockA.length; i++) {
            int colorA = blockA[i];
            int colorB = blockB[i];

            // Extrahiere die RGB-Komponenten
            float r1 = p.red(colorA);
            float g1 = p.green(colorA);
            float b1 = p.blue(colorA);

            float r2 = p.red(colorB);
            float g2 = p.green(colorB);
            float b2 = p.blue(colorB);

            // Berechne die quadrierte Distanz
            totalError += (r1 - r2) * (r1 - r2) +
                    (g1 - g2) * (g1 - g2) +
                    (b1 - b2) * (b1 - b2);
        }

        return totalError;
    }

    /**
     * Find the two most dominant palette colors in a block of pixels
     * With proper alpha handling - returns correct colors regardless of
     * transparency
     */
    public static int[] findDominantColors(int[] blockPixels, PApplet p) {
        int[] counts = new int[PALETTE_SIZE];
        int totalPixels = 0;

        // Count occurrences of each palette color, ignoring fully transparent pixels
        for (int pixel : blockPixels) {
            int alpha = (pixel >> 24) & 0xFF;

            if (alpha > 0) { // Nur nicht vollständig transparente Pixel berücksichtigen
                int nearestIndex = findNearestColorIndex(pixel, p);
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
     * Find the two most dominant palette colors in a block of pixels
     */
    public static int[] findDominantPaletteColors(int[] blockPixels, PApplet p, int PIXEL_COUNT) {
        int[] counts = new int[256];

        // Count occurrences of each palette color
        for (int i = 0; i < PIXEL_COUNT; i++) {
            int nearestIndex = ColorPalette.findNearestPaletteIndex(blockPixels[i], p);
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
     * Find the nearest palette color index for an RGB color
     * Mit xterm-256 kompatibler Farbbehandlung
     */
    static int findNearestPaletteIndex(int rgbColor, PApplet p) {
        // Extract alpha channel value
        int alpha = (rgbColor >> 24) & 0xFF;

        // Vollständig transparente Pixel als Schwarz behandeln (xterm Index 0)
        if (alpha == 0) {
            return 0; // In xterm ist 0 Schwarz
        }

        // Wenn der Pixel schwarz oder nahezu schwarz und opak ist,
        // direkt opakes Schwarz zurückgeben (Index 0)
        float r = p.red(rgbColor);
        float g = p.green(rgbColor);
        float b = p.blue(rgbColor);
        if (r <= 5 && g <= 5 && b <= 5 && alpha > 200) {
            return 0; // In xterm ist 0 Schwarz
        }

        // Für alle anderen Pixel den nächsten Farbindex in der Palette finden
        int bestIndex = 0;
        double minDistSq = Double.MAX_VALUE;

        // Die xterm Palette durchsuchen (alle Indizes)
        for (int i = 0; i < colors.length; i++) {
            int palColor = colors[i];
            float r2 = p.red(palColor);
            float g2 = p.green(palColor);
            float b2 = p.blue(palColor);

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
    public static int[] getColors() {
        return colors;
    }

    public static int[] getPalette() {
        return colors;
    }

    /**
     * Get the size of the color palette
     */
    public int size() {
        return PALETTE_SIZE;
    }
}