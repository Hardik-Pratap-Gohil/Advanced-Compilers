package com.example.ide.assembler.RISCV;


import javafx.scene.control.TextArea;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import static com.example.ide.assembler.util.outputFilePath;

public class RiscVAssembler {

    public static List<AssemblerError> assemble(String filePath, TextArea outputTextArea) {
        List<AssemblerError> errors = new ArrayList<>();

        String code = readFile(filePath);
        if (code == null || code.isEmpty()) {
            errors.add(new AssemblerError(0, "Error reading the assembly file or the file is empty."));
            displayErrors(errors, outputTextArea);
            return errors;
        }

        Lexer lexer = new Lexer(code, errors, outputTextArea);
        List<Token> tokens;
        try {
            tokens = lexer.tokenize();
        } catch (IllegalArgumentException e) {
            errors.add(new AssemblerError(lexer.getLine(), e.getMessage()));
            displayErrors(errors, outputTextArea);
            return errors;
        }
        System.out.println(tokens);
        Parser parser = new Parser(errors, outputTextArea);
        List<Instruction> instructions;
        try {
            instructions = parser.parse(tokens);
        } catch (IllegalArgumentException e) {
            errors.add(new AssemblerError(parser.getLine(), e.getMessage()));
            displayErrors(errors, outputTextArea);
            return errors;
        }
        System.out.println(instructions);

        // Write machine code to file
        String outputFilePath = outputFilePath(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            for (Instruction instruction : instructions) {
                writer.write(instruction.toMachineCode());
                writer.newLine();
            }
        } catch (IOException e) {
            errors.add(new AssemblerError(0, "Error writing to output file: " + e.getMessage()));
        }

        if (!errors.isEmpty()) {
            displayErrors(errors, outputTextArea);
        }

        return errors;
    }

    // Method to read the file content
    private static String readFile(String filePath) {
        StringBuilder code = new StringBuilder();
        try {
            Scanner scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine()) {
                code.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filePath);
            return null;
        }
        return code.toString();
    }

    // Method to display errors in the TextArea
    private static void displayErrors(List<AssemblerError> errors, TextArea outputTextArea) {
        if (errors.isEmpty()) {
            return;
        }

        // Sort errors by line number
        errors.sort(Comparator.comparingInt(AssemblerError::getLine));

        outputTextArea.clear(); // Clear existing text
        for (AssemblerError error : errors) {
            outputTextArea.appendText("Error at line " + error.getLine() + ": " + error.getMessage() + "\n");
        }
        outputTextArea.positionCaret(0); // Scroll to top
    }



}
