/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import java.util.Arrays;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.PersistentList;
import org.odenix.collections.internal.ListImplementation;

/// Persistent vector made of a trie of leaf buffers entirely filled with [MAX_BUFFER_SIZE][Utils#MAX_BUFFER_SIZE] elements and a tail having
/// from 1 to [MAX_BUFFER_SIZE][Utils#MAX_BUFFER_SIZE] elements.
public final class PersistentVector<E extends @Nullable Object> extends AbstractPersistentList<E> {
  private final @Nullable Object[] root;
  private final @Nullable Object[] tail;
  private final int size;
  private final int rootShift;

  /// @param root the root of trie part of the vector, must contain at least one leaf buffer
  /// @param tail the non-empty tail part of the vector
  /// @param size the size of the vector, must be greater than [MAX_BUFFER_SIZE][Utils#MAX_BUFFER_SIZE]
  /// @param rootShift specifies the height of the trie structure, so that `rootShift = (height - 1) * LOG_MAX_BUFFER_SIZE`;
  ///        elements in the {@code root} array are indexed with bits of the index starting from `rootShift` and until `rootShift + LOG_MAX_BUFFER_SIZE`.
  public PersistentVector(
      @Nullable Object[] root, @Nullable Object[] tail, int size, int rootShift) {
    if (size <= Utils.MAX_BUFFER_SIZE) {
      throw new IllegalArgumentException(
          "Trie-based persistent vector should have at least "
              + (Utils.MAX_BUFFER_SIZE + 1)
              + " elements, got "
              + size);
    }
    assert size - Utils.rootSize(size) <= Math.min(tail.length, Utils.MAX_BUFFER_SIZE)
        : "size - Utils.rootSize(size) <= Math.min(tail.length, Utils.MAX_BUFFER_SIZE)";
    this.root = root;
    this.tail = tail;
    this.size = size;
    this.rootShift = rootShift;
  }

  public static <E extends @Nullable Object> PersistentList<E> emptyOf() {
    return SmallPersistentVector.emptyOf();
  }

  private int rootSize() {
    return Utils.rootSize(size);
  }

  private int tailSize() {
    return size - rootSize();
  }

  /// Returns either leaf buffer of the trie or the tail, that contains element with the specified {@code index}.
  private @Nullable Object[] bufferFor(int index) {
    if (rootSize() <= index) {
      return tail;
    }
    var buffer = root;
    var shift = rootShift;
    while (shift > 0) {
      buffer = (@Nullable Object[]) buffer[Utils.indexSegment(index, shift)];
      shift -= Utils.LOG_MAX_BUFFER_SIZE;
    }
    return buffer;
  }

  @Override
  public E get(int index) {
    ListImplementation.checkElementIndex(index, size);
    var buffer = bufferFor(index);
    @SuppressWarnings("unchecked")
    var element = (E) buffer[index & Utils.MAX_BUFFER_SIZE_MINUS_ONE];
    return element;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public int indexOf(E element) {
    for (var index = 0; index < size; index++) {
      if (Objects.equals(get(index), element)) {
        return index;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(E element) {
    for (var index = size - 1; index >= 0; index--) {
      if (Objects.equals(get(index), element)) {
        return index;
      }
    }
    return -1;
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    ListImplementation.checkPositionIndex(index, size);
    return new PersistentVectorIterator<>(root, tail, index, size, rootShift / Utils.LOG_MAX_BUFFER_SIZE + 1);
  }

  @Override
  public PersistentList<E> add(E element) {
    var tailSize = tailSize();
    if (tailSize < Utils.MAX_BUFFER_SIZE) {
      var newTail = Arrays.copyOf(tail, Utils.MAX_BUFFER_SIZE);
      newTail[tailSize] = element;
      return new PersistentVector<>(root, newTail, size + 1, rootShift);
    }
    var newTail = Utils.presizedBufferWith(element);
    return pushFilledTail(root, tail, newTail);
  }

  /// Appends the specified entirely filled {@code tail} as a leaf buffer to the next free position in the {@code root} trie.
  private PersistentVector<E> pushFilledTail(
      @Nullable Object[] root, @Nullable Object[] filledTail, @Nullable Object[] newTail) {
    if (size >> Utils.LOG_MAX_BUFFER_SIZE > 1 << rootShift) {
      var newRoot = Utils.presizedBufferWith(root);
      var newRootShift = rootShift + Utils.LOG_MAX_BUFFER_SIZE;
      newRoot = pushTail(newRoot, newRootShift, filledTail);
      return new PersistentVector<>(newRoot, newTail, size + 1, newRootShift);
    }

    var newRoot = pushTail(root, rootShift, filledTail);
    return new PersistentVector<>(newRoot, newTail, size + 1, rootShift);
  }

  /// Appends the specified entirely filled {@code tail} as a leaf buffer to the next free position in the {@code root} trie.
  /// The trie must not be filled entirely.
  private @Nullable Object[] pushTail(
      @Nullable Object @Nullable [] root, int shift, @Nullable Object[] tail) {
    var bufferIndex = Utils.indexSegment(size - 1, shift);
    var newRootNode =
        root == null ? new @Nullable Object[Utils.MAX_BUFFER_SIZE] : Arrays.copyOf(root, Utils.MAX_BUFFER_SIZE);

    if (shift == Utils.LOG_MAX_BUFFER_SIZE) {
      newRootNode[bufferIndex] = tail;
    } else {
      newRootNode[bufferIndex] =
          pushTail((@Nullable Object[]) newRootNode[bufferIndex], shift - Utils.LOG_MAX_BUFFER_SIZE, tail);
    }
    return newRootNode;
  }

  @Override
  public PersistentList<E> set(int index, E element) {
    ListImplementation.checkElementIndex(index, size);
    if (rootSize() <= index) {
      var newTail = Arrays.copyOf(tail, Utils.MAX_BUFFER_SIZE);
      newTail[index & Utils.MAX_BUFFER_SIZE_MINUS_ONE] = element;
      return new PersistentVector<>(root, newTail, size, rootShift);
    }

    var newRoot = setInRoot(root, rootShift, index, element);
    return new PersistentVector<>(newRoot, tail, size, rootShift);
  }

  private @Nullable Object[] setInRoot(
      @Nullable Object[] root, int shift, int index, @Nullable Object element) {
    var bufferIndex = Utils.indexSegment(index, shift);
    var newRoot = Arrays.copyOf(root, Utils.MAX_BUFFER_SIZE);
    if (shift == 0) {
      newRoot[bufferIndex] = element;
    } else {
      newRoot[bufferIndex] =
          setInRoot(
              Objects.requireNonNull((@Nullable Object[]) newRoot[bufferIndex]),
              shift - Utils.LOG_MAX_BUFFER_SIZE,
              index,
              element);
    }
    return newRoot;
  }

  @Override
  public PersistentList<E> add(int index, E element) {
    ListImplementation.checkPositionIndex(index, size);
    if (index == size) {
      return add(element);
    }

    var rootSize = rootSize();
    if (index >= rootSize) {
      return insertIntoTail(root, index - rootSize, element);
    }

    var elementCarry = new ObjectRef(null);
    var newRoot = insertIntoRoot(root, rootShift, index, element, elementCarry);
    return insertIntoTail(newRoot, 0, elementCarry.value);
  }

  private PersistentList<E> insertIntoTail(
      @Nullable Object[] newRoot, int tailIndex, @Nullable Object element) {
    var tailSize = tailSize();
    var newTail = Arrays.copyOf(tail, Utils.MAX_BUFFER_SIZE);
    if (tailSize < Utils.MAX_BUFFER_SIZE) {
      System.arraycopy(tail, tailIndex, newTail, tailIndex + 1, tailSize - tailIndex);
      newTail[tailIndex] = element;
      return new PersistentVector<>(newRoot, newTail, size + 1, rootShift);
    }

    var lastElement = tail[Utils.MAX_BUFFER_SIZE_MINUS_ONE];
    System.arraycopy(tail, tailIndex, newTail, tailIndex + 1, tailSize - tailIndex - 1);
    newTail[tailIndex] = element;
    return pushFilledTail(newRoot, newTail, Utils.presizedBufferWith(lastElement));
  }

  /// Insert the specified {@code element} into the {@code root} trie at the specified trie {@code index}.
  ///
  /// {@code elementCarry} contains the last element of this trie that was popped out by the insertion operation.
  ///
  /// @return new root trie
  private @Nullable Object[] insertIntoRoot(
      @Nullable Object[] root, int shift, int index, @Nullable Object element, ObjectRef elementCarry) {
    var bufferIndex = Utils.indexSegment(index, shift);

    if (shift == 0) {
      var newRoot =
          bufferIndex == 0 ? new @Nullable Object[Utils.MAX_BUFFER_SIZE] : Arrays.copyOf(root, Utils.MAX_BUFFER_SIZE);
      System.arraycopy(root, bufferIndex, newRoot, bufferIndex + 1, Utils.MAX_BUFFER_SIZE_MINUS_ONE - bufferIndex);
      elementCarry.value = root[Utils.MAX_BUFFER_SIZE_MINUS_ONE];
      newRoot[bufferIndex] = element;
      return newRoot;
    }

    var newRoot = Arrays.copyOf(root, Utils.MAX_BUFFER_SIZE);
    var lowerLevelShift = shift - Utils.LOG_MAX_BUFFER_SIZE;
    newRoot[bufferIndex] =
        insertIntoRoot(
            Objects.requireNonNull((@Nullable Object[]) root[bufferIndex]),
            lowerLevelShift,
            index,
            element,
            elementCarry);

    for (var i = bufferIndex + 1; i < Utils.MAX_BUFFER_SIZE; i++) {
      if (newRoot[i] == null) {
        break;
      }
      newRoot[i] =
          insertIntoRoot(
              Objects.requireNonNull((@Nullable Object[]) root[i]),
              lowerLevelShift,
              0,
              elementCarry.value,
              elementCarry);
    }

    return newRoot;
  }

  @Override
  public PersistentList<E> removeAll(Predicate<? super E> predicate) {
    var builder = builder();
    builder.removeAllWithPredicate(predicate);
    return builder.build();
  }

  @Override
  public PersistentList<E> removeAt(int index) {
    ListImplementation.checkElementIndex(index, size);
    var rootSize = rootSize();
    if (index >= rootSize) {
      return removeFromTailAt(root, rootSize, rootShift, index - rootSize);
    }
    var newRoot = removeFromRootAt(root, rootShift, index, new ObjectRef(tail[0]));
    return removeFromTailAt(newRoot, rootSize, rootShift, 0);
  }

  private PersistentList<E> removeFromTailAt(
      @Nullable Object[] root, int rootSize, int shift, int index) {
    var tailSize = size - rootSize;
    assert index < tailSize : "index < tailSize";
    if (tailSize == 1) {
      return pullLastBufferFromRoot(root, rootSize, shift);
    }
    var newTail = Arrays.copyOf(tail, Utils.MAX_BUFFER_SIZE);
    if (index < tailSize - 1) {
      System.arraycopy(tail, index + 1, newTail, index, tailSize - index - 1);
    }
    newTail[tailSize - 1] = null;
    return new PersistentVector<>(root, newTail, rootSize + tailSize - 1, shift);
  }

  /// Extracts the last entirely filled leaf buffer from the trie of this vector and makes it a tail in the returned [PersistentVector].
  ///
  /// Used when there are no elements left in current tail.
  ///
  /// Requires the trie to contain at least one leaf buffer.
  ///
  /// If the trie becomes empty after the operation, returns a tail-only vector ([SmallPersistentVector]).
  private PersistentList<E> pullLastBufferFromRoot(
      @Nullable Object[] root, int rootSize, int shift) {
    if (shift == 0) {
      var buffer = root.length == Utils.MUTABLE_BUFFER_SIZE ? Arrays.copyOf(root, Utils.MAX_BUFFER_SIZE) : root;
      return new SmallPersistentVector<>(buffer);
    }
    var tailCarry = new ObjectRef(null);
    var newRoot = Objects.requireNonNull(pullLastBuffer(root, shift, rootSize - 1, tailCarry));
    var newTail = Objects.requireNonNull((@Nullable Object[]) tailCarry.value);

    if (newRoot[1] == null) {
      return new PersistentVector<>(
          Objects.requireNonNull((@Nullable Object[]) newRoot[0]),
          newTail,
          rootSize,
          shift - Utils.LOG_MAX_BUFFER_SIZE);
    }
    return new PersistentVector<>(newRoot, newTail, rootSize, shift);
  }

  /// Extracts the last leaf buffer from trie and returns new trie without it or `null` if there's no more leaf elements in this trie.
  ///
  /// {@code tailCarry} on output contains the extracted leaf buffer.
  private @Nullable Object @Nullable [] pullLastBuffer(
      @Nullable Object[] root, int shift, int index, ObjectRef tailCarry) {
    var bufferIndex = Utils.indexSegment(index, shift);

    @Nullable Object @Nullable [] newBufferAtIndex;
    if (shift == Utils.LOG_MAX_BUFFER_SIZE) {
      tailCarry.value = root[bufferIndex];
      newBufferAtIndex = null;
    } else {
      newBufferAtIndex =
          pullLastBuffer(
              Objects.requireNonNull((@Nullable Object[]) root[bufferIndex]),
              shift - Utils.LOG_MAX_BUFFER_SIZE,
              index,
              tailCarry);
    }

    if (newBufferAtIndex == null && bufferIndex == 0) {
      return null;
    }

    var newRoot = Arrays.copyOf(root, Utils.MAX_BUFFER_SIZE);
    newRoot[bufferIndex] = newBufferAtIndex;
    return newRoot;
  }

  /// Removes element from trie at the specified trie {@code index}.
  ///
  /// {@code tailCarry} on input contains the first element of the adjacent trie to fill the last vacant element with.
  /// {@code tailCarry} on output contains the first element of this trie.
  ///
  /// @return the new root of the trie.
  private @Nullable Object[] removeFromRootAt(
      @Nullable Object[] root, int shift, int index, ObjectRef tailCarry) {
    var bufferIndex = Utils.indexSegment(index, shift);

    if (shift == 0) {
      var newRoot =
          bufferIndex == 0 ? new @Nullable Object[Utils.MAX_BUFFER_SIZE] : Arrays.copyOf(root, Utils.MAX_BUFFER_SIZE);
      System.arraycopy(root, bufferIndex + 1, newRoot, bufferIndex, Utils.MAX_BUFFER_SIZE - bufferIndex - 1);
      newRoot[Utils.MAX_BUFFER_SIZE - 1] = tailCarry.value;
      tailCarry.value = root[bufferIndex];
      return newRoot;
    }

    var bufferLastIndex = Utils.MAX_BUFFER_SIZE_MINUS_ONE;
    if (root[bufferLastIndex] == null) {
      bufferLastIndex = Utils.indexSegment(rootSize() - 1, shift);
    }

    var newRoot = Arrays.copyOf(root, Utils.MAX_BUFFER_SIZE);
    var lowerLevelShift = shift - Utils.LOG_MAX_BUFFER_SIZE;

    for (var i = bufferLastIndex; i > bufferIndex; i--) {
      newRoot[i] =
          removeFromRootAt(
              Objects.requireNonNull((@Nullable Object[]) newRoot[i]),
              lowerLevelShift,
              0,
              tailCarry);
    }
    newRoot[bufferIndex] =
        removeFromRootAt(
            Objects.requireNonNull((@Nullable Object[]) newRoot[bufferIndex]),
            lowerLevelShift,
            index,
            tailCarry);

    return newRoot;
  }

  @Override
  public PersistentVectorBuilder<E> builder() {
    return new PersistentVectorBuilder<>(this, root, tail, rootShift);
  }
}
