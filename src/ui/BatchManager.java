package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.awt.FileDialog;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridLayout;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.ListSelectionModel;

import core.Project;
import core.RenameCommand;
import core.CommandManager;
import core.ProcessingCore;
import core.ResultGlyph;
import core.data.UscExportManager;
import core.data.ProjectFileManager;
import core.data.ProjectFileManager;
import core.ImageModel;

public class BatchManager extends JFrame implements ActionListener {

    private ControlPanel controlPanel;
    private ProcessingCore core;

    private JButton addImagesButton;
    private JButton removeImageButton;

    private JButton sortImagesButton;

    private JButton exportAllButton;
    private JButton exportButton;

    private JButton calcallButton;
    private JButton renameButton;
    private JButton saveProjectButton;
    private JButton openProjectButton;
    private JButton newProjectButton;

    private JList<String> imageList;
    private DefaultListModel<String> listModel;

    private String currentSelectedImageName = null;

    private Project project;

    public BatchManager(ControlPanel cp, ProcessingCore core) {
        this.controlPanel = cp;
        this.core = core;
        this.project = core.project;
        setTitle("Batch Manager");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        initUI();
    }

    private void initUI() {
        listModel = new DefaultListModel<>();
        imageList = new JList<>(listModel);
        imageList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        imageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = imageList.getSelectedValue();
                if (selected != null) {
                    onImageSelected(selected);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(imageList);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new java.awt.GridLayout(0, 2));

        renameButton = new JButton("Rename Image");
        renameButton.addActionListener(this);

        removeImageButton = new JButton("Remove Image");
        removeImageButton.addActionListener(this);

        sortImagesButton = new JButton("Sort Images");
        sortImagesButton.addActionListener(this);

        calcallButton = new JButton("Calc All");
        calcallButton.addActionListener(this);

        addImagesButton = new JButton("Add Images");
        addImagesButton.addActionListener(this);
        exportButton = new JButton("Export");
        exportButton.addActionListener(this);
        exportAllButton = new JButton("Export All");
        exportAllButton.addActionListener(this);
        saveProjectButton = new JButton("Save Project");
        saveProjectButton.addActionListener(this);
        openProjectButton = new JButton("Open Project");
        openProjectButton.addActionListener(this);

        newProjectButton = new JButton("New Project");
        newProjectButton.addActionListener(this);

        buttonPanel.add(addImagesButton);
        buttonPanel.add(removeImageButton);

        buttonPanel.add(renameButton);
        buttonPanel.add(calcallButton);

        buttonPanel.add(sortImagesButton);
        buttonPanel.add(new JLabel()); // spacer

        buttonPanel.add(exportButton);
        buttonPanel.add(exportAllButton);

        buttonPanel.add(new JLabel()); // spacer
        buttonPanel.add(new JLabel()); // spacer

        buttonPanel.add(openProjectButton);
        buttonPanel.add(saveProjectButton);

        buttonPanel.add(newProjectButton);
        

        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Public method to be called from ControlPanel or externally
    public void addImagesTrigger() {
        FileDialog fd = new FileDialog(this, "Select Images", FileDialog.LOAD);
        fd.setMultipleMode(true);
        fd.setFilenameFilter((dir, name) -> {
            String n = name.toLowerCase();
            return n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".jpeg") || n.endsWith(".bmp");
        });
        fd.setVisible(true);

        File[] files = fd.getFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                addImage(file);
            }
        }
        if (files.length > 0) {
            controlPanel.setState(ControlPanel.PanelState.EDIT);
        }
    }

    public void addImage(File file) {
        String name = file.getName();
        // Ensure unique name
        String key = name;
        int i = 1;
        while (project.getImgs().containsKey(key)) {
            key = name + " (" + i + ")";
            i++;
        }

        ImageModel model = new ImageModel(file.getAbsolutePath());
        // Initial setup for the model could go here ?

        project.addImage(key, model);
        listModel.addElement(key);
    }

    private void onImageSelected(String key) {
        if (currentSelectedImageName != null && currentSelectedImageName.equals(key))
            return;

        // Save state of the PREVIOUS image model before switching
        if (currentSelectedImageName != null) {
            updateCurrentModelFromCore();
        }

        currentSelectedImageName = key;
        ImageModel currentModel = project.getImage(currentSelectedImageName);
        core.imagePath = currentModel.getFilepath();

        ImageModel model = project.getImage(key);
        if (model != null) {
            // Load this image into ProcessingCore logic
            if (core != null) {
                ResultGlyph[][] updatedGrid = core.loadAndProcessImage(model.getFilepath(), model.getGridData(), false);
                model.setGridData(updatedGrid);
                controlPanel.updateForBatchSelection(key);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addImagesButton) {
            addImagesTrigger();
        } else if (e.getSource() == saveProjectButton) {
            saveProject();
        } else if (e.getSource() == exportAllButton) {
            exportAll();
        }else if (e.getSource() == exportButton) {
            exportCurrent();
        } else if (e.getSource() == removeImageButton) {
            removeImage();
        } else if (e.getSource() == calcallButton) {
            calcAll();
        } else if (e.getSource() == renameButton) {
            rename();
        } else if (e.getSource() == openProjectButton) {
            open();
        } else if (newProjectButton == e.getSource()) {
            newProject();
        }
        else if( e.getSource() == sortImagesButton){
         sortImages();
        }
        
    }

    void open(){
        if (imageList.getModel().getSize() > 0) {
            int result = javax.swing.JOptionPane.showOptionDialog(this,
                    "Batch active. Add to batch or discard?",
                    "Load Image",
                    javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[] { "Discard Batch", "Cancel" },
                    "Cancel");
            if (result == 1) {
                return;
            } else if (result == 0) {
                int saveResult = javax.swing.JOptionPane.showConfirmDialog(this, "Save project before discarding?",
                        "Save Project", javax.swing.JOptionPane.YES_NO_OPTION);
                if (saveResult == javax.swing.JOptionPane.YES_OPTION) {
                    saveProject();
                }
                core.loadFile();
            }
        } else {
            core.loadFile();
        }
        setProject(core.project);
    }
    
    void newProject(){
        boolean hasUnsaved = imageList.getModel().getSize() > 0 || CommandManager.actionPerformed;
        if (hasUnsaved) {
            String[] options = { "Save Current Project", "Discard Current Project", "Cancel" };
            int result = javax.swing.JOptionPane.showOptionDialog(this,
                    "Create new project. Save current project or discard?",
                    "New Project",
                    javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[2]);
            if (result == 2) { // Cancel
                return;
            } else if (result == 0) { // Save Current Project
                saveProject();
            }
        }

        project = new Project();
        core.project = project;
        listModel.clear();
        currentSelectedImageName = null;
        controlPanel.setState(ControlPanel.PanelState.SETUP);
    }
    
    void rename() {
        int[] selectedIndices = imageList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "No image selected to rename.");
            return;
        }

        String oldName = listModel.getElementAt(selectedIndices[0]);
        if (selectedIndices.length == 1) {
            String newName = javax.swing.JOptionPane.showInputDialog(this, "Enter new name:", oldName);
            if (newName != null && !newName.trim().isEmpty() && !project.getImgs().containsKey(newName)) {
                int index = selectedIndices[0];
                RenameCommand renameCmd = new RenameCommand(this, index, oldName, newName);
                core.commandManager.executeCommand(renameCmd);
            }
        } else {
            batchRenameForm();
        }
    }

    void replaceStringInName(int index, String target, String replacement) {
        String oldName = listModel.getElementAt(index);
        String newName = oldName.replace(target, replacement);
        if (!newName.equals(oldName) && !project.getImgs().containsKey(newName)) {
            RenameCommand renameCmd = new RenameCommand(this, index, oldName, newName);
            core.commandManager.executeCommand(renameCmd);
        }
    }

    public void batchRenameForm() {
        JDialog dialog = new JDialog(this, "Batch Rename", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 450);

        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        inputPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] options = { "Replace String", "Add Beginning", "Add End", "String_Index", "Shift Number" };
        JComboBox<String> strategyBox = new JComboBox<>(options);

        JTextField targetField = new JTextField();
        JLabel targetLabel = new JLabel("Target String:");

        JTextField newStringField = new JTextField();
        JLabel newStringLabel = new JLabel("New String / Base Name:");

        JSpinner changeIndex = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
        JLabel indexLabel = new JLabel("Index Change:");

        inputPanel.add(new JLabel("Strategy:"));
        inputPanel.add(strategyBox);
        inputPanel.add(targetLabel);
        inputPanel.add(targetField);
        inputPanel.add(newStringLabel);
        inputPanel.add(newStringField);
        inputPanel.add(indexLabel);
        inputPanel.add(changeIndex);

        JTextArea previewArea = new JTextArea();
        previewArea.setEditable(false);
        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));

        JPanel buttonPanel = new JPanel();
        JButton applyButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.NORTH);
        dialog.add(previewScroll, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        java.util.function.BiFunction<String, Integer, String> shiftNumber = (name, delta) -> {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)(?!.*\\d)").matcher(name);
            if (m.find()) {
                String numStr = m.group(1);
                try {
                    long val = Long.parseLong(numStr);
                    long updated = val + delta;
                    if (updated < 0)
                        return name; // Impossible per requirement

                    String format = "%0" + numStr.length() + "d";
                    String newNode = String.format(format, updated);

                    return new StringBuilder(name).replace(m.start(), m.end(), newNode).toString();
                } catch (NumberFormatException ex) {
                    return name;
                }
            }
            return name;
        };

        Runnable updatePreview = () -> {
            StringBuilder sb = new StringBuilder();
            String strategy = (String) strategyBox.getSelectedItem();
            String target = targetField.getText();
            String replacement = newStringField.getText();
            int delta = (Integer) changeIndex.getValue();

            int[] indices = imageList.getSelectedIndices();
            if (indices.length == 0) {
                sb.append("No images selected.");
            } else {
                for (int i = 0; i < indices.length; i++) {
                    String oldName = listModel.getElementAt(indices[i]);
                    String newName = oldName;

                    if ("Replace String".equals(strategy)) {
                        if (target != null && !target.isEmpty()) {
                            newName = oldName.replace(target, replacement);
                        }
                    } else if ("Add Beginning".equals(strategy)) {
                        newName = replacement + oldName;
                    } else if ("Add End".equals(strategy)) {
                        newName = oldName + replacement;
                    } else if ("String_Index".equals(strategy)) {
                        newName = replacement + "_" + i;
                    }

                    // Apply index change if applicable
                    if (delta != 0) {
                        newName = shiftNumber.apply(newName, delta);
                    }

                    sb.append(oldName).append(" -> ").append(newName).append("\n");
                }
            }
            previewArea.setText(sb.toString());
        };

        strategyBox.addActionListener(e -> {
            String strategy = (String) strategyBox.getSelectedItem();
            boolean isReplace = "Replace String".equals(strategy);
            targetField.setEnabled(isReplace);
            targetLabel.setEnabled(isReplace);

            // "Shift Number" implies no other text changes, just shifting.
            // But we allow shifting on ALL strategies now.
            // If "Shift Number" is selected, hide the text inputs to avoid confusion?
            // Or just treat "Shift Number" as "Keep Name + Shift".
            boolean isShiftOnly = "Shift Number".equals(strategy);

            boolean isTextStrategy = !isShiftOnly;
            newStringField.setEnabled(isTextStrategy);
            newStringLabel.setEnabled(isTextStrategy);

            // Allow index change on all options
            changeIndex.setEnabled(true);
            indexLabel.setEnabled(true);

            updatePreview.run();
        });

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updatePreview.run();
            }

            public void removeUpdate(DocumentEvent e) {
                updatePreview.run();
            }

            public void changedUpdate(DocumentEvent e) {
                updatePreview.run();
            }
        };
        targetField.getDocument().addDocumentListener(dl);
        newStringField.getDocument().addDocumentListener(dl);
        changeIndex.addChangeListener(e -> updatePreview.run());

        cancelButton.addActionListener(e -> dialog.dispose());

        applyButton.addActionListener(e -> {
            String strategy = (String) strategyBox.getSelectedItem();
            String target = targetField.getText();
            String replacement = newStringField.getText();
            int delta = (Integer) changeIndex.getValue();

            int[] indices = imageList.getSelectedIndices();

            for (int i = 0; i < indices.length; i++) {
                int idx = indices[i];
                String oldName = listModel.getElementAt(idx);
                String newName = oldName;

                if ("Replace String".equals(strategy)) {
                    if (target != null && !target.isEmpty()) {
                        newName = oldName.replace(target, replacement);
                    }
                } else if ("Add Beginning".equals(strategy)) {
                    newName = replacement + oldName;
                } else if ("Add End".equals(strategy)) {
                    newName = oldName + replacement;
                } else if ("String_Index".equals(strategy)) {
                    newName = replacement + "_" + i;
                }

                // Apply index change if applicable
                if (delta != 0) {
                    newName = shiftNumber.apply(newName, delta);
                }

                if (!newName.equals(oldName) && !project.getImgs().containsKey(newName)) {
                    RenameCommand renameCmd = new RenameCommand(this, idx, oldName, newName);
                    core.commandManager.executeCommand(renameCmd);
                }
            }
            dialog.dispose();
        });

        // Initial state
        boolean isReplace = "Replace String".equals(strategyBox.getSelectedItem());
        targetField.setEnabled(isReplace);
        targetLabel.setEnabled(isReplace);
        changeIndex.setEnabled(true);
        indexLabel.setEnabled(true);
        updatePreview.run();

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void renameImage(int index, String newName) {
        String currentName = listModel.getElementAt(index);
        // check for doubles
        if (project.getImgs().containsKey(newName)) {
            javax.swing.JOptionPane.showMessageDialog(this, "An image with the name '" + newName + "' already exists.");
            return;
        }

        ImageModel model = project.getImage(currentName);
        model.setName(newName);
        listModel.setElementAt(newName, index);
        project.getImgs().remove(currentName);
        project.addImage(newName, model);

        // ImageModel model = project.getImgs().remove(currentName);
        // project.addImage(newName, model);
        // listModel.setElementAt(newName, index);
        // model.setName(newName);
        // currentSelectedImageName = newName;
    }

    public void saveProject() {
        FileDialog fd = new FileDialog(this, "Save Project", FileDialog.SAVE);
        fd.setFile("*.uscP");
        fd.setVisible(true);

        if (fd.getFile() != null) {
            File file = new File(fd.getDirectory(), fd.getFile());
            if (!file.getName().endsWith(".uscP")) {
                file = new File(file.getAbsolutePath() + ".uscP");
            }
            // Before saving, ensure current changes in Core are captured into the current
            // model
            updateCurrentModelFromCore();


            // Reconstruct the map using LinkedHashMap to match listModel order
            java.util.Map<String, ImageModel> orderedImgs = new java.util.LinkedHashMap<>();
            for (int i = 0; i < listModel.size(); i++) {
                String key = listModel.getElementAt(i);
                ImageModel model = project.getImage(key);
                if (model != null) {
                    orderedImgs.put(key, model);
                }
            }
            project.setImgs(orderedImgs);

            ProjectFileManager.saveProject(project, file);
            CommandManager.actionPerformed = false;
        }
    }

    private void calcAll() {
        boolean forceReload = javax.swing.JOptionPane.showConfirmDialog(this,
                "Force reload of all images from disk? This may be slower but ensures all images are up to date.",
                "Force Reload", javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION;
        for (String key : project.getImgs().keySet()) {
            ImageModel model = project.getImage(key);
            if (model != null) {
                // Update model with new grid data directly from the return value
                model.setGridData(core.loadAndProcessImage(model.getFilepath(), model.getGridData(), forceReload));
            }
        }
    }

    private void exportCurrent() {
        if (core.hasSelection) {
            String[] options = { "Export Selection", "Export Full", "Cancel" };

            int saveResult = javax.swing.JOptionPane.showOptionDialog(this,
                    "Batch Export only Selection?",
                    "Export Batch Selection",
                    javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (saveResult == 1) { // Export Full (index 1)
                core.hasSelection = false;
            } else if (saveResult != 0) { // Not Export Selection (index 0)
                return;
            }
        }

        // String path = FileHandler.getSaveFolder("Select Export Folder");
        String path = UscExportManager.getFile("Export Current Image", FileDialog.SAVE, new String[]{"usc2"}).getAbsolutePath();
        if (path == null)
            return;
        
        UscExportManager.exportCurrent(path, core);
    }

    private void exportAll() {
        // TODO: get filepath
        if (core.hasSelection) {
            String[] options = { "Export Selection", "Export Full", "Cancel" };

            int saveResult = javax.swing.JOptionPane.showOptionDialog(this,
                    "Batch Export only Selection?",
                    "Export Batch Selection",
                    javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (saveResult == 1) { // Export Full (index 1)
                core.hasSelection = false;
            } else if (saveResult != 0) { // Not Export Selection (index 0)
                return;
            }
        }
        String path = UscExportManager.getSaveFolder("Select Export Folder");
        if (path == null)
            return;
        for (String key : project.getImgs().keySet()) {
            ImageModel model = project.getImage(key);
            if (model != null) {
                if (model.getGridData() == null) {
                    // Load and process if grid data is missing, updating the model
                    model.setGridData(core.loadAndProcessImage(model.getFilepath(), null, false));
                }
                String outPath = new File(path, key + ".usc2").getAbsolutePath();
                UscExportManager.exportImageModel(model, outPath, core);
            }

        }
    }

    private void removeImage(){
        int[] selectedIndices = imageList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "No image selected to remove.");
            return;
        }

        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            String name = listModel.getElementAt(selectedIndices[i]);
            project.getImgs().remove(name);
            listModel.removeElementAt(selectedIndices[i]);
        }

        // Clear current selection if it was removed
        if (currentSelectedImageName != null && !project.getImgs().containsKey(currentSelectedImageName)) {
            currentSelectedImageName = null;
            controlPanel.setState(ControlPanel.PanelState.SETUP);
        }
    }

    private void sortImages(){

        String[] options = { "Sort A-Z", "Sort Z-A", "Cancel" };
        int result = javax.swing.JOptionPane.showOptionDialog(this,
                "Sort images in batch manager?",
                "Sort Images",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);
        if (result == 2) { // Cancel
            return;
        }
        if(result == 0){
            // A-Z
            java.util.List<String> names = java.util.Collections.list(listModel.elements());
            java.util.Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            listModel.clear();
            for(String name : names){
                listModel.addElement(name);
            }
            return;
        }
        if(result == 1){
            // Z-A
            java.util.List<String> names = java.util.Collections.list(listModel.elements());
            java.util.Collections.sort(names, java.util.Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));
            listModel.clear();
            for(String name : names){
                listModel.addElement(name);
            }
            return;
        } 
    }

    public void updateCurrentModelFromCore() {
        if (currentSelectedImageName == null)
            return;
        ImageModel model = project.getImage(currentSelectedImageName);
        if (model != null) {
            // Capture grid, preferences from core
            if (core.resultGrid != null) {
                model.setGridData(core.resultGrid);
            }
            // Capture other settings
            model.addPreference("scale", String.valueOf(ControlPanel.usePreference));
            // etc
        }
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
        imageList.clearSelection();
        listModel.clear();
        for (String key : project.getImgs().keySet()) {
            listModel.addElement(key);
        }
    }
}
