import processing.core.PApplet;
import processing.core.PImage;

/**
 * Handles image loading, resizing, and extraction of pixel blocks
 */
public class ImageProcessor {
    private PApplet app;
    private PImage inputImage;
    private String imagePath;
    private int imageWidth;
    private int imageHeight;
    private int gridWidth;
    private int gridHeight;

    // Default display dimensions
    public static final int DEFAULT_DISPLAY_AREA_WIDTH = 400;
    public static final int DEFAULT_DISPLAY_AREA_HEIGHT = 240;

    private int displayAreaWidth = DEFAULT_DISPLAY_AREA_WIDTH;
    private int displayAreaHeight = DEFAULT_DISPLAY_AREA_HEIGHT;

    public enum ImageLoadingState {
        NONE,
        LOADING,
        LOADED,
        ERROR
    }

    private ImageLoadingState state = ImageLoadingState.NONE;

    public ImageProcessor(PApplet app) {
        this.app = app;
    }

    /**
     * Loads an image from the file system
     */
    public boolean loadFromFile(String path) {
        state = ImageLoadingState.LOADING;

        try {
            imagePath = path;
            inputImage = app.loadImage(path);

            if (inputImage == null) {
                Logger.println("Error loading image: " + path);
                state = ImageLoadingState.ERROR;
                return false;
            }

            inputImage.loadPixels();
            imageWidth = inputImage.width;
            imageHeight = inputImage.height;

            // Resize image to fit display area while maintaining aspect ratio
            resizeToFitDisplayArea();

            // Ensure image dimensions are multiples of the glyph size
            cropToFitGrid();

            if (gridWidth == 0 || gridHeight == 0) {
                Logger.println("Image too small after resizing/cropping.");
                state = ImageLoadingState.ERROR;
                return false;
            }

            state = ImageLoadingState.LOADED;
            return true;
        } catch (Exception e) {
            Logger.println("Exception loading image: " + e.getMessage());
            e.printStackTrace();
            state = ImageLoadingState.ERROR;
            return false;
        }
    }

    /**
     * Resize image to fit display area while maintaining aspect ratio
     */
    private void resizeToFitDisplayArea() {
        float imgAspect = (float) inputImage.width / inputImage.height;
        float targetAspect = (float) displayAreaWidth / displayAreaHeight;
        int targetWidth, targetHeight;

        if (imgAspect > targetAspect) {
            targetWidth = displayAreaWidth;
            targetHeight = (int) (displayAreaWidth / imgAspect);
        } else {
            targetHeight = displayAreaHeight;
            targetWidth = (int) (displayAreaHeight * imgAspect);
        }

        inputImage.resize(targetWidth, targetHeight);
        inputImage.loadPixels();

        imageWidth = inputImage.width;
        imageHeight = inputImage.height;

        gridWidth = imageWidth / GlyphManager.GLYPH_WIDTH;
        gridHeight = imageHeight / GlyphManager.GLYPH_HEIGHT;
    }

    /**
     * Crop image to ensure dimensions are multiples of the glyph size
     */
    private void cropToFitGrid() {
        int croppedWidth = gridWidth * GlyphManager.GLYPH_WIDTH;
        int croppedHeight = gridHeight * GlyphManager.GLYPH_HEIGHT;

        if (croppedWidth != imageWidth || croppedHeight != imageHeight) {
            Logger.println("Cropping image from " + imageWidth + "x" + imageHeight +
                    " to " + croppedWidth + "x" + croppedHeight + " to fit grid.");
            inputImage = inputImage.get(0, 0, croppedWidth, croppedHeight);
            inputImage.loadPixels();
            imageWidth = inputImage.width;
            imageHeight = inputImage.height;
        }
    }

    /**
     * Extract a block of pixels from the source image
     */
    public int[] extractBlockPixels(int gridX, int gridY) {
        if (inputImage == null || gridX < 0 || gridY < 0 ||
                gridX >= gridWidth || gridY >= gridHeight) {
            return new int[GlyphManager.PIXEL_COUNT];
        }

        int[] blockPixels = new int[GlyphManager.PIXEL_COUNT];
        int startX = gridX * GlyphManager.GLYPH_WIDTH;
        int startY = gridY * GlyphManager.GLYPH_HEIGHT;

        for (int y = 0; y < GlyphManager.GLYPH_HEIGHT; y++) {
            for (int x = 0; x < GlyphManager.GLYPH_WIDTH; x++) {
                int imgX = startX + x;
                int imgY = startY + y;
                blockPixels[y * GlyphManager.GLYPH_WIDTH + x] = inputImage.pixels[imgY * imageWidth + imgX];
            }
        }

        return blockPixels;
    }

    /**
     * Set the target display area dimensions
     */
    public void setDisplayAreaDimensions(int width, int height) {
        this.displayAreaWidth = width;
        this.displayAreaHeight = height;
    }

    // Getters
    public PImage getImage() {
        return inputImage;
    }

    public int getWidth() {
        return imageWidth;
    }

    public int getHeight() {
        return imageHeight;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public String getImagePath() {
        return imagePath;
    }

    public ImageLoadingState getState() {
        return state;
    }
}