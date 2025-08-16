public class Test1 {
    public static void main(String[] args) {
        A x;
        x = new A();
        System.out.println(x.foo(10));
    }
}

class A {
    A f1;
    int f2;

    public int foo(int p) {
        int x;
        boolean y;
        A a1;
        A a2;
        x = 10;
        y = true;
        a1 = new A();
        a2 = new A();
        a1.f1 = new A();
        if (y) {
            a1.bar(a2);
            a1.f2 = 20;
        }
        x = a1.f2;
        return x;
    }

    public void bar(A p1) {
        p1.f2 = 10;
    }
}
