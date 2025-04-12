/**
 * Represents a single character in the resulting ASCII art
 */
public class ResultGlyph {
    public int codePoint; // Unicode Codepoint
    public int fgIndex; // Foreground color index (0-255)
    public int bgIndex; // Background color index (0-255)

    public ResultGlyph(int cp, int fg, int bg) {
        this.codePoint = cp;
        this.fgIndex = fg;
        this.bgIndex = bg;
    }

    @Override
    public String toString() {
        return "CodePoint: " + codePoint + " (U+" + String.format("%04X", codePoint) + "), FG: " + fgIndex + ", BG: "
                + bgIndex;
    }
}