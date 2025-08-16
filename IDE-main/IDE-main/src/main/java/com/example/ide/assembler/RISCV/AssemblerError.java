package com.example.ide.assembler.RISCV;

public class AssemblerError {
    private final int line;
    private final String message;

    public AssemblerError(int line, String message) {
        this.line = line;
        this.message = message;
    }

    public int getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Error at line " + line + ": " + message;
    }
}
