package parser;

public class TerminalChar {
    char character;
    Color fgColor;
    Color bgColor;
    boolean hasFgColor = false;
    boolean hasBgColor = false;
    boolean hasChar = false;

    // Default-Konstruktor mit Standardfarben
    public TerminalChar() {
        this.fgColor = Color.WHITE;
        this.bgColor = Color.BLACK;
        this.hasFgColor = true;
        this.hasBgColor = true;
    }

    // Konstruktor mit Zeichen
    public TerminalChar(char character) {
        this();
        setCharacter(character);
    }

    // Konstruktor mit Zeichen und Farben
    public TerminalChar(char character, Color fgColor, Color bgColor) {
        setCharacter(character);
        setFgColor(fgColor);
        setBgColor(bgColor);
    }

    public void setCharacter(char character) {
        this.character = character;
        hasChar = true;
    }

    public char getCharacter() {
        return character;
    }

    public Color getBgColor() {
        return bgColor;
    }

    public Color getFgColor() {
        return fgColor;
    }

    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
        hasBgColor = (bgColor != null);
    }

    public void setFgColor(Color fgColor) {
        this.fgColor = fgColor;
        hasFgColor = (fgColor != null);
    }
}
