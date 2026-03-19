import syntaxtree.*;
import visitor.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class Main {
   public static void main(String [] args) {
      try {
         ByteArrayOutputStream buffer = new ByteArrayOutputStream();
         byte[] data = new byte[1024];
         int bytesRead;
         try {
            while ((bytesRead = System.in.read(data, 0, data.length)) != -1) {
               buffer.write(data, 0, bytesRead);
            }
            buffer.flush();
         } catch (IOException e) {
            System.out.println("Error reading input: " + e.getMessage());
            return;
         }

         // Get the full input as a string
         String fullInput = buffer.toString();

         // Extract the first line to get the number of registers
         String[] lines = fullInput.split("\n", 2);
         String firstLine = lines[0];

         // Parse the number of registers from the comment format /*N*/
         int numRegisters = 0;
         if (firstLine.trim().matches("/\\*\\d+\\*/.*")) {
            String numString = firstLine.substring(firstLine.indexOf("/*") + 2, firstLine.indexOf("*/"));
            numRegisters = Integer.parseInt(numString);
         } else {
            System.out.println("Warning: Could not find register count in first line. Using default of 0.");
         }

         // Create a new input stream with the full content for the parser
         ByteArrayInputStream inputForParser = new ByteArrayInputStream(fullInput.getBytes());

         // Use the input stream for parsing
         Node root = new A3Java(inputForParser).Goal();
         CFGGen cfgGen = new CFGGen();
         root.accept(cfgGen);

         ProgramCFG programCFG = cfgGen.getCFG();
         // BB.printBBDOT(programCFG);

         RunAnalysis ra = new RunAnalysis(programCFG);
         ra.startAnalysisBackward();

         // Result Map contains a mapping from statements to live variables at that statement
         HashMap<Node, Set<String>> resultMap = ra.getResultMap();
//         root.accept(new ResultPrinter(resultMap));

         // Assignment Starts here
         // You can write your own custom visitor(s)
         HashMap<String, Range> liveintervals = new HashMap<>();
         Set<String> formalParametersSet = new HashSet<>();
         root.accept(new LiveIntervalCalculator(resultMap,liveintervals,formalParametersSet));
//         System.out.println("--------------------------");
//         for(Map.Entry<String, Range> entry: liveintervals.entrySet()){
//            String unambiguousVariableName = entry.getKey();
//            int start = entry.getValue().getStart();
//            int end = entry.getValue().getEnd();
//            System.out.println(unambiguousVariableName + " :[" + String.valueOf(start) + "," + String.valueOf(end) + "]");
//         }

         List<Map.Entry<String, Range>> list = new ArrayList<>(liveintervals.entrySet());
         list.sort(Comparator.comparingInt(entry -> entry.getValue().getStart()));

//         // Print the sorted entries
//         System.out.println("--------------------------");
//         for (Map.Entry<String, Range> entry : list) {
//            String unambiguousVariableName = entry.getKey();
//            int start = entry.getValue().getStart();
//            int end = entry.getValue().getEnd();
//            System.out.println(unambiguousVariableName + " :[" + String.valueOf(start) + "," + String.valueOf(end) + "]");
//         }
//         System.out.println("--------------------------");
//         System.out.println("Total Registers: " + numRegisters);
//         System.out.println("--------------------------");

         LinearScanRegisterAllocation linearScanRegisterAllocation = new LinearScanRegisterAllocation(list,numRegisters,formalParametersSet);
         Map<String,String> registerAllocation = linearScanRegisterAllocation.analyse();
//         for (Map.Entry<String,String> entry: registerAllocation.entrySet()){
//            System.out.println(entry.getKey() + " : " + entry.getValue());
//         }
//
//         System.out.println("--------------------------");
         root.accept(new OutputGenerator(registerAllocation));
      }
      catch (ParseException e) {
         System.out.println(e.toString());
      }
   }
}
