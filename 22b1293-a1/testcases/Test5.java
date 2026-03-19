public class Test5 {
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
        A a3;
        B b1;
        B b2;
        C c1;
        D d1; 
        a1 = new A();
        a2 = new B();
        a3 = new C();
        b1 = new B();
        b2 = new B();
        c1 = new C();
        d1 = new D();
        a1.foo(10);
        a2.foo(20);
        a3.foo(30);
        b1.foo(30);
        a1.bar(b2);
        b2.bar(a1);
        c1.bar(a1);
        d1.bar(a1);
        return 10;
    }
}

class A {
    boolean f1;
    int f2;
    int f3;
    int f4;

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
    public void bar(A p2) {
        p2.f2 = 10;
    }
}

class C extends B {
    int f3;
    public void bar(A p3) {
        p3.f3 = 10;
    }
}

class D extends C {
    int f4;
    public void bar(A p4) {
        p4.f4 = 10;
    }
}