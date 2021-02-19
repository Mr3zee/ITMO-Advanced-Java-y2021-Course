import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        ArraySet<Integer> set = new ArraySet<>(List.of(10, 20, 30, 40, 50, 60, 70), new Cmp());
        System.out.println(Arrays.toString(set.headSet(10, false).toArray()));
        System.out.println(Arrays.toString(set.tailSet(10, false).toArray()));
        System.out.println();
    }

    private static class Cmp implements Comparator<Integer> {

        @Override
        public int compare(Integer o1, Integer o2) {
            return (o1 - o2);
        }
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
