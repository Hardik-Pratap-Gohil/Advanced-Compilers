import syntaxtree.*;
import visitor.GJNoArguDepthFirst;

public class CodePrinter extends GJNoArguDepthFirst<String> {

    @Override
    public String visit(NodeList n) {
        StringBuilder sb = new StringBuilder();
        for (Node node : n.nodes) {
            sb.append(node.accept(this)).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String visit(NodeListOptional n) {
        if (!n.present()) return "";
        StringBuilder sb = new StringBuilder();
        for (Node node : n.nodes) {
            sb.append(node.accept(this)).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String visit(NodeOptional n) {
        return n.present() ? n.node.accept(this) : "";
    }

    @Override
    public String visit(Goal n) {
        return n.f0.accept(this) + n.f1.accept(this) + n.f2.accept(this);
    }

    @Override
    public String visit(MainClass n) {
        return "class " + n.f1.accept(this) + " {\n" +
                "    public static void main(String[] " + n.f11.accept(this) + ") {\n" +
                n.f14.accept(this) +
                n.f15.accept(this) +
                "    }\n}\n";
    }

    @Override
    public String visit(ClassDeclaration n) {
        return "class " + n.f1.accept(this) + " {\n" +
                n.f3.accept(this) +  // Variable declarations
                n.f4.accept(this) +  // Method declarations
                "}\n";
    }

    @Override
    public String visit(MethodDeclaration n) {
        return "    public " + n.f1.accept(this) + " " + n.f2.accept(this) + "(" +
                n.f4.accept(this) + ") {\n" +
                n.f7.accept(this) +  // Variable declarations
                n.f8.accept(this) +  // Statements
                "        return " + n.f10.accept(this) + ";\n" +
                "    }\n";
    }

    @Override
    public String visit(VarDeclaration n) {
        return "    " + n.f0.accept(this) + " " + n.f1.accept(this) + ";";
    }

    @Override
    public String visit(AssignmentStatement n) {
        return "        " + n.f0.accept(this) + " = " + n.f2.accept(this) + ";";
    }

    @Override
    public String visit(PrintStatement n) {
        return "        System.out.println(" + n.f2.accept(this) + ");";
    }

    @Override
    public String visit(Expression n) {
        return n.f0.accept(this);
    }

    @Override
    public String visit(Identifier n) {
        return n.f0.tokenImage;
    }

    @Override
    public String visit(IntegerLiteral n) {
        return n.f0.tokenImage;
    }

    @Override
    public String visit(BooleanType n) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n) {
        return "int";
    }
}
