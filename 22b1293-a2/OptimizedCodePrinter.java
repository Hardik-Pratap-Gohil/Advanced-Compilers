import visitor.*;
import syntaxtree.*;
import java.util.*;

public class OptimizedCodePrinter implements GJNoArguVisitor<String> {
    private final ProgramCFG programCFG;
    private BB currentBB;
    private Iterator<Instruction> instructionIterator;
    private Map<String, ConstantValue> constantValues; // To store propagated constants

    public OptimizedCodePrinter(ProgramCFG programCFG) {
        this.programCFG = programCFG;
        this.constantValues = new HashMap<>();
        this.currentBB = null;
        this.instructionIterator = null;
    }

    @Override
    public String visit(NodeList n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.size(); i++) {
            sb.append(n.elementAt(i).accept(this));
        }
        return sb.toString();
    }

    @Override
    public String visit(NodeListOptional n) {
        StringBuilder sb = new StringBuilder();
        if (n.present()) {
            for (int i = 0; i < n.size(); i++) {
                sb.append(n.elementAt(i).accept(this));
            }
        }
        return sb.toString();
    }

    @Override
    public String visit(NodeOptional n) {
        if (n.present()) {
            return n.node.accept(this);
        }
        return "";
    }

    @Override
    public String visit(NodeSequence n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.size(); i++) {
            sb.append(n.elementAt(i).accept(this));
        }
        return sb.toString();
    }

    @Override
    public String visit(NodeToken n) {
        return n.tokenImage;
    }

    @Override
    public String visit(Goal n) {
        StringBuilder code = new StringBuilder();
        code.append(n.f0.accept(this)); // Main class
        code.append(n.f1.accept(this)); // Other class declarations
        return code.toString();
    }

    @Override
    public String visit(MainClass n) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(n.f1.f0.tokenImage).append(" {\n");
        sb.append("    public static void main(String[] ").append(n.f11.f0.tokenImage).append(") {\n");

        // Get the main method's basic block
        currentBB = programCFG.methodBBSet.get(programCFG.mainMethod);
        instructionIterator = currentBB != null ? currentBB.instructions.iterator() : null;

        // Process variable declarations
        sb.append(n.f14.accept(this));

        // Process statements with optimizations applied
        sb.append(n.f15.accept(this));

        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String visit(TypeDeclaration n) {
        return n.f0.accept(this);
    }

    @Override
    public String visit(ClassDeclaration n) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(n.f1.f0.tokenImage).append(" {\n");

        // Store current class name
        programCFG.currentClass = n.f1.f0.tokenImage;

        // Process field declarations
        sb.append(n.f3.accept(this));

        // Process method declarations
        sb.append(n.f4.accept(this));

        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String visit(ClassExtendsDeclaration n) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(n.f1.f0.tokenImage).append(" extends ").append(n.f3.f0.tokenImage).append(" {\n");

        // Store current class name
        programCFG.currentClass = n.f1.f0.tokenImage;

        // Process field declarations
        sb.append(n.f5.accept(this));

        // Process method declarations
        sb.append(n.f6.accept(this));

        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String visit(VarDeclaration n) {
        return "        " + n.f0.accept(this) + " " + n.f1.f0.tokenImage + ";\n";
    }

    @Override
    public String visit(MethodDeclaration n) {
        StringBuilder sb = new StringBuilder();
        String methodName = n.f2.f0.tokenImage;
        String fullMethodName = ProgramCFG.getMethodName(programCFG.currentClass, methodName);

        // Get the method's basic block
        currentBB = programCFG.methodBBSet.get(fullMethodName);
        instructionIterator = currentBB != null ? currentBB.instructions.iterator() : null;

        // Build method signature
        sb.append("    public ").append(n.f1.accept(this)).append(" ").append(methodName).append("(");
        sb.append(n.f4.accept(this)).append(") {\n");

        // Process variable declarations
        sb.append(n.f7.accept(this));

        // Process statements with optimizations applied
        sb.append(n.f8.accept(this));

        // Process return statement
        sb.append("        return ").append(n.f10.accept(this)).append(";\n");

        sb.append("    }\n\n");
        return sb.toString();
    }

    @Override
    public String visit(FormalParameterList n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.f0.accept(this));
        sb.append(n.f1.accept(this));
        return sb.toString();
    }

    @Override
    public String visit(FormalParameter n) {
        return n.f0.accept(this) + " " + n.f1.f0.tokenImage;
    }

    @Override
    public String visit(FormalParameterRest n) {
        return ", " + n.f1.accept(this);
    }

    @Override
    public String visit(Type n) {
        return n.f0.accept(this);
    }

    @Override
    public String visit(ArrayType n) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n) {
        return "int";
    }

    @Override
    public String visit(Statement n) {
        return n.f0.accept(this);
    }

    @Override
    public String visit(AssignmentStatement n) {
        String varName = n.f0.f0.tokenImage;

        // Try to find the corresponding instruction in the CFG
        if (instructionIterator != null && instructionIterator.hasNext()) {
            Instruction inst = instructionIterator.next();
            if (inst.instructionNode instanceof AssignmentStatement) {
                AssignmentStatement cfgAssign = (AssignmentStatement) inst.instructionNode;
                if (cfgAssign.f0.f0.tokenImage.equals(varName)) {
                    // Check if the right-hand side is a constant
                    String rhsVar = getConstantValue(cfgAssign.f2);
                    if (rhsVar != null) {
                        return "        " + varName + " = " + rhsVar + ";\n";
                    }
                    return "        " + varName + " = " + cfgAssign.f2.accept(this) + ";\n";
                }
            }
        }

        // If no match found in CFG, use the original expression but check for constants
        String optimizedExpr = getConstantValue(n.f2);
        if (optimizedExpr != null) {
            return "        " + varName + " = " + optimizedExpr + ";\n";
        }
        return "        " + varName + " = " + n.f2.accept(this) + ";\n";
    }

    @Override
    public String visit(ArrayAssignmentStatement n) {
        return "        " + n.f0.f0.tokenImage + "[" + n.f2.f0.tokenImage + "] = " + n.f5.f0.tokenImage + ";\n";
    }

    @Override
    public String visit(FieldAssignmentStatement n) {
        return "        " + n.f0.f0.tokenImage + "." + n.f2.f0.tokenImage + " = " + n.f4.f0.tokenImage + ";\n";
    }

    @Override
    public String visit(PrintStatement n) {
        String id = n.f2.f0.tokenImage;
        // Check if we can replace with a constant
        String constValue = getConstantValueForVar(id);
        if (constValue != null) {
            return "        System.out.println(" + constValue + ");\n";
        }
        return "        System.out.println(" + id + ");\n";
    }

    @Override
    public String visit(Block n) {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        String statements = n.f1.accept(this);
        // Indent the nested statements
        statements = statements.replace("\n", "\n    ");
        sb.append(statements);
        sb.append("        }\n");
        return sb.toString();
    }

    @Override
    public String visit(IfStatement n) {
        return n.f0.accept(this);
    }

    @Override
    public String visit(IfthenStatement n) {
        String condition = n.f2.f0.tokenImage;
        // Check if condition is a constant
        String constValue = getConstantValueForVar(condition);
        if (constValue != null) {
            if (constValue.equals("true")) {
                // Condition is always true, just include the 'then' block
                return n.f4.accept(this);
            } else if (constValue.equals("false")) {
                // Condition is always false, skip the block entirely
                return "";
            }
        }

        return "        if (" + condition + ") " + n.f4.accept(this);
    }

    @Override
    public String visit(IfthenElseStatement n) {
        String condition = n.f2.f0.tokenImage;
        // Check if condition is a constant
        String constValue = getConstantValueForVar(condition);
        if (constValue != null) {
            if (constValue.equals("true")) {
                // Condition is always true, just include the 'then' block
                return n.f4.accept(this);
            } else if (constValue.equals("false")) {
                // Condition is always false, just include the 'else' block
                return n.f6.accept(this);
            }
        }

        return "        if (" + condition + ") " + n.f4.accept(this) + " else " + n.f6.accept(this);
    }

    @Override
    public String visit(WhileStatement n) {
        String condition = n.f2.f0.tokenImage;
        // Check if condition is a constant
        String constValue = getConstantValueForVar(condition);
        if (constValue != null) {
            if (constValue.equals("true")) {
                // Condition is always true, create an infinite loop
                return "        while (true) " + n.f4.accept(this);
            } else if (constValue.equals("false")) {
                // Condition is always false, skip the loop entirely
                return "";
            }
        }

        return "        while (" + condition + ") " + n.f4.accept(this);
    }

    @Override
    public String visit(Expression n) {
        return n.f0.accept(this);
    }

    @Override
    public String visit(AndExpression n) {
        String left = n.f0.f0.tokenImage;
        String right = n.f2.f0.tokenImage;

        // Check if operands are constants
        String leftVal = getConstantValueForVar(left);
        String rightVal = getConstantValueForVar(right);

        if (leftVal != null && rightVal != null) {
            // Both are constants, compute the result
            if (leftVal.equals("true") && rightVal.equals("true")) {
                return "true";
            } else {
                return "false";
            }
        } else if (leftVal != null) {
            // Left is constant
            if (leftVal.equals("false")) {
                return "false";  // Short-circuit
            } else {
                return right;    // Result depends only on right operand
            }
        } else if (rightVal != null) {
            // Right is constant
            if (rightVal.equals("false")) {
                return "false";  // Will be false regardless
            } else {
                return left;     // Result depends only on left operand
            }
        }

        // No optimization possible
        return left + " && " + right;
    }

    @Override
    public String visit(OrExpression n) {
        String left = n.f0.f0.tokenImage;
        String right = n.f2.f0.tokenImage;

        // Check if operands are constants
        String leftVal = getConstantValueForVar(left);
        String rightVal = getConstantValueForVar(right);

        if (leftVal != null && rightVal != null) {
            // Both are constants, compute the result
            if (leftVal.equals("true") || rightVal.equals("true")) {
                return "true";
            } else {
                return "false";
            }
        } else if (leftVal != null) {
            // Left is constant
            if (leftVal.equals("true")) {
                return "true";   // Short-circuit
            } else {
                return right;    // Result depends only on right operand
            }
        } else if (rightVal != null) {
            // Right is constant
            if (rightVal.equals("true")) {
                return "true";   // Will be true regardless
            } else {
                return left;     // Result depends only on left operand
            }
        }

        // No optimization possible
        return left + " || " + right;
    }

    @Override
    public String visit(CompareExpression n) {
        String left = n.f0.f0.tokenImage;
        String right = n.f2.f0.tokenImage;

        // Check if operands are constants
        String leftVal = getConstantValueForVar(left);
        String rightVal = getConstantValueForVar(right);

        if (leftVal != null && rightVal != null) {
            try {
                // Both are integer constants, compute the result
                int leftInt = Integer.parseInt(leftVal);
                int rightInt = Integer.parseInt(rightVal);
                return (leftInt <= rightInt) ? "true" : "false";
            } catch (NumberFormatException e) {
                // Not numeric constants
            }
        }

        // Apply constant propagation to operands
        if (leftVal != null) {
            left = leftVal;
        }
        if (rightVal != null) {
            right = rightVal;
        }

        return left + " <= " + right;
    }

    @Override
    public String visit(neqExpression n) {
        String left = n.f0.f0.tokenImage;
        String right = n.f2.f0.tokenImage;

        // Check if operands are constants
        String leftVal = getConstantValueForVar(left);
        String rightVal = getConstantValueForVar(right);

        if (leftVal != null && rightVal != null) {
            try {
                // Both are integer constants, compute the result
                int leftInt = Integer.parseInt(leftVal);
                int rightInt = Integer.parseInt(rightVal);
                return (leftInt != rightInt) ? "true" : "false";
            } catch (NumberFormatException e) {
                // Check if boolean constants
                if (leftVal.equals(rightVal)) {
                    return "false";  // Same values, so not not-equal
                } else {
                    return "true";   // Different values, so not-equal
                }
            }
        }

        // Apply constant propagation to operands
        if (leftVal != null) {
            left = leftVal;
        }
        if (rightVal != null) {
            right = rightVal;
        }

        return left + " != " + right;
    }

    @Override
    public String visit(PrimaryExpression n) {
        return n.f0.accept(this);
    }

    @Override
    public String visit(IntegerLiteral n) {
        return n.f0.tokenImage;
    }

    @Override
    public String visit(TrueLiteral n) {
        return "true";
    }

    @Override
    public String visit(FalseLiteral n) {
        return "false";
    }

    @Override
    public String visit(Identifier n) {
        String varName = n.f0.tokenImage;
        // Try to replace with constant value
        String constValue = getConstantValueForVar(varName);
        return constValue != null ? constValue : varName;
    }

    @Override
    public String visit(ThisExpression n) {
        return "this";
    }

    @Override
    public String visit(ArrayAllocationExpression n) {
        String size = n.f3.f0.tokenImage;
        String constSize = getConstantValueForVar(size);
        return "new int[" + (constSize != null ? constSize : size) + "]";
    }

    @Override
    public String visit(AllocationExpression n) {
        return "new " + n.f1.f0.tokenImage + "()";
    }

    @Override
    public String visit(NotExpression n) {
        String var = n.f1.f0.tokenImage;
        String constValue = getConstantValueForVar(var);

        if (constValue != null) {
            if (constValue.equals("true")) {
                return "false";
            } else if (constValue.equals("false")) {
                return "true";
            }
        }

        return "!" + var;
    }

    @Override
    public String visit(PlusExpression n) {
        String left = n.f0.accept(this);
        String right = n.f2.accept(this);

        // Try to get constant values
        String leftConst = tryGetConstant(left);
        String rightConst = tryGetConstant(right);

        if (leftConst != null && rightConst != null) {
            try {
                // Both are numeric constants, perform addition
                int leftInt = Integer.parseInt(leftConst);
                int rightInt = Integer.parseInt(rightConst);
                return String.valueOf(leftInt + rightInt);
            } catch (NumberFormatException e) {
                // Not numeric constants
            }
        }

        // Replace variables with constants where possible
        if (leftConst != null) {
            left = leftConst;
        }
        if (rightConst != null) {
            right = rightConst;
        }

        // Optimize additions with 0
        if (left.equals("0")) {
            return right;
        }
        if (right.equals("0")) {
            return left;
        }

        return left + " + " + right;
    }

    @Override
    public String visit(MinusExpression n) {
        String left = n.f0.accept(this);
        String right = n.f2.accept(this);

        // Try to get constant values
        String leftConst = tryGetConstant(left);
        String rightConst = tryGetConstant(right);

        if (leftConst != null && rightConst != null) {
            try {
                // Both are numeric constants, perform subtraction
                int leftInt = Integer.parseInt(leftConst);
                int rightInt = Integer.parseInt(rightConst);
                return String.valueOf(leftInt - rightInt);
            } catch (NumberFormatException e) {
                // Not numeric constants
            }
        }

        // Replace variables with constants where possible
        if (leftConst != null) {
            left = leftConst;
        }
        if (rightConst != null) {
            right = rightConst;
        }

        // Optimize subtractions with 0
        if (right.equals("0")) {
            return left;
        }

        return left + " - " + right;
    }

    @Override
    public String visit(TimesExpression n) {
        String left = n.f0.accept(this);
        String right = n.f2.accept(this);

        // Try to get constant values
        String leftConst = tryGetConstant(left);
        String rightConst = tryGetConstant(right);

        if (leftConst != null && rightConst != null) {
            try {
                // Both are numeric constants, perform multiplication
                int leftInt = Integer.parseInt(leftConst);
                int rightInt = Integer.parseInt(rightConst);
                return String.valueOf(leftInt * rightInt);
            } catch (NumberFormatException e) {
                // Not numeric constants
            }
        }

        // Replace variables with constants where possible
        if (leftConst != null) {
            left = leftConst;
        }
        if (rightConst != null) {
            right = rightConst;
        }

        // Optimize multiplications with 0 and 1
        if (left.equals("0") || right.equals("0")) {
            return "0";
        }
        if (left.equals("1")) {
            return right;
        }
        if (right.equals("1")) {
            return left;
        }

        return left + " * " + right;
    }

    @Override
    public String visit(DivExpression n) {
        String left = n.f0.accept(this);
        String right = n.f2.accept(this);

        // Try to get constant values
        String leftConst = tryGetConstant(left);
        String rightConst = tryGetConstant(right);

        if (leftConst != null && rightConst != null) {
            try {
                // Both are numeric constants, perform division
                int leftInt = Integer.parseInt(leftConst);
                int rightInt = Integer.parseInt(rightConst);
                if (rightInt != 0) {  // Avoid division by zero
                    return String.valueOf(leftInt / rightInt);
                }
            } catch (NumberFormatException e) {
                // Not numeric constants
            }
        }

        // Replace variables with constants where possible
        if (leftConst != null) {
            left = leftConst;
        }
        if (rightConst != null) {
            right = rightConst;
        }

        // Optimize division by 1
        if (right.equals("1")) {
            return left;
        }

        return left + " / " + right;
    }

    @Override
    public String visit(ArrayLookup n) {
        String array = n.f0.f0.tokenImage;
        String index = n.f2.f0.tokenImage;

        // Try to get constant value for index
        String indexConst = getConstantValueForVar(index);
        if (indexConst != null) {
            index = indexConst;
        }

        return array + "[" + index + "]";
    }

    @Override
    public String visit(ArrayLength n) {
        return n.f0.f0.tokenImage + ".length";
    }

    @Override
    public String visit(MessageSend n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.f0.f0.tokenImage);
        sb.append(".");
        sb.append(n.f2.f0.tokenImage);
        sb.append("(");

        if (n.f4.present()) {
            sb.append(n.f4.accept(this));
        }

        sb.append(")");
        return sb.toString();
    }

    @Override
    public String visit(ArgList n) {
        StringBuilder sb = new StringBuilder();
        String argName = n.f0.f0.tokenImage;

        // Try to replace with constant
        String constValue = getConstantValueForVar(argName);
        sb.append(constValue != null ? constValue : argName);

        sb.append(n.f1.accept(this));
        return sb.toString();
    }

    @Override
    public String visit(ArgRest n) {
        String argName = n.f1.f0.tokenImage;
        // Try to replace with constant
        String constValue = getConstantValueForVar(argName);
        return ", " + (constValue != null ? constValue : argName);
    }

    // Helper method to get constant value for an expression
    private String getConstantValue(Expression expr) {
        if (expr.f0.choice instanceof PrimaryExpression) {
            PrimaryExpression pe = (PrimaryExpression) expr.f0.choice;
            if (pe.f0.choice instanceof Identifier) {
                Identifier id = (Identifier) pe.f0.choice;
                return getConstantValueForVar(id.f0.tokenImage);
            } else if (pe.f0.choice instanceof IntegerLiteral) {
                return ((IntegerLiteral) pe.f0.choice).f0.tokenImage;
            } else if (pe.f0.choice instanceof TrueLiteral) {
                return "true";
            } else if (pe.f0.choice instanceof FalseLiteral) {
                return "false";
            }
        }
        return null;
    }

    // Helper method to get constant value for a variable
    private String getConstantValueForVar(String varName) {
        if (constantValues != null && constantValues.containsKey(varName)) {
            ConstantValue value = constantValues.get(varName);
            if (value.isConstant) {
                if (value.type == ConstantValue.Type.INTEGER) {
                    return String.valueOf(value.intValue);
                } else if (value.type == ConstantValue.Type.BOOLEAN) {
                    return value.boolValue ? "true" : "false";
                }
            }
        }
        return null;
    }

    // Helper method to try to extract constant from string that might be a variable name or literal
    private String tryGetConstant(String value) {
        try {
            // Check if it's already a numeric literal
            Integer.parseInt(value);
            return value;
        } catch (NumberFormatException e) {
            // Not a numeric literal, check if it's a boolean literal
            if (value.equals("true") || value.equals("false")) {
                return value;
            }
            // Try to get constant value for variable
            return getConstantValueForVar(value);
        }
    }
}