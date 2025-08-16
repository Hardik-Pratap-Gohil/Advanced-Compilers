package com.example.ide.assembler.RISCV;

import javafx.scene.control.TextArea;

import java.util.*;

public class Parser {
    private int currentLine;
    private final List<AssemblerError> errors;
    private final TextArea outputTextArea;

    public Parser(List<AssemblerError> errors, TextArea outputTextArea) {
        this.errors = errors;
        this.outputTextArea = outputTextArea;
    }

    public int getLine() {
        return currentLine;
    }

    // Parse the assembly code and convert it to a list of instructions
    public List<Instruction> parse(List<Token> tokens) {
        List<Instruction> instructions = new ArrayList<>();

        int i = 0; // Index to track the current token
        while (i < tokens.size()) {
            Token token = tokens.get(i);
            currentLine = token.line;

            // Expect an opcode first
            if (!isOpcode(token)) {
                AssemblerError error = new AssemblerError(token.line,
                        "Unexpected token: " + token.lexeme + ". Expected an opcode.");
                errors.add(error);
                outputTextArea.appendText(error.toString() + "\n");
                i++;
                continue;
            }

            // Opcode is the first token
            TokenType opcode = token.type;

            // Collect operands for this instruction
            List<Token> operands = new ArrayList<>();
            i++; // Move to the next token

            while (i < tokens.size() && !isOpcode(tokens.get(i))) {
                Token current = tokens.get(i);

                // Skip commas
                if (current.type == TokenType.COMMA) {
                    i++;
                    continue;
                }

                // Handle addressing mode (e.g., 10(X6))
                if (current.type == TokenType.IMM && i + 2 < tokens.size() &&
                        tokens.get(i + 1).type == TokenType.LPAREN && // (
                        tokens.get(i + 2).type == TokenType.X) {     // Register (e.g., X6)

                    Token offset = current;                    // Immediate (e.g., 10)
                    Token baseRegister = tokens.get(i + 2);    // Register (e.g., X6)

                    // Add the base + offset addressing as two separate tokens
                    operands.add(offset);                      // 10
                    operands.add(baseRegister);                // X6

                    // Skip the '(' and ')' tokens
                    i += 4; // Advance past IMM, LPAREN, X, RPAREN
                    continue;
                }

                // Otherwise, add the token to operands
                operands.add(current);
                i++;
            }


            // Add the instruction to the list, even if operands are invalid (optional, based on requirements)
            instructions.add(new Instruction(opcode, operands, errors));
        }

        return instructions;
    }



    private int requiredOperands(TokenType opcode) {
        switch (opcode) {
            case ADD: case SUB: case AND: case OR: case XOR:
                return 3;
            case ADDI: case ANDI: case ORI: case XORI: case LW: case LB: case LH:
                return 3;
            case SW: case SH: case SB:
                return 3;
            case BEQ: case BNE:
                return 3;
            case JAL: case LUI:
                return 2;
            case FADD: case FDIV: case FMAX: case FMIN: case FMUL: case  FSQRT: case FSUB:
                return 3;
            default:
                return 0;
        }
    }

    // Helper method to check if a token is an opcode
    private static final EnumSet<TokenType> OPCODES = EnumSet.of(
            TokenType.ADD, TokenType.SUB, TokenType.XOR, TokenType.OR, TokenType.AND,
            TokenType.XORI, TokenType.ORI, TokenType.ANDI, TokenType.ADDI,
            TokenType.LB, TokenType.LH, TokenType.LW, TokenType.JALR,
            TokenType.SB, TokenType.SH, TokenType.SW, TokenType.BEQ,
            TokenType.BNE, TokenType.JAL, TokenType.LUI,
            TokenType.FADD, TokenType.FSUB, TokenType.FMUL, TokenType.FDIV,
            TokenType.FMIN, TokenType.FMAX, TokenType.FSQRT
    );

    private boolean isOpcode(Token token) {
        return OPCODES.contains(token.type);
    }
}
