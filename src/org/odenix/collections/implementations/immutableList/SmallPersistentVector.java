/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import java.util.Arrays;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.PersistentList;
import org.odenix.collections.internal.ListImplementation;

final class SmallPersistentVector<E extends @Nullable Object> extends AbstractPersistentList<E> {
  private static final SmallPersistentVector<?> EMPTY =
      new SmallPersistentVector<>(new @Nullable Object[0]);

  private final @Nullable Object[] buffer;

  SmallPersistentVector(@Nullable Object[] buffer) {
    assert buffer.length <= Utils.MAX_BUFFER_SIZE : "buffer.length <= Utils.MAX_BUFFER_SIZE";
    this.buffer = buffer;
  }

  static <E extends @Nullable Object> PersistentList<E> emptyOf() {
    @SuppressWarnings("unchecked")
    var empty = (PersistentList<E>) EMPTY;
    return empty;
  }

  @Override
  public int size() {
    return buffer.length;
  }

  private @Nullable Object[] bufferOfSize(int size) {
    return new @Nullable Object[size];
  }

  @Override
  public PersistentList<E> add(E element) {
    if (size() < Utils.MAX_BUFFER_SIZE) {
      var newBuffer = Arrays.copyOf(buffer, size() + 1);
      newBuffer[size()] = element;
      return new SmallPersistentVector<>(newBuffer);
    }
    var tail = Utils.presizedBufferWith(element);
    return new PersistentVector<>(buffer, tail, size() + 1, 0);
  }

  @Override
  public PersistentList<E> addAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return this;
    }
    if (size() + elements.size() <= Utils.MAX_BUFFER_SIZE) {
      var newBuffer = Arrays.copyOf(buffer, size() + elements.size());
      var index = size();
      for (var element : elements) {
        newBuffer[index++] = element;
      }
      return new SmallPersistentVector<>(newBuffer);
    }
    return mutate(list -> list.addAll(elements));
  }

  @Override
  public PersistentList<E> removeAll(Predicate<? super E> predicate) {
    var newSize = size();
    var removeMask = 0;

    for (var index = 0; index < size(); index++) {
      @SuppressWarnings("unchecked")
      var element = (E) buffer[index];
      if (predicate.test(element)) {
        newSize--;
        removeMask |= 1 << index;
      }
    }

    if (newSize == size()) {
      return this;
    }
    if (newSize == 0) {
      return emptyOf();
    }

    var newBuffer = Arrays.copyOf(buffer, newSize);
    var newIndex = Integer.numberOfTrailingZeros(removeMask);
    for (var index = newIndex + 1; index < size(); index++) {
      if (((removeMask >>> index) & 1) == 0) {
        newBuffer[newIndex++] = buffer[index];
      }
    }
    return new SmallPersistentVector<>(newBuffer);
  }

  @Override
  public PersistentList<E> addAll(int index, Collection<? extends E> elements) {
    ListImplementation.checkPositionIndex(index, size());
    if (elements.isEmpty()) {
      return this;
    }
    if (size() + elements.size() <= Utils.MAX_BUFFER_SIZE) {
      var newBuffer = bufferOfSize(size() + elements.size());
      System.arraycopy(buffer, 0, newBuffer, 0, index);
      System.arraycopy(buffer, index, newBuffer, index + elements.size(), size() - index);
      var position = index;
      for (var element : elements) {
        newBuffer[position++] = element;
      }
      return new SmallPersistentVector<>(newBuffer);
    }
    return mutate(list -> list.addAll(index, elements));
  }

  @Override
  public PersistentList<E> add(int index, E element) {
    ListImplementation.checkPositionIndex(index, size());
    if (index == size()) {
      return add(element);
    }
    if (size() < Utils.MAX_BUFFER_SIZE) {
      var newBuffer = bufferOfSize(size() + 1);
      System.arraycopy(buffer, 0, newBuffer, 0, index);
      System.arraycopy(buffer, index, newBuffer, index + 1, size() - index);
      newBuffer[index] = element;
      return new SmallPersistentVector<>(newBuffer);
    }

    var root = Arrays.copyOf(buffer, buffer.length);
    System.arraycopy(buffer, index, root, index + 1, size() - index - 1);
    root[index] = element;
    var tail = Utils.presizedBufferWith(buffer[Utils.MAX_BUFFER_SIZE_MINUS_ONE]);
    return new PersistentVector<>(root, tail, size() + 1, 0);
  }

  @Override
  public PersistentList<E> removeAt(int index) {
    ListImplementation.checkElementIndex(index, size());
    if (size() == 1) {
      return emptyOf();
    }
    var newBuffer = Arrays.copyOf(buffer, size() - 1);
    System.arraycopy(buffer, index + 1, newBuffer, index, size() - index - 1);
    return new SmallPersistentVector<>(newBuffer);
  }

  @Override
  public PersistentVectorBuilder<E> builder() {
    return new PersistentVectorBuilder<>(this, null, buffer, 0);
  }

  @Override
  public int indexOf(E element) {
    for (var index = 0; index < size(); index++) {
      if (Objects.equals(buffer[index], element)) {
        return index;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(E element) {
    for (var index = size() - 1; index >= 0; index--) {
      if (Objects.equals(buffer[index], element)) {
        return index;
      }
    }
    return -1;
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    ListImplementation.checkPositionIndex(index, size());
    @SuppressWarnings("unchecked")
    var typedBuffer = (E[]) buffer;
    return new BufferIterator<>(typedBuffer, index, size());
  }

  @Override
  public E get(int index) {
    ListImplementation.checkElementIndex(index, size());
    @SuppressWarnings("unchecked")
    var element = (E) buffer[index];
    return element;
  }

  @Override
  public PersistentList<E> set(int index, E element) {
    ListImplementation.checkElementIndex(index, size());
    var newBuffer = Arrays.copyOf(buffer, buffer.length);
    newBuffer[index] = element;
    return new SmallPersistentVector<>(newBuffer);
  }
}
