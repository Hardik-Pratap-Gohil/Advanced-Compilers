package com.example.ide.file;


import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;

public class FileManager {
    private File lastDirectory = null;

    public String readFileContent(File file) {
        try {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (java.io.IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return null;
        }
    }
    public File openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Assembly Files", "*.asm"));
        if (lastDirectory != null && lastDirectory.exists()) {
            fileChooser.setInitialDirectory(lastDirectory);
        }

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            lastDirectory = file.getParentFile();  // Update the last opened directory
            return file;  // Return the file object
        }
        return null;  // Return null if no file is selected or operation is canceled
    }

    // This method will save content to the specified file
    public boolean saveToFile(File file, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
            lastDirectory = file.getParentFile();  // Update the last opened directory
            return true;  // Return true if save was successful
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
            return false;  // Return false if an error occurred
        }
    }

    // Save method - saves the content to the current file
    public boolean save(File currentFile, String content) {
        if (currentFile != null) {
            // If the file exists, just save to it
            return saveToFile(currentFile, content);
        } else{
            return saveAs(new Stage(),content) != null;
        }
    }

    // Save As method - allows the user to choose a new location and file name
    public File saveAs(Stage stage, String content) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Assembly Files", "*.asm"));

        if (lastDirectory != null && lastDirectory.exists()) {
            fileChooser.setInitialDirectory(lastDirectory);
        }
        // Show the Save As dialog
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            boolean success = saveToFile(file, content);
            if (success) {
                return file; // Return the new file if save was successful
            }
        }
        return null; // Return null if saving was unsuccessful or canceled
    }
}
