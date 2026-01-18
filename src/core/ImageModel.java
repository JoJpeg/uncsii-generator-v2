package core;

import java.util.HashMap;
import java.util.Map;

public class ImageModel {
    private String filepath;
    private String name;
    // Storing gridData directly. ResultGlyph needs to be serializable by Gson (it is just fields, so yes)
    private ResultGlyph[][] gridData;
    
    // Preferences: scale, palette info, etc. 
    // Storing as a Map for flexibility
    private Map<String, String> preferences;

    public ImageModel(String filepath) {
        this.filepath = filepath;
        this.preferences = new HashMap<>();
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public ResultGlyph[][] getGridData() {
        return gridData;
    }

    public void setGridData(ResultGlyph[][] gridData) {
        this.gridData = gridData;
    }

    public Map<String, String> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, String> preferences) {
        this.preferences = preferences;
    }
    
    public void addPreference(String key, String value) {
        this.preferences.put(key, value);
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
