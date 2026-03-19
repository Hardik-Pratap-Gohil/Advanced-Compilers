/*1*/
import static a3.Memory.*;

class TC01 {
  public static void main(String[] args) {
    Object R0;
    alloca(0);
    R0 = new TestTC01();
    R0 = ((TestTC01) R0).foo();
    System.out.println(((int) R0));
  }
}

class TestTC01 {
    public int foo() {
        Object R0;
        alloca(2);
        store(1, 1); // a = 1; --> a was spilled to 1
        store(0, 2); // b = 2; --> b was spilled to 0
        R0 = ((int) load(1)) <= ((int) load(0)); // e = a <= b;
        while(((boolean) R0)) {
          store(0, ((int) load(1)) + ((int) load(0))); // b = a + b;
          R0 = ((int) load(0)) <= ((int) load(1)); // b <= a;
        }
        store(0, ((int) load(0))); // b = b;
        R0 = 2; // x = 2
        store(0, ((int) R0));  // b = x;
        return ((int) load(0)); // return b;
    }
}