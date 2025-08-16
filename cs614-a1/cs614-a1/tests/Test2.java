public class Test2 {
    public static void main(String[] args) {
        P obj;
        obj = new P();
        System.out.println(obj.foo(10));
    }
}

class P {
    public int foo(int p) {
        A a1;
        A a2;
        B b1;
        B b2;
        a1 = new A();
        a2 = new B();
        b1 = new B();
        b2 = new B();
        a1.foo(10);
        a2.foo(20);
        b1.foo(30);
        a1.bar(b2);
        b2.bar(a1);
        return 10;
    }
}

class A {
    boolean f1;
    int f2;

    public int foo(int p) {
        int x;
        x = 10;
        this.f2 = x;
        return x;
    }

    public void bar(A p1) {
        p1.f2 = 10;
    }
}

class B extends A {
    int f2;

    public void bar(A p1) {
        p1.f2 = 10;
    }
}
