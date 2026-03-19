/*1*/
class TC01 {
    public static void main(String[] args) {
        TestTC01 o;
        int res;
        o = new TestTC01();
        res = o.foo();
        System.out.println(res);
    }
}
class TestTC01 {
    public int foo() {
        int a;
        int b;
        int x;
        boolean e;
        a = 1;
        b = 2;
        e = a <= b;
        while(e) {
          b = a + b;
          e = b <= a;
        }
        b = b;
        x = 2;
        b = x;
        return b;
    }
}
