import java.util.Map;

public class MethodMetadata {
    private String name;
    private Map<String, String> arguments;
    private String originClass;

    public MethodMetadata(String name, Map<String, String> arguments, String originClass) {
        this.name = name;
        this.arguments = arguments;
        this.originClass = originClass;
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
}
