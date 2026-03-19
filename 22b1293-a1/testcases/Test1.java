public class Test1 {
    public static void main(String[] args) {
        AAA x;
        x = new AAA();
        System.out.println(x.foo(10));
    }
}

class AAA {
    AAA f1;
    int f2;

    public int foo(int p) {
        int x;
        boolean y;
        AAA a1;
        AAA a2;
        x = 10;
        y = true;
        a1 = new AAA();
        a2 = new AAA();
        a1.f1 = new AAA();
        if (y) {
            a1.bar(a2);
            a1.f2 = 20;
        }
        x = a1.f2;
        return x;
    }

    public void bar(AAA p1) {
        p1.f2 = 10;
    }
}
