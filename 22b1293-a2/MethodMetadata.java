import java.util.Map;

public class MethodMetadata {
    private String name;
    private Map<String, String> arguments;
    private String originClass;
    private String returnType;

    public MethodMetadata(String name, Map<String, String> arguments, String originClass, String returnType) {
        this.name = name;
        this.arguments = arguments;
        this.originClass = originClass;
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getArguments() {
        return arguments;
    }

    public String getOriginClass() {
        return originClass;
    }

    public void addVariable(String varName, String declaredType) {
        arguments.put(varName, declaredType);
    }

    public String getReturnType() {
        return returnType;
    }
}
