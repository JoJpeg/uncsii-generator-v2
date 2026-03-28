package core;
 
import logger.Logger;
import processing.core.PApplet;
import ui.ControlPanel;

public class HeadlessController {
    public static boolean outputPng = false;
    public static boolean outputUnc = false;
    public static boolean darkFirst = false;
    public static boolean lightFirst = false;
    public static int pngScale = 2;
    public static String imgPath = null;
    public static String inputFormat = null;
    public static String outputPath = null; 
    public static String outputFileName = null;
    
    public static void main(String[] args) {
            Logger.sysOut = true; // enable console logging for headless mode 
            Logger.println("Running in background mode...");
            handleArgs(args);    
    }

    public static void handleArgs(String[] args) {
        for (String arg : args) {
            if (arg.equals("-png"))
                outputPng = true;
            else if (arg.equals("-unc"))
                outputUnc = true;
            else if (arg.equals("-dark"))
                darkFirst = true;
            else if (arg.equals("-light"))
                lightFirst = true;
            else if (arg.startsWith("-s=")) {
                try {
                    pngScale = Integer.parseInt(arg.substring("-s=".length()));
                } catch (NumberFormatException e) {
                    Logger.println("Invalid scale value provided. Using default scale of 2.");
                    pngScale = 2;
                }   
            }
            else if(arg.startsWith("-help") || arg.startsWith("--help")){
                Logger.println("Usage: java -jar UnsciiGeneratorHeadless.jar [options] <input_image_path> <output_directory>");
                Logger.println("Options:");
                Logger.println("  -png           Export output as PNG image");
                Logger.println("  -unc           Export output as .unc2 file");
                Logger.println("  -dark          Prefer dark characters for rendering");
                Logger.println("  -light         Prefer light characters for rendering");
                Logger.println("  -s=<num>       Set scale factor for PNG output (default is 2)");
                return;
            }
            //Example usage:
            //java -jar UnsciiGeneratorHeadless.jar -b -png -unc -dark -s=2 /path/to/input/image.jpg /path/to/output/directory
            //Parameters:
            //-b : run in background mode (no GUI)
            //-png : output the result as a PNG file
            //-unc : output the result as a unc2 file
            //-dark : use dark first algo
            //-light: use light first algo
            //-s=<num> : set scale factor for PNG output (default is 2)
            // [path] : path to the input image
            // [path] : path to the output directory (if -png or -unc is specified)
            // 
            // the ouput file name will be derived from the input file name, with the appropriate extension added based on the output format(s) specified.
            // for png output, "_UNSCII" will be appended to the file name before the extension. (example: input.jpg -> input_UNSCII.png) to distinguish it from the original image.

            
        }
        if (!outputPng && !outputUnc) {
            outputPng = true; // default to png output if no output format is specified
        }


        for (String arg : args) {
            if (!arg.startsWith("-")) {
                if (imgPath == null)
                    imgPath = arg;
                else if (outputPath == null)
                    outputPath = arg;
            }
        }

        if (imgPath == null) {
            Logger.println("No image path provided for background mode. Exiting.");
            return;
        } else{
            //extracting filename
            int lastSlashIndex = imgPath.lastIndexOf('/');
            if (lastSlashIndex != -1) {
                outputFileName = imgPath.substring(lastSlashIndex + 1);
                int lastDotIndex = outputFileName.lastIndexOf('.');
                if (lastDotIndex != -1) {
                    outputFileName = outputFileName.substring(0, lastDotIndex) ;
                }
            } else {
                outputFileName = imgPath; // in case there's no path, just use the whole
            }
            Logger.println("Output file name: " + outputFileName);
        }

        if (outputPath == null) {
            Logger.println("No output path provided. saving at input path.");
            outputPath = imgPath;
            //stripping the filename from the path and adding the appropriate extension
            
            int lastSlashIndex = imgPath.lastIndexOf('/');
            if (lastSlashIndex != -1) {
                outputPath = imgPath.substring(0, lastSlashIndex);
            } else {
                outputPath = "."; // current directory
            }
    
        }

        if (darkFirst && lightFirst) {
            Logger.println("Cannot use both -dark and -light options. Please choose one. (using auto)");
            darkFirst = false;
            lightFirst = false;
        }

        if (darkFirst) {
            ControlPanel.algoPreference = ControlPanel.AlgoPreference.Dark;
        } else if (lightFirst) {
            ControlPanel.algoPreference = ControlPanel.AlgoPreference.Light;
        }
        handleProcess();
    }

    public static void handleProcess(){
        HeadlessCore core = new HeadlessCore();
        String[] args = {"UnsciiGeneratorHeadless"};
        PApplet.runSketch(args, core);
    }
}
