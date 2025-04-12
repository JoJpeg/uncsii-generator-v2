import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Ein Swing-Kontrollfenster, das Buttons für die Tastaturkürzel aus
 * ProcessingCore bereitstellt.
 */
public class ControlPanel extends JFrame implements ActionListener {

    private final ProcessingCore p;

    // Buttons
    private JButton loadButton;
    private JButton toggleViewButton;
    private JButton saveButton;
    private JButton restartButton;
    private JComboBox<String> scaleSelector;
    private TextArea logArea;

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
        if (core == null) {
            throw new IllegalArgumentException("ProcessingCore darf nicht null sein");
        }
        this.p = core;

        // Fenster-Konfiguration
        setTitle("ASCII Art Controls");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        // Layout erstellen
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(0, 1, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Dimension buttonSize = new Dimension(200, 30);

        // Komponenten erstellen
        loadButton = new JButton("Load File (L)");
        loadButton.setPreferredSize(buttonSize);
        loadButton.addActionListener(this);
        mainPanel.add(loadButton);

        toggleViewButton = new JButton("Toggle View (S)");
        toggleViewButton.setPreferredSize(buttonSize);
        toggleViewButton.addActionListener(this);
        mainPanel.add(toggleViewButton);

        saveButton = new JButton("Save Output (P)");
        saveButton.setPreferredSize(buttonSize);
        saveButton.addActionListener(this);
        mainPanel.add(saveButton);

        restartButton = new JButton("Restart Processing (R)");
        restartButton.setPreferredSize(buttonSize);
        restartButton.addActionListener(this);
        mainPanel.add(restartButton);

        // TextArea für Log-Ausgaben
        logArea = new TextArea(10, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.WHITE);

        mainPanel.add(new JScrollPane(logArea));

        // Panel für die Skalierungsauswahl
        JPanel scalePanel = new JPanel(new BorderLayout(5, 0));
        scalePanel.add(new JLabel("Display Scale:"), BorderLayout.WEST);

        String[] scales = { "1", "2", "3", "4", "5", "6", "7", "8" };
        scaleSelector = new JComboBox<>(scales);
        // Default-Wert setzen (2 ist der Standardwert in ProcessingCore)
        scaleSelector.setSelectedIndex(1);
        scaleSelector.addActionListener(this);
        scalePanel.add(scaleSelector, BorderLayout.CENTER);

        mainPanel.add(scalePanel);

        // Panel zum Fenster hinzufügen
        add(mainPanel);

        // Fenstergröße an Inhalt anpassen und anzeigen
        pack();
        setLocationRelativeTo(null); // Zentrieren auf dem Bildschirm
    }

    public void setState(State state) {
        if (state == State.SETUP) {
            toggleViewButton.setEnabled(false);
            saveButton.setEnabled(false);
            restartButton.setEnabled(false);
            scaleSelector.setEnabled(false);
        } else if (state == State.EDIT) {
            toggleViewButton.setEnabled(true);
            saveButton.setEnabled(true);
            restartButton.setEnabled(true);
            scaleSelector.setEnabled(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (e.getSource() == toggleViewButton) {
                p.keyPressed('s');
            } else if (e.getSource() == saveButton) {
                p.keyPressed('p');
            } else if (e.getSource() == restartButton) {
                p.keyPressed('r');
            } else if (e.getSource() == scaleSelector) {
                int selectedScale = scaleSelector.getSelectedIndex() + 1;
                p.keyPressed((char) ('0' + selectedScale));
            } else if (e.getSource() == loadButton) {
                p.keyPressed('l');
            }
        } catch (Exception ex) {
            System.err.println("Fehler bei Button-Aktion: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Aktualisiert den ausgewählten Skalierungsfaktor im UI.
     * 
     * @param scale Der aktuelle Skalierungsfaktor (1-8)
     */
    public void updateScaleSelector(int scale) {
        if (scale >= 1 && scale <= 8) {
            scaleSelector.setSelectedIndex(scale - 1);
        }
    }
}