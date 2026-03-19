import syntaxtree.*;
import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.Map;

public class Transformer {
    String classname;
    String methodname;
    Map<String,String> variableMap;
    String callerAssignmentTarget;
    String objectname;

    public Transformer(String classname, String methodname, Map<String,String> variableMap, String callerAssignmentTarget, String objectname) {
        this.classname = classname;
        this.methodname = methodname;
        this.variableMap = variableMap;
        this.callerAssignmentTarget = callerAssignmentTarget;
        this.objectname = objectname;

    }

    public AssignmentStatement transform(AssignmentStatement curr) {
        if (curr == null) return null;
        return new AssignmentStatement(
                transform(curr.f0),
                transform(curr.f2)
        );
    }

    private Expression transform(Expression expr) {
        if (expr == null) return null;

        NodeChoice choice = expr.f0;
        NodeChoice newChoice = null;

        if (choice.choice instanceof PrimaryExpression) {
            PrimaryExpression pe = (PrimaryExpression) choice.choice;
            newChoice = new NodeChoice(transformPrimaryExpression(pe), choice.which);
        } else if (choice.choice instanceof AndExpression) {
            AndExpression ae = (AndExpression) choice.choice;
            newChoice = new NodeChoice(new AndExpression(
                    transform(ae.f0),
                    ae.f1,
                    transform(ae.f2)
            ), choice.which);
        } else if (choice.choice instanceof CompareExpression) {
            CompareExpression ce = (CompareExpression) choice.choice;
            newChoice = new NodeChoice(new CompareExpression(
                    transform(ce.f0),
                    ce.f1,
                    transform(ce.f2)
            ), choice.which);
        } else if (choice.choice instanceof PlusExpression) {
            PlusExpression pe = (PlusExpression) choice.choice;
            newChoice = new NodeChoice(new PlusExpression(
                    transform(pe.f0),
                    pe.f1,
                    transform(pe.f2)
            ), choice.which);
        } else if (choice.choice instanceof MinusExpression) {
            MinusExpression me = (MinusExpression) choice.choice;
            newChoice = new NodeChoice(new MinusExpression(
                    transform(me.f0),
                    me.f1,
                    transform(me.f2)
            ), choice.which);
        } else if (choice.choice instanceof TimesExpression) {
            TimesExpression te = (TimesExpression) choice.choice;
            newChoice = new NodeChoice(new TimesExpression(
                    transform(te.f0),
                    te.f1,
                    transform(te.f2)
            ), choice.which);
        } else {
            // For any other types of expressions, keep them unchanged
            newChoice = choice;
        }

        return new Expression(newChoice);
    }

    private PrimaryExpression transformPrimaryExpression(PrimaryExpression pe) {
        if (pe == null) return null;

        NodeChoice choice = pe.f0;
        NodeChoice newChoice = null;

        if (choice.choice instanceof Identifier) {
            newChoice = new NodeChoice(transform((Identifier)choice.choice), choice.which);
        } else if (choice.choice instanceof NotExpression) {
            NotExpression ne = (NotExpression) choice.choice;
            newChoice = new NodeChoice(new NotExpression(
                    ne.f0,
                    transform(ne.f1)
            ), choice.which);
        } else {
            // For literals (IntegerLiteral, TrueLiteral, FalseLiteral) and ThisExpression
            // keep them unchanged
            newChoice = choice;
        }

        return new PrimaryExpression(newChoice);
    }

    public Identifier transform(Identifier curr) {
        if (curr == null) return null;
        String oldName = curr.f0.tokenImage;
        boolean isLocalVariableOrArgument = variableMap.containsKey(oldName);
        if (!isLocalVariableOrArgument) {
            // This is a field, so prepend "objectname."
            String newName = objectname + "." + oldName;
            return new Identifier(new NodeToken(newName));
        }
        String newName = variableMap.getOrDefault(oldName, oldName);
        return new Identifier(new NodeToken(newName));
    }

    public PrintStatement transform(PrintStatement curr) {
        if (curr == null) return null;
        return new PrintStatement(
                curr.f0,
                curr.f1,
                transform(curr.f2),
                curr.f3,
                curr.f4
        );
    }

    public ArrayAssignmentStatement transform(ArrayAssignmentStatement curr) {
        if (curr == null) return null;
        return new ArrayAssignmentStatement(
                transform(curr.f0),
                transform(curr.f2),
                transform(curr.f5)
        );
    }

    public FieldAssignmentStatement transform(FieldAssignmentStatement curr) {
        if (curr == null) return null;
        return new FieldAssignmentStatement(
                transform(curr.f0),
                transform(curr.f2),
                transform(curr.f4)
        );
    }

    public IfthenStatement transform(IfthenStatement curr) {
        if (curr == null) return null;
        return new IfthenStatement(
                curr.f0,
                curr.f1,
                transform(curr.f2),
                curr.f3,
                transformStatement(curr.f4)
        );
    }

    public IfthenElseStatement transform(IfthenElseStatement curr) {
        if (curr == null) return null;
        return new IfthenElseStatement(
                curr.f0,
                curr.f1,
                transform(curr.f2),
                curr.f3,
                transformStatement(curr.f4),
                curr.f5,
                transformStatement(curr.f6)
        );
    }

    public WhileStatement transform(WhileStatement curr) {
        if (curr == null) return null;
        return new WhileStatement(
                curr.f0,
                curr.f1,
                transform(curr.f2),
                curr.f3,
                transformStatement(curr.f4)
        );
    }

    private Statement transformStatement(Statement stmt) {
        if (stmt == null) return null;

        NodeChoice choice = stmt.f0;
        NodeChoice newChoice = null;

        if (choice.choice instanceof AssignmentStatement) {
            newChoice = new NodeChoice(
                    transform((AssignmentStatement)choice.choice),
                    choice.which
            );
        } else if (choice.choice instanceof ArrayAssignmentStatement) {
            newChoice = new NodeChoice(
                    transform((ArrayAssignmentStatement)choice.choice),
                    choice.which
            );
        } else if (choice.choice instanceof IfthenStatement) {
            newChoice = new NodeChoice(
                    transform((IfthenStatement)choice.choice),
                    choice.which
            );
        } else if (choice.choice instanceof IfthenElseStatement) {
            newChoice = new NodeChoice(
                    transform((IfthenElseStatement)choice.choice),
                    choice.which
            );
        } else if (choice.choice instanceof WhileStatement) {
            newChoice = new NodeChoice(
                    transform((WhileStatement)choice.choice),
                    choice.which
            );
        } else if (choice.choice instanceof PrintStatement) {
            newChoice = new NodeChoice(
                    transform((PrintStatement)choice.choice),
                    choice.which
            );
        } else {
            // For any other types of statements, keep them unchanged
            newChoice = choice;
        }

        return new Statement(newChoice);
    }
    private NodeListOptional transformStatementList(NodeListOptional statements) {
        if (statements == null) return null;
        NodeListOptional newStatements = new NodeListOptional();

        for (Node node : statements.nodes) {
            if (node instanceof Statement) {
                Statement stmt = (Statement) node;
                NodeChoice choice = stmt.f0;

                if (choice.choice instanceof AssignmentStatement) {
                    newStatements.addNode(new Statement(new NodeChoice(
                            transform((AssignmentStatement)choice.choice),
                            choice.which
                    )));
                } else if (choice.choice instanceof ArrayAssignmentStatement) {
                    newStatements.addNode(new Statement(new NodeChoice(
                            transform((ArrayAssignmentStatement)choice.choice),
                            choice.which
                    )));
                } else if (choice.choice instanceof IfStatement) {
                    // Handle if statements...
                } else if (choice.choice instanceof WhileStatement) {
                    newStatements.addNode(new Statement(new NodeChoice(
                            transform((WhileStatement)choice.choice),
                            choice.which
                    )));
                } else if (choice.choice instanceof PrintStatement) {
                    newStatements.addNode(new Statement(new NodeChoice(
                            transform((PrintStatement)choice.choice),
                            choice.which
                    )));
                }
            }
        }
        return newStatements;
    }
    public AssignmentStatement transform(MethodDeclaration curr) {
        if (curr == null) return null;

        // The return statement should be present in the method body
        if (curr.f10 == null) {
            System.out.println("Warning: No return statement found in method.");
            return null;
        }

        // Get the return identifier from the method
        Identifier returnIdentifier = curr.f10;
        if (returnIdentifier != null) {
            String originalReturnVarName = returnIdentifier.f0.tokenImage;

            // Lookup the transformed variable name using variableMap
            String transformedReturnVarName = variableMap.getOrDefault(originalReturnVarName, originalReturnVarName);
            Identifier transformedReturnId = new Identifier(new NodeToken(transformedReturnVarName));

            // Here we assume that the message send was part of an assignment statement
            // So we must replace the RHS of that assignment
            if (callerAssignmentTarget != null) { // Ensure we have a target variable
                Identifier targetVar = new Identifier(new NodeToken(callerAssignmentTarget));

                // Construct an assignment statement to replace the method call
                AssignmentStatement assignment = new AssignmentStatement(
                        targetVar,
                        new Expression(new NodeChoice(new PrimaryExpression(new NodeChoice(transformedReturnId, 3)), 11))
                );

                //System.out.println("Replacing return value: " + transformedReturnVarName + " -> " + callerAssignmentTarget);
                return assignment;
            } else {
                //System.out.println("Error: No valid assignment target found for inlined return.");
                return null;
            }
        }

        return null;
    }

}