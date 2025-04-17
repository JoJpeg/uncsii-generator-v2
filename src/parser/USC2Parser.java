package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import logger.Logger;

public class USC2Parser {

    private enum ParserState {
        READING_HEADER,
        READING_CHARS,
        READING_COLORS,
        FINISHED,
        ERROR
    }

    private static TerminalImage loadTerminalImage(String path) {

        // 1. Resource Path holen
        String resourcePath = new File(path).getAbsolutePath();

        if (resourcePath == null) {
            Logger.println("ERROR: TerminalImage resource not found for path: " + path);
            return null;
        }

        // Variablen für Header-Informationen und Status
        int width = -1;
        int height = -1;
        ParserState currentState = ParserState.READING_HEADER;
        TerminalImage terminalImage = null;
        int charLinesRead = 0;
        int colorLinesRead = 0;

        // 2. Datei lesen und parsen
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(resourcePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // line = line.trim(); // Leerzeichen am Anfang/Ende entfernen (wichtig für
                // Farbzeilen, weniger für
                // Zeichenzeilen)

                if (currentState == ParserState.ERROR) {
                    break; // Fehler aufgetreten, Verarbeitung stoppen
                }

                // Leere Zeilen generell überspringen (außer eventuell innerhalb von Blöcken,
                // wenn erlaubt)
                if (line.isEmpty() && currentState != ParserState.READING_CHARS) {
                    // Leere Zeilen im CHARS Block könnten problematisch sein, wenn die Höhe nicht
                    // passt
                    // Hier überspringen wir sie sicherheitshalber überall außer im CHARS Block
                    // Selbst im CHARS Block sollte eine Zeile mit Breite 0 nicht leer sein.
                    continue;
                }

                switch (currentState) {
                    case ERROR:
                        Logger.println("ERROR: Parser is in ERROR state, skipping line: " + line);
                        break;
                    case READING_HEADER:
                        if (line.startsWith("WIDTH=")) {
                            width = Integer.parseInt(line.substring("WIDTH=".length()));
                        } else if (line.startsWith("HEIGHT=")) {
                            height = Integer.parseInt(line.substring("HEIGHT=".length()));
                        } else if (line.equalsIgnoreCase("CHARS")) {
                            // Header ist fertig, überprüfe ob alles Nötige da ist
                            if (width <= 0 || height <= 0) {
                                Logger.println("ERROR: Invalid or missing WIDTH/HEIGHT before CHARS section in "
                                        + resourcePath);
                                currentState = ParserState.ERROR;
                                break;
                            }
                            // TerminalImage Objekt erstellen
                            terminalImage = new TerminalImage(width, height);
                            terminalImage.setPath(resourcePath); // Pfad im Objekt speichern
                            currentState = ParserState.READING_CHARS; // Zustand wechseln
                            Logger.println("DEBUG: Header parsed. Width=" + width + ", Height=" + height
                                    + ". Reading CHARS...");
                        }
                        // Ignoriere andere Header-Zeilen (TYPE, COLORS, DATA_FORMAT)
                        break;

                    case READING_CHARS:
                        if (charLinesRead >= height) {
                            // Wir haben bereits genug Zeichenzeilen gelesen, diese Zeile sollte "COLORS"
                            // sein
                            if (line.equalsIgnoreCase("COLORS")) {
                                currentState = ParserState.READING_COLORS;
                                Logger.println("DEBUG: Finished reading CHARS. Reading COLORS...");
                            } else {
                                continue;
                            }
                            break; // Aus dem Switch, nächste Iteration der While-Schleife
                        }

                        // Reguläre Zeichenzeile verarbeiten
                        String charLine = line; // Bei V3 ist die Zeile direkt der String
                        if (charLine.length() != width) {
                            Logger.println("ERROR: Character line " + charLinesRead + " has incorrect length. Expected "
                                    + width + ", found " + charLine.length() + " in " + resourcePath + ". Line: '"
                                    + charLine + "'");
                            currentState = ParserState.ERROR;
                            break;
                        }

                        // Zeichen in TerminalImage einfügen (ohne Farben erstmal)
                        for (int x = 0; x < width; x++) {
                            char c = charLine.charAt(x);
                            TerminalChar tc = new TerminalChar();
                            tc.setCharacter(c);
                            // Farben werden später gesetzt
                            terminalImage.setChar(tc, x, charLinesRead);
                        }
                        // Zeile auch im onlyChars Array speichern
                        terminalImage.onlyChars[charLinesRead] = charLine;

                        charLinesRead++;
                        break;

                    case READING_COLORS:
                        // Entferne oder kommentiere diesen Check aus, er wird durch die finale Prüfung
                        // abgedeckt
                        /*
                         * if (colorLinesRead >= height) {
                         * // Wir haben bereits genug Farbzeilen gelesen. Jede weitere Zeile ist
                         * unerwartet.
                         * Logger.println("WARNING: Extra data found after expected " + height +
                         * " color lines in " + resourcePath + ". Ignoring: '" + line + "'");
                         * currentState = ParserState.FINISHED; // Setze hier schon auf FINISHED
                         * break; // Aus dem Switch
                         * }
                         */

                        // Reguläre Farbzeile verarbeiten
                        String[] colorParts = line.split("\\s+");
                        if (colorParts.length != width * 2) {
                            Logger.println(
                                    "ERROR: Color line " + colorLinesRead + " has incorrect number of values. Expected "
                                            + (width * 2) + ", found " + colorParts.length + " in " + resourcePath);
                            currentState = ParserState.ERROR;
                            break;
                        }

                        // Farben den vorhandenen TerminalChars zuweisen
                        for (int i = 0; i < colorParts.length; i += 2) {
                            int x = i / 2; // Spaltenindex
                            try {
                                int fgIndex = Integer.parseInt(colorParts[i]);
                                int bgIndex = Integer.parseInt(colorParts[i + 1]);

                                TerminalChar tc = terminalImage.getChar(x, colorLinesRead);
                                if (tc == null) {
                                    Logger.println("INTERNAL ERROR: TerminalChar at (" + x + "," + colorLinesRead
                                            + ") is null during color processing in " + resourcePath);
                                    currentState = ParserState.ERROR;
                                    break; // Innere Schleife abbrechen
                                }

                                tc.setFgColor(Color.Indexed(fgIndex));
                                tc.setBgColor(Color.Indexed(bgIndex));

                            } catch (NumberFormatException e) {
                                Logger.println("ERROR: Invalid number format in color data row " + colorLinesRead
                                        + ", index " + i + " or " + (i + 1) + " of " + resourcePath + ". Value: '"
                                        + colorParts[i] + "' or '" + colorParts[i + 1] + "'");
                                currentState = ParserState.ERROR;
                                break; // Innere Schleife abbrechen
                            }
                        }
                        if (currentState == ParserState.ERROR)
                            break; // Wenn innere Schleife Fehler hatte

                        colorLinesRead++;

                        // ***** NEU: Prüfen, ob wir fertig sind *nach* dem Inkrementieren *****
                        if (colorLinesRead == height) {
                            currentState = ParserState.FINISHED; // Erfolgreich alle Farbzeilen gelesen
                            Logger.println("DEBUG: Finished reading COLORS. State set to FINISHED.");
                            // Kein 'break' hier, die Schleife wird beim nächsten Durchlauf ohnehin beendet
                            // (oder liest extra Zeilen im FINISHED State)
                        }
                        // ***** ENDE NEU *****

                        break; // Ende des READING_COLORS case
                    case FINISHED:
                        // Wenn wir hier sind, gibt es noch Zeilen nach dem erwarteten Ende. Warnung
                        // ausgeben.
                        Logger.println("WARNING: Extra data found after parsing completed in " + resourcePath
                                + ". Ignoring: '" + line + "'");
                        break;

                    // ERROR state wird nur geprüft, nicht aktiv betreten im switch
                }
            } // Ende while loop (Datei gelesen)

            // 3. Nach dem Lesen der Datei: Finale Überprüfung
            if (currentState == ParserState.ERROR) {
                Logger.println("ERROR: Parsing failed for " + resourcePath);
                return null; // Fehler aufgetreten
            }

            if (terminalImage == null) {
                Logger.println("ERROR: No CHARS section found or file empty/invalid: " + resourcePath);
                return null;
            }

            if (currentState == ParserState.READING_HEADER) {
                Logger.println(
                        "ERROR: Reached end of file while still parsing header (missing CHARS?): " + resourcePath);
                return null;
            }

            if (currentState == ParserState.READING_CHARS) {
                // Entweder nicht genug Zeichenzeilen ODER "COLORS" fehlt
                if (charLinesRead < height) {
                    Logger.println("ERROR: Reached end of file after reading only " + charLinesRead + " of " + height
                            + " character lines (missing COLORS?): " + resourcePath);
                } else {
                    Logger.println(
                            "ERROR: Reached end of file after reading character lines, but 'COLORS' keyword was missing: "
                                    + resourcePath);
                }
                return null;
            }
            if (currentState == ParserState.READING_COLORS) {
                // Nicht genug Farbzeilen gelesen
                Logger.println("ERROR: Reached end of file after reading only " + colorLinesRead + " of " + height
                        + " color lines: " + resourcePath);
                return null;
            }

            // Wenn wir hier sind und nicht im ERROR-State, sollte alles OK sein (oder
            // FINISHED)
            // Überprüfen wir nochmal explizit die Anzahl der gelesenen Zeilen
            if (charLinesRead != height) {
                Logger.println("ERROR: Mismatch in character lines read (" + charLinesRead + ") vs expected height ("
                        + height + ") for " + resourcePath);
                return null;
            }
            if (colorLinesRead != height) {
                Logger.println("ERROR: Mismatch in color lines read (" + colorLinesRead + ") vs expected height ("
                        + height + ") for " + resourcePath);
                return null;
            }

        } catch (IOException e) {
            Logger.println("ERROR: Could not read TerminalImage file: " + resourcePath + " - " + e.getMessage());
            return null;
        } catch (InvalidPathException e) {
            Logger.println("ERROR: Invalid resource path generated: " + resourcePath + " - " + e.getMessage());
            return null;
        } catch (NumberFormatException e) {
            // Kann im Header oder bei Farben auftreten
            Logger.println("ERROR: Could not parse number in " + resourcePath + " - State: " + currentState + " - "
                    + e.getMessage());
            return null;
        }

        // 4. Erfolgreich geparstes Bild zurückgeben
        Logger.println(
                "Successfully loaded TerminalImage (V3): " + path + " (Width: " + width + ", Height: " + height + ")");
        return terminalImage;
    }

}
