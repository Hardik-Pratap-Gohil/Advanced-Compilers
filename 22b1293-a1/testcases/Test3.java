public class Test3 {
  public static void main(String[] args) {
    Exec e;
    e = new Exec();
    e.exec();
  }
}

class Exec {
  public int exec() {
    Odd1 obj1;
    Odd1 obj2;
    Odd2 obj3;
    Odd2 obj4;

    obj1 = new Odd1();
    obj2 = new Even1();
    obj3 = new Odd2();
    obj4 = new Even2();

    obj1.foo(1); // obj1.foo = [Odd1.foo, Even1.foo, Odd2.foo, Even2.foo]
    obj2.foo(2); // obj2.foo = [Odd1.foo, Even1.foo, Odd2.foo, Even2.foo]
    obj3.foo(3); // obj3.foo = [Odd2.foo, Even2.foo]
    obj4.foo(4); // obj4.foo = [Odd2.foo, Even2.foo]

    return 0;
  }
}

class Odd1 {
  public int foo(int p) {
      return 10;
  }
}

class Even1 extends Odd1 {
  public int foo(int p) {
    return 10;
  }
}

class Odd2 extends Even1 {
  public int foo(int p) {
    return 10;
  }
}

class Even2 extends Odd2 {
  public int foo(int p) {
    return 10;
  }
}
