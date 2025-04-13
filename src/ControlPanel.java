import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

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

    // Undo/Redo-Buttons
    private JButton undoButton;
    private JButton redoButton;

    private JButton copyGlyphButton;
    private JButton pasteCharExtButton;
    private JButton pasteGlyphIntButton;
    private JButton copyColorsButton;
    private JButton pasteColorsButton;
    private JButton flipColorsButton; // New Button

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

    // Selection Area Labels
    private JLabel selectionXPosLabel;
    private JLabel selectionYPosLabel;
    private JLabel selectionWidthLabel;
    private JLabel selectionHeightLabel;

    private GlyphPreviewPanel glyphPreviewPanel; // Panel to display the clicked glyph
    private ResultGlyph currentClickedGlyph; // Store the currently clicked glyph
    private int[] currentColorPalette; // Store the palette
    private Map<Integer, Long> currentAsciiPatterns; // Store the patterns

    // Internal Clipboards
    private ResultGlyph internalClipboardGlyph = null; // For full glyph copy/paste
    private int internalClipboardFgIndex = -1; // For color-only copy/paste
    private int internalClipboardBgIndex = -1; // For color-only copy/paste

    // Singleton-Instanz
    private static ControlPanel Instance;

    public enum PanelState {
        SETUP,
        EDIT,
    }

    PanelState state = PanelState.SETUP;

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
                Color fgColor = Color.BLACK;
                Color bgColor = Color.WHITE;

                // Berechne richtige X-Position, damit beide Darstellungen übereinander liegen
                int xOffset = getWidth() - (ProcessingCore.GLYPH_WIDTH * PREVIEW_PIXEL_SIZE);

                for (int y = 0; y < ProcessingCore.GLYPH_HEIGHT; y++) {
                    for (int x = 0; x < ProcessingCore.GLYPH_WIDTH; x++) {
                        int bitIndex = y * ProcessingCore.GLYPH_WIDTH + x;
                        boolean pixelOn = ((pattern >> bitIndex) & 1L) == 1L;
                        g.setColor(pixelOn ? fgColor : bgColor);
                        // Positioniere die schwarz-weiße Darstellung an der gleichen Stelle wie die
                        // farbige
                        g.fillRect(xOffset + x * PREVIEW_PIXEL_SIZE, y * PREVIEW_PIXEL_SIZE,
                                PREVIEW_PIXEL_SIZE, PREVIEW_PIXEL_SIZE);
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

        undoButton = new JButton("Undo (Ctrl+Z)");
        undoButton.setPreferredSize(buttonSize);
        undoButton.addActionListener(this);
        buttonPanel.add(undoButton);

        redoButton = new JButton("Redo (Ctrl+Y)");
        redoButton.setPreferredSize(buttonSize);
        redoButton.addActionListener(this);
        buttonPanel.add(redoButton);

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

        // --- Selection Area Dimensions Panel ---
        JPanel selectionAreaPanel = new JPanel(new GridLayout(2, 2, 8, 2));
        selectionAreaPanel.setBorder(BorderFactory.createTitledBorder("Selection Area"));

        // Selection Position (X,Y)
        selectionAreaPanel.add(new JLabel("Position (X,Y):"));
        selectionXPosLabel = new JLabel("-,-", SwingConstants.CENTER);
        selectionAreaPanel.add(selectionXPosLabel);

        // Selection Size (Width,Height)
        selectionAreaPanel.add(new JLabel("Size (W,H):"));
        selectionWidthLabel = new JLabel("-,-", SwingConstants.CENTER);
        selectionAreaPanel.add(selectionWidthLabel);

        // Remove the unused labels that were previously used for separate values
        selectionYPosLabel = null;
        selectionHeightLabel = null;

        selectionInfoContainer.add(selectionAreaPanel);
        selectionInfoContainer.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer

        // --- Glyph Preview Panel ---
        glyphPreviewPanel = new GlyphPreviewPanel();
        glyphPreviewPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center horizontally
        selectionInfoContainer.add(glyphPreviewPanel); // Add preview panel below table
        selectionInfoContainer.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer

        // --- Button Rows for Copy/Paste ---
        // Row 1: Glyph Copy/Paste
        JPanel glyphCopyPastePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        copyGlyphButton = new JButton("Copy Glyph"); // Renamed
        copyGlyphButton.setToolTipText("Copy Char + Colors (Internal) & Char (System Clipboard)");
        copyGlyphButton.setEnabled(false);
        copyGlyphButton.addActionListener(this); // Use 'this' for button clicks
        glyphCopyPastePanel.add(copyGlyphButton);

        pasteCharExtButton = new JButton("Paste Char (Ext)"); // Renamed
        pasteCharExtButton.setToolTipText("Paste Char from System Clipboard, Keep Colors");
        pasteCharExtButton.setEnabled(false);
        pasteCharExtButton.addActionListener(this);
        glyphCopyPastePanel.add(pasteCharExtButton);

        pasteGlyphIntButton = new JButton("Paste Glyph (Int)"); // New
        pasteGlyphIntButton.setToolTipText("Paste Internally Copied Char + Colors");
        pasteGlyphIntButton.setEnabled(false);
        pasteGlyphIntButton.addActionListener(this);
        glyphCopyPastePanel.add(pasteGlyphIntButton);

        glyphCopyPastePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, copyGlyphButton.getPreferredSize().height));
        selectionInfoContainer.add(glyphCopyPastePanel);

        // Row 2: Color Copy/Paste/Flip
        JPanel colorCopyPastePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        copyColorsButton = new JButton("Copy Colors");
        copyColorsButton.setToolTipText("Copy Only Foreground/Background Colors (Cmd+Shift+C)"); // Updated tooltip
        copyColorsButton.setEnabled(false);
        copyColorsButton.addActionListener(this);
        colorCopyPastePanel.add(copyColorsButton);

        pasteColorsButton = new JButton("Paste Colors");
        pasteColorsButton.setToolTipText("Paste Only Internally Copied Colors (Cmd+Shift+V)"); // Updated tooltip
        pasteColorsButton.setEnabled(false);
        pasteColorsButton.addActionListener(this);
        colorCopyPastePanel.add(pasteColorsButton);

        flipColorsButton = new JButton("Flip Colors"); // New Button
        flipColorsButton.setToolTipText("Swap Foreground and Background Colors (Cmd+F)"); // Tooltip for new button
        flipColorsButton.setEnabled(false);
        flipColorsButton.addActionListener(this);
        colorCopyPastePanel.add(flipColorsButton); // Add new button to panel

        colorCopyPastePanel
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, copyColorsButton.getPreferredSize().height));
        selectionInfoContainer.add(colorCopyPastePanel);

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

    public void setState(PanelState state) {
        this.state = state;
        boolean editEnabled = (state == PanelState.EDIT);
        loadButton.setEnabled(true); // Always enabled? Or depends on state? Assuming always.
        toggleViewButton.setEnabled(editEnabled);
        saveButton.setEnabled(editEnabled);
        restartButton.setEnabled(editEnabled);
        scaleSelector.setEnabled(editEnabled);
        undoButton.setEnabled(editEnabled);
        redoButton.setEnabled(editEnabled);

        // Enablement of copy/paste buttons/actions depends on selection/clipboard
        // state,
        // handled mainly in updateClickedInfo. Disable all if not in EDIT state.
        if (!editEnabled) {
            updateSelectionInfo(-1, -1, null); // Clear hover info
            updateClickedInfo(-1, -1, null, null, null); // Clear clicked info
            copyGlyphButton.setEnabled(false);
            pasteCharExtButton.setEnabled(false);
            pasteGlyphIntButton.setEnabled(false);
            copyColorsButton.setEnabled(false);
            pasteColorsButton.setEnabled(false);
            flipColorsButton.setEnabled(false); // Disable new button
        }
    }

    public PanelState getPanelState() {
        return state;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Object source = e.getSource();
            // Handle Button Clicks
            if (source == toggleViewButton)
                p.keyPressed('s');
            else if (source == saveButton)
                p.keyPressed('p');
            else if (source == restartButton)
                p.keyPressed('r');
            else if (source == scaleSelector)
                p.keyPressed((char) ('0' + scaleSelector.getSelectedIndex() + 1));
            else if (source == loadButton)
                p.keyPressed('l');
            else if (source == undoButton)
                p.undoAction(); // Call the undo method in ProcessingCore
            else if (source == redoButton)
                p.redoAction(); // Call the redo method in ProcessingCore
            else if (source == copyGlyphButton)
                copyInternalGlyphAndExternalChar();
            else if (source == pasteCharExtButton)
                pasteCharacterFromClipboard();
            else if (source == pasteGlyphIntButton)
                pasteInternalGlyph();
            else if (source == copyColorsButton)
                copyInternalColors();
            else if (source == pasteColorsButton)
                pasteInternalColors();
            else if (source == flipColorsButton) // Handle new button click
                p.flipClickedGlyphColors(); // Call the new method in ProcessingCore

        } catch (Exception ex) {
            Logger.println("Fehler bei Button-Aktion: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Copies the character to the system clipboard (external).
     */
    private void copyCharacterToClipboard() {
        if (currentClickedGlyph != null) {
            try {
                String character = new String(Character.toChars(currentClickedGlyph.codePoint));
                StringSelection stringSelection = new StringSelection(character);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                Logger.println("Copied char '" + character + "' to system clipboard.");
            } catch (Exception ex) {
                Logger.println("Error copying character to system clipboard: " + ex.getMessage());
            }
        }
    }

    /**
     * Copies the full glyph data internally and the character externally. (Cmd+C)
     */
    public void copyInternalGlyphAndExternalChar() {
        if (currentClickedGlyph != null) {
            // Internal copy
            internalClipboardGlyph = new ResultGlyph(
                    currentClickedGlyph.codePoint,
                    currentClickedGlyph.fgIndex,
                    currentClickedGlyph.bgIndex);
            Logger.println("Copied glyph internally: " + glyphToString(internalClipboardGlyph));
            // External copy (character only)
            copyCharacterToClipboard();
            // Update UI (enable paste buttons)
            updatePasteButtonStates();
        } else {
            Logger.println("No glyph selected to copy.");
        }
    }

    /**
     * Copies only the colors internally. (Cmd+Shift+C)
     */
    public void copyInternalColors() {
        if (currentClickedGlyph != null) {
            internalClipboardFgIndex = currentClickedGlyph.fgIndex;
            internalClipboardBgIndex = currentClickedGlyph.bgIndex;
            Logger.println(
                    "Copied colors internally: FG=" + internalClipboardFgIndex + ", BG=" + internalClipboardBgIndex);
            // Update UI (enable paste buttons)
            updatePasteButtonStates();
        } else {
            Logger.println("No glyph selected to copy colors from.");
        }
    }

    /**
     * Pastes character from system clipboard, keeping existing colors. (Cmd+V)
     * Prüft zuerst, ob ein internes Glyph vorhanden ist, das bevorzugt wird.
     * Falls nicht, wird der externe Zeichenzwischenspeicher verwendet.
     */
    public void pasteCharacterFromClipboard() {
        if (currentClickedGlyph == null) {
            Logger.println("No glyph selected to paste into.");
            return;
        }

        // Prüfe zuerst, ob ein internes Glyph vorhanden ist (Zeichen + Farben)
        if (internalClipboardGlyph != null) {
            Logger.println("Internal glyph available, using that instead of system clipboard.");
            pasteInternalGlyph(); // Nutze die intern gespeicherte Glyphe mit Farben
            return;
        }

        // Prüfe als nächstes, ob Farben intern gespeichert sind
        if (internalClipboardFgIndex != -1 && internalClipboardBgIndex != -1) {
            // Lese nur das Zeichen aus der Zwischenablage
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String pastedText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    if (pastedText != null && !pastedText.isEmpty()) {
                        char charToPaste = pastedText.charAt(0);
                        // Erstelle ein kombiniertes Glyph und füge es ein
                        Logger.println("Using character from system clipboard with internally stored colors.");
                        ResultGlyph combinedGlyph = new ResultGlyph(
                                (int) charToPaste,
                                internalClipboardFgIndex,
                                internalClipboardBgIndex);
                        p.replaceClickedGlyphWithGlyph(combinedGlyph);
                        return;
                    }
                } catch (Exception ex) {
                    Logger.println("Error accessing system clipboard: " + ex.getMessage());
                }
            }
        }

        // Fallback: Nutze nur den externen Zwischenspeicher
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);

        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String pastedText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                if (pastedText != null && !pastedText.isEmpty()) {
                    char charToPaste = pastedText.charAt(0);
                    Logger.println("Pasting external character '" + charToPaste + "' (keeping current colors).");
                    p.replaceClickedGlyph(charToPaste); // Call the method in ProcessingCore
                } else {
                    Logger.println("System clipboard is empty or contains no text.");
                }
            } catch (Exception ex) {
                Logger.println("Error pasting from system clipboard: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            Logger.println("System clipboard does not contain text.");
        }
    }

    /**
     * Pastes the internally copied glyph (char + colors). (Cmd+Alt+V)
     */
    public void pasteInternalGlyph() {
        if (currentClickedGlyph == null) {
            Logger.println("No glyph selected to paste into.");
            return;
        }
        if (internalClipboardGlyph != null) {
            Logger.println("Pasting internal glyph: " + glyphToString(internalClipboardGlyph));
            // Call the method in ProcessingCore to replace the entire glyph
            p.replaceClickedGlyphWithGlyph(internalClipboardGlyph);
            // UI update happens automatically because replaceClickedGlyphWithGlyph calls
            // updateClickedInfo
        } else {
            Logger.println("Internal glyph clipboard is empty.");
        }
    }

    /**
     * Pastes the internally copied colors only. (Cmd+Shift+V)
     */
    public void pasteInternalColors() {
        if (currentClickedGlyph == null) {
            Logger.println("No glyph selected to paste colors onto.");
            return;
        }
        if (internalClipboardFgIndex != -1 && internalClipboardBgIndex != -1) {
            Logger.println(
                    "Pasting internal colors: FG=" + internalClipboardFgIndex + ", BG=" + internalClipboardBgIndex);
            // Call the method in ProcessingCore to replace colors
            p.replaceClickedGlyphColors(internalClipboardFgIndex, internalClipboardBgIndex); // This line requires the
                                                                                             // method in ProcessingCore
            // UI update happens automatically because replaceClickedGlyphColors calls
            // updateClickedInfo
        } else {
            Logger.println("Internal color clipboard is empty.");
        }
    }

    /**
     * Updates the selection area information in the control panel.
     * Shows the position and dimensions of the current selection area.
     * 
     * @param hasSelection Whether there is an active selection
     * @param startX       The starting X coordinate of the selection
     * @param startY       The starting Y coordinate of the selection
     * @param endX         The ending X coordinate of the selection
     * @param endY         The ending Y coordinate of the selection
     */
    public void updateSelectionAreaInfo(boolean hasSelection, int startX, int startY, int endX, int endY) {
        if (hasSelection && startX >= 0 && startY >= 0 && endX >= 0 && endY >= 0) {
            // Normalize coordinates to find top-left corner and dimensions
            int minX = Math.min(startX, endX);
            int minY = Math.min(startY, endY);
            int maxX = Math.max(startX, endX);
            int maxY = Math.max(startY, endY);

            int width = maxX - minX + 1; // +1 because it's inclusive
            int height = maxY - minY + 1; // +1 because it's inclusive

            // Update labels with selection information
            selectionXPosLabel.setText(String.format("%d,%d", minX, minY));
            selectionWidthLabel.setText(String.format("%d,%d", width, height));

            Logger.println("Selection Area: (" + minX + "," + minY + ") Width=" + width + " Height=" + height);
        } else {
            // No selection, clear the information
            selectionXPosLabel.setText("-,-");
            selectionWidthLabel.setText("-,-");
        }
    }

    /**
     * Helper to format glyph info for logging.
     */
    private String glyphToString(ResultGlyph g) {
        if (g == null)
            return "null";
        String charStr = Character.isDefined(g.codePoint) && !Character.isISOControl(g.codePoint)
                ? "'" + new String(Character.toChars(g.codePoint)) + "'"
                : "U+" + String.format("%04X", g.codePoint);
        return String.format("%s (FG:%d, BG:%d)", charStr, g.fgIndex, g.bgIndex);
    }

    /**
     * Updates the enabled state of paste buttons based on internal clipboard
     * content.
     */
    private void updatePasteButtonStates() {
        boolean glyphSelected = currentClickedGlyph != null;
        boolean internalGlyphAvailable = internalClipboardGlyph != null;
        boolean internalColorsAvailable = internalClipboardFgIndex != -1 && internalClipboardBgIndex != -1;

        pasteGlyphIntButton.setEnabled(glyphSelected && internalGlyphAvailable);
        pasteColorsButton.setEnabled(glyphSelected && internalColorsAvailable);
        pasteCharExtButton.setEnabled(glyphSelected);
        flipColorsButton.setEnabled(glyphSelected); // Enable/disable flip button based on selection
    }

    /**
     * Aktualisiert die Labels und das Vorschaufenster für die zuletzt angeklickte
     * Zelle.
     */
    public void updateClickedInfo(int x, int y, ResultGlyph glyph, int[] colorPalette,
            Map<Integer, Long> asciiPatterns) {
        this.currentClickedGlyph = glyph;
        this.currentColorPalette = colorPalette;
        this.currentAsciiPatterns = asciiPatterns;

        boolean glyphSelected = (x >= 0 && y >= 0 && glyph != null);
        Logger.println("updateClickedInfo: glyphSelected=" + glyphSelected);

        if (glyphSelected) {
            clickedXPosLabel.setText(String.valueOf(x));
            clickedYPosLabel.setText(String.valueOf(y));
            String charDisplay = Character.isDefined(glyph.codePoint) && !Character.isISOControl(glyph.codePoint)
                    ? "'" + new String(Character.toChars(glyph.codePoint)) + "' "
                    : "";
            clickedCodepointLabel
                    .setText(String.format("%s%d (U+%04X)", charDisplay, glyph.codePoint, glyph.codePoint));
            clickedFgIndexLabel.setText(String.valueOf(glyph.fgIndex));
            clickedBgIndexLabel.setText(String.valueOf(glyph.bgIndex));
        } else {
            clickedXPosLabel.setText("-");
            clickedYPosLabel.setText("-");
            clickedCodepointLabel.setText("-");
            clickedFgIndexLabel.setText("-");
            clickedBgIndexLabel.setText("-");
        }

        copyGlyphButton.setEnabled(glyphSelected);
        copyColorsButton.setEnabled(glyphSelected);
        flipColorsButton.setEnabled(glyphSelected); // Enable/disable flip button based on selection

        updatePasteButtonStates();

        if (glyphPreviewPanel != null) {
            glyphPreviewPanel.repaint();
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
            hoverCodepointLabel.setText(String.format("U+%04X", glyph.codePoint));
            hoverFgIndexLabel.setText(String.valueOf(glyph.fgIndex));
            hoverBgIndexLabel.setText(String.valueOf(glyph.bgIndex));
        } else {
            hoverXPosLabel.setText("-");
            hoverYPosLabel.setText("-");
            hoverCodepointLabel.setText("-");
            hoverFgIndexLabel.setText("-");
            hoverBgIndexLabel.setText("-");
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
            ActionListener[] listeners = scaleSelector.getActionListeners();
            for (ActionListener l : listeners) {
                scaleSelector.removeActionListener(l);
            }

            scaleSelector.setSelectedIndex(scale - 1);

            for (ActionListener l : listeners) {
                scaleSelector.addActionListener(l);
            }
        }
    }

    public void appendLog(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append(message + "\n");
        } else {
            SwingUtilities.invokeLater(() -> append(message));
        }
    }

    private void append(String message) {
        if (logArea == null)
            return;
        logArea.append(message + "\n");
    }

    public static ControlPanel get() {
        return Instance;
    }

    public static void log(String message) {
        if (Instance != null) {
            Instance.appendLog(message);
        } else {
            System.out.println("Log (ControlPanel not ready): " + message);
        }
    }
}