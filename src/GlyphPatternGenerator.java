import java.awt.Font; // Import für java.awt.Font
import java.util.HashMap;
import java.util.Map;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;

public class GlyphPatternGenerator {

    private PApplet p;
    private PFont pFont; // Processing Font
    private Font nativeFont; // Java AWT Font
    private PGraphics glyphRenderer;
    private final int GLYPH_WIDTH = 8;
    private final int GLYPH_HEIGHT = 8;
    // private final int PIXEL_COUNT = GLYPH_WIDTH * GLYPH_HEIGHT;
    private final float BRIGHTNESS_THRESHOLD = 128.0f;

    // Unicode-Bereich zum Scannen (0 bis BMP Ende)
    // Kann angepasst werden, falls spezifische höhere Blöcke benötigt werden.
    private final int MIN_CODEPOINT = 0x0000;
    private final int MAX_CODEPOINT = 0xFFFF; // Basic Multilingual Plane

    public GlyphPatternGenerator(PApplet papplet, String fontPath, float fontSize) {
        this.p = papplet;
        try {
            this.pFont = p.createFont(fontPath, fontSize, true); // true für smooth (wird aber eh gebinarisiert)
            if (this.pFont == null) {
                throw new RuntimeException("PFont creation returned null for: " + fontPath);
            }
            // Hole das native AWT Font Objekt
            Object nativeObj = this.pFont.getNative();
            if (nativeObj instanceof Font) {
                this.nativeFont = (Font) nativeObj;
            } else {
                throw new RuntimeException("Could not get native java.awt.Font from PFont.");
            }

        } catch (Exception e) {
            System.err.println("Error loading font or getting native font: " + fontPath);
            throw new RuntimeException(e);
        }

        // Off-Screen Buffer
        this.glyphRenderer = p.createGraphics(GLYPH_WIDTH, GLYPH_HEIGHT);
        // Renderer konfigurieren (basierend auf deinen früheren Erkenntnissen)
        this.glyphRenderer.beginDraw();
        this.glyphRenderer.textFont(this.pFont); // Hier PFont setzen
        this.glyphRenderer.textAlign(PConstants.LEFT, PConstants.TOP);
        // WICHTIG: Textgröße im Renderer explizit setzen!
        // PFont Größe beim Laden ist nur ein Hinweis, hier zählt's.
        this.glyphRenderer.textSize(8); // Deine funktionierende Größe
        this.glyphRenderer.background(0); // Schwarz (Pixel 'aus')
        this.glyphRenderer.fill(255); // Weiß (Pixel 'an')
        this.glyphRenderer.endDraw();
        System.out.println("GlyphRenderer prepared with font: " + this.nativeFont.getFontName() + ", size "
                + this.glyphRenderer.textFont.getSize());

    }

    /**
     * Generiert die Pattern-Bibliothek für alle von der Font unterstützten
     * Unicode-Codepoints im definierten Bereich.
     * 
     * @return Eine Map von Integer (Codepoint) zu Long (64-bit Pattern).
     */
    public Map<Integer, Long> generatePatterns() {
        Map<Integer, Long> patterns = new HashMap<>();
        int glyphCount = 0;

        System.out.println("Scanning Unicode range U+" + String.format("%04X", MIN_CODEPOINT) +
                " to U+" + String.format("%04X", MAX_CODEPOINT) + " for displayable glyphs...");

        for (int codePoint = MIN_CODEPOINT; codePoint <= MAX_CODEPOINT; codePoint++) {
            // Prüfen, ob die Font ein Glyph für diesen Codepoint hat
            if (nativeFont.canDisplay(codePoint)) {

                // Überspringe einige problematische/nicht-druckbare Kontrollzeichen
                // (außer speziellen wie Leerzeichen, die wichtig sein könnten)
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    // Optional: Loggen, welche übersprungen werden
                    // System.out.println("Skipping control character: U+" + String.format("%04X",
                    // codePoint));
                    continue;
                }

                // Konvertiere Codepoint zu String für die text() Methode
                String charStr = Character.toString(codePoint);

                // Rendere das Zeichen
                glyphRenderer.beginDraw();
                glyphRenderer.background(0); // Buffer löschen
                glyphRenderer.fill(255);
                // Zeichne den String am Ursprung (0,0 wegen TOP,LEFT Ausrichtung)
                glyphRenderer.text(charStr, 0, 0);
                glyphRenderer.endDraw();

                // Pixel auslesen und Pattern erstellen
                glyphRenderer.loadPixels();
                long pattern = 0L;
                boolean patternIsEmpty = true; // Prüfen, ob das gerenderte Glyph leer ist
                for (int y = 0; y < GLYPH_HEIGHT; y++) {
                    for (int x = 0; x < GLYPH_WIDTH; x++) {
                        int pixelIndex = y * GLYPH_WIDTH + x;
                        int pixelColor = glyphRenderer.pixels[pixelIndex];
                        float brightness = p.brightness(pixelColor);

                        if (brightness > BRIGHTNESS_THRESHOLD) {
                            pattern |= (1L << pixelIndex);
                            patternIsEmpty = false;
                        }
                    }
                }

                // Füge Pattern nur hinzu, wenn es nicht komplett leer ist
                // (Manche Fonts haben 'leere' Glyphen für nicht-unterstützte Zeichen,
                // obwohl canDisplay true sagt, oder für Leerzeichen etc.)
                // Wir wollen leere Patterns evtl. explizit (z.B. für Leerzeichen U+0020)
                if (!patternIsEmpty || codePoint == ' ') { // Leerzeichen explizit erlauben
                    patterns.put(codePoint, pattern);
                    glyphCount++;
                } else {
                    // Optional: Loggen, welche Codepoints leere Glyphen erzeugt haben
                    // System.out.println("Skipping empty glyph for U+" + String.format("%04X",
                    // codePoint));
                }

                // Fortschrittsanzeige alle 1000 geprüften Codepoints
                if (codePoint % 1000 == 0) {
                    System.out.print(".");
                }

            } // end if canDisplay
        } // end for codePoint

        System.out.println("\nFinished scanning.");
        System.out.println("Found and generated patterns for " + glyphCount + " glyphs.");

        // Füge manuell ein leeres Pattern für Codepoint 0 hinzu, falls es fehlt
        // (Kann manchmal als Fallback nützlich sein)
        patterns.putIfAbsent(0, 0L);

        return patterns;
    }

    /**
     * Hilfsmethode zum Debuggen: Gibt ein 8x8 Pattern auf der Konsole aus.
     */
    public static void printPattern(long pattern) {
        // (Keine Änderung hier nötig)
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int bitIndex = y * 8 + x;
                if (((pattern >> bitIndex) & 1L) == 1L) {
                    System.out.print("#");
                } else {
                    System.out.print(".");
                }
            }
            System.out.println();
        }
    }
}