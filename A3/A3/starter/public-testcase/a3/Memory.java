package a3;
public class Memory {
    public static Object storage[];
    
    public static void alloca(int size) {
        storage = new Object[size];
    }
    
    public static void store(int idx, Object o) {
        storage[idx] = o;
    }

    public static Object load(int idx) {
        return (Object)storage[idx];
    }
}
