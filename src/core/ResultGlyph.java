package core;

/**
 * Represents a single character in the resulting ASCII art
 */
public class ResultGlyph {
    public int codePoint; // Unicode Codepoint
    public int fgIndex; // Foreground color index (0-255)
    public int bgIndex; // Background color index (0-255)
    public int alpha; // Alpha value (0-255)

    // Constructor
    public ResultGlyph(int cp, int fg, int bg) {
        this.codePoint = cp;
        this.fgIndex = fg;
        this.bgIndex = bg;
        this.alpha = 255; // Default to fully opaque
    }

    // Constructor with alpha value
    public ResultGlyph(int cp, int fg, int bg, int alpha) {
        this.codePoint = cp;
        this.fgIndex = fg;
        this.bgIndex = bg;
        this.alpha = alpha;
    }

    /**
     * Check if the background of this glyph is transparent
     * 
     * @return true if background uses the transparent color index (0)
     */
    public boolean hasTransparentBackground() {
        return bgIndex == 0;
    }

    /**
     * Check if the foreground of this glyph is transparent
     * 
     * @return true if foreground uses the transparent color index (0)
     */
    public boolean hasTransparentForeground() {
        return fgIndex == 0;
    }

    /**
     * Check if this glyph is generally transparent (low alpha)
     * 
     * @return true if alpha value is below 128
     */
    public boolean isTransparent() {
        return alpha < 128;
    }

    /**
     * Get the alpha value normalized to 0-1 range
     * 
     * @return alpha value from 0.0 (transparent) to 1.0 (opaque)
     */
    public float getAlphaNormalized() {
        return alpha / 255.0f;
    }

    /**
     * Create a copy of this glyph
     * 
     * @return A new ResultGlyph with the same values
     */
    public ResultGlyph copy() {
        return new ResultGlyph(codePoint, fgIndex, bgIndex, alpha);
    }

    /**
     * Create a string representation for debugging
     */
    @Override
    public String toString() {
        return String.format("Glyph[cp=%d, fg=%d, bg=%d, α=%d]",
                codePoint, fgIndex, bgIndex, alpha);
    }
}