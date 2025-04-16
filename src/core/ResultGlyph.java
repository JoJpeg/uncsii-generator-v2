package core;

/**
 * Represents a single character in the resulting ASCII art
 */
public class ResultGlyph {
    public int codePoint; // Unicode Codepoint
    public int fgIndex; // Foreground color index (0-255)
    public int bgIndex; // Background color index (0-255)

    // Constructor
    public ResultGlyph(int cp, int fg, int bg) {
        this.codePoint = cp;
        this.fgIndex = fg;
        this.bgIndex = bg;
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
     * Create a copy of this glyph
     * 
     * @return A new ResultGlyph with the same values
     */
    public ResultGlyph copy() {
        return new ResultGlyph(codePoint, fgIndex, bgIndex);
    }
}