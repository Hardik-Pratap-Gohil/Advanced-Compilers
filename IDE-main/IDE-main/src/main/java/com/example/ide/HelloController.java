package com.example.ide;

import com.example.ide.assembler.RISCV.AssemblerError;
import com.example.ide.assembler.RISCV.RiscVAssembler;
import com.example.ide.file.FileManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.*;

public class HelloController implements Initializable {

    @FXML
    private TabPane tabPane;  // TabPane to hold all open tabs

    @FXML
    private SplitPane mainSplitPane;

    @FXML
    private TabPane outputTabPane;


    @FXML
    private Label statusLabel;  // Label to show the status of save operations

    @FXML
    private TextArea codeEditor;

    @FXML
    private TextArea outputTextArea;

    @FXML
    private TextArea serialMonitorTextArea;



    private FileManager fileManager;  // Instance of FileManager class
    private Map<Tab, String> tabFileMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainSplitPane.setDividerPositions(1);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> updateCurrentFile(newTab));
        fileManager = new FileManager();
    }

    private void updateCurrentFile(Tab tab) {
        String filePath = tab != null ? tabFileMap.get(tab) : null;
        statusLabel.setText("Current file: " + (filePath != null ? filePath : "No file selected"));
    }

    public HelloController() {
        fileManager = new FileManager();  // Initialize FileManager
    }

    public void assemble() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null) {
            statusLabel.setText("No tab selected.");
            return;
        }

        String filePath = tabFileMap.get(currentTab);
        if (filePath == null) {
            // Prompt user to save the file if it hasn't been saved yet
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Save File");
            alert.setHeaderText("The file needs to be saved before assembling.");
            alert.setContentText("Do you want to save the file?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                saveAs();  // This should update filePath in tabFileMap if the user proceeds
                filePath = tabFileMap.get(currentTab);  // Update filePath after saving
                if (filePath == null) {
                    statusLabel.setText("File save cancelled. Assembly aborted.");
                    return;
                }
            } else {
                statusLabel.setText("Assembly cancelled.");
                return;  // Exit the method if the user cancels or doesn't save
            }
        }

        // Proceed with assembling only if there is a file to assemble
        if (filePath != null) {
            outputTextArea.clear(); // Clear any previous output
            List<AssemblerError> errors = RiscVAssembler.assemble(filePath, outputTextArea);

            if (errors.isEmpty()) {
                statusLabel.setText("Assembly completed successfully for " + new File(filePath).getName());
                outputTextArea.appendText("Assembly completed successfully.\n");
            } else {
                statusLabel.setText("Assembly completed with errors for " + new File(filePath).getName());
                toggleOutput(); // Ensure the output console is visible
            }
        }
    }



    public void toggleOutput() {
        adjustSplitPane(outputTabPane.isVisible() && outputTabPane.getSelectionModel().getSelectedIndex() == 0, 0);
    }

    public void toggleSerialMonitor() {
        adjustSplitPane(outputTabPane.isVisible() && outputTabPane.getSelectionModel().getSelectedIndex() == 1, 1);
    }

    private void adjustSplitPane(boolean shouldHide, int tabIndex) {
        if (shouldHide) {
            // Hide the pane if it's currently visible and the tab to be toggled is active
            mainSplitPane.setDividerPositions(1);  // Hide the pane by adjusting the divider
            outputTabPane.setVisible(false);
        } else {
            // Otherwise, show the pane and ensure the correct tab is selected
            outputTabPane.setVisible(true);
            outputTabPane.getSelectionModel().select(tabIndex);
            mainSplitPane.setDividerPositions(0.7);  // 70% for the editor, 30% for the output
        }
    }
    // Method to create a new "Untitled" file
    public void newFile() {
        Tab newTab = new Tab("Untitled");
        TextArea editor = new TextArea();
        newTab.setContent(editor);
        newTab.setClosable(true);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
        tabFileMap.put(newTab, null);
        statusLabel.setText("New file created.");
    }

    // Method to open a file and load its content into a new tab
    public void open() {
        Stage stage = new Stage();
        File file = fileManager.openFile(stage);
        if (file != null) {
            String content = fileManager.readFileContent(file);
            Tab newTab = new Tab(file.getName());
            TextArea editor = new TextArea(content);
            newTab.setContent(editor);
            tabPane.getTabs().add(newTab);
            tabPane.getSelectionModel().select(newTab);
            tabFileMap.put(newTab, file.getPath());
            statusLabel.setText("File opened successfully: " + file.getName());
        } else {
            statusLabel.setText("File opening canceled.");
        }
    }

    // Method to save the content of the current tab
    public void save() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            TextArea editor = (TextArea) currentTab.getContent();
            String content = editor.getText();
            String filePath = tabFileMap.get(currentTab);
            if (filePath == null) {
                saveAs();  // Trigger Save As dialog if file has no path
            } else {
                File file = new File(filePath);
                boolean success = fileManager.save(file, content);
                if (success) {
                    statusLabel.setText("File saved: " + file.getName());
                } else {
                    statusLabel.setText("Error saving file.");
                }
            }
        }
    }

    // Save As method - lets the user choose where to save the file
    public void saveAs() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            TextArea editor = (TextArea) currentTab.getContent();
            String content = editor.getText();
            File file = fileManager.saveAs(new Stage(), content);
            if (file != null) {
                currentTab.setText(file.getName());
                tabFileMap.put(currentTab, file.getPath());
                statusLabel.setText("File saved as: " + file.getName());
            } else {
                statusLabel.setText("Error saving file.");
            }
        }
    }

    // Method to handle the Close action on the "File" menu
    public void closeFile() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            confirmSaveBeforeClose(currentTab);
        }
    }

    private void confirmSaveBeforeClose(Tab tab) {
        TextArea editor = (TextArea) tab.getContent();
        String filePath = tabFileMap.get(tab);
        if (editor.getText().isEmpty() || filePath != null) {
            tabPane.getTabs().remove(tab);
            tabFileMap.remove(tab);
            statusLabel.setText("Tab closed.");
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "You have unsaved changes. Do you want to save them before closing?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    saveAs();
                } else if (response == ButtonType.NO) {
                    tabPane.getTabs().remove(tab);
                    tabFileMap.remove(tab);
                    statusLabel.setText("Tab closed without saving.");
                }
            });
        }
    }




    // Method to handle the "Exit" action from the menu
    public void exit() {
        // Exit the application (you can add confirmation dialog if needed)
        System.exit(0);
    }


}
