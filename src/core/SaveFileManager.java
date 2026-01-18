package core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import logger.Logger;

public class SaveFileManager {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void saveProject(Project project, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(project, writer);
            Logger.println("Project saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            Logger.println("Error: Failed to save project: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Project loadProject(File file) {
        try (FileReader reader = new FileReader(file)) {
            Project project = gson.fromJson(reader, Project.class);
            Logger.println("Project loaded from " + file.getAbsolutePath());
            return project;
        } catch (IOException e) {
            Logger.println("Error: Failed to load project: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
