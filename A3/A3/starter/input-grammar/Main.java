import syntaxtree.*;
import visitor.*;

import java.util.*;

public class Main {
   public static void main(String [] args) {
      try {

         Node root = new A3Java(System.in).Goal();
         CFGGen cfgGen = new CFGGen();
         root.accept(cfgGen);

         ProgramCFG programCFG = cfgGen.getCFG();
         // BB.printBBDOT(programCFG);

         RunAnalysis ra = new RunAnalysis(programCFG);
         ra.startAnalysisBackward();

         // Result Map contains a mapping from statements to live variables at that statement
         HashMap<Node, Set<String>> resultMap = ra.getResultMap();
         root.accept(new ResultPrinter(resultMap));

         // Assignment Starts here
         // You can write your own custom visitor(s)
      }
      catch (ParseException e) {
         System.out.println(e.toString());
      }
   }
}
