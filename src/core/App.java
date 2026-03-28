package core;
import logger.Logger;
import processing.core.PApplet;

public class App {
    public static void main(String[] args) throws Exception {
        //available args:
        //-b : run in background mode (no GUI)
        //-png : output the result as a PNG file 
        //-unc : output the result as a unc2 file
        //-dark : use dark first algo
        //-light: use light first algo
        // [path] : path to the input image 
        // [path] : path to the output file (if -png or -unc is specified)

        //syntax example: 
        // java -jar ascii-art-generator.jar -b -png -unc -dark input.jpg output.png


        if(args.length > 0 && args[0].equals("-b")) {
            // Headless mode (no GUI)
            Logger.println("Running in Headless Mode");
            HeadlessController.main(args);
        } else {
            Logger.println("Starting UNSCII Converter");
            PApplet.main("core.ProcessingCore");
        }

    }

}

// TODO:
/*
 * Make it possible to change the dimesions
 * Load usc2
 * 
 * 
 */
