import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Toolkit;

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

    // Selection Info Labels (Hover) - Renamed for clarity
    private JLabel hoverXPosLabel;
    private JLabel hoverYPosLabel;
    private JLabel hoverCodepointLabel;
    private JLabel hoverFgIndexLabel;
    private JLabel hoverBgIndexLabel;

    // Clicked Selection Info Labels (New)
    private JLabel clickedXPosLabel;
    private JLabel clickedYPosLabel;
    private JLabel clickedCodepointLabel;
    private JLabel clickedFgIndexLabel;
    private JLabel clickedBgIndexLabel;

    // Singleton-Instanz
    private static ControlPanel Instance;

    public enum State {
        SETUP,
        EDIT,
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

        // --- Selection Info Panels ---
        JPanel selectionInfoContainer = new JPanel();
        selectionInfoContainer.setLayout(new BoxLayout(selectionInfoContainer, BoxLayout.Y_AXIS));

        // --- Hover Selection Info Panel ---
        JPanel hoverSelectionPanel = new JPanel(new GridLayout(0, 2, 5, 2));
        hoverSelectionPanel.setBorder(BorderFactory.createTitledBorder("Hover Info"));
        hoverSelectionPanel.add(new JLabel("X:"));
        hoverXPosLabel = new JLabel("-"); // Renamed
        hoverSelectionPanel.add(hoverXPosLabel);
        hoverSelectionPanel.add(new JLabel("Y:"));
        hoverYPosLabel = new JLabel("-"); // Renamed
        hoverSelectionPanel.add(hoverYPosLabel);
        hoverSelectionPanel.add(new JLabel("Codepoint:"));
        hoverCodepointLabel = new JLabel("-"); // Renamed
        hoverSelectionPanel.add(hoverCodepointLabel);
        hoverSelectionPanel.add(new JLabel("FG Index:"));
        hoverFgIndexLabel = new JLabel("-"); // Renamed
        hoverSelectionPanel.add(hoverFgIndexLabel);
        hoverSelectionPanel.add(new JLabel("BG Index:"));
        hoverBgIndexLabel = new JLabel("-"); // Renamed
        hoverSelectionPanel.add(hoverBgIndexLabel);
        selectionInfoContainer.add(hoverSelectionPanel);
        selectionInfoContainer.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer

        // --- Clicked Selection Info Panel (New) ---
        JPanel clickedSelectionPanel = new JPanel(new GridLayout(0, 2, 5, 2));
        clickedSelectionPanel.setBorder(BorderFactory.createTitledBorder("Clicked Info"));
        clickedSelectionPanel.add(new JLabel("X:"));
        clickedXPosLabel = new JLabel("-"); // New
        clickedSelectionPanel.add(clickedXPosLabel);
        clickedSelectionPanel.add(new JLabel("Y:"));
        clickedYPosLabel = new JLabel("-"); // New
        clickedSelectionPanel.add(clickedYPosLabel);
        clickedSelectionPanel.add(new JLabel("Codepoint:"));
        clickedCodepointLabel = new JLabel("-"); // New
        clickedSelectionPanel.add(clickedCodepointLabel);
        clickedSelectionPanel.add(new JLabel("FG Index:"));
        clickedFgIndexLabel = new JLabel("-"); // New
        clickedSelectionPanel.add(clickedFgIndexLabel);
        clickedSelectionPanel.add(new JLabel("BG Index:"));
        clickedBgIndexLabel = new JLabel("-"); // New
        clickedSelectionPanel.add(clickedBgIndexLabel);
        selectionInfoContainer.add(clickedSelectionPanel);
        selectionInfoContainer.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer

        // --- Copy Button (New) ---
        copyCharButton = new JButton("Copy Clicked Char");
        copyCharButton.setPreferredSize(new Dimension(buttonSize.width, buttonSize.height)); // Use preferred size
        copyCharButton.setEnabled(false); // Disabled initially
        copyCharButton.addActionListener(this);
        JPanel copyButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Center the button
        copyButtonPanel.add(copyCharButton);
        selectionInfoContainer.add(copyButtonPanel);

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
            updateClickedInfo(-1, -1, null); // Clear clicked info
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
     *
     * @param x     Die X-Koordinate der Zelle (-1 wenn keine Auswahl).
     * @param y     Die Y-Koordinate der Zelle (-1 wenn keine Auswahl).
     * @param glyph Das ResultGlyph-Objekt der Zelle (null wenn keine Auswahl).
     */
    public void updateSelectionInfo(int x, int y, ResultGlyph glyph) { // Renamed method
        if (x >= 0 && y >= 0 && glyph != null) {
            hoverXPosLabel.setText(String.valueOf(x));
            hoverYPosLabel.setText(String.valueOf(y));
            hoverCodepointLabel.setText(String.format("%d (U+%04X)", glyph.codePoint, glyph.codePoint));
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
     * Aktualisiert die Labels für die zuletzt angeklickte Zelle.
     *
     * @param x     Die X-Koordinate der Zelle (-1 wenn keine Auswahl).
     * @param y     Die Y-Koordinate der Zelle (-1 wenn keine Auswahl).
     * @param glyph Das ResultGlyph-Objekt der Zelle (null wenn keine Auswahl).
     */
    public void updateClickedInfo(int x, int y, ResultGlyph glyph) { // New method
        if (x >= 0 && y >= 0 && glyph != null) {
            clickedXPosLabel.setText(String.valueOf(x));
            clickedYPosLabel.setText(String.valueOf(y));
            clickedCodepointLabel.setText(String.format("%d (U+%04X)", glyph.codePoint, glyph.codePoint));
            clickedFgIndexLabel.setText(String.valueOf(glyph.fgIndex));
            clickedBgIndexLabel.setText(String.valueOf(glyph.bgIndex));
            copyCharButton.setEnabled(true); // Enable copy button
        } else {
            clickedXPosLabel.setText("-");
            clickedYPosLabel.setText("-");
            clickedCodepointLabel.setText("-");
            clickedFgIndexLabel.setText("-");
            clickedBgIndexLabel.setText("-");
            copyCharButton.setEnabled(false); // Disable copy button
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