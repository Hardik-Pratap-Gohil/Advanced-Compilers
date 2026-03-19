import syntaxtree.FormalParameterList;
import syntaxtree.Type;
import syntaxtree.VarDeclaration;

import java.util.*;

public class Bindings {
    public HashMap<String, ClassResource> bindings;

    public HashMap<String, ClassResource> getBindings() {
        return bindings;
    }

    // Constructor to initialize the bindings map
    public Bindings() {
        this.bindings = new HashMap<>();
    }
    public void sort() {
        // Use a TreeMap to sort the HashMap by keys lexicographically
        TreeMap<String, ClassResource> sortedMap = new TreeMap<>(bindings);

        // Clear the original map and re-populate it with sorted entries
        bindings.clear();
        bindings.putAll(sortedMap);

        for (Map.Entry<String,ClassResource> entry: bindings.entrySet()){
            ClassResource classResource = entry.getValue();
        }
    }
    public void putClass(String className){
        bindings.put(className,new ClassResource());
    }

    public void putField(String className, String fieldName, String fieldType){
        ClassResource classResource = bindings.get(className);
        classResource.addField(fieldName,fieldType);
    }
    public void putMethod(String className, String methodName, HashMap<String,String> args, String returnType) {
        ClassResource classResource = bindings.get(className);
        classResource.addMethod(methodName,args,className,returnType);


    }

    public void addVarDeclInMethod(String className, String methodName, String varName, String varType){
        ClassResource classResource = bindings.get(className);
        classResource.addVarDecInMethod(varName,methodName,varType);

    }

    // Print all bindings
    public void printAll() {
        for (Map.Entry<String, ClassResource> entry : bindings.entrySet()) {
            System.out.println("Class: " + entry.getKey());
            entry.getValue().printFields();
            entry.getValue().printMethods();
            entry.getValue().printChild();
            System.out.println();
        }
    }
    // Set the child class for inheritance
    public void setChildClass(String currentClassName, String parentClassName) {
        ClassResource parentResource = bindings.get(parentClassName);
        ClassResource childResource = bindings.get(currentClassName);

        if (parentResource == null || childResource == null) {
            throw new IllegalArgumentException("Parent or child class does not exist.");
        }

        // Add child class to the parent's list
        parentResource.setChildClass(currentClassName);

        // Duplicate or override methods in the child class
        for (Map.Entry<String, MethodMetadata> methodEntry : parentResource.getMethodMetadata().entrySet()) {
            String methodName = methodEntry.getKey();
            MethodMetadata parentMethod = methodEntry.getValue();

            // Override only if not already defined in child
            if (!childResource.getMethodMetadata().containsKey(methodName)) {
                childResource.addMethodMetadata(methodName, parentMethod.getArguments(), parentMethod.getOriginClass(), parentMethod.getReturnType());
            }
        }
    }

    public List<String> lookupPossibleTypes(String declaredType, String calledMethodName) {
        List<String> possibleTypes = new ArrayList<>();

        // Use a queue for BFS traversal of the class hierarchy
        Queue<String> queue = new LinkedList<>();
        queue.add(declaredType); // Start from the declared type

        while (!queue.isEmpty()) {
            String currentClass = queue.poll();
            ClassResource classResource = bindings.get(currentClass);

            if (classResource != null) {
                // Check if the current class contains the method
                if (classResource.getMethodMetadata().containsKey(calledMethodName)) {
                    MethodMetadata methodMetadata = classResource.getMethodMetadata().get(calledMethodName);

                    // Add the origin class of the method
                    String originClass = methodMetadata.getOriginClass();
                    if (!possibleTypes.contains(originClass)) {
                        possibleTypes.add(originClass);
                    }
                }

                // Add all child classes of the current class to the queue
                queue.addAll(classResource.getChildClass());
            }
        }

        return possibleTypes; // Return all possible classes containing the method
    }



    // Lookup for variables/fields
    public String lookupVariable(String className, String methodName, String varName) {
        String currentClass = className;

        // Traverse the class hierarchy
        while (currentClass != null) {
            ClassResource classResource = bindings.get(currentClass);
            if (classResource != null) {
                // Check if the variable is in the current method's scope
                if (methodName != null && classResource.getMethodMetadata().containsKey(methodName)) {
                    Map<String, String> methodVars = classResource.getMethodMetadata().get(methodName).getArguments();
                    if (methodVars.containsKey(varName)) {
                        return methodVars.get(varName); // Variable in the method
                    }
                }

                // Check if the variable is in the class fields
                if (classResource.getFields().containsKey(varName)) {
                    return classResource.getFields().get(varName); // Variable in class fields
                }

                // Move up to the superclass
                currentClass = getParentClass(currentClass);
            } else {
                break; // Class not found, stop traversal
            }
        }

        return null; // Variable not found
    }

    // Lookup for fields only (not arguments or local variables)
    public String lookupFieldOnly(String className, String methodName, String varName) {
        String currentClass = className;
        ClassResource classResourcebase = bindings.get(currentClass);

        if (methodName != null && classResourcebase != null){
            Map<String, MethodMetadata> methodMetadataMap = classResourcebase.getMethodMetadata();
            if (methodMetadataMap.containsKey(methodName)) {
                MethodMetadata methodMetadata = methodMetadataMap.get(methodName);
                if (methodMetadata != null) {
                    Map<String, String> methodVars = methodMetadata.getArguments();
                    if (methodVars != null && methodVars.containsKey(varName)) {
                        return null; // Ignore arguments or local variables
                    }
                }
            }

        }
        // Traverse the class hierarchy looking for fields
        while (currentClass != null) {
            ClassResource classResource = bindings.get(currentClass);
            if (classResource != null) {
                // First check if the variable is a field in the current class

                if (classResource.getFields().containsKey(varName)) {
                    return currentClass; // Return the class that owns this field
                }

                // Move up to the superclass
                currentClass = getParentClass(currentClass);
            } else {
                break; // Class not found, stop traversal
            }
        }

        return null; // If the variable is not found as a field
    }




    // Helper method to get the parent class of a given class
    private String getParentClass(String className) {
        ClassResource classResource = bindings.get(className);
        if (classResource != null) {
            List<String> childClasses = classResource.getChildClass();
            // Reverse lookup: find the parent class that has this as a child
            for (Map.Entry<String, ClassResource> entry : bindings.entrySet()) {
                if (entry.getValue().getChildClass().contains(className)) {
                    return entry.getKey(); // Parent class
                }
            }
        }
        return null;
    }




}