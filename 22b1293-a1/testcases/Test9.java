public class Test9 {
    public static void main(String[] args) {
        IterativeFibonacci generator;
        generator = new IterativeFibonacci();
        generator.generate(10);
    }
}

class IterativeFibonacci {
    public void generate(int n) {
        FibonacciCalculator calculator;
        calculator = new FibonacciCalculator();
        calculator.calculate(n); // FibonacciCalculator.calculate
    }
}

class FibonacciCalculator {
    public void calculate(int n) {
        int a;
        int b;
        int temp; 
        int i;
        boolean res;
        int one;
        one = 1;
        a = 0;
        b = 1;
        i = 2;

        System.out.println(a);
        System.out.println(b);
        res = i <= n;

        while (res) {
            temp = a + b;
            a = b;
            b = temp;
            System.out.println(b);
            i = i + one;
            res = i <= n;
        }
    }
}
