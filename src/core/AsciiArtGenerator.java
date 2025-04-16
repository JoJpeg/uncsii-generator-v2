package core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import logger.Logger;

/**
 * Handles the core ASCII art generation algorithms
 */
public class AsciiArtGenerator {
    private GlyphManager glyphManager;
    private ColorPalette colorPalette;
    private ImageProcessor imageProcessor;

    public AsciiArtGenerator(GlyphManager glyphManager, ColorPalette colorPalette, ImageProcessor imageProcessor) {
        this.glyphManager = glyphManager;
        this.colorPalette = colorPalette;
        this.imageProcessor = imageProcessor;
    }

    /**
     * Generate ASCII art using exact matching when possible, with fallback to
     * approximation
     */
    public AsciiArtResult generateAsciiArt(boolean exactMatchingEnabled) {
        int gridWidth = imageProcessor.getGridWidth();
        int gridHeight = imageProcessor.getGridHeight();

        if (gridWidth <= 0 || gridHeight <= 0) {
            Logger.println("Invalid grid dimensions: " + gridWidth + "x" + gridHeight);
            return null;
        }

        AsciiArtResult result = new AsciiArtResult(gridWidth, gridHeight);

        Logger.println("Starting ASCII conversion...");
        long startTime = System.currentTimeMillis();

        for (int gridY = 0; gridY < gridHeight; gridY++) {
            for (int gridX = 0; gridX < gridWidth; gridX++) {
                int[] blockPixels = imageProcessor.extractBlockPixels(gridX, gridY);
                ResultGlyph glyph = null;

                // Try exact matching if enabled
                if (exactMatchingEnabled) {
                    glyph = findExactMatch(blockPixels);
                }

                // Fall back to approximation if no exact match or exact matching disabled
                if (glyph == null) {
                    glyph = findApproximateMatch(blockPixels);
                }

                result.set(gridX, gridY, glyph);
            }

            // Show progress
            if ((gridY + 1) % 10 == 0 || gridY == gridHeight - 1) {
                Logger.println("Processed row " + (gridY + 1) + "/" + gridHeight);
            }
        }

        long endTime = System.currentTimeMillis();
        Logger.println("Conversion finished in " + (endTime - startTime) + " ms.");

        return result;
    }

    /**
     * Try to find an exact match for a block of pixels
     */
    private ResultGlyph findExactMatch(int[] blockPixels) {
        Set<Integer> uniqueIndices = new HashSet<>();
        int[] quantizedIndices = new int[GlyphManager.PIXEL_COUNT];

        // Extract average alpha from block pixels for debugging
        int totalAlpha = 0;
        for (int pixel : blockPixels) {
            totalAlpha += (pixel >> 24) & 0xFF;
        }
        int avgAlpha = totalAlpha / blockPixels.length;

        // Quantize pixels to palette indices
        for (int i = 0; i < GlyphManager.PIXEL_COUNT; i++) {
            int nearestIndex = colorPalette.findNearestColorIndex(blockPixels[i]);
            uniqueIndices.add(nearestIndex);
            quantizedIndices[i] = nearestIndex;
        }

        // Special case: Single-color block
        if (uniqueIndices.size() == 1) {
            int singleIndex = uniqueIndices.iterator().next();
            Map<Integer, Long> patterns = glyphManager.getAllPatterns();

            // Try to find either a completely filled or completely empty glyph
            for (Map.Entry<Integer, Long> entry : patterns.entrySet()) {
                long pattern = entry.getValue();
                boolean isAllOn = pattern == -1L; // All bits set (all pixels on)
                boolean isAllOff = pattern == 0L; // All bits clear (all pixels off)

                if (isAllOn || isAllOff) {
                    int codePoint = entry.getKey();
                    // For solid blocks, we'll make both FG and BG the same color
                    return new ResultGlyph(codePoint, singleIndex, singleIndex, avgAlpha);
                }
            }

            // If no solid pattern found, continue to normal matching process
            // by finding a second color to use with the block
        }

        // Exact match only possible with exactly 2 colors (or one color with special
        // handling)
        if (uniqueIndices.size() != 2) {
            return null;
        }

        // Get the two unique colors
        Integer[] indices = uniqueIndices.toArray(new Integer[0]);
        int indexA = indices[0];
        int indexB = indices[1];
        int colorA = colorPalette.getColor(indexA);
        int colorB = colorPalette.getColor(indexB);

        Map<Integer, Long> patterns = glyphManager.getAllPatterns();

        // Try each glyph pattern
        for (Map.Entry<Integer, Long> entry : patterns.entrySet()) {
            int currentCodePoint = entry.getKey();
            long currentPattern = entry.getValue();

            // Try color combination A
            boolean matchA = true;
            for (int i = 0; i < GlyphManager.PIXEL_COUNT; i++) {
                boolean pixelOn = ((currentPattern >> i) & 1L) == 1L;
                int simulatedQuantizedIndex = pixelOn ? indexA : indexB;
                if (simulatedQuantizedIndex != quantizedIndices[i]) {
                    matchA = false;
                    break;
                }
            }

            if (matchA) {
                int[] simulatedExactA = glyphManager.simulateBlock(currentPattern, colorA, colorB);
                if (glyphManager.compareBlocksExactly(blockPixels, simulatedExactA)) {
                    return new ResultGlyph(currentCodePoint, indexA, indexB, avgAlpha);
                }
            }

            // Try color combination B
            boolean matchB = true;
            for (int i = 0; i < GlyphManager.PIXEL_COUNT; i++) {
                boolean pixelOn = ((currentPattern >> i) & 1L) == 1L;
                int simulatedQuantizedIndex = pixelOn ? indexB : indexA;
                if (simulatedQuantizedIndex != quantizedIndices[i]) {
                    matchB = false;
                    break;
                }
            }

            if (matchB) {
                int[] simulatedExactB = glyphManager.simulateBlock(currentPattern, colorB, colorA);
                if (glyphManager.compareBlocksExactly(blockPixels, simulatedExactB)) {
                    return new ResultGlyph(currentCodePoint, indexB, indexA, avgAlpha);
                }
            }
        }

        return null; // No exact match found
    }

    /**
     * Find an approximate match for a block using dominant colors
     */
    private ResultGlyph findApproximateMatch(int[] blockPixels) {
        // Find two dominant colors
        int[] dominantIndices = colorPalette.findDominantColors(blockPixels);
        int color1Index = dominantIndices[0];
        int color2Index = dominantIndices[1];

        // Extract average alpha from block pixels for debugging
        int totalAlpha = 0;
        for (int pixel : blockPixels) {
            totalAlpha += (pixel >> 24) & 0xFF;
        }
        int avgAlpha = totalAlpha / blockPixels.length;

        // Find best character and color combination
        int bestCodePoint = 0;
        int bestFgIndex = color1Index;
        int bestBgIndex = color2Index;
        double minError = Double.MAX_VALUE;

        Map<Integer, Long> patterns = glyphManager.getAllPatterns();

        // Iterate through all glyph patterns
        for (Map.Entry<Integer, Long> entry : patterns.entrySet()) {
            int currentCodePoint = entry.getKey();
            long currentPattern = entry.getValue();

            // Calculate error for both color assignments
            double errorA = glyphManager.calculateMatchError(currentPattern, color1Index, color2Index, blockPixels,
                    colorPalette);
            double errorB = glyphManager.calculateMatchError(currentPattern, color2Index, color1Index, blockPixels,
                    colorPalette);

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

        // Return result with extracted alpha value for debugging
        return new ResultGlyph(bestCodePoint, bestFgIndex, bestBgIndex, avgAlpha);
    }
}