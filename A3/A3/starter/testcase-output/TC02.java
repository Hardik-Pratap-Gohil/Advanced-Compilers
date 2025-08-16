/*2*/
import static a3.Memory.*;

class TC02 {
    public static void main(String[] args) {
        Object R0;
	Object R1;
        alloca(0);
        R0 = new TestTC02();
        R0 = ((TestTC02) R0).foo();
        System.out.println(((int) R0));
    }
}

class TestTC02 {
    public int foo() {
        Object R0;
        Object R1;
        alloca(1);
        R0 = 5; // R0 = a
        R1 = 6; // R1 = b
        R1 = ((int) R0) + ((int) R1); // R0 = a, R1 = c
        store(0, ((int) R1) + ((int) R0));  // SPILL D to 0
        R0 = ((int) R0) - ((int) R1); // R0 = e, R1 = c 
        R0 = ((int) load(0)) - ((int) R0); // R0 = t, R1 = c
        return ((int) R0);
    }
}
