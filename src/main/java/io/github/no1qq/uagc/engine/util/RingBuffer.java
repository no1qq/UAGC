package io.github.no1qq.uagc.engine.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class RingBuffer<T> implements Iterable<T> {

    private final Object[] elements;
    private int head;
    private int size;

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.elements = new Object[capacity];
    }

    public void add(T element) {
        Objects.requireNonNull(element, "element");
        elements[head] = element;
        head = (head + 1) % elements.length;
        if (size < elements.length) {
            size++;
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int indexFromOldest) {
        if (indexFromOldest < 0 || indexFromOldest >= size) {
            throw new IndexOutOfBoundsException("index " + indexFromOldest + " size " + size);
        }
        int start = (head - size + elements.length) % elements.length;
        return (T) elements[(start + indexFromOldest) % elements.length];
    }

    @SuppressWarnings("unchecked")
    public T last() {
        if (size == 0) {
            throw new NoSuchElementException("buffer is empty");
        }
        return (T) elements[(head - 1 + elements.length) % elements.length];
    }

    public T lastOrNull() {
        return size == 0 ? null : last();
    }

    public T fromEnd(int offset) {
        return get(size - 1 - offset);
    }

    public T fromEndOrNull(int offset) {
        int index = size - 1 - offset;
        return index < 0 || index >= size ? null : get(index);
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return elements.length;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == elements.length;
    }

    public void clear() {
        java.util.Arrays.fill(elements, null);
        head = 0;
        size = 0;
    }

    public List<T> toList() {
        if (size == 0) {
            return Collections.emptyList();
        }
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(get(i));
        }
        return list;
    }

    public List<T> newestFirst(int limit) {
        int count = Math.min(limit, size);
        if (count <= 0) {
            return Collections.emptyList();
        }
        List<T> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(fromEnd(i));
        }
        return list;
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return new java.util.Iterator<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public T next() {
                if (cursor >= size) {
                    throw new NoSuchElementException();
                }
                return get(cursor++);
            }
        };
    }
}
