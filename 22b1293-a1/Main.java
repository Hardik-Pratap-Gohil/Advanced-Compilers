import syntaxtree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {
   public static void main(String [] args) {
      try {
         Node root = new A1JavaParser(System.in).Goal();

         Bindings bindings = new Bindings();

         root.accept(new BindingResolver<>(),bindings);

         bindings.sort();

         root.accept(new OutputGenerator<>(), bindings);

      }
      catch (ParseException e) {
         System.out.println(e.toString());
      }
   }
} 
