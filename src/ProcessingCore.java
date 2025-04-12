import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter; // For saving the result
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Hinweis: Entfernt, da nicht im Code verwendet: import com.jogamp.newt.Display;

import processing.core.PApplet;
import processing.core.PImage;

// Klasse ResultGlyph bleibt unverändert
class ResultGlyph {
    int codePoint; // Unicode Codepoint statt char
    int fgIndex; // 0-255
    int bgIndex; // 0-255

    ResultGlyph(int cp, int fg, int bg) {
        this.codePoint = cp;
        this.fgIndex = fg;
        this.bgIndex = bg;
    }
}

public class ProcessingCore extends PApplet {

    // --- Font Pattern Variables ---
    GlyphPatternGenerator patternGenerator;
    Map<Integer, Long> asciiPatterns; // <-- Geändert zu Integer Key
    final int GLYPH_WIDTH = 8;
    final int GLYPH_HEIGHT = 8;
    final int PIXEL_COUNT = GLYPH_WIDTH * GLYPH_HEIGHT;
    final int displayAreaWidth = 400; // Angepasst an size() Breite
    final int displayAreaHeight = 240; // Angepasst an size() Höhe

    // --- Image & Conversion Variables ---
    PImage inputImage;
    ResultGlyph[][] resultGrid; // Verwendet jetzt die angepasste ResultGlyph Klasse
    int gridWidth;
    int gridHeight;
    String imagePath; // Wird in settings() gesetzt
    String outputPath = "output.usc"; // Standardausgabe
    int[] colorPalette; // Wird in setupPalette gefüllt

    // --- Display Variables ---
    int DISPLAY_SCALE = 2; // Skalierung für die *Ergebnis*-Anzeige (1 = 8x8 pro Char)
    boolean showSourceImage = false; // Zum Umschalten der Anzeige

    // --- Control Panel Variable ---
    private ControlPanel controlPanel;

    @Override
    public void settings() {
        // Fenstergröße definieren
        size(800, 480);
        noSmooth(); // Wichtig für Pixel-Look
    }

    @Override
    public void setup() {
        noSmooth();
        setupPalette(); // Farbpalette initialisieren
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/Ambient Mean/Xorm/Hypno Flute/renders/visual ready/16_9/hypno in flower.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/Ambient Mean/Xorm/Hypno Flute/renders/visual ready/16_9/crow spottet.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/Ambient Mean/Xorm/Hypno Flute/renders/visual ready/16_9/xorm logo.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/JoJpeg/Fun Stuff/Dirigent/Dirigent Xorm Intro 3.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/JoJpeg/Fun Stuff/Dirigent/Dirigent Font Test.png";
        imagePath = "/Users/jonaseschner/Dropbox (Privat)/JoJpeg/Fun Stuff/Dirigent/Dirigent Xorm Screen 1.png";
        outputPath = "Dirigent Xorm Screen 1.usc";

        // --- Font Pattern Generierung ---
        String fontPath = "/Users/jonaseschner/IdeaProjects/unscii-generator/src/resources/unscii-8.ttf";
        float fontSize = 8; // Basierend auf früheren Tests

        System.out.println("Generating glyph patterns..."); // Verwende System.out
        try {
            patternGenerator = new GlyphPatternGenerator(this, fontPath, fontSize);
            asciiPatterns = patternGenerator.generatePatterns();
            System.out.println("Generated " + asciiPatterns.size() + " patterns."); // Verwende System.out

            if (asciiPatterns.isEmpty()) {
                throw new RuntimeException("Pattern generation resulted in an empty map.");
            }
            int testCodePoint = 'A'; // Codepoint für 'A'
            System.out.println("\nPattern for U+" + String.format("%04X", testCodePoint) + " ('A'):"); // Verwende
                                                                                                       // System.out
            if (asciiPatterns.containsKey(testCodePoint)) {
                GlyphPatternGenerator.printPattern(asciiPatterns.get(testCodePoint));
            } else {
                System.out.println("Pattern not found."); // Verwende System.out
            }

        } catch (Exception e) {
            System.err.println("Failed to initialize patterns: " + e.getMessage());
            e.printStackTrace();
            exitActual(); // Korrekte Methode zum Beenden
        }

        // --- Bild laden und verarbeiten ---
        imagePath = FileHandler.getFileJ("Select Image").getAbsolutePath();
        loadAndProcessImage(imagePath);

        System.out.println("\n--- Conversion Setup ---"); // Verwende System.out
        System.out.println(
                "Image loaded: " + (inputImage != null ? inputImage.width + "x" + inputImage.height : "Failed")); // Verwende
                                                                                                                  // System.out
        System.out.println("Grid dimensions: " + gridWidth + "x" + gridHeight); // Verwende System.out
        System.out.println("Total blocks: " + (gridWidth * gridHeight)); // Verwende System.out
        System.out.println("Press 's' to switch between Source Image and ASCII Art."); // Verwende System.out
        System.out.println("Press 'p' to save the result to 'output.usc' file."); // Verwende System.out
        System.out.println("Press '1'-'8' to change display scale."); // Verwende System.out

        // Initialanzeige
        background(0);

        // Erstelle und zeige das Kontrollpanel an
        controlPanel = new ControlPanel(this);
        controlPanel.setVisible(true);

        // Positioniere das Kontrollpanel rechts neben dem Hauptfenster
        try {
            int x = getJFrame().getX() + getJFrame().getWidth();
            int y = getJFrame().getY();
            controlPanel.setLocation(x, y);
        } catch (Exception e) {
            System.err.println("Konnte Kontrollpanel nicht positionieren: " + e.getMessage());
        }
    }

    @Override
    public void draw() {
        background(30); // Hintergrund des Fensters

        if (showSourceImage && inputImage != null) {
            float imgAspect = (float) inputImage.width / inputImage.height;
            float canvasAspect = (float) width / height;
            int drawW, drawH;

            if (imgAspect > canvasAspect) {
                drawW = width;
                drawH = (int) (width / imgAspect);
            } else {
                drawH = height;
                drawW = (int) (height * imgAspect);
            }
            int drawX = (width - drawW) / 2;
            int drawY = (height - drawH) / 2;

            image(inputImage, drawX, drawY, drawW, drawH);

        } else if (resultGrid != null) {
            drawResult();
        } else {
            fill(255, 0, 0);
            textSize(20);
            textAlign(CENTER, CENTER);
            text("No result to display.", width / 2, height / 2);
        }
    }

    void loadAndProcessImage(String path) {
        inputImage = loadImage(path);
        if (inputImage == null) {
            System.err.println("Error loading image: " + path);
            return;
        }
        inputImage.loadPixels();

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

        int croppedWidth = gridWidth * GLYPH_WIDTH;
        int croppedHeight = gridHeight * GLYPH_HEIGHT;
        if (croppedWidth != inputImage.width || croppedHeight != inputImage.height) {
            System.out.println("Cropping image from " + inputImage.width + "x" + inputImage.height +
                    " to " + croppedWidth + "x" + croppedHeight + " to fit grid."); // Verwende System.out
            inputImage = inputImage.get(0, 0, croppedWidth, croppedHeight);
            inputImage.loadPixels();
        }

        if (gridWidth == 0 || gridHeight == 0) {
            System.err.println("Image too small after resizing/cropping for an 8x8 grid.");
            return;
        }

        resultGrid = new ResultGlyph[gridHeight][gridWidth];

        System.out.println("Starting ASCII conversion...");
        long startTime = System.currentTimeMillis();

        generateAsciiArtExact();

        long endTime = System.currentTimeMillis();
        System.out.println("Conversion finished in " + (endTime - startTime) + " ms.");
    }

    void generateAsciiArtExact() {
        for (int gridY = 0; gridY < gridHeight; gridY++) {
            for (int gridX = 0; gridX < gridWidth; gridX++) {
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

                ResultGlyph exactMatchResult = findExactMatch(blockPixels);

                if (exactMatchResult != null) {
                    resultGrid[gridY][gridX] = exactMatchResult;
                    continue; // Nächste Iteration der gridX-Schleife
                }

                // *** Fallback: Wenn kein exakter Match, dann Annäherung verwenden ***

                // 2. Finde die zwei dominanten Palettenfarben im Block (Annäherung)
                int[] dominantIndices = findDominantPaletteColors(blockPixels);
                int color1Index = dominantIndices[0];
                int color2Index = dominantIndices[1];

                // 3. Finde das beste Zeichen (Codepoint) und die beste Farbkombination (FG/BG)
                // per SSD (Annäherung)
                int bestCodePoint = 0; // Default zu Codepoint 0 (oft NULL oder leer) <-- Geändert zu int
                int bestFgIndex = color1Index;
                int bestBgIndex = color2Index;
                double minError = Double.MAX_VALUE;

                // Iteriere durch alle Glyphen-Patterns (Map<Integer, Long>)
                for (Map.Entry<Integer, Long> entry : asciiPatterns.entrySet()) {
                    int currentCodePoint = entry.getKey(); // <-- Key ist Integer (Codepoint)
                    long currentPattern = entry.getValue();

                    // Berechne Fehler für beide Farbzuweisungen
                    double errorA = calculateMatchError(currentPattern, color1Index, color2Index, blockPixels);
                    double errorB = calculateMatchError(currentPattern, color2Index, color1Index, blockPixels);

                    // Prüfe und update besten Match
                    if (errorA < minError) {
                        minError = errorA;
                        bestCodePoint = currentCodePoint; // <-- Codepoint speichern
                        bestFgIndex = color1Index;
                        bestBgIndex = color2Index;
                    }
                    if (errorB < minError) {
                        minError = errorB;
                        bestCodePoint = currentCodePoint; // <-- Codepoint speichern
                        bestFgIndex = color2Index;
                        bestBgIndex = color1Index;
                    }
                } // Ende Schleife über Patterns

                // Speichere das beste *approximierte* Ergebnis für diesen Block
                resultGrid[gridY][gridX] = new ResultGlyph(bestCodePoint, bestFgIndex, bestBgIndex); // <-- Codepoint
                                                                                                     // übergeben

            } // Ende Schleife gridX
              // Optional: Fortschritt anzeigen
            if ((gridY + 1) % 10 == 0 || gridY == gridHeight - 1) {
                System.out.println("Processed row " + (gridY + 1) + "/" + gridHeight); // Verwende System.out
            }
        } // Ende Schleife gridY
    }

    // Alternative Methode: Nur Annäherung (falls Exakt-Check nicht gewünscht oder
    // zu langsam)
    void generateAsciiArtApproxOnly() {
        for (int gridY = 0; gridY < gridHeight; gridY++) {
            for (int gridX = 0; gridWidth < gridWidth; gridX++) {
                int[] blockPixels = new int[PIXEL_COUNT];
                int startX = gridX * GLYPH_WIDTH;
                int startY = gridY * GLYPH_HEIGHT;
                for (int y = 0; y < GLYPH_HEIGHT; y++) {
                    for (int x = 0; x < GLYPH_WIDTH; x++) {
                        blockPixels[y * GLYPH_WIDTH + x] = inputImage.pixels[(startY + y) * inputImage.width
                                + (startX + x)];
                    }
                }

                int[] dominantIndices = findDominantPaletteColors(blockPixels);
                int color1Index = dominantIndices[0];
                int color2Index = dominantIndices[1];

                int bestCodePoint = 0;
                int bestFgIndex = color1Index;
                int bestBgIndex = color2Index;
                double minError = Double.MAX_VALUE;

                for (Map.Entry<Integer, Long> entry : asciiPatterns.entrySet()) {
                    int currentCodePoint = entry.getKey();
                    long currentPattern = entry.getValue();

                    double errorA = calculateMatchError(currentPattern, color1Index, color2Index, blockPixels);
                    double errorB = calculateMatchError(currentPattern, color2Index, color1Index, blockPixels);

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

                resultGrid[gridY][gridX] = new ResultGlyph(bestCodePoint, bestFgIndex, bestBgIndex);
            }
            if ((gridY + 1) % 10 == 0 || gridY == gridHeight - 1) {
                System.out.println("Processed row " + (gridY + 1) + "/" + gridHeight);
            }
        }
    }

    int[] findDominantPaletteColors(int[] blockPixels) {
        int[] counts = new int[256];

        for (int i = 0; i < PIXEL_COUNT; i++) {
            int nearestIndex = findNearestPaletteIndex(blockPixels[i]);
            counts[nearestIndex]++;
        }

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

        if (bestIndex1 == -1)
            bestIndex1 = 0;
        if (bestIndex2 == -1 || bestIndex2 == bestIndex1) {
            bestIndex2 = (bestIndex1 == 0) ? 15 : 0;
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

    double calculateMatchError(long pattern, int fgIndex, int bgIndex, int[] originalBlockPixels) {
        double totalError = 0;
        int fgColor = colorPalette[fgIndex];
        int bgColor = colorPalette[bgIndex];
        float fgR = red(fgColor);
        float fgG = green(fgColor);
        float fgB = blue(fgColor);
        float bgR = red(bgColor);
        float bgG = green(bgColor);
        float bgB = blue(bgColor);

        for (int i = 0; i < PIXEL_COUNT; i++) {
            boolean pixelOn = ((pattern >> i) & 1L) == 1L;
            float simR = pixelOn ? fgR : bgR;
            float simG = pixelOn ? fgG : bgG;
            float simB = pixelOn ? fgB : bgB;
            int originalColor = originalBlockPixels[i];
            float origR = red(originalColor);
            float origG = green(originalColor);
            float origB = blue(originalColor);
            totalError += (origR - simR) * (origR - simR) + (origG - simG) * (origG - simG)
                    + (origB - simB) * (origB - simB);
        }
        return totalError;
    }

    int[] simulateBlock(long pattern, int fgColor, int bgColor) {
        int[] pixels = new int[PIXEL_COUNT];
        for (int i = 0; i < PIXEL_COUNT; i++) {
            boolean pixelOn = ((pattern >> i) & 1L) == 1L;
            pixels[i] = pixelOn ? fgColor : bgColor;
        }
        return pixels;
    }

    boolean compareBlocksExactly(int[] blockA, int[] blockB) {
        for (int i = 0; i < PIXEL_COUNT; i++) {
            if (blockA[i] != blockB[i])
                return false;
        }
        return true;
    }

    ResultGlyph findExactMatch(int[] blockPixels) {
        Set<Integer> uniqueIndices = new HashSet<>();
        int[] quantizedIndices = new int[PIXEL_COUNT];

        for (int i = 0; i < PIXEL_COUNT; i++) {
            int nearestIndex = findNearestPaletteIndex(blockPixels[i]);
            uniqueIndices.add(nearestIndex);
            quantizedIndices[i] = nearestIndex;
        }

        if (uniqueIndices.size() != 2) {
            return null;
        }

        Integer[] indices = uniqueIndices.toArray(new Integer[0]);
        int indexA = indices[0];
        int indexB = indices[1];
        int colorA = colorPalette[indexA];
        int colorB = colorPalette[indexB];

        for (Map.Entry<Integer, Long> entry : asciiPatterns.entrySet()) {
            int currentCodePoint = entry.getKey();
            long currentPattern = entry.getValue();

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

        return null;
    }

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
            double distSq = (r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                bestIndex = i;
            }
            if (minDistSq == 0)
                break;
        }
        return bestIndex;
    }

    void drawResult() {
        if (resultGrid == null)
            return;

        int cellWidth = GLYPH_WIDTH * DISPLAY_SCALE;
        int cellHeight = GLYPH_HEIGHT * DISPLAY_SCALE;

        int totalGridWidthPixels = gridWidth * cellWidth;
        int totalGridHeightPixels = gridHeight * cellHeight;
        int startDrawX = (width - totalGridWidthPixels) / 2;
        int startDrawY = (height - totalGridHeightPixels) / 2;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                ResultGlyph glyphInfo = resultGrid[y][x];
                if (glyphInfo == null)
                    continue;

                long pattern = asciiPatterns.getOrDefault(glyphInfo.codePoint, 0L);
                int fgColor = colorPalette[glyphInfo.fgIndex];
                int bgColor = colorPalette[glyphInfo.bgIndex];

                int screenX = startDrawX + x * cellWidth;
                int screenY = startDrawY + y * cellHeight;

                displayScaledGlyph(pattern, screenX, screenY, DISPLAY_SCALE, bgColor, fgColor);
            }
        }
    }

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

    void saveResultOldFormat(String filePath) {
        if (resultGrid == null) {
            System.out.println("No result to save.");
            return;
        }

        System.out.println("Saving result to " + filePath + " ...");
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
                    line.append(g.codePoint).append(" ").append(g.fgIndex).append(" ").append(g.bgIndex);
                    if (x < gridWidth - 1) {
                        line.append(" ");
                    }
                }
                writer.println(line.toString());
            }
            writer.flush();
            System.out.println("Result saved successfully.");
        } catch (Exception e) {
            System.err.println("Error saving result file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void saveResult(String filePath) {
        filePath = filePath + "2";
        if (resultGrid == null) {
            System.out.println("No result to save.");
            return;
        }
        if (gridWidth <= 0 || gridHeight <= 0) {
            System.out.println("Invalid grid dimensions, cannot save.");
            return;
        }

        System.out.println("Saving result to " + filePath + " (filtering control chars)...");

        try (FileOutputStream fos = new FileOutputStream(filePath);
                OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                PrintWriter writer = new PrintWriter(osw)) {

            writer.println("TYPE=USCII_ART_V3_SEPARATED");
            writer.println("WIDTH=" + gridWidth);
            writer.println("HEIGHT=" + gridHeight);
            writer.println("COLORS=xterm256");
            writer.println("DATA_FORMAT=CHARS_GRID; FG_BG_GRID");
            writer.println();

            writer.println("CHARS");
            for (int y = 0; y < gridHeight; y++) {
                StringBuilder charLine = new StringBuilder(gridWidth);
                for (int x = 0; x < gridWidth; x++) {
                    ResultGlyph g = resultGrid[y][x];
                    if (g != null) {
                        int cp = g.codePoint;

                        if (cp == 0) {
                            charLine.append(' ');
                        } else if (Character.isISOControl(cp) && !Character.isWhitespace(cp)) {
                            charLine.append('.');
                        } else {
                            charLine.append(Character.toString(cp));
                        }
                    } else {
                        charLine.append('?');
                    }
                }
                writer.println(charLine.toString());
            }
            writer.println();

            writer.println("COLORS");
            for (int y = 0; y < gridHeight; y++) {
                StringBuilder colorLine = new StringBuilder(gridWidth * 8);
                for (int x = 0; x < gridWidth; x++) {
                    ResultGlyph g = resultGrid[y][x];
                    if (g != null) {
                        colorLine.append(g.fgIndex).append(" ").append(g.bgIndex);
                    } else {
                        colorLine.append("0 0");
                    }
                    if (x < gridWidth - 1) {
                        colorLine.append(" ");
                    }
                }
                writer.println(colorLine.toString());
            }

            writer.flush();
            System.out.println("Result saved successfully in V3 format (control chars filtered).");

        } catch (Exception e) {
            System.err.println("Error saving result file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void keyPressed() {
        keyPressed(key);
    }

    public void keyPressed(char k) {
        if (k == 's' || k == 'S') {
            showSourceImage = !showSourceImage;
            System.out.println("Toggled view: " + (showSourceImage ? "Source Image" : "ASCII Art"));
        } else if (k == 'p' || k == 'P') {
            saveResult(outputPath);
        } else if (k >= '1' && k <= '8') {
            DISPLAY_SCALE = k - '0';
            System.out.println("Set display scale to: " + DISPLAY_SCALE);

            if (controlPanel != null) {
                controlPanel.updateScaleSelector(DISPLAY_SCALE);
            }
        } else if (k == 'r' || k == 'R') {
            fill(128);
            textSize(32);
            textAlign(CENTER, CENTER);
            text("Restarting...", width / 2, height / 2);
            loadAndProcessImage(imagePath);
        }
    }

    void setupPalette() {
        colorPalette = new int[] {
                color(0, 0, 0), color(128, 0, 0), color(0, 128, 0), color(128, 128, 0), color(0, 0, 128),
                color(128, 0, 128), color(0, 128, 128), color(192, 192, 192),
                color(128, 128, 128), color(255, 0, 0), color(0, 255, 0), color(255, 255, 0), color(0, 0, 255),
                color(255, 0, 255), color(0, 255, 255), color(255, 255, 255),
                color(0, 0, 0), color(0, 0, 95), color(0, 0, 135), color(0, 0, 175), color(0, 0, 215), color(0, 0, 255),
                color(0, 95, 0), color(0, 95, 95), color(0, 95, 135), color(0, 95, 175), color(0, 95, 215),
                color(0, 95, 255),
                color(0, 135, 0), color(0, 135, 95), color(0, 135, 135), color(0, 135, 175), color(0, 135, 215),
                color(0, 135, 255),
                color(0, 175, 0), color(0, 175, 95), color(0, 175, 135), color(0, 175, 175), color(0, 175, 215),
                color(0, 175, 255),
                color(0, 215, 0), color(0, 215, 95), color(0, 215, 135), color(0, 215, 175), color(0, 215, 215),
                color(0, 215, 255),
                color(0, 255, 0), color(0, 255, 95), color(0, 255, 135), color(0, 255, 175), color(0, 255, 215),
                color(0, 255, 255),
                color(95, 0, 0), color(95, 0, 95), color(95, 0, 135), color(95, 0, 175), color(95, 0, 215),
                color(95, 0, 255),
                color(95, 95, 0), color(95, 95, 95), color(95, 95, 135), color(95, 95, 175), color(95, 95, 215),
                color(95, 95, 255),
                color(95, 135, 0), color(95, 135, 95), color(95, 135, 135), color(95, 135, 175), color(95, 135, 215),
                color(95, 135, 255),
                color(95, 175, 0), color(95, 175, 95), color(95, 175, 135), color(95, 175, 175), color(95, 175, 215),
                color(95, 175, 255),
                color(95, 215, 0), color(95, 215, 95), color(95, 215, 135), color(95, 215, 175), color(95, 215, 215),
                color(95, 215, 255),
                color(95, 255, 0), color(95, 255, 95), color(95, 255, 135), color(95, 255, 175), color(95, 255, 215),
                color(95, 255, 255),
                color(135, 0, 0), color(135, 0, 95), color(135, 0, 135), color(135, 0, 175), color(135, 0, 215),
                color(135, 0, 255),
                color(135, 95, 0), color(135, 95, 95), color(135, 95, 135), color(135, 95, 175), color(135, 95, 215),
                color(135, 95, 255),
                color(135, 135, 0), color(135, 135, 95), color(135, 135, 135), color(135, 135, 175),
                color(135, 135, 215), color(135, 135, 255),
                color(135, 175, 0), color(135, 175, 95), color(135, 175, 135), color(135, 175, 175),
                color(135, 175, 215), color(135, 175, 255),
                color(135, 215, 0), color(135, 215, 95), color(135, 215, 135), color(135, 215, 175),
                color(135, 215, 215), color(135, 215, 255),
                color(135, 255, 0), color(135, 255, 95), color(135, 255, 135), color(135, 255, 175),
                color(135, 255, 215), color(135, 255, 255),
                color(175, 0, 0), color(175, 0, 95), color(175, 0, 135), color(175, 0, 175), color(175, 0, 215),
                color(175, 0, 255),
                color(175, 95, 0), color(175, 95, 95), color(175, 95, 135), color(175, 95, 175), color(175, 95, 215),
                color(175, 95, 255),
                color(175, 135, 0), color(175, 135, 95), color(175, 135, 135), color(175, 135, 175),
                color(175, 135, 215), color(175, 135, 255),
                color(175, 175, 0), color(175, 175, 95), color(175, 175, 135), color(175, 175, 175),
                color(175, 175, 215), color(175, 175, 255),
                color(175, 215, 0), color(175, 215, 95), color(175, 215, 135), color(175, 215, 175),
                color(175, 215, 215), color(175, 215, 255),
                color(175, 255, 0), color(175, 255, 95), color(175, 255, 135), color(175, 255, 175),
                color(175, 255, 215), color(175, 255, 255),
                color(215, 0, 0), color(215, 0, 95), color(215, 0, 135), color(215, 0, 175), color(215, 0, 215),
                color(215, 0, 255),
                color(215, 95, 0), color(215, 95, 95), color(215, 95, 135), color(215, 95, 175), color(215, 95, 215),
                color(215, 95, 255),
                color(215, 135, 0), color(215, 135, 95), color(215, 135, 135), color(215, 135, 175),
                color(215, 135, 215), color(215, 135, 255),
                color(215, 175, 0), color(215, 175, 95), color(215, 175, 135), color(215, 175, 175),
                color(215, 175, 215), color(215, 175, 255),
                color(215, 215, 0), color(215, 215, 95), color(215, 215, 135), color(215, 215, 175),
                color(215, 215, 215), color(215, 215, 255),
                color(215, 255, 0), color(215, 255, 95), color(215, 255, 135), color(215, 255, 175),
                color(215, 255, 215), color(215, 255, 255),
                color(255, 0, 0), color(255, 0, 95), color(255, 0, 135), color(255, 0, 175), color(255, 0, 215),
                color(255, 0, 255),
                color(255, 95, 0), color(255, 95, 95), color(255, 95, 135), color(255, 95, 175), color(255, 95, 215),
                color(255, 95, 255),
                color(255, 135, 0), color(255, 135, 95), color(255, 135, 135), color(255, 135, 175),
                color(255, 135, 215), color(255, 135, 255),
                color(255, 175, 0), color(255, 175, 95), color(255, 175, 135), color(255, 175, 175),
                color(255, 175, 215), color(255, 175, 255),
                color(255, 215, 0), color(255, 215, 95), color(255, 215, 135), color(255, 215, 175),
                color(255, 215, 215), color(255, 215, 255),
                color(255, 255, 0), color(255, 255, 95), color(255, 255, 135), color(255, 255, 175),
                color(255, 255, 215), color(255, 255, 255),
                color(8, 8, 8), color(18, 18, 18), color(28, 28, 28), color(38, 38, 38), color(48, 48, 48),
                color(58, 58, 58),
                color(68, 68, 68), color(78, 78, 78), color(88, 88, 88), color(98, 98, 98), color(108, 108, 108),
                color(118, 118, 118),
                color(128, 128, 128), color(138, 138, 138), color(148, 148, 148), color(158, 158, 158),
                color(168, 168, 168), color(178, 178, 178),
                color(188, 188, 188), color(198, 198, 198), color(208, 208, 208), color(218, 218, 218),
                color(228, 228, 228), color(238, 238, 238)
        };
        if (colorPalette.length != 256) {
            System.err.println("WARNING: Color Palette size is not 256!");
        }
    }

    void fileSelected(File selection) {
        if (selection == null) {
            FileHandler.handle(null);
        } else {
            FileHandler.handle(selection);
        }
    }

    @Override
    public void exit() {
        if (controlPanel != null) {
            controlPanel.dispose();
        }
        super.exit();
    }

    private java.awt.Frame getJFrame() {
        try {
            return (java.awt.Frame) ((processing.awt.PSurfaceAWT.SmoothCanvas) surface.getNative()).getFrame();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Hauptmethode zum Starten der Anwendung.
     * 
     * @param args Kommandozeilenargumente (werden nicht verwendet)
     */
    public static void main(String[] args) {
        try {
            PApplet.main(new String[] { "ProcessingCore" });
        } catch (Exception e) {
            System.err.println("Fehler beim Starten der Anwendung: " + e.getMessage());
            e.printStackTrace();
        }
    }
}