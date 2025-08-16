# R-Type Instructions
ADD X1, X2, X3          # Valid
ADD X1, X2, 10          # Invalid: Third operand must be a register
ADD X1, X2              # Invalid: Missing third operand
ADD X1                  # Invalid: Missing second and third operands
ADD 10, X2, X3          # Invalid: First operand must be a register

SUB X4, X5, X6          # Valid
SUB X4, 5, X6           # Invalid: Second operand must be a register

# I-Type Instructions
ADDI X1, X2, 15         # Valid
ADDI X1, X2, -10        # Valid: Negative immediate
ADDI X1, 10, X2         # Invalid: Second operand must be a register
ADDI X1, X2, X3         # Invalid: Third operand must be an immediate
ADDI X1, X2             # Invalid: Missing immediate operand
ADDI 15, X2, X3         # Invalid: First operand must be a register

# S-Type Instructions
SW 0x10, X1, X2         # Valid
SW -4, X3, X4           # Valid: Negative immediate
SW X1, X2, X3           # Invalid: First operand must be an immediate
SW 0x10, X3             # Invalid: Missing third operand

# B-Type Instructions
BEQ -12, X1, X2         # Valid
BNE 0x20, X3, X4        # Valid
BEQ X1, X2, X3          # Invalid: First operand must be an immediate
BNE 10, X3              # Invalid: Missing third operand

# U-Type Instructions
LUI X1, 0x10000         # Valid
LUI X2, -1              # Valid: Negative immediate for upper 20 bits
LUI X1, X2              # Invalid: Second operand must be an immediate
LUI 0x20                # Invalid: Missing destination register

# J-Type Instructions
JAL X1, 0x100           # Valid
JAL 0x200               # Invalid: Missing destination register
JAL X1, X2              # Invalid: Second operand must be an immediate

# F-Type Instructions
FADD X1, X2, X3         # Valid
FSUB X4, X5, X6         # Valid
FMUL X7, X8, X9         # Valid
FADD X1, X2, 5          # Invalid: Third operand must be a register
FMUL X7                 # Invalid: Missing two operands

# Edge Cases for Syntax
ADD X32, X0, X31        # Valid: Maximum register values
BEQ 0, X1, X2           # Valid: Immediate of zero
SW 0, X1, X2            # Valid: Immediate of zero
