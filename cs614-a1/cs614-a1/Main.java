import syntaxtree.*;
import visitor.*;

public class Main {
   public static void main(String [] args) {
      try {
         Node root = new A1JavaParser(System.in).Goal();
         System.out.println("Program parsed successfully");
         root.accept(new GJNoArguDepthFirst<>());
      }
      catch (ParseException e) {
         System.out.println(e.toString());
      }
   }
} 
