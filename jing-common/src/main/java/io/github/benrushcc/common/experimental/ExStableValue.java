package io.github.benrushcc.common.experimental;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/// Closely assembles jdk's StableValue for experimental usage
@SuppressWarnings("unused")
public interface ExStableValue<T> {

    boolean trySet(T content);


    T orElse(T other);


    T orElseThrow();


    boolean isSet();


    T orElseSet(Supplier<? extends T> supplier);


    void setOrThrow(T content);


    boolean equals(Object obj);


    int hashCode();

    final class Default<T> implements ExStableValue<T> {
        final Lock lock = new ReentrantLock();
        T value;
        @Override
        public boolean trySet(T content) {
            lock.lock();
            try {
                if(value == null) {
                    value = content;
                    return true;
                } else {
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public T orElse(T other) {
            lock.lock();
            try {
                return value == null ? other : value;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public T orElseThrow() {
            lock.lock();
            try {
                if(value == null) {
                    throw new NoSuchElementException();
                }
                return value;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean isSet() {
            lock.lock();
            try {
                return value != null;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public T orElseSet(Supplier<? extends T> supplier) {
            lock.lock();
            try {
                if(value == null) {
                    value = supplier.get();
                }
                return value;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void setOrThrow(T content) {
            lock.lock();
            try {
                if(value == null) {
                    value = content;
                } else {
                    throw new NoSuchElementException();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    static <T> ExStableValue<T> of() {
        return new Default<>();
    }


    static <T> ExStableValue<T> of(T content) {
        Default<T> d = new Default<>();
        d.setOrThrow(content);
        return d;
    }

    static <T> Supplier<T> supplier(Supplier<? extends T> underlying) {
        return underlying::get;
    }

    static <R> IntFunction<R> intFunction(int size,
                                          IntFunction<? extends R> underlying) {
        return underlying::apply;
    }

    static <T, R> Function<T, R> function(Set<? extends T> inputs,
                                          Function<? super T, ? extends R> underlying) {
        return underlying::apply;
    }

    static <E> List<E> list(int size,
                            IntFunction<? extends E> mapper) {
        return IntStream.range(0, size).mapToObj(mapper).collect(Collectors.toUnmodifiableList());
    }

    static <K, V> Map<K, V> map(Set<K> keys,
                                Function<? super K, ? extends V> mapper) {
        return keys.stream().collect(Collectors.toUnmodifiableMap(key -> key, mapper));
    }
}
