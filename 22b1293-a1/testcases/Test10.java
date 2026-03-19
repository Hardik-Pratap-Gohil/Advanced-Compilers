public class Test10 {
    public static void main(String[] ooo_ooo) {
        O o;
        o = new O();
        o.OOOO();
    }
}

class O {
    int OO;
    public int OOO() { return 0; }
    public void OOOO() {
        int OOOOOO;
        OOOOOO = this.OOO(); // this.OOO = [O.OOO, OO.OOO, OOO.OOO]
        System.out.println(OOOOOO);
    }
}

class OO extends O {
    int OO;
    public int OOO() { return 0; }
    public void OOOO() {
        int OOOOOO;
        OOOOOO = this.OOO(); // this.OOO = [OO.OOO, OOO.OOO]
        System.out.println(OOOOOO);
    }
}

class OOO extends OO {
    int OO;
    public int OOO() { return 0; }
    public void OOOO() {
        int OOOOOO;
        OOOOOO = this.OOO(); // this.OOO = [OOO.OOO]
        System.out.println(OOOOOO);
    }
}

