package resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import logger.Logger; 

public final class ResourceLoader {

    static ArrayList<String> tempFiles = new ArrayList<>();
    static String localResourcePath = "";

    static {
        setLocalResourcePath();

    } // Utility class

    public static void setLocalResourcePath(){
        try {
            localResourcePath = "" + new File(new File(".").getCanonicalPath()).getPath() + "/resources/";
            Logger.println("Local resource path set to: " + localResourcePath);
        } catch (IOException e) {

            System.err.println("Could not determine local resource path.");
            e.printStackTrace();
        }
    }

    /**
     * Loads a resource as InputStream.
     *
     * @param contextClass Class for the ClassLoader context.
     * @param resourcePath Path to the resource in the classpath (e.g.
     *                     "resources/myfile.txt").
     * @return InputStream of the resource.
     * @throws ResourceNotFoundException When the resource is not found.
     */
    public static InputStream loadResourceAsStream(Class<?> contextClass, String resourceFileName) {
        // Correct path if necessary (no leading slash for
        // ClassLoader.getResourceAsStream)
        String correctedPath = resourceFileName.startsWith("/") ? resourceFileName.substring(1) : resourceFileName;
        //this is only for when you have a path that goes like de/name/project/...
        String pathPre = "resources/";

        correctedPath = correctedPath.startsWith(pathPre) ? correctedPath : pathPre + correctedPath; 
        InputStream inputStream = contextClass.getClassLoader().getResourceAsStream(correctedPath);

        if (inputStream == null) {
            // return null;
            throw new ResourceNotFoundException("Resource not found: " + correctedPath);
        }
        return inputStream;
    }

    /**
     * Loads a resource from the classpath and copies it to a temporary
     * file.
     * The temporary file is marked for deletion when the JVM exits (via
     * deleteOnExit).
     * Note: For long-running applications, explicit deletion is often better.
     *
     * @param contextClass Class for the ClassLoader context.
     * @param resourcePath Path to the resource in the classpath (e.g.
     *                     "resources/myfile.txt").
     * @return A File object that points to the temporary file in the filesystem.
     * @throws ResourceNotFoundException When the resource is not found in the classpath.
     * @throws IOException               When an error occurs while creating or
     *                                   writing the temporary file.
     */
    public static File getResourceAsTempFile(Class<?> contextClass, String resourceFileName)
            throws ResourceNotFoundException, IOException {

        try (InputStream inputStream = loadResourceAsStream(contextClass, resourceFileName)) {
            String suffix = null;
            int dotIndex = resourceFileName.lastIndexOf('.');
            if (dotIndex != -1 && dotIndex < resourceFileName.length() - 1) {
                suffix = resourceFileName.substring(dotIndex);
            }
            File tempFile = File.createTempFile("resource-", suffix != null ? suffix : ".tmp");
            if(inputStream == null) {
                throw new ResourceNotFoundException("Resource not found: " + resourceFileName);
            }
            tempFile.deleteOnExit();
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tempFiles.add(tempFile.getAbsolutePath());
            return tempFile;

        } // InputStream is automatically closed here.
          // IOException from createTempFile or Files.copy is propagated.
    }   
 

    public static File getOrWriteResourceLocally(Class<?> contextClass, String resourceFileName)
            throws ResourceNotFoundException, IOException {

        try (InputStream inputStream = loadResourceAsStream(contextClass, resourceFileName)) {

            File localFolder = new File(localResourcePath);

            // check if the required folder already exists
            if (!localFolder.exists()) {
                localFolder.mkdirs();
                Logger.println("Created local resource folder at: " + localFolder.getAbsolutePath());
            }

            // check if there is a file in the local folder with the required name
            File localFile = new File(localFolder, resourceFileName);
            if (localFile.exists()) {
 

                Logger.println("Local resource already exists: " + localFile.getAbsolutePath());
                return localFile;
            }

            Files.copy(inputStream, localFolder.toPath().resolve(resourceFileName), StandardCopyOption.REPLACE_EXISTING);
            // Return the File object that points to the temporary file.
            return localFile;
        }
    }

    public static String getTempResourcePath(Class<?> contextClass, String resourceFileName) {
        try {
            File file = ResourceLoader.getResourceAsTempFile(contextClass, resourceFileName);
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Error loading resource: " + resourceFileName, e);
        }
    }

    /***
     * 
     * @param contextClass
     * @param resourceFileName
     * @return Returns the required resource if available. The returned resource
     *         exists temporarily per loading.
     *         If you load it again, the last one will be overwritten by the jar
     *         default one.
     * @throws RuntimeException if the resource could not be loaded.
     */
    public static File getTempResourceFile(Class<?> contextClass, String resourceFileName) {
        try {
            return ResourceLoader.getResourceAsTempFile(contextClass, resourceFileName);
        } catch (IOException e) {
            throw new RuntimeException("Error loading resource: " + resourceFileName, e);
        }
    }

    public static File getLocalResourceFile(Class<?> contextClass, String resourceFileName) {
        try {
            return ResourceLoader.getOrWriteResourceLocally(contextClass, resourceFileName);
        } catch (IOException e) {
            throw new RuntimeException("Error loading resource: " + resourceFileName, e);
        }
    }

    /** Exception for resources not found. */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static void cleanupTempFiles() {
        for (String filePath : tempFiles) {
            File file = new File(filePath);
            if (file.exists()) {
                if (file.delete()) {
                    Logger.println("Deleted temp file: " + filePath);
                } else {
                    Logger.println("Failed to delete temp file: " + filePath);
                }
            }
        }
        tempFiles.clear();
    }
 
    
}
