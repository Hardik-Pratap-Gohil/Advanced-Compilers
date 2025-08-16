package com.example.ide.assembler;

import java.io.File;

public class util {
    public static String outputFilePath(String filePath){
        File inputFile = new File(filePath);
        String filename = inputFile.getName();
        String outputFileName = filename.substring(0, filename.lastIndexOf('.')) + ".bin";

        // Define the output directory and ensure it exists
        String outputDir = "src/main/resources/com/example/ide/output/";
        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdirs();  // Make the directory structure if it does not exist
        }

        return outputDir + outputFileName;
    }
}
