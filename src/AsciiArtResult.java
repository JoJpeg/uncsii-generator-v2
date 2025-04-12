/**
 * Encapsulates a complete ASCII art conversion result
 */
public class AsciiArtResult {
    private ResultGlyph[][] grid;
    private int width;
    private int height;

    public AsciiArtResult(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new ResultGlyph[height][width];
    }

    public ResultGlyph get(int x, int y) {
        if (isValidPosition(x, y)) {
            return grid[y][x];
        }
        return null;
    }

    public void set(int x, int y, ResultGlyph glyph) {
        if (isValidPosition(x, y)) {
            grid[y][x] = glyph;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ResultGlyph[][] getGrid() {
        return grid;
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isEmpty() {
        return width == 0 || height == 0 || grid == null;
    }
}