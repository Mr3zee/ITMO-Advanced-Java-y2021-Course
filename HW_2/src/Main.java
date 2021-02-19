import java.util.Objects;

public class Main {
    public static void main(String[] args) {

    }

    private static class A implements Comparable<A> {
        private final int a;

        public A(int a) {
            this.a = a;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof A)) return false;
            A a1 = (A) o;
            return a == a1.a;
        }

        @Override
        public int hashCode() {
            return Objects.hash(a);
        }

        @Override
        public int compareTo(A o) {
            return a - o.a;
        }
    }

    private static class B extends A {

        public B(int a) {
            super(a);
        }
    }
}
