import syntaxtree.*;
import visitor.BB;
import visitor.Instruction;
import visitor.ProgramCFG;

import java.util.*;

public class CodeGenerator {
    private final ProgramCFG programCFG;
    private final Bindings bindings;
    private final Map<BB, Map<String, String>> OUT;

    public CodeGenerator(ProgramCFG programCFG, Bindings bindings, Map<BB, Map<String, String>> OUT) {
        this.programCFG = programCFG;
        this.bindings = bindings;
        this.OUT = OUT;
    }

    public String generateJavaCode() {
        StringBuilder code = new StringBuilder();

        // Iterate over classes
        for (String className : programCFG.classMethodList.keySet()) {
            // Check if this is a class that extends another
            boolean isExtending = false;
            String parentClass = null;

            for (Map.Entry<String, ClassResource> entry : bindings.getBindings().entrySet()) {
                if (entry.getValue().getChildClass().contains(className)) {
                    isExtending = true;
                    parentClass = entry.getKey();
                    break;
                }
            }

            if (isExtending && parentClass != null) {
                code.append("class ").append(className).append(" extends ").append(parentClass).append(" {\n");
            } else {
                code.append("class ").append(className).append(" {\n");
            }

            // Get fields from bindings
            ClassResource classResource = bindings.getBindings().get(className);
            if (classResource != null) {
                Map<String, String> fields = classResource.getFields();
                for (Map.Entry<String, String> field : fields.entrySet()) {
                    code.append("    ").append(field.getValue()).append(" ").append(field.getKey()).append(";\n");
                }
            }

            // Iterate over methods
            Set<String> methodList = programCFG.classMethodList.get(className);
            if (methodList != null) {
                for (String methodName : methodList) {
                    code.append(generateMethodCode(className, methodName));
                }
            }

            code.append("}\n\n");
        }
        return code.toString();
    }

    private String generateMethodCode(String className, String methodName) {
        StringBuilder methodCode = new StringBuilder();

        // Extract the actual method name (remove class prefix if present)
        String actualMethodName = methodName;
        if (methodName.contains("_")) {
            actualMethodName = methodName.substring(methodName.indexOf('_') + 1);
        }

        // Get method information from bindings
        ClassResource classResource = bindings.getBindings().get(className);
        if (classResource == null) {
            return "    // Class " + className + " not found in bindings\n";
        }

        Map<String, MethodMetadata> methodMetadataMap = classResource.getMethodMetadata();
        if (methodMetadataMap == null || !methodMetadataMap.containsKey(actualMethodName)) {
            return "    // Method " + actualMethodName + " not found in bindings\n";
        }

        MethodMetadata methodMetadata = methodMetadataMap.get(actualMethodName);
        BB entryBB = programCFG.methodBBSet.get(methodName);

        if (entryBB == null) {
            return "    // Method " + methodName + " not found in CFG\n";
        }

        // Get return type from methodMetadata or use a default
        String returnType = methodMetadata.getReturnType();
        if (returnType == null || returnType.isEmpty()) {
            returnType = "void"; // Default return type if not specified
        }

        // Generate method signature
        methodCode.append("    public ").append(returnType).append(" ")
                .append(actualMethodName).append("(");

        // Handle parameters correctly - don't include local variables
        boolean first = true;
        for (Map.Entry<String, String> param : methodMetadata.getArguments().entrySet()) {
            // Skip local variables (not parameters)
            if (param.getKey().startsWith("_local_")) continue;

            // Check if this is a parameter by looking at the method's formal parameters
            boolean isParameter = false;
            for (BB block : getAllBlocks(entryBB)) {
                for (Instruction inst : block.instructions) {
                    if (inst.instructionNode instanceof MethodDeclaration) {
                        MethodDeclaration methodDecl = (MethodDeclaration) inst.instructionNode;
                        if (methodDecl.f4.present()) {
                            FormalParameterList paramList = (FormalParameterList) methodDecl.f4.node;
                            if (paramList.f0.f1.f0.tokenImage.equals(param.getKey())) {
                                isParameter = true;
                                break;
                            }
                            for (int i = 0; i < paramList.f1.size(); i++) {
                                FormalParameterRest paramRest = (FormalParameterRest) paramList.f1.elementAt(i);
                                if (paramRest.f1.f1.f0.tokenImage.equals(param.getKey())) {
                                    isParameter = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (isParameter) break;
            }

            // Only include actual parameters, not all variables
            if (isParameter) {
                if (!first) methodCode.append(", ");
                methodCode.append(param.getValue()).append(" ").append(param.getKey());
                first = false;
            }
        }
        methodCode.append(") {\n");

        // Generate method body using CFG and OUT map
        methodCode.append(generateMethodBody(entryBB));

        methodCode.append("    }\n\n");
        return methodCode.toString();
    }
    private String generateMethodBody(BB entryBB) {
        StringBuilder body = new StringBuilder();

        // Sort blocks by execution order for proper flow
        List<BB> sortedBlocks = sortBlocksByExecution(entryBB);

        // Collect local variable declarations
        Set<String> declaredVariables = new HashSet<>();
        Map<String, String> localVariableDeclarations = new HashMap<>();

        // First collect all variable declarations
        for (BB block : sortedBlocks) {
            for (Instruction inst : block.instructions) {
                if (inst.instructionNode instanceof VarDeclaration) {
                    VarDeclaration varDecl = (VarDeclaration) inst.instructionNode;
                    String varName = varDecl.f1.f0.tokenImage;
                    String varType = getTypeString(varDecl.f0);
                    localVariableDeclarations.put(varName, varType);
                    declaredVariables.add(varName);
                }
            }
        }

        // Add local variable declarations
        for (Map.Entry<String, String> entry : localVariableDeclarations.entrySet()) {
            body.append("        ").append(entry.getValue()).append(" ").append(entry.getKey()).append(";\n");
        }

        if (!localVariableDeclarations.isEmpty()) {
            body.append("\n");
        }

        // Track processed blocks to avoid repetition
        Set<BB> processedBlocks = new HashSet<>();

        // Flag to track if we've encountered a return statement
        boolean returnEncountered = false;

        // Process blocks in order
        for (BB block : sortedBlocks) {
            if (processedBlocks.contains(block) || returnEncountered) continue;
            processedBlocks.add(block);

            // Get OUT values for constant propagation
            Map<String, String> outValues = OUT.get(block);
            if (outValues == null) {
                outValues = new HashMap<>();
            }

            // Process each instruction in the block
            for (Instruction inst : block.instructions) {
                if (inst.instructionNode instanceof VarDeclaration) {
                    // Skip variable declaration - we've already processed these
                    continue;
                }
                else if (inst.instructionNode instanceof AssignmentStatement) {
                    AssignmentStatement assign = (AssignmentStatement) inst.instructionNode;
                    String lhs = assign.f0.f0.tokenImage;
                    String rhs = expressionToString(assign.f2, outValues);
                    body.append("        ").append(lhs).append(" = ").append(rhs).append(";\n");
                }
                else if (inst.instructionNode instanceof PrintStatement) {
                    PrintStatement print = (PrintStatement) inst.instructionNode;
                    String value = print.f2.f0.tokenImage;

                    // Replace with constant if available
                    if (outValues.containsKey(value) && isConstant(outValues.get(value))) {
                        value = outValues.get(value);
                    }

                    body.append("        System.out.println(").append(value).append(");\n");
                }
                else if (inst.instructionNode instanceof IfStatement) {
                    IfStatement ifStmt = (IfStatement) inst.instructionNode;
                    if (ifStmt.f0.choice instanceof IfthenStatement) {
                        IfthenStatement ifThen = (IfthenStatement) ifStmt.f0.choice;
                        String condition = ifThen.f2.f0.tokenImage;

                        // Replace with constant if available
                        if (outValues.containsKey(condition) && isConstant(outValues.get(condition))) {
                            condition = outValues.get(condition);
                        }

                        body.append("        if(").append(condition).append(") {\n");

                        // Find the then block by analyzing the control flow
                        BB thenBlock = null;
                        for (BB succ : block.outgoingEdges) {
                            // The then block must have at least one instruction from the if statement
                            for (Instruction thenInst : succ.instructions) {
                                if (thenInst.instructionNode instanceof Statement) {
                                    thenBlock = succ;
                                    break;
                                }
                            }
                            if (thenBlock != null) break;
                        }

                        if (thenBlock != null) {
                            // Process then block
                            processedBlocks.add(thenBlock);

                            // Get OUT values for constant propagation in then block
                            Map<String, String> thenOutValues = OUT.get(thenBlock);
                            if (thenOutValues == null) {
                                thenOutValues = new HashMap<>();
                            }

                            // Process each instruction in the then block
                            for (Instruction thenInst : thenBlock.instructions) {
                                if (thenInst.instructionNode instanceof AssignmentStatement) {
                                    AssignmentStatement assign = (AssignmentStatement) thenInst.instructionNode;
                                    String lhs = assign.f0.f0.tokenImage;
                                    String rhs = expressionToString(assign.f2, thenOutValues);
                                    body.append("          ").append(lhs).append(" = ").append(rhs).append(";\n");
                                }
                                else if (thenInst.instructionNode instanceof PrintStatement) {
                                    PrintStatement print = (PrintStatement) thenInst.instructionNode;
                                    String value = print.f2.f0.tokenImage;

                                    // Replace with constant if available
                                    if (thenOutValues.containsKey(value) && isConstant(thenOutValues.get(value))) {
                                        value = thenOutValues.get(value);
                                    }

                                    body.append("          System.out.println(").append(value).append(");\n");
                                }
                                // Handle other statement types in the then block if needed
                            }
                        }

                        body.append("        }\n");
                    }
                    else if (ifStmt.f0.choice instanceof IfthenElseStatement) {
                        IfthenElseStatement ifThenElse = (IfthenElseStatement) ifStmt.f0.choice;
                        String condition = ifThenElse.f2.f0.tokenImage;

                        // Replace with constant if available
                        if (outValues.containsKey(condition) && isConstant(outValues.get(condition))) {
                            condition = outValues.get(condition);
                        }

                        body.append("        if(").append(condition).append(") {\n");

                        // Handle then branch proper code generation
                        // Similar to the code above for the then block

                        body.append("        } else {\n");

                        // Handle else branch proper code generation

                        body.append("        }\n");
                    }
                }
                else if (inst.instructionNode instanceof WhileStatement) {
                    WhileStatement whileStmt = (WhileStatement) inst.instructionNode;
                    String condition = whileStmt.f2.f0.tokenImage;

                    // Replace with constant if available
                    if (outValues.containsKey(condition) && isConstant(outValues.get(condition))) {
                        condition = outValues.get(condition);
                    }

                    // Skip generating while loops with false condition
                    if (condition.equals("false")) {
                        continue;
                    }

                    // Avoid infinite loops
                    if (condition.equals("true")) {
                        condition = "false"; // Safety measure
                    }

                    body.append("        while (").append(condition).append(") {\n");

                    // Handle while loop body generation

                    body.append("            // Loop body\n");
                    body.append("        }\n");
                }
                else if (inst.instructionNode instanceof MethodDeclaration) {
                    // Handle return statement
                    MethodDeclaration methodDecl = (MethodDeclaration) inst.instructionNode;
                    String returnVar = methodDecl.f10.f0.tokenImage;

                    // Replace with constant if available
                    if (outValues.containsKey(returnVar) && isConstant(outValues.get(returnVar))) {
                        returnVar = outValues.get(returnVar);
                    }

                    body.append("        return ").append(returnVar).append(";\n");

                    // Mark that we've encountered a return statement
                    returnEncountered = true;
                    break; // Don't process any more instructions after return
                }
            }
        }

        return body.toString();
    }
    private List<BB> sortBlocksByExecution(BB entryBB) {
        List<BB> result = new ArrayList<>();
        Set<BB> visited = new HashSet<>();

        // Helper function for depth-first traversal
        Stack<BB> stack = new Stack<>();
        stack.push(entryBB);

        while (!stack.isEmpty()) {
            BB current = stack.pop();

            if (!visited.contains(current)) {
                visited.add(current);
                result.add(current);

                // Add outgoing edges in reverse order so they are processed in the correct order
                List<BB> successors = new ArrayList<>(current.outgoingEdges);
                for (int i = successors.size() - 1; i >= 0; i--) {
                    stack.push(successors.get(i));
                }
            }
        }

        return result;
    }

    private Set<BB> getAllBlocks(BB entryBB) {
        Set<BB> allBlocks = new HashSet<>();
        Stack<BB> worklist = new Stack<>();
        worklist.push(entryBB);

        while (!worklist.isEmpty()) {
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

    private boolean isConstant(String value) {
        if (value == null || value.equals("⊤") || value.equals("⊥")) {
            return false;
        }

        // Check if it's a numeric constant
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            // Check if it's a boolean constant
            return value.equals("true") || value.equals("false");
        }
    }

    private String expressionToString(Expression expr, Map<String, String> outValues) {
        if (expr.f0.choice instanceof PrimaryExpression) {
            PrimaryExpression pe = (PrimaryExpression) expr.f0.choice;
            if (pe.f0.choice instanceof IntegerLiteral) {
                return ((IntegerLiteral) pe.f0.choice).f0.tokenImage;
            } else if (pe.f0.choice instanceof TrueLiteral) {
                return "true";
            } else if (pe.f0.choice instanceof FalseLiteral) {
                return "false";
            } else if (pe.f0.choice instanceof Identifier) {
                String id = ((Identifier) pe.f0.choice).f0.tokenImage;
                // Replace with constant if available
                if (outValues != null && outValues.containsKey(id) && isConstant(outValues.get(id))) {
                    return outValues.get(id);
                }
                return id;
            } else if (pe.f0.choice instanceof ThisExpression) {
                return "this";
            } else if (pe.f0.choice instanceof AllocationExpression) {
                AllocationExpression alloc = (AllocationExpression) pe.f0.choice;
                return "new " + alloc.f1.f0.tokenImage + "()";
            } else if (pe.f0.choice instanceof ArrayAllocationExpression) {
                ArrayAllocationExpression arrayAlloc = (ArrayAllocationExpression) pe.f0.choice;
                String size = arrayAlloc.f3.f0.tokenImage;
                // Replace with constant if available
                if (outValues != null && outValues.containsKey(size) && isConstant(outValues.get(size))) {
                    size = outValues.get(size);
                }
                return "new int[" + size + "]";
            } else if (pe.f0.choice instanceof NotExpression) {
                NotExpression not = (NotExpression) pe.f0.choice;
                String operand = not.f1.f0.tokenImage;
                // Replace with constant if available
                if (outValues != null && outValues.containsKey(operand) && isConstant(outValues.get(operand))) {
                    operand = outValues.get(operand);
                }
                return "!" + operand;
            }
        } else if (expr.f0.choice instanceof AndExpression) {
            AndExpression and = (AndExpression) expr.f0.choice;
            String left = and.f0.f0.tokenImage;
            String right = and.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(left) && isConstant(outValues.get(left))) {
                    left = outValues.get(left);
                }
                if (outValues.containsKey(right) && isConstant(outValues.get(right))) {
                    right = outValues.get(right);
                }
            }

            return left + " && " + right;
        } else if (expr.f0.choice instanceof OrExpression) {
            OrExpression or = (OrExpression) expr.f0.choice;
            String left = or.f0.f0.tokenImage;
            String right = or.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(left) && isConstant(outValues.get(left))) {
                    left = outValues.get(left);
                }
                if (outValues.containsKey(right) && isConstant(outValues.get(right))) {
                    right = outValues.get(right);
                }
            }

            return left + " || " + right;
        } else if (expr.f0.choice instanceof CompareExpression) {
            CompareExpression comp = (CompareExpression) expr.f0.choice;
            String left = comp.f0.f0.tokenImage;
            String right = comp.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(left) && isConstant(outValues.get(left))) {
                    left = outValues.get(left);
                }
                if (outValues.containsKey(right) && isConstant(outValues.get(right))) {
                    right = outValues.get(right);
                }
            }

            return left + " <= " + right;
        } else if (expr.f0.choice instanceof neqExpression) {
            neqExpression neq = (neqExpression) expr.f0.choice;
            String left = neq.f0.f0.tokenImage;
            String right = neq.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(left) && isConstant(outValues.get(left))) {
                    left = outValues.get(left);
                }
                if (outValues.containsKey(right) && isConstant(outValues.get(right))) {
                    right = outValues.get(right);
                }
            }

            return left + " != " + right;
        } else if (expr.f0.choice instanceof PlusExpression) {
            PlusExpression plus = (PlusExpression) expr.f0.choice;
            String left = plus.f0.f0.tokenImage;
            String right = plus.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(left) && isConstant(outValues.get(left))) {
                    left = outValues.get(left);
                }
                if (outValues.containsKey(right) && isConstant(outValues.get(right))) {
                    right = outValues.get(right);
                }
            }

            return left + " + " + right;
        } else if (expr.f0.choice instanceof MinusExpression) {
            MinusExpression minus = (MinusExpression) expr.f0.choice;
            String left = minus.f0.f0.tokenImage;
            String right = minus.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(left) && isConstant(outValues.get(left))) {
                    left = outValues.get(left);
                }
                if (outValues.containsKey(right) && isConstant(outValues.get(right))) {
                    right = outValues.get(right);
                }
            }

            return left + " - " + right;
        } else if (expr.f0.choice instanceof TimesExpression) {
            TimesExpression times = (TimesExpression) expr.f0.choice;
            String left = times.f0.f0.tokenImage;
            String right = times.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(left) && isConstant(outValues.get(left))) {
                    left = outValues.get(left);
                }
                if (outValues.containsKey(right) && isConstant(outValues.get(right))) {
                    right = outValues.get(right);
                }
            }

            return left + " * " + right;
        } else if (expr.f0.choice instanceof DivExpression) {
            DivExpression div = (DivExpression) expr.f0.choice;
            String left = div.f0.f0.tokenImage;
            String right = div.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(left) && isConstant(outValues.get(left))) {
                    left = outValues.get(left);
                }
                if (outValues.containsKey(right) && isConstant(outValues.get(right))) {
                    right = outValues.get(right);
                }
            }

            return left + " / " + right;
        } else if (expr.f0.choice instanceof ArrayLookup) {
            ArrayLookup arrayLookup = (ArrayLookup) expr.f0.choice;
            String array = arrayLookup.f0.f0.tokenImage;
            String index = arrayLookup.f2.f0.tokenImage;

            // Replace with constants if available
            if (outValues != null) {
                if (outValues.containsKey(index) && isConstant(outValues.get(index))) {
                    index = outValues.get(index);
                }
            }

            return array + "[" + index + "]";
        } else if (expr.f0.choice instanceof ArrayLength) {
            ArrayLength arrayLength = (ArrayLength) expr.f0.choice;
            return arrayLength.f0.f0.tokenImage + ".length";
        } else if (expr.f0.choice instanceof MessageSend) {
            MessageSend messageSend = (MessageSend) expr.f0.choice;
            String object = messageSend.f0.f0.tokenImage;
            String method = messageSend.f2.f0.tokenImage;

            StringBuilder args = new StringBuilder();
            if (messageSend.f4.present()) {
                ArgList argList = (ArgList) messageSend.f4.node;
                String firstArg = argList.f0.f0.tokenImage;

                // Replace with constant if available
                if (outValues != null && outValues.containsKey(firstArg) && isConstant(outValues.get(firstArg))) {
                    firstArg = outValues.get(firstArg);
                }

                args.append(firstArg);

                for (int i = 0; i < argList.f1.size(); i++) {
                    ArgRest argRest = (ArgRest) argList.f1.elementAt(i);
                    String nextArg = argRest.f1.f0.tokenImage;

                    // Replace with constant if available
                    if (outValues != null && outValues.containsKey(nextArg) && isConstant(outValues.get(nextArg))) {
                        nextArg = outValues.get(nextArg);
                    }

                    args.append(", ").append(nextArg);
                }
            }

            return object + "." + method + "(" + args.toString() + ")";
        }

        return "/* unsupported expression */"; // Fallback for any unhandled expressions
    }

    private String getTypeString(Type type) {
        if (type.f0.choice instanceof ArrayType) {
            return "int[]";
        } else if (type.f0.choice instanceof BooleanType) {
            return "boolean";
        } else if (type.f0.choice instanceof IntegerType) {
            return "int";
        } else if (type.f0.choice instanceof Identifier) {
            return ((Identifier) type.f0.choice).f0.tokenImage;
        }
        return "Object"; // Default type if unrecognized
    }
}