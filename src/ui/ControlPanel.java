package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import javax.swing.JTextArea;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import core.ColorPalette;
import core.ProcessingCore;
import core.ResultGlyph;
import logger.Logger;

/**
 * Ein Swing-Kontrollfenster, das Buttons für die Tastaturkürzel aus
 * ProcessingCore bereitstellt.
 */
public class ControlPanel extends JFrame implements ActionListener {

    private final ProcessingCore p;
    private BatchManager batchManager;

    // Buttons & Controls
    // private JButton openButton;
    private JButton toggleViewButton;
    // private JButton exportButton;
    private JButton restartButton;
    // private JButton batchButton;
    // private JButton saveProjectButton;
    private JComboBox<String> scaleSelector;
    private JTextArea logArea;

    // Undo/Redo-Buttons
    private JButton undoButton;
    private JButton redoButton;

    private JButton copyGlyphButton;
    private JButton pasteCharExtButton;
    private JButton pasteGlyphIntButton;
    private JButton copyColorsButton;
    private JButton pasteColorsButton;
    private JButton flipColorsButton;
    private JButton flipGlyphButton;
    private JButton centerImageButton;
    private JButton transparentBgButton;

    // Selection Info Labels
    private JLabel hoverPosLabel;
    private JLabel hoverCodepointLabel;
    private JLabel hoverFgIndexLabel;
    private JLabel hoverBgIndexLabel;
    private JLabel hoverAlphaLabel;

    private JLabel clickedPosLabel;
    private JLabel clickedCodepointLabel;
    private JLabel clickedFgIndexLabel;
    private JLabel clickedBgIndexLabel;
    private JLabel clickedAlphaLabel;

    // Selection Area Labels
    private JLabel selectionXPosLabel;
    private JLabel selectionWidthLabel;

    private GlyphPreviewPanel glyphPreviewPanel;
    private ResultGlyph currentClickedGlyph;
    private Map<Integer, Long> currentAsciiPatterns;

    // Internal Clipboards
    private ResultGlyph internalClipboardGlyph = null;
    private int internalClipboardFgIndex = -1;
    private int internalClipboardBgIndex = -1;

    public static boolean preferLightForeground = true;
    public static boolean usePreference = false;

    private static ControlPanel Instance;

    public enum PanelState {
        SETUP,
        EDIT,
    }

    public enum AlgoPreference {
        Light,
        Dark,
        Threshold_Light,
        Threshold_Dark,
        None,
    }

    public static AlgoPreference algoPreference = AlgoPreference.None;
    public static double algoDeltaThreshold = 100;
    PanelState state = PanelState.SETUP;

    private class GlyphPreviewPanel extends JPanel {
        private static final int PREVIEW_PIXEL_SIZE = 8;
        private static final int PREVIEW_WIDTH = ProcessingCore.GLYPH_WIDTH * PREVIEW_PIXEL_SIZE;
        private static final int PREVIEW_HEIGHT = (ProcessingCore.GLYPH_HEIGHT * PREVIEW_PIXEL_SIZE) * 2;

        GlyphPreviewPanel() {
            setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            drawGlyphColored(g);
            drawOnlyGlyph(g);
        }

        public void drawOnlyGlyph(Graphics g) {
            if (currentClickedGlyph != null && ColorPalette.getColors() != null && currentAsciiPatterns != null) {
                long pattern = currentAsciiPatterns.getOrDefault(currentClickedGlyph.codePoint, 0L);
                Color fgColor = Color.BLACK;
                Color bgColor = Color.WHITE;

                int yOffset = ProcessingCore.GLYPH_HEIGHT * PREVIEW_PIXEL_SIZE;

                for (int y = 0; y < ProcessingCore.GLYPH_HEIGHT; y++) {
                    for (int x = 0; x < ProcessingCore.GLYPH_WIDTH; x++) {
                        int bitIndex = y * ProcessingCore.GLYPH_WIDTH + x;
                        boolean pixelOn = ((pattern >> bitIndex) & 1L) == 1L;
                        g.setColor(pixelOn ? fgColor : bgColor);
                        g.fillRect(x * PREVIEW_PIXEL_SIZE, yOffset + (y * PREVIEW_PIXEL_SIZE), PREVIEW_PIXEL_SIZE,
                                PREVIEW_PIXEL_SIZE);
                    }
                }
            }
        }

        public void drawGlyphColored(Graphics g) {
            if (currentClickedGlyph != null && ColorPalette.getColors() != null && currentAsciiPatterns != null) {
                long pattern = currentAsciiPatterns.getOrDefault(currentClickedGlyph.codePoint, 0L);
                int fgColorInt = ColorPalette.getColors()[currentClickedGlyph.fgIndex];
                int bgColorInt = ColorPalette.getColors()[currentClickedGlyph.bgIndex];
                Color fgColor = new Color(fgColorInt, true);
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
                g.setColor(Color.DARK_GRAY);
                // g.drawString("", 2, PREVIEW_HEIGHT / 4);
            }
        }
    }

    public ControlPanel(ProcessingCore core) {
        Instance = this;
        if (core == null) {
            throw new IllegalArgumentException("ProcessingCore cannot be null");
        }
        this.p = core;
        this.batchManager = new BatchManager(this, core);

        batchManager.setVisible(true);

        setTitle("Controls");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        // Compact layout
        JPanel mainPanel = new JPanel(new BorderLayout(3, 3));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // --- 1. Toolbar Section (Buttons) ---
        JPanel toolbarPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        // openButton = createButton("Open", "Load File (L)");
        toggleViewButton = createButton("Image", "Toggle View (S)");
        // exportButton = createButton("Export", "Export Output (P)");
        restartButton = createButton("Recalc", "Restart Processing (R)");
        
        // batchButton = createButton("Batch", "Open Batch Manager");
        // saveProjectButton = createButton("Save Project", "Save Project");

        JPanel fileRow = new JPanel(new GridLayout(1, 6, 3, 0));
        // fileRow.add(openButton);
        // // fileRow.add(exportButton);
        // fileRow.add(saveProjectButton);
        // fileRow.add(batchButton);
        fileRow.add(restartButton);
        fileRow.add(toggleViewButton);

        gbc.gridx = 0; gbc.gridy = 0;
        toolbarPanel.add(fileRow, gbc);

        gbc.gridx = 0; gbc.gridy = 0;
        toolbarPanel.add(fileRow, gbc);

        // Row 2: Edit & View Actions
        undoButton = createButton("< Undo", "Undo (Ctrl+Z)");
        redoButton = createButton("Redo >", "Redo (Ctrl+Y)");
        centerImageButton = createButton("Center", "Center Image");
        
        String[] scales = { "1x", "2x", "3x", "4x", "5x", "6x", "7x", "8x" };
        scaleSelector = new JComboBox<>(scales);
        scaleSelector.setSelectedIndex(1);
        scaleSelector.addActionListener(this);
        scaleSelector.setToolTipText("Display Scale");
        
        JPanel editRow = new JPanel(new GridLayout(1, 4, 3, 0));
        editRow.add(undoButton);
        editRow.add(redoButton);
        editRow.add(centerImageButton);
        editRow.add(scaleSelector);
        
        gbc.gridy = 1;
        toolbarPanel.add(editRow, gbc);

        // Row 3: Algorithm Settings
        JPanel algoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        algoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Algorithm", 0, 0, new Font("SansSerif", Font.PLAIN, 10)));
        
        JComboBox<String> algoPreferenceSelector = new JComboBox<>(
                new String[] { "Light", "Dark", "Thresh_L", "Thresh_D", "None" });
        algoPreferenceSelector.setSelectedItem(algoPreference.toString());
        algoPreferenceSelector.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        JSpinner algoDeltaThresholdSpinner = new JSpinner();
        algoDeltaThresholdSpinner.setPreferredSize(new Dimension(60, 22));
        algoDeltaThresholdSpinner.setValue(algoDeltaThreshold);
        algoDeltaThresholdSpinner.addChangeListener(e -> {
            int value = (int) algoDeltaThresholdSpinner.getValue();
            algoDeltaThreshold = (double) value;
        });
        algoDeltaThresholdSpinner.setEnabled(algoPreference == AlgoPreference.Threshold_Dark || algoPreference == AlgoPreference.Threshold_Light);

        algoPreferenceSelector.addActionListener(e -> {
            String selected = (String) algoPreferenceSelector.getSelectedItem();
            if (selected != null) {
                 if(selected.equals("Thresh_L")) selected = "Threshold_Light";
                 if(selected.equals("Thresh_D")) selected = "Threshold_Dark";
                 try {
                     algoPreference = AlgoPreference.valueOf(selected);
                 } catch(IllegalArgumentException ex) {
                     if(selected.equals("Light")) algoPreference = AlgoPreference.Light;
                     if(selected.equals("Dark")) algoPreference = AlgoPreference.Dark;
                 }
                 
                if (algoPreference == AlgoPreference.Light || algoPreference == AlgoPreference.Threshold_Light) {
                    usePreference = true; preferLightForeground = true;
                } else if (algoPreference == AlgoPreference.Dark || algoPreference == AlgoPreference.Threshold_Dark) {
                    usePreference = true; preferLightForeground = false;
                } else {
                    usePreference = false;
                }
                algoDeltaThresholdSpinner.setEnabled(algoPreference == AlgoPreference.Threshold_Dark ||
                        algoPreference == AlgoPreference.Threshold_Light);
            }
        });
        
        algoPanel.add(algoPreferenceSelector);
        algoPanel.add(new JLabel("Th:"));
        algoPanel.add(algoDeltaThresholdSpinner);

        gbc.gridy = 2;
        toolbarPanel.add(algoPanel, gbc);

        mainPanel.add(toolbarPanel, BorderLayout.NORTH);

        // --- 2. Inspector & Tools Section ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        
        // Info Panel (Left)
        JPanel infoWrapper = new JPanel(new GridBagLayout());
        infoWrapper.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Inspector", 0, 0, new Font("SansSerif", Font.PLAIN, 11)));
        GridBagConstraints igbc = new GridBagConstraints();
        igbc.insets = new Insets(1, 3, 1, 3);
        igbc.anchor = GridBagConstraints.WEST;
        
        // Headers
        addLabel(infoWrapper, "Click", 1, 0, igbc);
        addLabel(infoWrapper, "Hover", 2, 0, igbc);

        // Data Rows
        clickedPosLabel = new JLabel("-,-"); hoverPosLabel = new JLabel("-,-");
        clickedPosLabel.setPreferredSize(new Dimension(80, 15));
        hoverPosLabel.setPreferredSize(new Dimension(80, 15));
        addInfoRow(infoWrapper, "Pos", clickedPosLabel, hoverPosLabel, 1);
        
        clickedCodepointLabel = new JLabel("-"); hoverCodepointLabel = new JLabel("-");
        addInfoRow(infoWrapper, "CP", clickedCodepointLabel, hoverCodepointLabel, 2);
        
        clickedFgIndexLabel = new JLabel("-"); hoverFgIndexLabel = new JLabel("-");
        addInfoRow(infoWrapper, "FG", clickedFgIndexLabel, hoverFgIndexLabel, 3);
        
        clickedBgIndexLabel = new JLabel("-"); hoverBgIndexLabel = new JLabel("-");
        addInfoRow(infoWrapper, "BG", clickedBgIndexLabel, hoverBgIndexLabel, 4);
        
        clickedAlphaLabel = new JLabel("-"); hoverAlphaLabel = new JLabel("-");
        addInfoRow(infoWrapper, "Alp", clickedAlphaLabel, hoverAlphaLabel, 5);

        // Area info
        selectionXPosLabel = new JLabel("-,-");
        selectionWidthLabel = new JLabel("-x-");
        igbc.gridx = 0; igbc.gridy = 6; igbc.gridwidth = 4;
        igbc.insets = new Insets(5, 2, 1, 2);
        infoWrapper.add(new JLabel("Sel: "), igbc);
        
        // Small sub-panel for selection stats
        JPanel selStats = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        selStats.add(selectionXPosLabel);
        selStats.add(selectionWidthLabel);
        igbc.gridy = 7;
        infoWrapper.add(selStats, igbc);

        // Glyph Preview
        glyphPreviewPanel = new GlyphPreviewPanel();
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.gridx = 4; pgbc.gridy = 0;
        pgbc.gridheight = 8;
        pgbc.insets = new Insets(2, 10, 2, 2);
        pgbc.anchor = GridBagConstraints.NORTHWEST;
        infoWrapper.add(glyphPreviewPanel, pgbc);
        
        // Push everything to top-left
        GridBagConstraints spacerGbc = new GridBagConstraints();
        spacerGbc.gridx = 5; spacerGbc.gridy = 10;
        spacerGbc.weightx = 1.0; spacerGbc.weighty = 1.0;
        infoWrapper.add(new JLabel(""), spacerGbc);

        centerPanel.add(infoWrapper, BorderLayout.CENTER);

        // Tools / Actions Panel
        JPanel toolsPanel = new JPanel(new GridLayout(3, 3, 2, 2));
        toolsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Actions", 0, 0, new Font("SansSerif", Font.PLAIN, 11)));
        
        copyGlyphButton = createSmallButton("Copy Glyph", "Copy Glyph (Int) + Char (Ext)");
        pasteCharExtButton = createSmallButton("Paste Char", "Paste Char (Ext)");
        pasteGlyphIntButton = createSmallButton("Paste Glyph", "Paste Glyph (Int)");
        
        copyColorsButton = createSmallButton("Copy Colors", "Copy Colors");
        pasteColorsButton = createSmallButton("Paste Colors", "Paste Colors");
        flipColorsButton = createSmallButton("Flip Colors", "Flip Colors");
        
        flipGlyphButton = createSmallButton("Flip Glyph", "Invert Glyph Pattern");
        transparentBgButton = createSmallButton("Alpha BG", "Set Transparent Background");
        
        JButton dummy = new JButton("-"); dummy.setEnabled(false);

        toolsPanel.add(copyGlyphButton);
        toolsPanel.add(pasteCharExtButton);
        toolsPanel.add(pasteGlyphIntButton);
        toolsPanel.add(copyColorsButton);
        toolsPanel.add(pasteColorsButton);
        toolsPanel.add(flipColorsButton);
        toolsPanel.add(flipGlyphButton);
        toolsPanel.add(transparentBgButton);
        toolsPanel.add(dummy);

        centerPanel.add(toolsPanel, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // --- Log ---
        logArea = new JTextArea(4, 30);
        logArea.setEditable(false);

        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.LIGHT_GRAY);
        mainPanel.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }
    
    private void addLabel(JPanel p, String text, int x, int y, GridBagConstraints gbc) {
        gbc.gridx = x; gbc.gridy = y;
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        p.add(l, gbc);
    }
    
    private void addInfoRow(JPanel p, String label, JLabel clickL, JLabel hovrL, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 3, 0, 3);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = y;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        p.add(lbl, gbc);
        
        gbc.gridx = 1; 
        clickL.setFont(new Font("Monospaced", Font.PLAIN, 11));
        p.add(clickL, gbc);
        
        gbc.gridx = 2; 
        hovrL.setFont(new Font("Monospaced", Font.PLAIN, 11));
        p.add(hovrL, gbc);
    }

    private JButton createButton(String text, String tooltip) {
        JButton b = new JButton(text);
        b.setToolTipText(tooltip);
        b.setMargin(new Insets(2, 2, 2, 2));
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        b.addActionListener(this);
        return b;
    }
    
    private JButton createSmallButton(String text, String tooltip) {
        JButton b = new JButton(text);
        b.setToolTipText(tooltip);
        b.setMargin(new Insets(1, 1, 1, 1));
        b.setFont(new Font("SansSerif", Font.PLAIN, 10));
        b.addActionListener(this);
        b.setEnabled(false);
        return b;
    }

    public void updateForBatchSelection(String name) {
        setTitle("Controls - " + name);
    }

    public void setState(PanelState state) {
        this.state = state;
        boolean editEnabled = (state == PanelState.EDIT);
        // openButton.setEnabled(true);
        toggleViewButton.setEnabled(editEnabled);
        // exportButton.setEnabled(editEnabled);
        restartButton.setEnabled(editEnabled);
        scaleSelector.setEnabled(editEnabled);
        undoButton.setEnabled(editEnabled);
        redoButton.setEnabled(editEnabled);
        centerImageButton.setEnabled(editEnabled);

        if (!editEnabled) {
            updateSelectionInfo(-1, -1, null);
            updateClickedInfo(-1, -1, null, null, null);
            disableEditButtons();
        }
    }
    
    private void disableEditButtons() {
        copyGlyphButton.setEnabled(false);
        pasteCharExtButton.setEnabled(false);
        pasteGlyphIntButton.setEnabled(false);
        copyColorsButton.setEnabled(false);
        pasteColorsButton.setEnabled(false);
        flipColorsButton.setEnabled(false);
        flipGlyphButton.setEnabled(false);
        transparentBgButton.setEnabled(false);
    }

    public PanelState getPanelState() {
        return state;
    }

    private boolean useSelectionForActions() {
        return p.hasSelection &&
                p.selectionStartX >= 0 && p.selectionStartY >= 0 &&
                p.selectionEndX >= 0 && p.selectionEndY >= 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Object source = e.getSource();
            if (source == toggleViewButton) p.keyPressed('s');
            // else if (source == exportButton) p.keyPressed('p');
            else if (source == restartButton) p.keyPressed('r');
            else if (source == scaleSelector) p.keyPressed((char) ('0' + scaleSelector.getSelectedIndex() + 1));
            // else if (source == openButton) {
            //     if (batchManager.isVisible() || !batchManager.getProject().getImgs().isEmpty()) {
            //           int result = javax.swing.JOptionPane.showOptionDialog(this, 
            //               "Batch active. Add to batch or discard?", 
            //               "Load Image", 
            //               javax.swing.JOptionPane.YES_NO_CANCEL_OPTION, 
            //               javax.swing.JOptionPane.QUESTION_MESSAGE, 
            //               null, 
            //               new String[]{"Add to Batch", "Discard Batch", "Cancel"}, 
            //               "Add to Batch");
                      
            //           if (result == 0) { 
            //                batchManager.addImagesTrigger();
            //           } else if (result == 1) { 
            //                int saveResult = javax.swing.JOptionPane.showConfirmDialog(this, "Save project before discarding?", "Save Project", javax.swing.JOptionPane.YES_NO_OPTION);
            //                if (saveResult == javax.swing.JOptionPane.YES_OPTION) {
            //                    batchManager.saveProject();
            //                }
            //                batchManager.setProject(new core.Project());
            //                batchManager.setVisible(false);
            //                p.keyPressed('l');
            //           }
            //      } else {
            //          p.keyPressed('l');
            //      }
            // }
            // else if (source == batchButton) {
            //      if (batchManager.isVisible() || !batchManager.getProject().getImgs().isEmpty()) {
            //           int result = javax.swing.JOptionPane.showOptionDialog(this, 
            //               "Batch is currently active. Add new images or discard current batch?", 
            //               "New Batch", 
            //               javax.swing.JOptionPane.YES_NO_CANCEL_OPTION, 
            //               javax.swing.JOptionPane.QUESTION_MESSAGE, 
            //               null, 
            //               new String[]{"Add to Batch", "Discard & New", "Cancel"}, 
            //               "Add to Batch");
            //            if(result == 0) {
            //                batchManager.setVisible(true);
            //                batchManager.addImagesTrigger();
            //            } else if (result == 1) {
            //                 int saveResult = javax.swing.JOptionPane.showConfirmDialog(this, "Save project before discarding?", "Save Project", javax.swing.JOptionPane.YES_NO_OPTION);
            //                 if (saveResult == javax.swing.JOptionPane.YES_OPTION) {
            //                     batchManager.saveProject();
            //                 }
            //                 batchManager.setProject(new core.Project());
            //                 batchManager.setVisible(true);
            //                 batchManager.addImagesTrigger();
            //            }
            //      } else {
            //          batchManager.setVisible(true);
            //          batchManager.addImagesTrigger();
            //      }
            // } else if (source == saveProjectButton) {
            //      if (batchManager.isVisible() || !batchManager.getProject().getImgs().isEmpty()) {
            //          batchManager.saveProject();
            //      } else {
            //          java.awt.FileDialog fd = new java.awt.FileDialog(this, "Save Project", java.awt.FileDialog.SAVE);
            //          fd.setFile("*.uscP");
            //          fd.setVisible(true);
                     
            //          if (fd.getFile() != null) {
            //              java.io.File file = new java.io.File(fd.getDirectory(), fd.getFile());
            //              if (!file.getName().endsWith(".uscP")) {
            //                  file = new java.io.File(file.getAbsolutePath() + ".uscP");
            //              }
            //              String path = p.getImagePath();
            //              if (path != null) {
            //                  core.Project tempProj = new core.Project();
            //                  core.ImageModel model = new core.ImageModel(path);
            //                  if(p.resultGrid != null) model.setGridData(p.resultGrid);
            //                  model.addPreference("scale", String.valueOf(usePreference));
            //                  tempProj.addImage(new java.io.File(path).getName(), model);
            //                  core.SaveFileManager.saveProject(tempProj, file);
            //              } else {
            //                  logger.Logger.println("No image to save.");
            //              }
            //          }
            //      }
            // }
            else if (source == undoButton) p.undoAction();
            else if (source == redoButton) p.redoAction();
            else if (source == centerImageButton) p.centerImage();
            else if (source == copyGlyphButton) copyInternalGlyphAndExternalChar();
            else if (source == pasteCharExtButton) pasteCharacterFromClipboard();
            else if (source == pasteGlyphIntButton) pasteInternalGlyph();
            else if (source == copyColorsButton) copyInternalColors();
            else if (source == pasteColorsButton) pasteInternalColors();
            else if (source == flipColorsButton) {
                if (useSelectionForActions()) p.flipSelectedGlyphColors();
                else p.flipClickedGlyphColors();
            } else if (source == flipGlyphButton) {
                if (useSelectionForActions()) p.invertGlyphPatternForSelection();
                else p.invertGlyphPattern();
            } else if (source == transparentBgButton) {
                if (useSelectionForActions()) p.setTransparentBackgroundForSelection();
                else p.setTransparentBackground();
            }
        } catch (Exception ex) {
            Logger.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void copyCharacterToClipboard() {
        if (currentClickedGlyph != null) {
            try {
                String character = new String(Character.toChars(currentClickedGlyph.codePoint));
                StringSelection stringSelection = new StringSelection(character);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                Logger.println("Copied char '" + character + "'");
            } catch (Exception ex) {
                Logger.println("Error copying: " + ex.getMessage());
            }
        }
    }

    public void copyInternalGlyphAndExternalChar() {
        if (currentClickedGlyph != null) {
            internalClipboardGlyph = new ResultGlyph(
                    currentClickedGlyph.codePoint,
                    currentClickedGlyph.fgIndex,
                    currentClickedGlyph.bgIndex);
            
            internalClipboardFgIndex = currentClickedGlyph.fgIndex;
            internalClipboardBgIndex = currentClickedGlyph.bgIndex;
            Logger.println("Copied glyph internally");
            copyCharacterToClipboard();
            updatePasteButtonStates();
        }
    }

    public void copyInternalColors() {
        if (currentClickedGlyph != null) {
            internalClipboardFgIndex = currentClickedGlyph.fgIndex;
            internalClipboardBgIndex = currentClickedGlyph.bgIndex;
            Logger.println("Copied colors: FG=" + internalClipboardFgIndex + ", BG=" + internalClipboardBgIndex);
            updatePasteButtonStates();
        }
    }

    public void pasteCharacterFromClipboard() {
        if (currentClickedGlyph == null) return;

        // if (internalClipboardGlyph != null) {
        //     pasteInternalGlyph();
        //     return;
        // }

        // if (internalClipboardFgIndex != -1 && internalClipboardBgIndex != -1) {
        //     Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //     Transferable contents = clipboard.getContents(null);
        //     if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        //         try {
        //             String pastedText = (String) contents.getTransferData(DataFlavor.stringFlavor);
        //             if (pastedText != null && !pastedText.isEmpty()) {
        //                 char charToPaste = pastedText.charAt(0);
        //                 ResultGlyph combinedGlyph = new ResultGlyph(
        //                         (int) charToPaste,
        //                         internalClipboardFgIndex,
        //                         internalClipboardBgIndex);
        //                 p.replaceClickedGlyphWithGlyph(combinedGlyph);
        //                 return;
        //             }
        //         } catch (Exception ex) { Logger.println("Clipboard Error: " + ex.getMessage()); }
        //     }
        // }

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String pastedText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                if (pastedText != null && !pastedText.isEmpty()) {
                    p.replaceClickedGlyph(pastedText.charAt(0));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    public void pasteInternalGlyph() {
        if (currentClickedGlyph == null) return;
        if (internalClipboardGlyph != null) {
            p.replaceClickedGlyphWithGlyph(internalClipboardGlyph);
        }
    }

    public void pasteInternalColors() {
        if (currentClickedGlyph == null) return;
        if (internalClipboardFgIndex != -1 && internalClipboardBgIndex != -1) {
            p.replaceClickedGlyphColors(internalClipboardFgIndex, internalClipboardBgIndex);
        }
    }

    public void updateSelectionAreaInfo(boolean hasSelection, int startX, int startY, int endX, int endY) {
        if (hasSelection && startX >= 0 && startY >= 0 && endX >= 0 && endY >= 0) {
            int minX = Math.min(startX, endX);
            int minY = Math.min(startY, endY);
            int maxX = Math.max(startX, endX);
            int maxY = Math.max(startY, endY);
            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            selectionXPosLabel.setText(minX + "," + minY);
            selectionWidthLabel.setText(width + "x" + height);
        } else {
            selectionXPosLabel.setText("-,-");
            selectionWidthLabel.setText("-x-");
        }
    }

    private void updatePasteButtonStates() {
        boolean glyphSelected = currentClickedGlyph != null || useSelectionForActions();
        boolean internalGlyphAvailable = internalClipboardGlyph != null;
        boolean internalColorsAvailable = internalClipboardFgIndex != -1 && internalClipboardBgIndex != -1;

        pasteGlyphIntButton.setEnabled(glyphSelected && internalGlyphAvailable);
        pasteColorsButton.setEnabled(glyphSelected && internalColorsAvailable);
        pasteCharExtButton.setEnabled(glyphSelected);
        flipColorsButton.setEnabled(glyphSelected);
        flipGlyphButton.setEnabled(glyphSelected);
        transparentBgButton.setEnabled(glyphSelected);
    }

    public void updateClickedInfo(int x, int y, ResultGlyph glyph, int[] colorPalette, Map<Integer, Long> asciiPatterns) {
        this.currentClickedGlyph = glyph;
        this.currentAsciiPatterns = asciiPatterns;
        boolean glyphSelected = (x >= 0 && y >= 0 && glyph != null);

        if (glyphSelected) {
            clickedPosLabel.setText(x + "," + y);
            clickedCodepointLabel.setText(String.format("U+%04X", glyph.codePoint));
            clickedFgIndexLabel.setText(String.valueOf(glyph.fgIndex));
            clickedBgIndexLabel.setText(String.valueOf(glyph.bgIndex));
            if (colorPalette != null && glyph.bgIndex >= 0 && glyph.bgIndex < colorPalette.length) {
                int alpha = (colorPalette[glyph.bgIndex] >> 24) & 0xFF;
                clickedAlphaLabel.setText(String.valueOf(alpha));
            } else {
                clickedAlphaLabel.setText("-");
            }
        } else {
            clickedPosLabel.setText("-,-");
            clickedCodepointLabel.setText("-"); clickedFgIndexLabel.setText("-");
            clickedBgIndexLabel.setText("-"); clickedAlphaLabel.setText("-");
        }

        copyGlyphButton.setEnabled(glyphSelected);
        copyColorsButton.setEnabled(glyphSelected);
        flipColorsButton.setEnabled(glyphSelected);
        flipGlyphButton.setEnabled(glyphSelected);
        transparentBgButton.setEnabled(glyphSelected);
        updatePasteButtonStates();
        if (glyphPreviewPanel != null) glyphPreviewPanel.repaint();
    }

    public void updateSelectionInfo(int x, int y, ResultGlyph glyph) {
        if (x >= 0 && y >= 0 && glyph != null) {
            hoverPosLabel.setText(x + "," + y);
            hoverCodepointLabel.setText(String.format("%04X", glyph.codePoint));
            hoverFgIndexLabel.setText(String.valueOf(glyph.fgIndex));
            hoverBgIndexLabel.setText(String.valueOf(glyph.bgIndex));
        } else {
            hoverPosLabel.setText("-,-");
            hoverCodepointLabel.setText("-"); hoverFgIndexLabel.setText("-");
            hoverBgIndexLabel.setText("-");
        }
    }

    public void updateScaleSelector(int scale) {
        if (scale >= 1 && scale <= 8 && scaleSelector.getSelectedIndex() != (scale - 1)) {
            ActionListener[] listeners = scaleSelector.getActionListeners();
            for (ActionListener l : listeners) scaleSelector.removeActionListener(l);
            scaleSelector.setSelectedIndex(scale - 1);
            for (ActionListener l : listeners) scaleSelector.addActionListener(l);
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
        if (logArea == null) return;
        logArea.append(message + "\n");
    }

    public static ControlPanel get() { return Instance; }
    public static void log(String message) {
        if (Instance != null) Instance.appendLog(message);
        else System.out.println("Log: " + message);
    }
}