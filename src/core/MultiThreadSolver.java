package core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import logger.Logger;

public class  MultiThreadSolver {
    ProcessingCore core;
    
    public MultiThreadSolver(ProcessingCore core) {
        this.core = core;
    }

    void generateAsciiArtExact() {
        AtomicInteger processedRows = new AtomicInteger(0);
        
        IntStream.range(0, core.gridHeight).parallel().forEach(gridY -> {
            for (int gridX = 0; gridX < core.gridWidth; gridX++) {
                int[] blockPixels = core.extractBlockPixels(gridX, gridY);

                // Try to find an exact match first
                ResultGlyph exactMatchResult = core.findExactMatch(blockPixels);
                if (exactMatchResult != null) {
                    core.resultGrid[gridY][gridX] = exactMatchResult;
                    continue;
                }

                // Fall back to approximation if no exact match
                core.resultGrid[gridY][gridX] = core.findApproximateMatch(blockPixels);
            }
            
            // Optional: Thread-safe progress update (may impact performance slightly)
            int current = processedRows.incrementAndGet();
            if (current % 10 == 0 || current == core.gridHeight) {
                // Logger.println("Processed row " + current + "/" + gridHeight); 
            }
        });
    }

    void generateAsciiArtApproxOnly() {
        IntStream.range(0, core.gridHeight).parallel().forEach(gridY -> {
            for (int gridX = 0; gridX < core.gridWidth; gridX++) {
                int[] blockPixels = core.extractBlockPixels(gridX, gridY);
                core.resultGrid[gridY][gridX] = core.findApproximateMatch(blockPixels);
            }
        });
    }

    public void reprocessSelectedArea() {
        if (!core.hasSelection || core.inputImage == null || core.resultGrid == null) {
            Logger.println("Cannot reprocess: No selection or image not loaded.");
            return;
        }
        // Normalize selection coordinates
        int minX = ProcessingCore.min(core.selectionStartX, core.selectionEndX);
        int maxX = ProcessingCore.max(core.selectionStartX, core.selectionEndX);
        int minY = ProcessingCore.min(core.selectionStartY, core.selectionEndY);
        int maxY = ProcessingCore.max(core.selectionStartY, core.selectionEndY);

        Logger.println("Reprocessing area from (" + minX + "," + minY + ") to (" + maxX + "," + maxY + ")");

        long startTime = System.currentTimeMillis();

        // Process only the selected blocks
        IntStream.rangeClosed(minY, maxY).parallel().forEach(gridY -> {
            for (int gridX = minX; gridX <= maxX; gridX++) {
                int[] blockPixels = core.extractBlockPixels(gridX, gridY);

                // Try to find an exact match first
                ResultGlyph exactMatchResult = core.findExactMatch(blockPixels);
                if (exactMatchResult != null) {
                    core.resultGrid[gridY][gridX] = exactMatchResult;
                    continue;
                }

                // Fall back to approximation if no exact match
                core.resultGrid[gridY][gridX] = core.findApproximateMatch(blockPixels);
            }
        });

        long endTime = System.currentTimeMillis();
        Logger.println("Selection reprocessing finished in " + (endTime - startTime) + " ms.");

    }
}
