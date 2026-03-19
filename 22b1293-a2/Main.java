import syntaxtree.*;
import visitor.*;

import java.io.FileWriter;
import java.util.*;

public class Main {
   public static void main(String [] args) {
       try {
           Node root = new a2java(System.in).Goal();
           CFGGen cfgGen = new CFGGen();
           root.accept(cfgGen);
           ProgramCFG programCFG = cfgGen.getCFG();

           // Printing the DOT file
           //BB.printBBDOT(programCFG);

           // For iterating over the program
//           for (String className : programCFG.classMethodList.keySet()) {
//               Set<String> methodList = programCFG.classMethodList.get(className);
//               System.out.println("Class: " + className);
//               for (String methodName : methodList) {
//                   System.out.println("Method: " + methodName);
//                   BB currentMethodBB = programCFG.methodBBSet.get(methodName);
//                   BB.printBB(currentMethodBB);
//               }
//           }


           // Assignment Starts here
           // You can write your own custom visitor(s)
           Bindings bindings = new Bindings();

           root.accept(new BindingResolver<>(), bindings);

           bindings.sort();

//         bindings.printAll();


           Node newRoot = (Node) root.accept(new MethodInliner<>(programCFG), bindings); // Store updated AST


           CFGGen cfgGennew = new CFGGen();
           newRoot.accept(cfgGennew);
           ProgramCFG programCFGnew = cfgGennew.getCFG();
//           for (String className : programCFGnew.classMethodList.keySet()) {
//               Set<String> methodList = programCFGnew.classMethodList.get(className);
//               System.out.println("Class: " + className);
//               for (String methodName : methodList) {
//                   System.out.println("Method: " + methodName);
//                   BB currentMethodBB = programCFGnew.methodBBSet.get(methodName);
//                   BB.printBB(currentMethodBB);
//               }
//           }
           Bindings newBindings = new Bindings();

           root.accept(new BindingResolver<>(), newBindings);

           newBindings.sort();
           // Create and run constant propagation analysis
           ConstantPropagationAnalysis analysis = new ConstantPropagationAnalysis();
           analysis.analyze(programCFGnew, newBindings);

           // Use  the original CFG and the bindings and the IN and OUT and currentvalues inside the execution block and write a visitor from the root node which visits
           OptimizedCodePrinter printer = new OptimizedCodePrinter(programCFGnew);
           String optimizedCode = (String) newRoot.accept(printer);

// Print the final optimized Java code
           //System.out.println("=== Final Optimized Java Code ===");
           System.out.println(optimizedCode);

      }
      catch (ParseException e) {
         System.out.println(e.toString());
      }

   }
}
