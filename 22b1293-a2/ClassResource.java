import java.util.*;

public class ClassResource {
    private List<String> childClass; // Child classes
    private Map<String, String> fields; // Fields of the class
    private Map<String, MethodMetadata> methodMetadata; // Methods and their metadata

    public ClassResource() {
        this.fields = new HashMap<>();
        this.methodMetadata = new HashMap<>();
        this.childClass = new ArrayList<>();
    }

    // Getters and setters
    public Map<String, String> getFields() {
        return fields;
    }

    public Map<String, MethodMetadata> getMethodMetadata() {
        return methodMetadata;
    }

    public List<String> getChildClass() {
        return childClass;
    }

    public void setChildClass(String child) {
        childClass.add(child);
    }

    // Add field
    public void addField(String name, String declaredType) {
        fields.put(name, declaredType);
    }

    // Add method with metadata
    public void addMethod(String name, Map<String, String> args, String originClass, String returnType) {
        methodMetadata.put(name, new MethodMetadata(name, args, originClass, returnType)); // Override any existing method
    }


    // Add method metadata directly
    public void addMethodMetadata(String name, Map<String, String> args, String originClass, String returnType) {
        methodMetadata.put(name, new MethodMetadata(name, args, originClass,returnType));
    }

    // Add variable declaration inside a method
    public void addVarDecInMethod(String varName, String methodName, String declaredType) {
        MethodMetadata method = methodMetadata.get(methodName);
        if (method != null) {
            method.addVariable(varName, declaredType);
        }
    }

    // Print child classes
    public void printChild() {
        if (childClass.isEmpty()) {
            System.out.println("No children.");
            return;
        }
        System.out.println("Children: " + String.join(", ", childClass));
    }

    // Print fields
    public void printFields() {
        System.out.println("Fields: " + fields);
    }

    // Print methods
    public void printMethods() {
        System.out.println("Methods:");
        for (Map.Entry<String, MethodMetadata> entry : methodMetadata.entrySet()) {
            System.out.println("  " + entry.getKey() + " (from " + entry.getValue().getOriginClass() + ")");
        }
    }
}
