package com.example.ide.assembler.RISCV;

import java.util.List;

public class Instruction {
    private final TokenType opcode; // The mnemonic (e.g., ADD, SUB)
    private final List<Token> operands; // List of operands (e.g., registers, immediates)
    private final List<AssemblerError> errors; // Shared error list

    // Constructor
    public Instruction(TokenType opcode, List<Token> operands, List<AssemblerError> errors) {
        this.opcode = opcode;
        this.operands = operands;
        this.errors = errors;
    }

    public TokenType getOpcode() {
        return opcode;
    }

    public List<Token> getOperands() {
        return operands;
    }

    @Override
    public String toString() {
        return opcode + " " + operands;
    }

    // Convert the instruction to its machine code representation
    public String toMachineCode() {
        try {
            return switch (opcode) {
                case ADD -> convertRTypeInstruction("0000000", "000"); // R-type
                case SUB -> convertRTypeInstruction("0100000", "000"); // R-type
                case XOR -> convertRTypeInstruction("0000000", "100"); // R-type
                case OR -> convertRTypeInstruction("0000000", "110"); // R-type
                case AND -> convertRTypeInstruction("0000000", "111"); // R-type
                case ADDI -> convertITypeInstruction("0010011","000"); // I-type
                case XORI -> convertITypeInstruction("0010011","100"); // I-type
                case ORI -> convertITypeInstruction("0010011","110"); // I-type
                case ANDI -> convertITypeInstruction("0010011","111"); // I-type
                case LB -> convertITypeInstruction("0000011","000"); // I-type (Load byte)
                case LH -> convertITypeInstruction("0000011","001"); // I-type (Load half-word)
                case LW -> convertITypeInstruction("0000011","010"); // I-type (Load word)
                case SB -> convertSTypeInstruction("000"); // S-type (Store byte)
                case SH -> convertSTypeInstruction("001"); // S-type (Store half-word)
                case SW -> convertSTypeInstruction("010"); // S-type (Store word)
                case BEQ -> convertBTypeInstruction("000"); // B-type (Branch if equal)
                case BNE -> convertBTypeInstruction("001"); // B-type (Branch if not equal)
                case JAL -> convertJTypeInstruction(); // J-type
                case LUI -> convertUTypeInstruction(); // U-type
                case FADD -> convertFTypeInstruction("00000", "000"); // F-type
                case FSUB -> convertFTypeInstruction("00001", "000"); // F-type
                case FMUL -> convertFTypeInstruction("00010", "000"); // F-type
                case FDIV -> convertFTypeInstruction("00011", "000"); // F-type
                case FMIN -> convertFTypeInstruction("00100", "001"); // F-type
                case FMAX -> convertFTypeInstruction("00101", "010"); // F-type
                case FSQRT -> convertFTypeInstruction("00110", "000"); // F-type
                default -> throw new IllegalArgumentException("Unsupported opcode: " + opcode);
            };
        } catch (Exception e) {
            errors.add(new AssemblerError(0, "Instruction Error: " + opcode + " - " + e.getMessage()));
            return "ERROR";
        }
    }

    // Helper method to convert R-Type instructions
    private String convertRTypeInstruction(String funct7, String funct3) {
        if (operands.size() == 3) {
            for (int i = 0; i < operands.size(); i++) {
                if (operands.get(i).type != TokenType.X) {
                    reportTypeError(i, TokenType.X, operands.get(i).type);
                    return "ERROR";
                }
            }
            String rd = registerToBinary(operands.get(0).lexeme);
            String rs1 = registerToBinary(operands.get(1).lexeme);
            String rs2 = registerToBinary(operands.get(2).lexeme);
            return funct7 + rs2 + rs1 + funct3 + rd + "0110011";
        }
        reportOperandError(3);
        return "ERROR";
    }

    private String convertITypeInstruction(String opcode, String funct3) {
        if (operands.size() == 3) {
            Token rdToken = operands.get(0);
            Token rs1Token = operands.get(1);
            Token immToken = operands.get(2);

            // Validate rd (destination register) must be of type X
            if (rdToken.type != TokenType.X) {
                reportTypeError(0, TokenType.X, rdToken.type);
                return "ERROR";
            }

            // Validate rs1 (source register) must be of type X
            if (rs1Token.type != TokenType.X) {
                reportTypeError(1, TokenType.X, rs1Token.type);
                return "ERROR";
            }

            // Validate imm (immediate) must be of type IMM
            if (immToken.type != TokenType.IMM) {
                reportTypeError(2, TokenType.IMM, immToken.type);
                return "ERROR";
            }

            // Generate binary values
            String rd = registerToBinary(rdToken.lexeme);
            String rs1 = registerToBinary(rs1Token.lexeme);
            int imm = (int) immToken.literal; // Retrieve the literal value for IMM
            String immBinary = padBinaryString(Integer.toBinaryString(imm & 0xFFF), 12);

            return immBinary + rs1 + funct3 + rd + opcode;
        }

        // Report error if the number of operands is incorrect
        reportOperandError(3);
        return "ERROR";
    }

    private String convertSTypeInstruction(String funct3) {
        if (operands.size() == 3) {
            Token immToken = operands.get(0);
            Token rs2Token = operands.get(1);
            Token rs1Token = operands.get(2);

            // Validate imm (immediate) must be of type IMM
            if (immToken.type != TokenType.IMM) {
                reportTypeError(0, TokenType.IMM, immToken.type);
                return "ERROR";
            }

            // Validate rs2 (source register) must be of type X
            if (rs2Token.type != TokenType.X) {
                reportTypeError(1, TokenType.X, rs2Token.type);
                return "ERROR";
            }

            // Validate rs1 (base register) must be of type X
            if (rs1Token.type != TokenType.X) {
                reportTypeError(2, TokenType.X, rs1Token.type);
                return "ERROR";
            }

            // Generate binary values
            String rs2 = registerToBinary(rs2Token.lexeme);
            String rs1 = registerToBinary(rs1Token.lexeme);
            int imm = (int) immToken.literal; // Retrieve the literal value for IMM
            String immBinary = padBinaryString(Integer.toBinaryString(imm & 0xFFF), 12);

            // Construct S-Type binary instruction
            return immBinary.substring(0, 7) + rs2 + rs1 + funct3 + immBinary.substring(7) + "0100011";
        }

        // Report error if the number of operands is incorrect
        reportOperandError(3);
        return "ERROR";
    }
    private String convertBTypeInstruction(String funct3) {
        if (operands.size() == 3) {
            Token immToken = operands.get(0);
            Token rs2Token = operands.get(1);
            Token rs1Token = operands.get(2);

            // Validate imm (immediate) must be of type IMM
            if (immToken.type != TokenType.IMM) {
                reportTypeError(0, TokenType.IMM, immToken.type);
                return "ERROR";
            }

            // Validate rs2 (source register) must be of type X
            if (rs2Token.type != TokenType.X) {
                reportTypeError(1, TokenType.X, rs2Token.type);
                return "ERROR";
            }

            // Validate rs1 (base register) must be of type X
            if (rs1Token.type != TokenType.X) {
                reportTypeError(2, TokenType.X, rs1Token.type);
                return "ERROR";
            }

            // Generate binary values
            String rs2 = registerToBinary(rs2Token.lexeme);
            String rs1 = registerToBinary(rs1Token.lexeme);
            int imm = (int) immToken.literal; // Retrieve the literal value for IMM
            String immBinary = padBinaryString(Integer.toBinaryString(imm & 0x1FFF), 13); // 13-bit immediate

            // Construct B-Type binary instruction
            String immPart1 = immBinary.charAt(0) + immBinary.substring(2, 8); // Bits [12, 10:5]
            String immPart2 = immBinary.substring(8, 12) + immBinary.charAt(1); // Bits [4:1, 11]
            return immPart1 + rs2 + rs1 + funct3 + immPart2 + "1100011";
        }

        // Report error if the number of operands is incorrect
        reportOperandError(3);
        return "ERROR";
    }


    private String convertUTypeInstruction() {
        if (operands.size() == 2) {
            Token rdToken = operands.get(0);
            Token immToken = operands.get(1);

            // Validate rd (destination register) must be of type X
            if (rdToken.type != TokenType.X) {
                reportTypeError(0, TokenType.X, rdToken.type);
                return "ERROR";
            }

            // Generate binary values for the immediate
            String immBinary;
            if (immToken.type == TokenType.IMM) {
                // Integer immediate value
                int imm = (int) immToken.literal;
                immBinary = padBinaryString(Integer.toBinaryString(imm & 0xFFFFF), 20); // Ensure 20-bit immediate
            } else if (immToken.type == TokenType.IMM_FLOAT) {
                // Floating-point immediate value (convert to IEEE 754 and extract upper 20 bits)
                float imm = (float) immToken.literal;
                int ieee754 = Float.floatToIntBits(imm); // Convert to raw IEEE 754
                immBinary = padBinaryString(Integer.toBinaryString((ieee754 >> 12) & 0xFFFFF), 20); // Upper 20 bits
            } else {
                reportTypeError(1, TokenType.IMM, immToken.type);
                return "ERROR";
            }

            // Convert rd (destination register) to binary
            String rd = registerToBinary(rdToken.lexeme);

            // Construct U-Type binary instruction
            return immBinary + rd + (opcode == TokenType.LUI ? "0110111" : "0010111");
        }

        // Report error if the number of operands is incorrect
        reportOperandError(2);
        return "ERROR";
    }



    private String convertJTypeInstruction() {
        if (operands.size() == 2) {
            Token rdToken = operands.get(0);
            Token immToken = operands.get(1);

            // Validate rd (destination register) must be of type X
            if (rdToken.type != TokenType.X) {
                reportTypeError(0, TokenType.X, rdToken.type);
                return "ERROR";
            }

            // Validate imm (immediate value) must be of type IMM
            if (immToken.type != TokenType.IMM) {
                reportTypeError(1, TokenType.IMM, immToken.type);
                return "ERROR";
            }

            // Convert register and immediate value to binary
            String rd = registerToBinary(rdToken.lexeme);
            int imm = (int) immToken.literal;

            // Mask immediate to ensure it fits within 21 bits
            String immBinary = padBinaryString(Integer.toBinaryString(imm & 0x1FFFFF), 21);

            // Encode immediate in J-Type format
            String immEncoded = immBinary.charAt(0)                   // Bit 20
                    + immBinary.substring(10, 20)                    // Bits 10-19
                    + immBinary.charAt(9)                            // Bit 9
                    + immBinary.substring(1, 9);                     // Bits 1-8

            // Construct the final machine code
            return immEncoded + rd + "1101111"; // J-Type opcode
        }

        // Report error for incorrect operand count
        reportOperandError(2);
        return "ERROR";
    }
    private String convertFTypeInstruction(String funct5, String rm) {
        if (operands.size() == 3) {
            Token rdToken = operands.get(0);
            Token rs1Token = operands.get(1);
            Token rs2Token = operands.get(2);

            // Validate rd (destination register) must be of type X
            if (rdToken.type != TokenType.X) {
                reportTypeError(0, TokenType.X, rdToken.type);
                return "ERROR";
            }

            // Validate rs1 (source register 1) must be of type X
            if (rs1Token.type != TokenType.X) {
                reportTypeError(1, TokenType.X, rs1Token.type);
                return "ERROR";
            }

            // Validate rs2 (source register 2) must be of type X
            if (rs2Token.type != TokenType.X) {
                reportTypeError(2, TokenType.X, rs2Token.type);
                return "ERROR";
            }

            // Convert registers to binary
            String rd = registerToBinary(rdToken.lexeme);
            String rs1 = registerToBinary(rs1Token.lexeme);
            String rs2 = registerToBinary(rs2Token.lexeme);

            // Construct the final machine code
            return funct5 + rs2 + rs1 + rm + rd + "1010011"; // F-Type opcode
        }

        // Report error for incorrect operand count
        reportOperandError(3);
        return "ERROR";
    }


    private void reportTypeError(int operandIndex, TokenType expected, TokenType actual) {
        int line = operands.get(operandIndex).line; // Get the correct line number
        String errorMessage = String.format("Instruction %s expected Operand %d to be of type %s, but found %s",
                opcode, operandIndex + 1, expected, actual);
        errors.add(new AssemblerError(line, errorMessage)); // Use the correct line number
    }


    private void reportOperandError(int expected) {
        int line = operands.isEmpty() ? 0 : operands.get(0).line; // Use the first operand's line number, or 0 if none
        String errorMessage = String.format("Instruction %s expects %d operands, but got %d",
                opcode, expected, operands.size());
        errors.add(new AssemblerError(line, errorMessage)); // Use the correct line number
    }


    private String registerToBinary(String reg) {
        if (reg.startsWith("X")) {
            int regNum = Integer.parseInt(reg.substring(1));
            return String.format("%05d", Integer.parseInt(Integer.toBinaryString(regNum)));
        }
        reportOperandError(1);
        return "00000";
    }

    private String padBinaryString(String binary, int length) {
        return "0".repeat(Math.max(0, length - binary.length())) + binary;
    }
}