/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.internal.ListImplementation;

/// A generic immutable ordered collection of elements. Methods in this interface support only read-only access to the immutable list.
///
/// Modification operations are supported through the [PersistentList][PersistentList] interface.
///
/// Implementors of this interface take responsibility to be immutable.
/// Once constructed they must contain the same elements in the same order.
///
/// @param <E> the type of elements contained in the list. The immutable list is covariant on its element type.
@SuppressWarnings("unused")
public interface ImmutableList<E extends @Nullable Object> extends ImmutableCollection<E> {
  /// Returns an immutable list containing all elements of the specified collection.
  ///
  /// If the specified collection is already an immutable list, returns it as is.
  static <T extends @Nullable Object> ImmutableList<T> from(Iterable<T> iterable) {
    if (iterable instanceof ImmutableList<T> list) {
      return list;
    }
    return PersistentList.from(iterable);
  }

  /// Returns an immutable list containing all elements of the specified array.
  static <T extends @Nullable Object> ImmutableList<T> from(T[] array) {
    return PersistentList.from(array);
  }

  /// Returns an immutable list containing all elements of the specified sequence.
  static <T extends @Nullable Object> ImmutableList<T> from(Stream<? extends T> stream) {
    return PersistentList.from(stream);
  }

  /// Returns an immutable list containing all characters.
  static ImmutableList<Character> from(CharSequence chars) {
    return PersistentList.from(chars);
  }

  /// Returns the element at the specified index in the list.
  ///
  /// @throws IndexOutOfBoundsException if {@code index} is less than zero or greater than or equal to {@code size} of this list.
  E get(int index);

  /// Returns the index of the first occurrence of the specified element in the list, or `-1` if the specified
  /// element is not contained in the list.
  ///
  /// For lists containing more than {@code Int.MAX_VALUE} elements, a result of this function is unspecified.
  int indexOf(E element);

  /// Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
  /// element is not contained in the list.
  ///
  /// For lists containing more than {@code Int.MAX_VALUE} elements, a result of this function is unspecified.
  int lastIndexOf(E element);

  /// Returns a list iterator over the elements in this list (in proper sequence).
  ListIterator<E> listIterator();

  /// Returns a list iterator over the elements in this list (in proper sequence), starting at the specified {@code index}.
  ///
  /// @throws IndexOutOfBoundsException if {@code index} is less than zero or greater than or equal to {@code size} of this list.
  ListIterator<E> listIterator(int index);

  /// Returns a view of the portion of this list between the specified {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
  ///
  /// The returned list is backed by this list.
  ///
  /// @throws IndexOutOfBoundsException if {@code fromIndex} is less than zero or {@code toIndex} is greater than the size of this list.
  /// @throws IllegalArgumentException if {@code fromIndex} is greater than {@code toIndex}.
  default ImmutableList<E> subList(int fromIndex, int toIndex) {
    return new SubList<>(this, fromIndex, toIndex);
  }

  final class SubList<E extends @Nullable Object> implements ImmutableList<E> {
    private final ImmutableList<E> source;
    private final int fromIndex;
    private final int size;

    SubList(ImmutableList<E> source, int fromIndex, int toIndex) {
      ListImplementation.checkRangeIndexes(fromIndex, toIndex, source.size());
      this.source = source;
      this.fromIndex = fromIndex;
      this.size = toIndex - fromIndex;
    }

    @Override
    public E get(int index) {
      ListImplementation.checkElementIndex(index, size);
      return source.get(fromIndex + index);
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public boolean contains(E element) {
      return indexOf(element) != -1;
    }

    @Override
    public boolean containsAll(Collection<? extends E> elements) {
      for (var element : elements) {
        if (!contains(element)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Iterator<E> iterator() {
      return listIterator();
    }

    @Override
    public int indexOf(E element) {
      for (var i = 0; i < size; i++) {
        if (Objects.equals(source.get(fromIndex + i), element)) {
          return i;
        }
      }
      return -1;
    }

    @Override
    public int lastIndexOf(E element) {
      for (var i = size - 1; i >= 0; i--) {
        if (Objects.equals(source.get(fromIndex + i), element)) {
          return i;
        }
      }
      return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
      return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
      ListImplementation.checkPositionIndex(index, size);
      return new ListIterator<>() {
        private int currentIndex = index;

        @Override
        public boolean hasNext() {
          return currentIndex < size;
        }

        @Override
        public E next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          return get(currentIndex++);
        }

        @Override
        public boolean hasPrevious() {
          return currentIndex > 0;
        }

        @Override
        public E previous() {
          if (!hasPrevious()) {
            throw new NoSuchElementException();
          }
          return get(--currentIndex);
        }

        @Override
        public int nextIndex() {
          return currentIndex;
        }

        @Override
        public int previousIndex() {
          return currentIndex - 1;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void set(E element) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void add(E element) {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      ListImplementation.checkRangeIndexes(fromIndex, toIndex, size);
      return new SubList<>(source, this.fromIndex + fromIndex, this.fromIndex + toIndex);
    }

    @Override
    public int hashCode() {
      return ListImplementation.orderedHashCode(this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return ListImplementation.orderedEquals(this, other);
    }

    @Override
    public String toString() {
      var result = new StringJoiner(", ", "[", "]");
      for (var element : this) {
        result.add(String.valueOf(element));
      }
      return result.toString();
    }
  }
}
