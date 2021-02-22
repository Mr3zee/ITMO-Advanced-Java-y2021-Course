import exceptions.ASNoSuchElementException;
import exceptions.ASUnsupportedOperationException;

import java.util.*;


public class ArraySet<T> implements NavigableSet<T> {
    private final T[] array;
    private final Comparator<? super T> comparator;

    @SuppressWarnings("unchecked")
    public ArraySet(final Collection<? extends T> array, final Comparator<? super T> comparator) {
        TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(array);
        this.array = (T[]) treeSet.toArray();
        this.comparator = comparator;
    }

    public ArraySet(final Comparator<? super T> comparator) {
        this(List.of(), comparator);
    }

    public ArraySet(final T[] array, final Comparator<? super T> comparator) {
        this(Arrays.asList(array), comparator);
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

    private ArraySet<T> emptySet() {
        return new ArraySet<>(comparator);
    }

    private int searchIndex(final T t, int shift, boolean inclusive) {
        int position = Arrays.binarySearch(array, t, comparator);
        if (position >= 0) {
            position += inclusive ? 0 : shift;
        } else {
            position = (-position - 1) + ((shift > 0) ? shift - 1 : shift);
        }
        return (0 <= position && position < size()) ? position : -1;
    }

    private T search(final T t, int shift, boolean inclusive) {
        int index = searchIndex(t, shift, inclusive);
        return index == -1 ? null : array[index];
    }

    @Override
    public T lower(final T t) {
        return search(t, -1, false);
    }

    @Override
    public T floor(final T t) {
        return search(t, -1, true);
    }

    @Override
    public T ceiling(final T t) {
        return search(t, 1, true);
    }

    @Override
    public T higher(final T t) {
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
        return array.length;
    }

    @Override
    public boolean isEmpty() {
        return array.length == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        try {
            return o != null && Arrays.binarySearch(array, (T) o, comparator) >= 0;
        } catch (ClassCastException ignored) {
            return false;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(array).iterator();
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(array, size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1> T1[] toArray(T1[] a) {
        int size = size();
        if (a.length < size) {
            return (T1[]) Arrays.copyOf(array, size(), a.getClass());
        }
        System.arraycopy(array, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public boolean add(final T t) {
        throw new ASUnsupportedOperationException("add");
    }

    @Override
    public boolean remove(final Object o) {
        throw new ASUnsupportedOperationException("remove");
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return c.stream().map(this::contains).reduce(true, (acc, a) -> acc && a);
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        throw new ASUnsupportedOperationException("addAll");
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new ASUnsupportedOperationException("retainAll");
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new ASUnsupportedOperationException("removeAll");
    }

    @Override
    public void clear() {
        throw new ASUnsupportedOperationException("clear");
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(array, Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, boolean fromInclusive, final T toElement, boolean toInclusive) {
        int fromIndex = searchIndex(fromElement, 1, fromInclusive);
        int toIndex = searchIndex(toElement, -1 , toInclusive);
        return (fromIndex != -1 && toIndex != -1 && fromIndex <= toIndex)
                ? new ArraySet<>(Arrays.copyOfRange(array, fromIndex, toIndex + 1), comparator)
                : emptySet();
    }

    @Override
    public NavigableSet<T> headSet(final T toElement, boolean inclusive) {
        if (isEmpty()) {
            return emptySet();
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement, boolean inclusive) {
        if (isEmpty()) {
            return emptySet();
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(final T fromElement, final T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(final T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        checkIsEmpty();
        return array[0];
    }

    @Override
    public T last() {
        checkIsEmpty();
        return array[size() - 1];
    }

    private void checkIsEmpty() {
        if (isEmpty()) {
            throw new ASNoSuchElementException();
        }
    }
}
