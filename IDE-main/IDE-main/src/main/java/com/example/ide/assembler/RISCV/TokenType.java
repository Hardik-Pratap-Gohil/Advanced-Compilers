package com.example.ide.assembler.RISCV;

public enum TokenType {
    // Instructions
    ADD, SUB, XOR, OR, AND,
    XORI, ORI, ANDI, ADDI, LB, LH, LW, JALR,
    SB, SH, SW,
    BEQ, BNE, JAL, LUI,
    FADD, FSUB, FMUL, FDIV, FMIN, FMAX, FSQRT,

    // Registers (X1,X2,X3,,, X32)
    X,

    // Immediate values
    IMM, IMM_FLOAT,

    // Update TokenType to include:
    LPAREN, // For '('
    RPAREN, // For ')'
    COMMA   // For ','


    }
