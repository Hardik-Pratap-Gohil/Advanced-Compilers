import syntaxtree.*;
import visitor.*;

public class Main {
   public static void main(String [] args) {
      try {
         Node root = new a4java(System.in).Goal();
         Object o = root.accept(new GJNoArguDepthFirst());
         System.out.println("Program Parsed Sucessfully.");
         // Call your visitor here
      }
      catch (ParseException e) {
         System.out.println(e.toString());
      }
   }
} 
