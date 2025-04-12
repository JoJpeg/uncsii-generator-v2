import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Toolkit;
import java.util.Map; // Import Map

/**
 * Ein Swing-Kontrollfenster, das Buttons für die Tastaturkürzel aus
 * ProcessingCore bereitstellt.
 */
public class ControlPanel extends JFrame implements ActionListener {

    private final ProcessingCore p;

    // Buttons & Controls
    private JButton loadButton;
    private JButton toggleViewButton;
    private JButton saveButton;
    private JButton restartButton;
    private JComboBox<String> scaleSelector;
    private TextArea logArea;

    private JButton copyCharButton; // New button for copying

    // Selection Info Labels
    private JLabel hoverXPosLabel;
    private JLabel hoverYPosLabel;
    private JLabel hoverCodepointLabel;
    private JLabel hoverFgIndexLabel;
    private JLabel hoverBgIndexLabel;

    private JLabel clickedXPosLabel;
    private JLabel clickedYPosLabel;
    private JLabel clickedCodepointLabel;
    private JLabel clickedFgIndexLabel;
    private JLabel clickedBgIndexLabel;

    private GlyphPreviewPanel glyphPreviewPanel; // Panel to display the clicked glyph
    private ResultGlyph currentClickedGlyph; // Store the currently clicked glyph
    private int[] currentColorPalette; // Store the palette
    private Map<Integer, Long> currentAsciiPatterns; // Store the patterns

    // Singleton-Instanz
    private static ControlPanel Instance;

    public enum State {
        SETUP,
        EDIT,
    }

    /**
     * Innere Klasse zum Zeichnen des ausgewählten Glyphs.
     */
    private class GlyphPreviewPanel extends JPanel {
        private static final int PREVIEW_PIXEL_SIZE = 10; // Size of each pixel in the preview
        private static final int PREVIEW_WIDTH = ProcessingCore.GLYPH_WIDTH * PREVIEW_PIXEL_SIZE;
        private static final int PREVIEW_HEIGHT = ProcessingCore.GLYPH_HEIGHT * PREVIEW_PIXEL_SIZE;

        GlyphPreviewPanel() {
            setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
            setBorder(BorderFactory.createLineBorder(Color.GRAY)); // Add a border
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Draw background
            drawGlyphColored(g);
            drawOnlyGlyph(g);
        }

        public void drawOnlyGlyph(Graphics g) {
            if (currentClickedGlyph != null && currentColorPalette != null && currentAsciiPatterns != null) {
                long pattern = currentAsciiPatterns.getOrDefault(currentClickedGlyph.codePoint, 0L);
                int fgColorInt = currentColorPalette[currentClickedGlyph.fgIndex];
                int bgColorInt = currentColorPalette[currentClickedGlyph.bgIndex];
                Color fgColor = Color.BLACK;
                Color bgColor = Color.WHITE;

                for (int y = 0; y < ProcessingCore.GLYPH_HEIGHT; y++) {
                    for (int x = 0; x < ProcessingCore.GLYPH_WIDTH; x++) {
                        int bitIndex = y * ProcessingCore.GLYPH_WIDTH + x;
                        boolean pixelOn = ((pattern >> bitIndex) & 1L) == 1L;
                        g.setColor(pixelOn ? fgColor : bgColor);
                        g.fillRect((x + ProcessingCore.GLYPH_WIDTH + 1) * PREVIEW_PIXEL_SIZE, y * PREVIEW_PIXEL_SIZE,
                                PREVIEW_PIXEL_SIZE,
                                PREVIEW_PIXEL_SIZE);
                    }
                }
            }
        }

        public void drawGlyphColored(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());

            if (currentClickedGlyph != null && currentColorPalette != null && currentAsciiPatterns != null) {
                long pattern = currentAsciiPatterns.getOrDefault(currentClickedGlyph.codePoint, 0L);
                int fgColorInt = currentColorPalette[currentClickedGlyph.fgIndex];
                int bgColorInt = currentColorPalette[currentClickedGlyph.bgIndex];
                Color fgColor = new Color(fgColorInt, true); // Use Processing color int directly
                Color bgColor = new Color(bgColorInt, true);

                for (int y = 0; y < ProcessingCore.GLYPH_HEIGHT; y++) {
                    for (int x = 0; x < ProcessingCore.GLYPH_WIDTH; x++) {
                        int bitIndex = y * ProcessingCore.GLYPH_WIDTH + x;
                        boolean pixelOn = ((pattern >> bitIndex) & 1L) == 1L;
                        g.setColor(pixelOn ? fgColor : bgColor);
                        g.fillRect(x * PREVIEW_PIXEL_SIZE, y * PREVIEW_PIXEL_SIZE, PREVIEW_PIXEL_SIZE,
                                PREVIEW_PIXEL_SIZE);
                    }
                }
            } else {
                // Optionally draw a placeholder if nothing is selected
                g.setColor(Color.DARK_GRAY);
                g.drawString("No Glyph", 10, PREVIEW_HEIGHT / 2);
            }
        }
    }

    /**
     * Erstellt ein neues Kontrollfenster für die ProcessingCore-Anwendung.
     * 
     * @param core Die ProcessingCore-Instanz, die gesteuert werden soll.
     */
    public ControlPanel(ProcessingCore core) {
        Instance = this; // Setze die Singleton-Instanz
        if (core == null) {
            throw new IllegalArgumentException("ProcessingCore darf nicht null sein");
        }
        this.p = core;

        // Fenster-Konfiguration
        setTitle("ASCII Art Controls");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        // Layout erstellen
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5)); // Use BorderLayout
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel for Buttons and Scale ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS)); // Vertical stack

        Dimension buttonSize = new Dimension(200, 30); // Keep preferred size

        // --- Buttons Panel (Grid Layout) ---
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // Back to GridLayout for buttons

        loadButton = new JButton("Load File (L)");
        loadButton.setPreferredSize(buttonSize); // Use preferred size
        loadButton.addActionListener(this);
        buttonPanel.add(loadButton);

        toggleViewButton = new JButton("Toggle View (S)");
        toggleViewButton.setPreferredSize(buttonSize);
        toggleViewButton.addActionListener(this);
        buttonPanel.add(toggleViewButton);

        saveButton = new JButton("Save Output (P)");
        saveButton.setPreferredSize(buttonSize);
        saveButton.addActionListener(this);
        buttonPanel.add(saveButton);

        restartButton = new JButton("Restart Processing (R)");
        restartButton.setPreferredSize(buttonSize);
        restartButton.addActionListener(this);
        buttonPanel.add(restartButton);

        topPanel.add(buttonPanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer

        // --- Scale Selector Panel (Flow Layout) ---
        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scalePanel.add(new JLabel("Display Scale:"));
        String[] scales = { "1", "2", "3", "4", "5", "6", "7", "8" };
        scaleSelector = new JComboBox<>(scales);
        scaleSelector.setSelectedIndex(1); // Default to 2
        scaleSelector.addActionListener(this);
        scalePanel.add(scaleSelector);
        // Constrain the panel's max height to prevent vertical stretching
        scalePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleSelector.getPreferredSize().height));
        topPanel.add(scalePanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer

        // --- Selection Info Container ---
        JPanel selectionInfoContainer = new JPanel();
        selectionInfoContainer.setLayout(new BoxLayout(selectionInfoContainer, BoxLayout.Y_AXIS)); // Vertical layout
                                                                                                   // for table + button
        selectionInfoContainer.setBorder(BorderFactory.createTitledBorder("Selection Info"));

        // --- Selection Table Panel (GridLayout) ---
        JPanel selectionTablePanel = new JPanel(new GridLayout(5, 3, 8, 2));

        // Row 1: X Coordinate
        selectionTablePanel.add(new JLabel("X:"));
        clickedXPosLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(clickedXPosLabel);
        hoverXPosLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(hoverXPosLabel);

        // Row 2: Y Coordinate
        selectionTablePanel.add(new JLabel("Y:"));
        clickedYPosLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(clickedYPosLabel);
        hoverYPosLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(hoverYPosLabel);

        // Row 3: Codepoint
        selectionTablePanel.add(new JLabel("Codepoint:"));
        clickedCodepointLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(clickedCodepointLabel);
        hoverCodepointLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(hoverCodepointLabel);

        // Row 4: FG Index
        selectionTablePanel.add(new JLabel("FG Index:"));
        clickedFgIndexLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(clickedFgIndexLabel);
        hoverFgIndexLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(hoverFgIndexLabel);

        // Row 5: BG Index
        selectionTablePanel.add(new JLabel("BG Index:"));
        clickedBgIndexLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(clickedBgIndexLabel);
        hoverBgIndexLabel = new JLabel("-", SwingConstants.CENTER);
        selectionTablePanel.add(hoverBgIndexLabel);

        selectionInfoContainer.add(selectionTablePanel); // Add table to container
        selectionInfoContainer.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer

        // --- Glyph Preview Panel ---
        glyphPreviewPanel = new GlyphPreviewPanel();
        glyphPreviewPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center horizontally
        selectionInfoContainer.add(glyphPreviewPanel); // Add preview panel below table
        selectionInfoContainer.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer

        // --- Copy Button ---
        copyCharButton = new JButton("Copy Clicked Char");
        copyCharButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Center button horizontally
        copyCharButton.setEnabled(false); // Disabled initially
        copyCharButton.addActionListener(this);
        selectionInfoContainer.add(copyCharButton); // Add button below preview

        // Add panels to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(selectionInfoContainer, BorderLayout.CENTER); // Selection info below buttons

        // --- Log Area ---
        logArea = new TextArea(8, 50); // Adjusted size
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.WHITE);
        mainPanel.add(new JScrollPane(logArea), BorderLayout.SOUTH); // Log area at the bottom

        add(mainPanel);
        pack();
        setLocationRelativeTo(null); // Zentrieren auf dem Bildschirm
    }

    public void setState(State state) {
        boolean editEnabled = (state == State.EDIT);
        loadButton.setEnabled(true); // Always enabled? Or depends on state? Assuming always.
        toggleViewButton.setEnabled(editEnabled);
        saveButton.setEnabled(editEnabled);
        restartButton.setEnabled(editEnabled);
        scaleSelector.setEnabled(editEnabled);
        // Copy button state depends on whether something is clicked, handled separately
        if (!editEnabled) {
            updateSelectionInfo(-1, -1, null); // Clear hover info
            updateClickedInfo(-1, -1, null, null, null); // Clear clicked info
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Object source = e.getSource();
            if (source == toggleViewButton) {
                p.keyPressed('s');
            } else if (source == saveButton) {
                p.keyPressed('p');
            } else if (source == restartButton) {
                p.keyPressed('r');
            } else if (source == scaleSelector) {
                int selectedScale = scaleSelector.getSelectedIndex() + 1;
                p.keyPressed((char) ('0' + selectedScale));
            } else if (source == loadButton) {
                p.keyPressed('l');
            } else if (source == copyCharButton) {
                copyClickedCharacterToClipboard();
            }
        } catch (Exception ex) {
            Logger.println("Fehler bei Button-Aktion: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void copyClickedCharacterToClipboard() {
        ResultGlyph clicked = p.getClickedGlyph(); // Need getter in ProcessingCore
        if (clicked != null) {
            try {
                String character = new String(Character.toChars(clicked.codePoint));
                StringSelection stringSelection = new StringSelection(character);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                Logger.println(
                        "Copied character '" + character + "' (Codepoint: " + clicked.codePoint + ") to clipboard.");
            } catch (Exception ex) {
                Logger.println("Error copying character to clipboard: " + ex.getMessage());
            }
        } else {
            Logger.println("No character selected to copy.");
        }
    }

    /**
     * Aktualisiert die Labels für die Zelle unter dem Mauszeiger (Hover).
     * (Updates Column 3 of the table)
     *
     * @param x     Die X-Koordinate der Zelle (-1 wenn keine Auswahl).
     * @param y     Die Y-Koordinate der Zelle (-1 wenn keine Auswahl).
     * @param glyph Das ResultGlyph-Objekt der Zelle (null wenn keine Auswahl).
     */
    public void updateSelectionInfo(int x, int y, ResultGlyph glyph) {
        if (x >= 0 && y >= 0 && glyph != null) {
            hoverXPosLabel.setText(String.valueOf(x));
            hoverYPosLabel.setText(String.valueOf(y));
            hoverCodepointLabel.setText(String.format("U+%04X", glyph.codePoint)); // Keep hex format for hover
            hoverFgIndexLabel.setText(String.valueOf(glyph.fgIndex)); // Set individual FG label
            hoverBgIndexLabel.setText(String.valueOf(glyph.bgIndex)); // Set individual BG label
        } else {
            hoverXPosLabel.setText("-");
            hoverYPosLabel.setText("-");
            hoverCodepointLabel.setText("-");
            hoverFgIndexLabel.setText("-"); // Clear individual FG label
            hoverBgIndexLabel.setText("-"); // Clear individual BG label
        }
    }

    /**
     * Aktualisiert die Labels und das Vorschaufenster für die zuletzt angeklickte
     * Zelle.
     * (Updates Column 2 of the table and the preview panel)
     *
     * @param x             Die X-Koordinate der Zelle (-1 wenn keine Auswahl).
     * @param y             Die Y-Koordinate der Zelle (-1 wenn keine Auswahl).
     * @param glyph         Das ResultGlyph-Objekt der Zelle (null wenn keine
     *                      Auswahl).
     * @param colorPalette  Die Farbpalette.
     * @param asciiPatterns Die Glyphenmuster.
     */
    public void updateClickedInfo(int x, int y, ResultGlyph glyph, int[] colorPalette,
            Map<Integer, Long> asciiPatterns) {
        // Store the data for the preview panel
        this.currentClickedGlyph = glyph;
        this.currentColorPalette = colorPalette;
        this.currentAsciiPatterns = asciiPatterns;

        if (x >= 0 && y >= 0 && glyph != null) {
            clickedXPosLabel.setText(String.valueOf(x));
            clickedYPosLabel.setText(String.valueOf(y));
            // Display character if printable, otherwise just codepoint
            String charDisplay = Character.isDefined(glyph.codePoint) && !Character.isISOControl(glyph.codePoint)
                    ? "'" + new String(Character.toChars(glyph.codePoint)) + "' "
                    : "";
            clickedCodepointLabel
                    .setText(String.format("%s%d (U+%04X)", charDisplay, glyph.codePoint, glyph.codePoint));
            clickedFgIndexLabel.setText(String.valueOf(glyph.fgIndex));
            clickedBgIndexLabel.setText(String.valueOf(glyph.bgIndex));
            copyCharButton.setEnabled(true);
        } else {
            clickedXPosLabel.setText("-");
            clickedYPosLabel.setText("-");
            clickedCodepointLabel.setText("-");
            clickedFgIndexLabel.setText("-");
            clickedBgIndexLabel.setText("-");
            copyCharButton.setEnabled(false);
        }

        // Trigger repaint of the preview panel
        if (glyphPreviewPanel != null) {
            glyphPreviewPanel.repaint();
        }
    }

    /**
     * Aktualisiert den ausgewählten Skalierungsfaktor im UI, ohne einen ActionEvent
     * auszulösen.
     * Wird von ProcessingCore aufgerufen, wenn die Skala per Tastatur/Mausrad
     * geändert wird.
     *
     * @param scale Der aktuelle Skalierungsfaktor (1-8)
     */
    public void updateScaleSelector(int scale) {
        if (scale >= 1 && scale <= 8 && scaleSelector.getSelectedIndex() != (scale - 1)) {
            // Temporär den Listener entfernen, um Endlosschleife zu vermeiden
            ActionListener[] listeners = scaleSelector.getActionListeners();
            for (ActionListener l : listeners) {
                scaleSelector.removeActionListener(l);
            }

            scaleSelector.setSelectedIndex(scale - 1);

            // Listener wieder hinzufügen
            for (ActionListener l : listeners) {
                scaleSelector.addActionListener(l);
            }
        }
    }

    public void appendLog(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(message + "\n");
        } else {
            SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
        }
    }

    public static ControlPanel get() {
        return Instance;
    }

    public static void log(String message) {
        if (Instance != null) {
            Instance.appendLog(message);
        } else {
            // Fallback or queue if needed
            System.out.println("Log (ControlPanel not ready): " + message);
        }
    }
}