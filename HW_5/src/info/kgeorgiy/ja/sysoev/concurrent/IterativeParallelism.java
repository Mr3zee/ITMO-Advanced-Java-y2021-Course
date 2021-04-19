package info.kgeorgiy.ja.sysoev.concurrent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class IterativeParallelism implements ScalarIP {

    private static <T, R> R find(
            final int threads,
            final List<? extends T> values,
            final BiFunction<R, T, R> processorFunction,
            final R acc1,
            final BiFunction<R, R, R> collectorFunction,
            final R acc2
    ) throws InterruptedException {
        int mainTreads = Math.min(threads, values.size());
        int window = values.size() / mainTreads;
        List<Thread> pool = new ArrayList<>();
        List<Callable<R>> tasks = new ArrayList<>();
        for (int i = 0; i < mainTreads; i++) {
            int start = i * window;
            Processor<T, R> processor = new Processor<>(acc1, processorFunction);
            Callable<R> task = new Callable<>(() -> forEach(values, processor, start, Math.min(start + window, values.size())));
            Thread thread = new Thread(task);
            thread.start();
            pool.add(thread);
            tasks.add(task);
        }
        for (Thread thread : pool) {
            thread.join();
        }
        Processor<R, R> collector = new Processor<>(acc2, collectorFunction);
        for (Callable<R> task : tasks) {
            collector.add(task.get());
        }
        return collector.getResult();
    }

    private static <T> T find(
            final int threads,
            final List<? extends T> values,
            final BiFunction<T, T, T> processorFunction,
            final T acc
    ) throws InterruptedException {
        return find(threads, values, processorFunction, acc, processorFunction, acc);
    }

    private static <T, R> R forEach(final List<? extends T> values, final Processor<T, R> processor, int left, int right) {
        for (int i = left; i < right; i++) {
            processor.add(values.get(i));
        }
        return processor.getResult();
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.size() == 0) return null;
        return find(
                threads, values,
                (acc, val) -> comparator.compare(acc, val) >= 0 ? acc : val, values.get(0)
        );
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.size() == 0) return null;
        return find(
                threads, values,
                (acc, val) -> comparator.compare(acc, val) <= 0 ? acc : val, values.get(0)
        );
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return find(threads, values,
                (acc, val) -> acc && predicate.test(val), true,
                (v1, v2) -> v1 && v2, true
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return find(threads, values,
                (acc, val) -> acc || predicate.test(val), false,
                (v1, v2) -> v1 || v2, false
        );
    }

    private static class Callable<T> implements Runnable {
        private volatile T value;
        private final Supplier<T> supplier;

        public Callable(final Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public void run() {
            value = supplier.get();
        }

        public T get() {
            return value;
        }
    }

    private static class Processor<T, R> {
        private R accumulator;
        private final BiFunction<R, T, R> converter;

        private Processor(final R accumulator, final BiFunction<R, T, R> converter) {
            this.accumulator = accumulator;
            this.converter = converter;
        }

        public void add(final T value) {
            accumulator = converter.apply(accumulator, value);
        }

        public R getResult() {
            return accumulator;
        }
    }
}
