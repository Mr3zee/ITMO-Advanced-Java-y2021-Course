import exceptions.ASNoSuchElementException;
import exceptions.ASUnsupportedOperationException;

import java.util.*;


public class ArraySet<T> implements NavigableSet<T> {
    private final ArrayList<T> array;
    private final Comparator<? super T> comparator;

    public ArraySet(final Collection<? extends T> array, final Comparator<? super T> comparator) {
        TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(array);
        this.array = new ArrayList<>(treeSet);
        this.comparator = comparator;
    }

    public ArraySet(final Comparator<? super T> comparator) {
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

    private int searchIndex(T t, int shift, boolean inclusive) {
        int position = Collections.binarySearch(array, t, comparator);
        if (position >= 0) {
            position += inclusive ? 0 : shift;
        } else {
            position = (-position - 1) + ((shift > 0) ? shift - 1 : shift);
        }
        return (0 <= position && position < size()) ? position : -1;
    }

    private T search(T t, int shift, boolean inclusive) {
        int index = searchIndex(t, shift, inclusive);
        return index == -1 ? null : array.get(index);
    }

    @Override
    public T lower(T t) {
        return search(t, -1, false);
    }

    @Override
    public T floor(T t) {
        return search(t, -1, true);
    }

    @Override
    public T ceiling(T t) {
        return search(t, 1, true);
    }

    @Override
    public T higher(T t) {
        return search(t, 1, false);
    }

    @Override
    public T pollFirst() {
        throw new ASUnsupportedOperationException("pollFirst");
    }

    @Override
    public T pollLast() {
        throw new ASUnsupportedOperationException("pollLast");
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public boolean isEmpty() {
        return array.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        try {
            return o != null && Collections.binarySearch(array, (T) o, comparator) >= 0;
        } catch (ClassCastException ignored) {
            return false;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(array).iterator();
    }

    @Override
    public Object[] toArray() {
        return array.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return array.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new ASUnsupportedOperationException("add");
    }

    @Override
    public boolean remove(Object o) {
        throw new ASUnsupportedOperationException("remove");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().map(this::contains).reduce(true, (acc, a) -> acc && a);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new ASUnsupportedOperationException("addAll");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new ASUnsupportedOperationException("retainAll");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new ASUnsupportedOperationException("removeAll");
    }

    @Override
    public void clear() {
        throw new ASUnsupportedOperationException("clear");
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ArrayList<>(array), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int fromIndex = searchIndex(fromElement, 1, fromInclusive);
        int toIndex = searchIndex(toElement, -1 , toInclusive);
        List<T> subList = (fromIndex != -1 && toIndex != -1 && fromIndex <= toIndex) ? array.subList(fromIndex, toIndex + 1) : List.of();
        return new ArraySet<>(subList, comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<T>(comparator);
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        checkIsEmpty();
        return array.get(0);
    }

    @Override
    public T last() {
        checkIsEmpty();
        return array.get(size() - 1);
    }

    private void checkIsEmpty() {
        if (isEmpty()) {
            throw new ASNoSuchElementException();
        }
    }

    public void print() {
        System.out.println(Arrays.toString(toArray()));
    }
}
