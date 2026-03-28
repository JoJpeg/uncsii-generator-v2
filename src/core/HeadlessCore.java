package core;

import core.data.ImageExporter;
import core.data.UscExportManager;
import logger.Logger; 

public class HeadlessCore extends ProcessingCore {

    public static void main(String[] args) {
        System.out.println("Called Main..");
    }

    @Override
    public void setup() {
        surface.setVisible(false);
        headless = true;
        super.init();
        noLoop();
        loadAndProcessImage(HeadlessController.imgPath);

        if (HeadlessController.outputUnc) {
            String path = HeadlessController.outputPath + "/" + HeadlessController.outputFileName + ".unc2";
            Logger.println("Exporting .unc2 to: " + path);
            UscExportManager.exportCurrent(path, this);
        }
        if (HeadlessController.outputPng) {
            String path = HeadlessController.outputPath + "/" + HeadlessController.outputFileName + "_UNSCII.png";
            Logger.println("Exporting .png to: " + path);
            ImageExporter.exportAsPng( 
                HeadlessController.pngScale, 
                path, 
                this);
        }
        Logger.println("All done :)");
        exit();
    }

}
