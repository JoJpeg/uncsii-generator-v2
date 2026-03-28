package core.data;

import core.ProcessingCore;
import logger.Logger;
import processing.core.PGraphics;

public class ImageExporter {
    public static void exportAsPng(int scale, String outputPath, ProcessingCore core){
        
        PGraphics img = core.getResultingImage(10);
        //confirm pixel dimension of img: 
        Logger.println("Exporting image with dimensions: " + img.width + "x" + img.height);
        if(img != null){
            Logger.println("Calculating image success..");
        } 
        img.save(outputPath);
        Logger.println("Image saved to: " + outputPath);
    }
}
