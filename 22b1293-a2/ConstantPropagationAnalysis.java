import syntaxtree.*;
import visitor.BB;
import visitor.Instruction;
import visitor.ProgramCFG;

import java.util.*;

public class ConstantPropagationAnalysis {
    private Map<BB, Map<String, String>> IN;
    private Map<BB, Map<String, String>> OUT;

    public Map<BB, Map<String, String>> getOUTMap() {
        return OUT;
    }

    public ConstantPropagationAnalysis() {
        IN = new HashMap<>();
        OUT = new HashMap<>();
    }

    // Main method to run constant propagation on the CFG for each method
    public void analyze(ProgramCFG programCFG, Bindings bindings) {
        for (String className : programCFG.classMethodList.keySet()) {
            Set<String> methodList = programCFG.classMethodList.get(className);
            for (String methodName : methodList) {
                analyzeMethod(className, methodName, programCFG, bindings);
//                BB currentMethodBB = programCFG.methodBBSet.get(methodName);
//                BB.printBB(currentMethodBB);
            }
        }
    }

    // Analyze each method's CFG
    private void analyzeMethod(String className, String methodName, ProgramCFG programCFG, Bindings bindings) {
        BB entryBB = programCFG.methodBBSet.get(methodName);

        // Get method variables and arguments
        MethodMetadata methodMetadata = bindings.getBindings()
                .get(className)
                .getMethodMetadata()
                .get(methodName.substring(methodName.indexOf('_') + 1));
        Map<String, String> variables = methodMetadata.getArguments();

        // Get all blocks in the method's CFG
        Set<BB> allBlocks = getAllBlocks(entryBB);

        // Initialize IN and OUT for all blocks
        initializeMaps(allBlocks, variables);

        // Perform fixed-point iteration for constant propagation
        performFixedPointIteration(allBlocks, variables);

        // Update the AST based on the constant propagation results
        updateCFG(allBlocks);
        //printOutMaps();
    }

    // Get all blocks from the entry basic block using DFS
    private Set<BB> getAllBlocks(BB entryBB) {
        Set<BB> allBlocks = new HashSet<>();
        Stack<BB> worklist = new Stack<>();
        worklist.push(entryBB);

        while (!worklist.empty()) {
            BB current = worklist.pop();
            if (!allBlocks.contains(current)) {
                allBlocks.add(current);
                for (BB succ : current.outgoingEdges) {
                    worklist.push(succ);
                }
            }
        }
        return allBlocks;
    }

    // Initialize IN and OUT for each block
    private void initializeMaps(Set<BB> allBlocks, Map<String, String> variables) {
        for (BB block : allBlocks) {
            IN.put(block, new HashMap<>());
            OUT.put(block, new HashMap<>());
            for (String var : variables.keySet()) {
                IN.get(block).put(var, "⊤"); // Unknown (⊤) at start
                OUT.get(block).put(var, "⊤");
            }
        }
    }

    // Perform constant propagation using fixed-point iteration
    private void performFixedPointIteration(Set<BB> allBlocks, Map<String, String> variables) {
        boolean changed;
        do {
            changed = false;

            for (BB block : allBlocks) {
                Map<String, String> oldIN = new HashMap<>(IN.get(block));
                Map<String, String> oldOUT = new HashMap<>(OUT.get(block));

                // Compute IN[block] by meeting over all predecessors
                if (!block.incomingEdges.isEmpty()) {
                    for (String var : variables.keySet()) {
                        String meetResult = computeMeetOverPredecessors(block, var);
                        IN.get(block).put(var, meetResult);
                    }
                }

                // Copy IN to OUT as initial values
                OUT.get(block).putAll(new HashMap<>(IN.get(block)));

                // Apply flow-sensitive transfer function instruction by instruction
                applyFlowSensitiveTransferFunction(block);

                // Check if values changed
                if (!oldIN.equals(IN.get(block)) || !oldOUT.equals(OUT.get(block))) {
                    changed = true;
                }
            }
        } while (changed);
    }

    // Compute meet over predecessors
    private String computeMeetOverPredecessors(BB block, String var) {
        String meetResult = null;
        for (BB pred : block.incomingEdges) {
            String predValue = OUT.get(pred).get(var);
            if (meetResult == null) {
                meetResult = predValue;
            } else {
                meetResult = meet(meetResult, predValue);
            }
        }
        return meetResult;
    }

    // Flow-sensitive transfer function that processes each instruction
    private void applyFlowSensitiveTransferFunction(BB block) {
        // Start with IN values
        Map<String, String> currentValues = new HashMap<>(IN.get(block));

        // Process each instruction in sequence
        for (Instruction inst : block.instructions) {
            if (inst.instructionNode instanceof AssignmentStatement) {
                AssignmentStatement assign = (AssignmentStatement) inst.instructionNode;
                String lhs = assign.f0.f0.tokenImage;
                String newValue = evaluateExpression(assign.f2, currentValues);

                // Update currentValues with the new value
                currentValues.put(lhs, newValue);
            }
        }

        // Set OUT to the final values after processing all instructions
        OUT.put(block, currentValues);
    }

    // Meet operation for constant propagation
    private String meet(String val1, String val2) {
        if (val1.equals(val2)) return val1;  // Same constant → keep it
        if (val1.equals("⊥") || val2.equals("⊥")) return "⊥";  // Bottom propagates
        if (val1.equals("⊤")) return val2;  // If one is unknown, use the other value
        if (val2.equals("⊤")) return val1;  // If one is unknown, use the other value
        return "⊥";  // Conflicting constants → Bottom
    }


    // Evaluate expressions during constant propagation
    private String evaluateExpression(Expression expr, Map<String, String> values) {
        if (expr.f0.choice instanceof PrimaryExpression) {
            PrimaryExpression pe = (PrimaryExpression) expr.f0.choice;

            // Integer Literal: Return the constant value
            if (pe.f0.choice instanceof IntegerLiteral) {
                return ((IntegerLiteral) pe.f0.choice).f0.tokenImage;
            }

            // True Literal: Return "true"
            else if (pe.f0.choice instanceof TrueLiteral) {
                return "true";
            }

            // False Literal: Return "false"
            else if (pe.f0.choice instanceof FalseLiteral) {
                return "false";
            }

            // Identifier: Look up the variable's value in current values
            else if (pe.f0.choice instanceof Identifier) {
                String varName = ((Identifier) pe.f0.choice).f0.tokenImage;
                return values.getOrDefault(varName, "⊤"); // Use "⊤" if unknown
            }

            // Negation Expression (!x)
            else if (pe.f0.choice instanceof NotExpression) {
                NotExpression notExpr = (NotExpression) pe.f0.choice;
                String value = values.getOrDefault(notExpr.f1.f0.tokenImage, "⊤"); // Get variable value
                if ("true".equals(value)) return "false";
                if ("false".equals(value)) return "true";
                return "⊤"; // Unknown
            }
        }

        // Binary Expressions
        else if (expr.f0.choice instanceof PlusExpression) {
            PlusExpression plusExpr = (PlusExpression) expr.f0.choice;
            return evaluateBinaryExpression("+", plusExpr.f0, plusExpr.f2, values);
        } else if (expr.f0.choice instanceof MinusExpression) {
            MinusExpression minusExpr = (MinusExpression) expr.f0.choice;
            return evaluateBinaryExpression("-", minusExpr.f0, minusExpr.f2, values);
        } else if (expr.f0.choice instanceof TimesExpression) {
            TimesExpression timesExpr = (TimesExpression) expr.f0.choice;
            return evaluateBinaryExpression("*", timesExpr.f0, timesExpr.f2, values);
        } else if (expr.f0.choice instanceof DivExpression) {
            DivExpression divExpr = (DivExpression) expr.f0.choice;
            return evaluateBinaryExpression("/", divExpr.f0, divExpr.f2, values);
        } else if (expr.f0.choice instanceof AndExpression) {
            AndExpression andExpr = (AndExpression) expr.f0.choice;
            return evaluateBinaryExpression("&&", andExpr.f0, andExpr.f2, values);
        } else if (expr.f0.choice instanceof OrExpression) {
            OrExpression orExpr = (OrExpression) expr.f0.choice;
            return evaluateBinaryExpression("||", orExpr.f0, orExpr.f2, values);
        } else if (expr.f0.choice instanceof CompareExpression) {
            CompareExpression cmpExpr = (CompareExpression) expr.f0.choice;
            return evaluateBinaryExpression("<=", cmpExpr.f0, cmpExpr.f2, values);
        } else if (expr.f0.choice instanceof neqExpression) {
            neqExpression neqExpr = (neqExpression) expr.f0.choice;
            return evaluateBinaryExpression("!=", neqExpr.f0, neqExpr.f2, values);
        }

        // If the expression is not handled, return unknown ("⊤")
        return "⊤";
    }
//    private String evaluateBinaryExpression(String op, Identifier left, Identifier right, Map<String, String> values) {
//        String leftVal = values.getOrDefault(left.f0.tokenImage, "⊤");
//        String rightVal = values.getOrDefault(right.f0.tokenImage, "⊤");
//
////        // If either operand is unknown, return "⊤"
////        if (leftVal.equals("⊤") || rightVal.equals("⊤")) return "⊤";
////        if (leftVal.equals("⊥") || rightVal.equals("⊥")) return "⊥";
//        if (isNumeric(leftVal) && isNumeric(rightVal)) {
//            int leftInt = Integer.parseInt(leftVal);
//            int rightInt = Integer.parseInt(rightVal);
//            switch (op) {
//                case "+":
//                    return String.valueOf(leftInt + rightInt);
//                case "-":
//                    return String.valueOf(leftInt - rightInt);
//                case "*":
//                    return String.valueOf(leftInt * rightInt);
//                case "/":
//                    if (rightInt == 0) return "⊥"; // Division by zero
//                    return String.valueOf(leftInt / rightInt);
//                case "<=":
//                    return String.valueOf(leftInt <= rightInt);
//            }
//        }
//
//        // For boolean operations
//        if (isBoolean(leftVal)&&isBoolean(rightVal)){
//            boolean leftBool = Boolean.parseBoolean(leftVal);
//            boolean rightBool = Boolean.parseBoolean(rightVal);
//            switch (op) {
//                case "&&":
//                    return String.valueOf(leftBool && rightBool);
//                case "||":
//                    return String.valueOf(leftBool || rightBool);
//                case "!=":
//                    return String.valueOf(!leftVal.equals(rightVal));
//
//            }
//        }
//
//        return leftVal + " " + op + " " + rightVal;
//
//    }
//    private boolean isBoolean(String value) {
//        return "true".equals(value) || "false".equals(value);
//    }
    private String evaluateBinaryExpression(String op, Identifier left, Identifier right, Map<String, String> values) {
        String leftVal = values.getOrDefault(left.f0.tokenImage, "⊤");
        String rightVal = values.getOrDefault(right.f0.tokenImage, "⊤");

        // If either operand is unknown, return "⊤"
        if (leftVal.equals("⊤") || rightVal.equals("⊤")) return "⊤";
        if (leftVal.equals("⊥") || rightVal.equals("⊥")) return "⊥";

        try {
            // For numeric operations
            if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<=")) {
                int leftInt = Integer.parseInt(leftVal);
                int rightInt = Integer.parseInt(rightVal);

                switch (op) {
                    case "+":
                        return String.valueOf(leftInt + rightInt);
                    case "-":
                        return String.valueOf(leftInt - rightInt);
                    case "*":
                        return String.valueOf(leftInt * rightInt);
                    case "/":
                        if (rightInt == 0) return "⊥"; // Division by zero
                        return String.valueOf(leftInt / rightInt);
                    case "<=":
                        return String.valueOf(leftInt <= rightInt);
                }
            }
            // For boolean operations
            else if (op.equals("&&") || op.equals("||")) {
                boolean leftBool = leftVal.equals("true");
                boolean rightBool = rightVal.equals("true");

                switch (op) {
                    case "&&":
                        return String.valueOf(leftBool && rightBool);
                    case "||":
                        return String.valueOf(leftBool || rightBool);
                }
            }
            // For equality comparison
            else if (op.equals("!=")) {
                return String.valueOf(!leftVal.equals(rightVal));
            }

            return "⊤"; // Unknown operation
        } catch (Exception e) {
            return "⊤"; // Catch parsing errors
        }
    }

    // Update the CFG with constant values
    private void updateCFG(Set<BB> allBlocks) {
        for (BB block : allBlocks) {
            // Start with IN values
            Map<String, String> currentValues = new HashMap<>(IN.get(block));

            // Process each instruction and update the AST
            for (Instruction inst : block.instructions) {
                if (inst.instructionNode instanceof AssignmentStatement) {
                    AssignmentStatement assign = (AssignmentStatement) inst.instructionNode;
                    String lhs = assign.f0.f0.tokenImage;

                    // Evaluate the RHS with current values
                    String rhsValue = evaluateExpression(assign.f2, currentValues);

                    // If RHS evaluates to a constant, replace with that constant in the AST
                    if (isConstant(rhsValue)) {
                        currentValues.put(lhs, rhsValue);
                        assign.f2 = createConstantExpression(rhsValue);
                    }
                    else {
                        Expression partiallyUpdatedExpr = partiallyUpdateExpressionAST(assign.f2, currentValues);

                        // Only update if we got something different than the original
                        if (!partiallyUpdatedExpr.toString().equals(assign.f2.toString())) {
                            assign.f2 = partiallyUpdatedExpr;
                        }

                        // Update the current value mapping
                        currentValues.put(lhs, rhsValue);                    }
                }
                else if (inst.instructionNode instanceof ArrayAssignmentStatement) {
                    ArrayAssignmentStatement arrayAssign = (ArrayAssignmentStatement) inst.instructionNode;
                    String arrayName = arrayAssign.f0.f0.tokenImage;
                    String indexVar = arrayAssign.f2.f0.tokenImage;
                    String valueVar = arrayAssign.f5.f0.tokenImage;

                    // Replace array index if constant
                    if (currentValues.containsKey(indexVar) && isConstant(currentValues.get(indexVar))) {
                        arrayAssign.f2.f0 = new NodeToken(currentValues.get(indexVar));
                    }

                    // Replace assigned value if constant
                    if (currentValues.containsKey(valueVar) && isConstant(currentValues.get(valueVar))) {
                        arrayAssign.f5.f0 = new NodeToken(currentValues.get(valueVar));
                    }
                }
                else if (inst.instructionNode instanceof MethodDeclaration) {
                    MethodDeclaration methodDeclaration = (MethodDeclaration) inst.instructionNode;

                    if (methodDeclaration.f10 != null) {
                        String returnVar = methodDeclaration.f10.f0.tokenImage;
                        String returnVal = currentValues.get(returnVar);

                        // ✅ Only replace if `returnVar` is *always* a constant in all paths
                        boolean isDefinitelyConstant = true;
                        for (BB blockk : OUT.keySet()) {
                            if (!OUT.get(blockk).get(returnVar).equals(returnVal)) {
                                isDefinitelyConstant = false;
                                break;
                            }
                        }

                        // Only replace if `a` is always constant in all execution paths
                        if (isDefinitelyConstant && isConstant(returnVal)) {
                            methodDeclaration.f10.f0 = new NodeToken(returnVal);
                        }
                    }

                } else if (inst.instructionNode instanceof FieldAssignmentStatement) {
                    FieldAssignmentStatement fieldAssign = (FieldAssignmentStatement) inst.instructionNode;
                    String fieldObject = fieldAssign.f0.f0.tokenImage;  // `obj`
                    String fieldName = fieldAssign.f2.f0.tokenImage;   // `x`
                    String assignedVar = fieldAssign.f4.f0.tokenImage; // `a`

                    // If the assigned value is constant, propagate it
                    if (currentValues.containsKey(assignedVar) && isConstant(currentValues.get(assignedVar))) {
                        fieldAssign.f4.f0 = new NodeToken(currentValues.get(assignedVar));
                    }
                }  else if (inst.instructionNode instanceof PrintStatement) {
                    PrintStatement printStmt = (PrintStatement) inst.instructionNode;
                    String printVar = printStmt.f2.f0.tokenImage;

                    // If the variable being printed is a constant, replace it
                    if (currentValues.containsKey(printVar) && isConstant(currentValues.get(printVar))) {
                        printStmt.f2.f0 = new NodeToken(currentValues.get(printVar));
                    }
                }
            }
        }
    }

    private Expression partiallyUpdateExpressionAST(Expression expr, Map<String, String> values) {
        if (expr.f0.choice instanceof PrimaryExpression) {
            PrimaryExpression pe = (PrimaryExpression) expr.f0.choice;

            if (pe.f0.choice instanceof Identifier) {
                String varName = ((Identifier) pe.f0.choice).f0.tokenImage;
                String constVal = values.getOrDefault(varName, "⊤");

                // If the variable has a constant value, replace it
                if (isConstant(constVal)) {
                    return createConstantExpression(constVal);
                }
            }
            else if (pe.f0.choice instanceof ArrayAllocationExpression){
                ArrayAllocationExpression arrayAlloc = (ArrayAllocationExpression) pe.f0.choice;
                String sizeVar = arrayAlloc.f3.f0.tokenImage;

                // 🔹 If the size is constant, replace `new int[x]` → `new int[CONST]`
                if (values.containsKey(sizeVar) && isConstant(values.get(sizeVar))) {
                    arrayAlloc.f3.f0 = new NodeToken(values.get(sizeVar));
                }
                return expr;


            }

            // Return the original expression if it's already a constant or unknown variable
            return expr;
        }
        else if (expr.f0.choice instanceof ArrayLookup) {
            ArrayLookup arrLookup = (ArrayLookup) expr.f0.choice;
            String arrayName = arrLookup.f0.f0.tokenImage;
            String indexVar = arrLookup.f2.f0.tokenImage;

            // ✅ If the index is constant, replace only the index
            if (values.containsKey(indexVar) && isConstant(values.get(indexVar))) {
                return createExpressionFromString(arrayName + "[" + values.get(indexVar) + "]");
            }
        }
        // Handle binary expressions
        else if (expr.f0.choice instanceof PlusExpression) {
            PlusExpression plusExpr = (PlusExpression) expr.f0.choice;
            PlusExpression newExpr = new PlusExpression(
                    updateIdentifierIfConstant(plusExpr.f0, values),
                    plusExpr.f1,
                    updateIdentifierIfConstant(plusExpr.f2, values)
            );
            return new Expression(new NodeChoice(newExpr, 0));
        }
        else if (expr.f0.choice instanceof MinusExpression) {
            MinusExpression minusExpr = (MinusExpression) expr.f0.choice;
            MinusExpression newExpr = new MinusExpression(
                    updateIdentifierIfConstant(minusExpr.f0, values),
                    minusExpr.f1,
                    updateIdentifierIfConstant(minusExpr.f2, values)
            );
            return new Expression(new NodeChoice(newExpr, 1));
        }
        else if (expr.f0.choice instanceof TimesExpression) {
            TimesExpression timesExpr = (TimesExpression) expr.f0.choice;
            TimesExpression newExpr = new TimesExpression(
                    updateIdentifierIfConstant(timesExpr.f0, values),
                    timesExpr.f1,
                    updateIdentifierIfConstant(timesExpr.f2, values)
            );
            return new Expression(new NodeChoice(newExpr, 2));
        }
        else if (expr.f0.choice instanceof DivExpression) {
            DivExpression divExpr = (DivExpression) expr.f0.choice;
            DivExpression newExpr = new DivExpression(
                    updateIdentifierIfConstant(divExpr.f0, values),
                    divExpr.f1,
                    updateIdentifierIfConstant(divExpr.f2, values)
            );
            return new Expression(new NodeChoice(newExpr, 3));
        }
        else if (expr.f0.choice instanceof AndExpression) {
            AndExpression andExpr = (AndExpression) expr.f0.choice;
            AndExpression newExpr = new AndExpression(
                    updateIdentifierIfConstant(andExpr.f0, values),
                    andExpr.f1,
                    updateIdentifierIfConstant(andExpr.f2, values)
            );
            return new Expression(new NodeChoice(newExpr, 4));
        }
        else if (expr.f0.choice instanceof OrExpression) {
            OrExpression orExpr = (OrExpression) expr.f0.choice;
            OrExpression newExpr = new OrExpression(
                    updateIdentifierIfConstant(orExpr.f0, values),
                    orExpr.f1,
                    updateIdentifierIfConstant(orExpr.f2, values)
            );
            return new Expression(new NodeChoice(newExpr, 5));
        }
        else if (expr.f0.choice instanceof CompareExpression) {
            CompareExpression cmpExpr = (CompareExpression) expr.f0.choice;
            CompareExpression newExpr = new CompareExpression(
                    updateIdentifierIfConstant(cmpExpr.f0, values),
                    cmpExpr.f1,
                    updateIdentifierIfConstant(cmpExpr.f2, values)
            );
            return new Expression(new NodeChoice(newExpr, 6));
        }
        else if (expr.f0.choice instanceof neqExpression) {
            neqExpression neqExpr = (neqExpression) expr.f0.choice;
            neqExpression newExpr = new neqExpression(
                    updateIdentifierIfConstant(neqExpr.f0, values),
                    neqExpr.f1,
                    updateIdentifierIfConstant(neqExpr.f2, values)
            );
            return new Expression(new NodeChoice(newExpr, 7));
        }

        // Return the original expression if type not handled
        return expr;
    }

    // Helper method to update an identifier with a constant if available
    private Identifier updateIdentifierIfConstant(Identifier id, Map<String, String> values) {
        String varName = id.f0.tokenImage;
        String constVal = values.getOrDefault(varName, "⊤");

        // If the variable has a constant value, create a new identifier with that value
        if (isConstant(constVal)) {
            return new Identifier(new NodeToken(constVal));
        }

        // Otherwise return the original identifier
        return id;
    }

    // Create a constant expression from a string value
    private Expression createExpressionFromString(String value) {
        // If the value is a constant, create a constant expression
        if (isNumeric(value)) {
            return createConstantExpression(value);
        }

        // Otherwise, create an expression with an identifier
        Identifier id = new Identifier(new NodeToken(value));
        PrimaryExpression pe = new PrimaryExpression(new NodeChoice(id, 3));
        return new Expression(new NodeChoice(pe, 11));
    }

    private Expression createConstantExpression(String value) {
        NodeChoice primaryChoice;
        if (isNumeric(value)) {
            primaryChoice = new NodeChoice(new IntegerLiteral(new NodeToken(value)), 0);
        } else if (value.equals("true")) {
            primaryChoice = new NodeChoice(new TrueLiteral(), 1);
        } else if (value.equals("false")) {
            primaryChoice = new NodeChoice(new FalseLiteral(), 2);
        } else {
            throw new IllegalArgumentException("Invalid constant value: " + value);
        }
        PrimaryExpression pe = new PrimaryExpression(primaryChoice);
        return new Expression(new NodeChoice(pe, 11));
    }

    // Checks if a value is a constant (not unknown or error)
    private boolean isConstant(String value) {
        return value != null && !value.equals("⊤") && !value.equals("⊥");
    }

    // Checks if a value is numeric
    private boolean isNumeric(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Print the OUT maps for debugging
    public void printOutMaps() {
        System.out.println("=== OUT Maps for Each Basic Block ===");
        for (Map.Entry<BB, Map<String, String>> entry : OUT.entrySet()) {
            BB block = entry.getKey();
            Map<String, String> outMap = entry.getValue();

            System.out.println("Basic Block: " + block.name);
            System.out.print("{");
            boolean first = true;
            for (Map.Entry<String, String> varEntry : outMap.entrySet()) {
                if (!first) {
                    System.out.print(", ");
                }
                first = false;
                System.out.print(varEntry.getKey() + " -> " + varEntry.getValue());
            }
            System.out.println("}");
            System.out.println("--------------------------------");
        }

        // Also print instruction-level flow-sensitive analysis
        System.out.println("\n=== Flow-Sensitive Analysis ===");
        for (BB block : IN.keySet()) {
            System.out.println("Basic Block: " + block.name);

            // Start with IN values
            Map<String, String> currentValues = new HashMap<>(IN.get(block));
            System.out.println("IN: " + mapToString(currentValues));

            // Show value changes after each instruction
            for (Instruction inst : block.instructions) {
                if (inst.instructionNode instanceof AssignmentStatement) {
                    AssignmentStatement assign = (AssignmentStatement) inst.instructionNode;
                    String lhs = assign.f0.f0.tokenImage;
                    String prevValue = currentValues.getOrDefault(lhs, "⊤");
                    String newValue = evaluateExpression(assign.f2, currentValues);

                    // Update current values
                    currentValues.put(lhs, newValue);

                    // Print the instruction and its effect
                    System.out.println("  " + lhs + " = " + expressionToString(assign.f2) +
                            " (Changed " + lhs + " from " + prevValue + " to " + newValue + ")");
                    System.out.println("  Current values: " + mapToString(currentValues));
                }
            }

            System.out.println("OUT: " + mapToString(OUT.get(block)));
            System.out.println("--------------------------------");
        }
    }

    // Helper method to convert expression to string for printing
    private String expressionToString(Expression expr) {
        if (expr.f0.choice instanceof PrimaryExpression) {
            PrimaryExpression pe = (PrimaryExpression) expr.f0.choice;
            if (pe.f0.choice instanceof IntegerLiteral) {
                return ((IntegerLiteral) pe.f0.choice).f0.tokenImage;
            } else if (pe.f0.choice instanceof TrueLiteral) {
                return "true";
            } else if (pe.f0.choice instanceof FalseLiteral) {
                return "false";
            } else if (pe.f0.choice instanceof Identifier) {
                return ((Identifier) pe.f0.choice).f0.tokenImage;
            }
        } else if (expr.f0.choice instanceof PlusExpression) {
            PlusExpression plusExpr = (PlusExpression) expr.f0.choice;
            return plusExpr.f0.f0.tokenImage + " + " + plusExpr.f2.f0.tokenImage;
        } else if (expr.f0.choice instanceof MinusExpression) {
            MinusExpression minusExpr = (MinusExpression) expr.f0.choice;
            return minusExpr.f0.f0.tokenImage + " - " + minusExpr.f2.f0.tokenImage;
        }
        // Add more cases as needed for other expression types

        return expr.toString(); // Fallback
    }

    // Helper method to convert map to string for printing
    private String mapToString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}