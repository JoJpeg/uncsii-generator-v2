package parser;
import logger.Logger;

public class TerminalImage {
    int width;
    int height;
    TerminalChar[][] image;
    String[] onlyChars;
    String path;
    boolean isSetup = false;

    public TerminalImage(int width, int height) {
        this.width = width;
        this.height = height;
        image = new TerminalChar[height][width];
        // Initialisiere alle Array-Felder mit neuen TerminalChar Objekten
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image[y][x] = new TerminalChar();
            }
        }
        onlyChars = new String[height];
    }

    public void setChar(TerminalChar character, int x, int y) {
        if (!inBounds(x, y))
            return;
        if (character == null) {
            return;
        }
        image[y][x] = character;
        isSetup = true;
    }

    public TerminalChar getChar(int x, int y) {
        if (!inBounds(x, y)) {
            Logger.println("getChar out of bounds: " + x + " " + y);
            return null;
        }
        return image[y][x];
    }

    public boolean inBounds(int x, int y) {
        if (x < 0 || x >= width)
            return false;
        if (y < 0 || y >= height)
            return false;
        return true;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public TerminalChar[][] getImage() {
        return image;
    }

}
