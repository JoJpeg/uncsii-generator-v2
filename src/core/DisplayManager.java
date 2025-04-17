package core;
import processing.core.PApplet;
import processing.core.PImage;
import ui.ControlPanel;


import logger.Logger;

/**
 * Handles drawing, rendering, and display operations
 */
public class DisplayManager {
    private PApplet app;
    private GlyphManager glyphManager;
    private ColorPalette colorPalette;

    // Display properties
    private int displayScale = 2;
    private boolean showSourceImage = false;
    private int drawX = 0;
    private int drawY = 0;
    private int drawW = 0;
    private int drawH = 0;

    // Selection state
    private int mouseGridX = -1;
    private int mouseGridY = -1;
    private ResultGlyph selectedGlyph = null;
    private int clickedGridX = -1;
    private int clickedGridY = -1;
    private ResultGlyph clickedGlyph = null;
    private ControlPanel controlPanel;

    public DisplayManager(PApplet app, GlyphManager glyphManager, ColorPalette colorPalette) {
        this.app = app;
        this.glyphManager = glyphManager;
        this.colorPalette = colorPalette;

        // Initialize display position at the center
        this.drawX = app.width / 2;
        this.drawY = app.height / 2;
    }

    /**
     * Set the control panel for ui updates
     */
    public void setControlPanel(ControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    /**
     * Draw the welcome screen
     */
    public void drawWelcomeScreen() {
        app.background(0);
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.text("Unscii Generator", app.width / 2, app.height / 2);
    }

    /**
     * Draw the source image
     */
    public void drawSourceImage(PImage inputImage, int gridWidth, int gridHeight) {
        if (inputImage == null)
            return;

        float imgAspect = (float) inputImage.width / inputImage.height;
        float canvasAspect = (float) app.width / app.height;
        int cellWidth = GlyphManager.GLYPH_WIDTH * displayScale;
        int cellHeight = GlyphManager.GLYPH_HEIGHT * displayScale;
        int w = gridWidth * cellWidth;
        int h = gridHeight * cellHeight;

        if (imgAspect > canvasAspect) {
            drawW = w;
            drawH = (int) (w / imgAspect);
        } else {
            drawH = h;
            drawW = (int) (h * imgAspect);
        }

        app.imageMode(PApplet.CENTER);
        app.image(inputImage, drawX, drawY, drawW, drawH);
        app.imageMode(PApplet.CORNER);

        // Clear selection info when showing source image
        if (controlPanel != null) {
            controlPanel.updateSelectionInfo(-1, -1, null);
            controlPanel.updateClickedInfo(-1, -1, null, colorPalette.getPalette(), glyphManager.getPatterns());
        }

        mouseGridX = -1;
        mouseGridY = -1;
        selectedGlyph = null;
    }

    /**
     * Draw the ASCII art result
     */
    public void drawResult(AsciiArtResult result) {
        if (result == null || result.isEmpty()) {
            return;
        }

        int cellWidth = GlyphManager.GLYPH_WIDTH * displayScale;
        int cellHeight = GlyphManager.GLYPH_HEIGHT * displayScale;
        int gridWidth = result.getWidth();
        int gridHeight = result.getHeight();

        int totalGridWidthPixels = gridWidth * cellWidth;
        int totalGridHeightPixels = gridHeight * cellHeight;
        int gridOriginX = app.width / 2 - totalGridWidthPixels / 2 + drawX - app.width / 2;
        int gridOriginY = app.height / 2 - totalGridHeightPixels / 2 + drawY - app.height / 2;

        // Draw all glyphs
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                ResultGlyph glyphInfo = result.get(x, y);
                if (glyphInfo == null) {
                    continue;
                }

                long pattern = glyphManager.getPattern(glyphInfo.codePoint);
                int fgColor = colorPalette.getColor(glyphInfo.fgIndex);
                int bgColor = colorPalette.getColor(glyphInfo.bgIndex);
                int screenX = gridOriginX + x * cellWidth;
                int screenY = gridOriginY + y * cellHeight;

                displayScaledGlyph(pattern, screenX, screenY, displayScale, bgColor, fgColor);
            }
        }

        // Handle hover highlight
        handleHoverHighlight(gridOriginX, gridOriginY, cellWidth, cellHeight,
                totalGridWidthPixels, totalGridHeightPixels, gridWidth, gridHeight, result);

        // Handle clicked highlight
        handleClickedHighlight(gridOriginX, gridOriginY, cellWidth, cellHeight);
    }

    /**
     * Handle hover highlight in result view
     */
    private void handleHoverHighlight(int gridOriginX, int gridOriginY, int cellWidth, int cellHeight,
            int totalGridWidthPixels, int totalGridHeightPixels,
            int gridWidth, int gridHeight, AsciiArtResult result) {
        int mouseRelativeX = app.mouseX - gridOriginX;
        int mouseRelativeY = app.mouseY - gridOriginY;
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
            this.selectedGlyph = result.get(this.mouseGridX, this.mouseGridY);

            // Update Control Panel (Hover Info)
            if (controlPanel != null) {
                controlPanel.updateSelectionInfo(this.mouseGridX, this.mouseGridY, this.selectedGlyph);
            }

            // Draw Hover Highlight Rectangle
            int highlightX = gridOriginX + this.mouseGridX * cellWidth;
            int highlightY = gridOriginY + this.mouseGridY * cellHeight;
            app.noFill();
            app.stroke(255, 255, 0); // Yellow outline for hover
            app.strokeWeight(1);
            app.rect(highlightX, highlightY, cellWidth, cellHeight);
            app.noStroke();
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
            app.noFill();
            app.stroke(0, 255, 255); // Cyan outline for clicked
            app.strokeWeight(2); // Make it slightly thicker
            app.rect(clickedHighlightX + 1, clickedHighlightY + 1, cellWidth - 2, cellHeight - 2); // Inset slightly
            app.noStroke();
        }
    }

    /**
     * Display a scaled glyph with the given pattern and colors
     */
    void displayScaledGlyph(long pattern, int screenX, int screenY, int pixelSize, int bgCol, int fgCol) {
        app.noStroke();
        for (int y = 0; y < GlyphManager.GLYPH_HEIGHT; y++) {
            for (int x = 0; x < GlyphManager.GLYPH_WIDTH; x++) {
                int bitIndex = y * GlyphManager.GLYPH_WIDTH + x;
                boolean pixelOn = ((pattern >> bitIndex) & 1L) == 1L;
                app.fill(pixelOn ? fgCol : bgCol);
                app.rect(screenX + x * pixelSize, screenY + y * pixelSize, pixelSize, pixelSize);
            }
        }
    }

    /**
     * Handle mouse press event for selection
     */
    public void handleMousePressed(AsciiArtResult result, int mouseX, int mouseY) {
        if (showSourceImage || result == null || result.isEmpty()) {
            return;
        }

        int cellWidth = GlyphManager.GLYPH_WIDTH * displayScale;
        int cellHeight = GlyphManager.GLYPH_HEIGHT * displayScale;
        int gridWidth = result.getWidth();
        int gridHeight = result.getHeight();
        int totalGridWidthPixels = gridWidth * cellWidth;
        int totalGridHeightPixels = gridHeight * cellHeight;
        int gridOriginX = app.width / 2 - totalGridWidthPixels / 2 + drawX - app.width / 2;
        int gridOriginY = app.height / 2 - totalGridHeightPixels / 2 + drawY - app.height / 2;

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
            clickedGlyph = result.get(clickedGridX, clickedGridY);

            // Update the control panel with the clicked info
            if (controlPanel != null) {
                controlPanel.updateClickedInfo(clickedGridX, clickedGridY, clickedGlyph, colorPalette.getPalette(),
                        glyphManager.getPatterns());
            }

            Logger.println("Clicked cell: X=" + clickedGridX + ", Y=" + clickedGridY);
        } else {
            // Reset clicked selection if outside and update control panel
            clickedGridX = -1;
            clickedGridY = -1;
            clickedGlyph = null;
            if (controlPanel != null) {
                controlPanel.updateClickedInfo(-1, -1, null, colorPalette.getPalette(), glyphManager.getPatterns());
            }
        }
    }

    /**
     * Handle mouse drag event for panning
     */
    public void handleMouseDragged(int mouseX, int mouseY, int pmouseX, int pmouseY, int mouseButton) {
        // Only pan if left mouse button is held
        if (mouseButton == PApplet.LEFT) {
            int deltaX = mouseX - pmouseX;
            int deltaY = mouseY - pmouseY;
            drawX += deltaX;
            drawY += deltaY;
        }
    }

    /**
     * Set the display scale for the ASCII art
     */
    public void setDisplayScale(int newScale, int gridWidth, int gridHeight) {
        newScale = PApplet.constrain(newScale, 1, 8);

        if (newScale == displayScale) {
            return;
        }

        int oldScale = displayScale;

        // Calculate current grid and mouse positions
        float oldCellW = GlyphManager.GLYPH_WIDTH * oldScale;
        float oldCellH = GlyphManager.GLYPH_HEIGHT * oldScale;
        float oldTotalW = gridWidth * oldCellW;
        float oldTotalH = gridHeight * oldCellH;
        float oldGridOriginX = (app.width - oldTotalW) / 2f + drawX - app.width / 2f;
        float oldGridOriginY = (app.height - oldTotalH) / 2f + drawY - app.height / 2f;

        // Convert mouse position to world coordinates
        float worldX = (app.mouseX - oldGridOriginX) / oldScale;
        float worldY = (app.mouseY - oldGridOriginY) / oldScale;

        // Apply new scale
        displayScale = newScale;
        Logger.println("Set display scale to: " + displayScale);

        // Adjust drawing position to keep the point under the mouse unchanged
        float newCellW = GlyphManager.GLYPH_WIDTH * newScale;
        float newCellH = GlyphManager.GLYPH_HEIGHT * newScale;
        float newTotalW = gridWidth * newCellW;
        float newTotalH = gridHeight * newCellH;

        float newDrawX = app.mouseX - worldX * newScale - (app.width - newTotalW) / 2f + app.width / 2f;
        float newDrawY = app.mouseY - worldY * newScale - (app.height - newTotalH) / 2f + app.height / 2f;

        drawX = PApplet.round(newDrawX);
        drawY = PApplet.round(newDrawY);

        // Update the control panel if it exists
        if (controlPanel != null) {
            controlPanel.updateScaleSelector(displayScale);
        }
    }

    // Getters and setters
    public void toggleSourceImage() {
        this.showSourceImage = !this.showSourceImage;
    }

    public boolean isShowingSourceImage() {
        return showSourceImage;
    }

    public int getDisplayScale() {
        return displayScale;
    }

    public ResultGlyph getClickedGlyph() {
        return clickedGlyph;
    }

    public void resetSelection() {
        clickedGridX = -1;
        clickedGridY = -1;
        clickedGlyph = null;
        mouseGridX = -1;
        mouseGridY = -1;
        selectedGlyph = null;
        if (controlPanel != null) {
            controlPanel.updateSelectionInfo(-1, -1, null);
            controlPanel.updateClickedInfo(-1, -1, null, colorPalette.getPalette(), glyphManager.getPatterns());
        }
    }
}