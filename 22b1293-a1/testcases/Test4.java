public class Test4 {
  public static void main(String[] args) {
    B b;
    b = new B();
    System.out.println(b.foo(b));
  }
}

class AAA {
  int poke;
  public int foo(B a) {
    int x;
    int y;
    int z;
    this.poke = 42; // this.poke = AAA.poke
    x = 11;
    y = a.poke; // a.poke = B.poke
    z = x + y;
    return z;
  }
}

class B extends AAA {
  int poke;
  public int foo(B a) {
    int n1;
    int n2;
    int res;
    B x;
    boolean bbbb;

    this.poke = 22; // this.poke = B.poke
    x = new B();
    bbbb = true;
    if (bbbb) {
      return 0;
    }
    n1 = this.foo(null); // this.foo = [B.foo]
    n2 = a.foo(null); // a.foo = [B.foo]
    res = n1 + n2;
    return n1 + n2;
  }

}