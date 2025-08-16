/*2*/
class TC02 {
    public static void main(String[] args) {
        TestTC02 o;
        int res;
        o = new TestTC02();
        res = o.foo();
        System.out.println(res);
    }
}
class TestTC02 {
    public int foo() {
        int a;
        int b;
        int c;
        int d;
        int e;
        int t;
        a = 5;
        b = 6;
        c = a + b;
        d = c + a;
        e = a - c;
        t = d - e;
        return t;
    }
}
