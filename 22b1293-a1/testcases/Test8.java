public class Test8 {
    public static void main(String[] args) {
        
    }
}
class A{
    public void foo(){
        A a;    
        a= new A();
        a.foo();    //a.foo = [A.foo, B.foo, C.foo, D.foo, E.foo, F.foo]
    }
}
class B extends A{
    public void foo(){
    }
}
class C extends B{  
    public void foo(){
    }

}
class D extends C{
    public void foo(){
        D d;
        d= new D();
        d.foo();    //d.foo = [D.foo, E.foo, F.foo]
    }

}
class E extends D{
    public void foo(){
    }

}
class F extends E{  
    public void foo(){  
        A a;
        a.foo();    //a.foo = [A.foo, B.foo, C.foo, D.foo, E.foo, F.foo]
    }
}
