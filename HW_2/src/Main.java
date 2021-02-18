import java.util.List;

public class Main {
    public static void main(String[] args) {
        ArraySet<Integer> set = new ArraySet<>(List.of(10, 20, 30));
        for (Integer a : set) {
            System.out.println(a);
        }
        System.out.println();
        var it = set.descendingSet();
        System.out.println(it.descendingIterator().next());
    }
}
