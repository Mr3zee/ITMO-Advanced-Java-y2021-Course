import java.util.*;


public class ArraySet<T> implements NavigableSet<T> {
    private final NavigableSet<T> array;
    private final Comparator<? super T> comparator;

    public ArraySet(final Collection<? extends T> array, final Comparator<? super T> comparator) {
        TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(array);
        this.array = Collections.unmodifiableNavigableSet(treeSet);
        this.comparator = comparator;
    }

    public ArraySet(final Comparator<T> comparator) {
        this(List.of(), comparator);
    }

    public ArraySet(final Collection<? extends T> array) {
        this(array, null);
    }

    public ArraySet() {
        this(List.of(), null);
    }

    public ArraySet(final ArraySet<T> other) {
        this.array = other.array;
        this.comparator = other.comparator;
    }

    @Override
    public T lower(T t) {
        return this.array.lower(t);
    }

    @Override
    public T floor(T t) {
        return this.array.floor(t);
    }

    @Override
    public T ceiling(T t) {
        return this.array.ceiling(t);
    }

    @Override
    public T higher(T t) {
        return this.array.higher(t);
    }

    @Override
    public T pollFirst() {
        throw new ArraySetUOException("pollFirst");
    }

    @Override
    public T pollLast() {
        throw new ArraySetUOException("pollLast");
    }

    @Override
    public int size() {
        return this.array.size();
    }

    @Override
    public boolean isEmpty() {
        return this.array.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.array.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return this.array.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.array.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return this.array.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new ArraySetUOException("add");
    }

    @Override
    public boolean remove(Object o) {
        throw new ArraySetUOException("remove");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.array.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new ArraySetUOException("addAll");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new ArraySetUOException("retainAll");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new ArraySetUOException("removeAll");
    }

    @Override
    public void clear() {
        throw new ArraySetUOException("clear");
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return this.array.descendingSet();
    }

    @Override
    public Iterator<T> descendingIterator() {
        return this.array.descendingIterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return this.array.subSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return this.array.headSet(toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return this.array.tailSet(fromElement, inclusive);
    }

    @Override
    public Comparator<? super T> comparator() {
        return this.comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return this.array.subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return this.array.headSet(toElement);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return this.array.tailSet(fromElement);
    }

    @Override
    public T first() {
        return this.array.first();
    }

    @Override
    public T last() {
        return this.array.last();
    }
}
