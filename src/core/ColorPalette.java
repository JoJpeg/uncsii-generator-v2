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
     * With improved transparency handling
     */
    private void setupXterm256Palette() {
        // Index 0: Vollständig transparent (komplett durchsichtig)
        colors[0] = app.color(0, 0, 0, 0);

        // Index 1: Komplett schwarz (vollständig opak)
        // Es ist wichtig, dass Index 1 vollständig opakes Schwarz ist
        colors[1] = app.color(0, 0, 0, 255);

        // Standard 16 Farben (jetzt startend ab Index 2)
        int[] basic16 = {
                app.color(128, 0, 0), app.color(0, 128, 0), app.color(128, 128, 0),
                app.color(0, 0, 128), app.color(128, 0, 128), app.color(0, 128, 128), app.color(192, 192, 192),
                app.color(128, 128, 128), app.color(255, 0, 0), app.color(0, 255, 0), app.color(255, 255, 0),
                app.color(0, 0, 255), app.color(255, 0, 255), app.color(0, 255, 255), app.color(255, 255, 255)
        };

        // Kopiere die 16 Standardfarben (startend ab Index 2)
        System.arraycopy(basic16, 0, colors, 2, basic16.length);

        // Generiere den 216-Farben-Würfel (6x6x6)
        int index = 18; // 2 (für die ersten beiden Indizes) + 16 (für die Standardfarben)
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    int red = r > 0 ? (r * 40 + 55) : 0;
                    int green = g > 0 ? (g * 40 + 55) : 0;
                    int blue = b > 0 ? (b * 40 + 55) : 0;
                    // Setze immer den Alpha-Wert auf vollständig opak
                    colors[index++] = app.color(red, green, blue, 255);
                }
            }
        }

        // Generiere die Graustufen-Rampe (24 Farben)
        for (int i = 0; i < 24; i++) {
            int value = i * 10 + 8;
            // Setze immer den Alpha-Wert auf vollständig opak
            colors[index++] = app.color(value, value, value, 255);
        }

        Logger.println("Palette initialisiert mit verbesserter Alpha-Kanal-Unterstützung:");
        Logger.println("- Index 0: Vollständig transparent (Alpha = 0)");
        Logger.println("- Index 1: Vollständig opakes Schwarz (Alpha = 255)");

        if (index != PALETTE_SIZE) {
            Logger.println("WARNUNG: Farbpalettengröße ist nicht " + PALETTE_SIZE + "!");
        }
    }

    /**
     * Find the nearest palette color index for an RGB color
     * With improved alpha channel support
     */
    public int findNearestColorIndex(int rgbColor) {
        // Check for transparency first
        int alpha = (rgbColor >> 24) & 0xFF;

        // Only treat completely transparent pixels (alpha == 0) as transparent
        // This ensures black pixels with alpha > 0 are not mistakenly treated as
        // transparent
        if (alpha == 0) {
            return 0; // Assuming index 0 is reserved for transparent color
        }

        int bestIndex = 1; // Start from index 1 (skip transparent)
        double minDistSq = Double.MAX_VALUE;

        float r1 = app.red(rgbColor);
        float g1 = app.green(rgbColor);
        float b1 = app.blue(rgbColor);

        // Start searching from index 1 to skip the transparent color at index 0
        for (int i = 1; i < colors.length; i++) {
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
     * With improved handling of black pixels
     */
    public int[] findDominantColors(int[] blockPixels) {
        int[] counts = new int[PALETTE_SIZE];

        // Count occurrences of each palette color
        int totalNonTransparentPixels = 0;
        for (int pixel : blockPixels) {
            int alpha = (pixel >> 24) & 0xFF;

            // Behandle nur vollständig transparente Pixel (alpha = 0) als transparent
            int nearestIndex;
            if (alpha == 0) {
                nearestIndex = 0; // Transparenzindex
            } else {
                // Wenn der Pixel schwarz oder fast schwarz ist und opak, dann verwende den
                // Index für opakes Schwarz (1)
                float r = app.red(pixel);
                float g = app.green(pixel);
                float b = app.blue(pixel);

                if (r <= 5 && g <= 5 && b <= 5 && alpha > 200) {
                    // Fast schwarz und opak
                    nearestIndex = 1; // Direkt als opakes Schwarz einstufen
                } else {
                    // Normal nächste Farbe finden
                    nearestIndex = findNearestColorIndex(pixel);
                }

                totalNonTransparentPixels++;
            }

            counts[nearestIndex]++;
        }

        // Find the two most frequent colors
        int bestIndex1 = -1, bestIndex2 = -1;
        int maxCount1 = -1, maxCount2 = -1;

        // Priorisiere nicht-transparente Farben, wenn ein signifikanter Teil des Blocks
        // nicht transparent ist
        boolean hasSignificantContent = totalNonTransparentPixels > blockPixels.length * 0.2; // Mindestens 20% nicht
                                                                                              // transparent

        // Zuerst nicht-transparente Farben zählen, wenn genug Inhalt vorhanden ist
        if (hasSignificantContent) {
            for (int i = 1; i < PALETTE_SIZE; i++) { // Start bei 1, um Transparenz zu überspringen
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
        }

        // Wenn nicht genug nicht-transparente Pixel oder keine dominanten Farben
        // gefunden wurden, alle Indizes berücksichtigen
        if (!hasSignificantContent || bestIndex1 == -1) {
            maxCount1 = maxCount2 = -1;
            bestIndex1 = bestIndex2 = -1;

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
        }

        // Handle edge cases
        if (bestIndex1 == -1) {
            // Wenn keine Farbe gefunden wurde, verwende opakes Schwarz
            bestIndex1 = 1; // Opakes Schwarz
        }

        if (bestIndex2 == -1 || bestIndex2 == bestIndex1) {
            // Wähle einen guten Kontrast zur ersten Farbe
            // Wenn die erste Farbe Schwarz ist, wähle Weiß als zweite, ansonsten Schwarz
            bestIndex2 = (bestIndex1 == 1) ? 17 : 1; // Index 17 sollte Weiß sein, 1 ist opakes Schwarz

            // Versuche eine dritte Farbe zu finden, wenn die ersten beiden identisch sind
            int thirdMaxCount = -1;
            int thirdBestIndex = -1;
            for (int i = 0; i < PALETTE_SIZE; i++) {
                if (i != bestIndex1 && counts[i] > thirdMaxCount) {
                    thirdMaxCount = counts[i];
                    thirdBestIndex = i;
                }
            }

            if (thirdBestIndex != -1 && thirdMaxCount > 0) {
                bestIndex2 = thirdBestIndex;
            }
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