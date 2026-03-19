public class Test6 {
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
        A e1;
        boolean b3; 
        b3 = true;
        a1 = new A();
        a2 = new B();
        a3 = new C();
        b1 = new B();
        b2 = new B();
        c1 = new C();
        d1 = new D();
        if(b3) {
            e1 = new A();
        } else {
            e1 = new E();
        }
        a1.foo(10);
        a2.foo(20);
        a3.foo(30);
        b1.foo(30);
        a1.bar(b2);
        b2.bar(a1);
        c1.bar(a1);
        d1.bar(a1);
        d1.foobar(d1);
        e1.bar(d1);
        return 10;
    }
}

class A {
    boolean f1;
    int f2;
    int f3;
    int f4;
    int f5;
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

class C extends B {
    int f3;
    public void bar(A p1) {
        p1.f3 = 10;
    }
}

class D extends A {
    int f4;
    int f5;
    public void foobar(D p1) {
        p1.f4 = 10;
    }
}

class E extends D{
    int f5;
    public void foobar(D p1) {
        p1.f5 = 10;
    }
}