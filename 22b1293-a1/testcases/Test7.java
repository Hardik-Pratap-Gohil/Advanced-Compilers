public class Test7 {
    public static void main(String[] args) {
        P obj;
        obj = new P();
        System.out.println(obj.foo(10));
    }
}

class P {
    int f2;
    public int foo(int p) {
        A a1;
        A a2;
        B b1;
        A b2;
        boolean b3; 
        b3 = true;
        a1 = new A();
        a2 = new B();
        b1 = new B();
        b2 = new B();
        a1.foo(a1, a2);
        while(b3) {
            b1.bar(b2);
            b3 = false;
        }
        return 10;
    }
}

class A {
    boolean f1;
    int f2;
    int f3;
    A f4;
    public int foo(A p1, A p2) {
        int x;
        B temp1;
        C temp2;
        C temp3;
        A temp4;
        x = 10;
        temp1 = new B();
        temp2 = new C();
        temp3 = new C();
        this.f2 = x;
        this.f4 = temp1;
        temp1.f4 = temp2;
        temp2.f4 = temp3;
        temp4 = temp2.f4;
        temp4.bar(temp1);
        return x;
    }

    public void bar(A p1) {
        p1.f2 = 10;
    }
}

class B extends A {
    int f2;
    B f4;
    public void bar(A p1) {
        p1.f4 = new B();
        p1.f2 = 10;
    }
}

class C extends B {
    int f3;
    C f4;
    public void bar(A p1) {
        p1.f4 = new C();
        p1 = new C();
        p1.f3 = 10;
    }
}