/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableSet;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.internal.DeltaCounter;
import org.odenix.collections.internal.MutabilityOwnership;

final class TrieNode<E extends @Nullable Object> {
  static final int MAX_BRANCHING_FACTOR = 32;
  static final int LOG_MAX_BRANCHING_FACTOR = 5;
  static final int MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1;
  static final int MAX_SHIFT = 30;

  static final TrieNode<?> EMPTY = new TrieNode<>(0, new @Nullable Object[0]);

  int bitmap;
  @Nullable Object[] buffer;
  private final @Nullable MutabilityOwnership ownedBy;

  TrieNode(int bitmap, @Nullable Object[] buffer, @Nullable MutabilityOwnership ownedBy) {
    this.bitmap = bitmap;
    this.buffer = buffer;
    this.ownedBy = ownedBy;
  }

  TrieNode(int bitmap, @Nullable Object[] buffer) {
    this(bitmap, buffer, null);
  }

  /// Gets trie index segment of the specified {@code index} at the level specified by {@code shift}.
  ///
  /// `shift` equal to zero corresponds to the root level.
  /// For each lower level `shift` increments by [LOG_MAX_BRANCHING_FACTOR][#LOG_MAX_BRANCHING_FACTOR].
  static int indexSegment(int index, int shift) {
    return (index >> shift) & MAX_BRANCHING_FACTOR_MINUS_ONE;
  }

  private boolean hasNoCellAt(int positionMask) {
    return (bitmap & positionMask) == 0;
  }

  int indexOfCellAt(int positionMask) {
    return Integer.bitCount(bitmap & (positionMask - 1));
  }

  private E elementAtIndex(int index) {
    @SuppressWarnings("unchecked")
    var element = (E) buffer[index];
    return element;
  }

  private TrieNode<E> nodeAtIndex(int index) {
    @SuppressWarnings("unchecked")
    var node = Objects.requireNonNull((TrieNode<E>) buffer[index]);
    return node;
  }

  private TrieNode<E> addElementAt(int positionMask, E element, @Nullable MutabilityOwnership owner) {
    var index = indexOfCellAt(positionMask);
    var newBitmap = bitmap | positionMask;
    var newBuffer = addElementAtIndex(buffer, index, element);
    return setProperties(newBitmap, newBuffer, owner);
  }

  private TrieNode<E> setProperties(
      int newBitmap, @Nullable Object[] newBuffer, @Nullable MutabilityOwnership owner) {
    if (ownedBy != null && ownedBy == owner) {
      bitmap = newBitmap;
      buffer = newBuffer;
      return this;
    }
    return new TrieNode<>(newBitmap, newBuffer, owner);
  }

  /// The given {@code newNode} must not be a part of any persistent set instance.
  private TrieNode<E> canonicalizeNodeAtIndex(
      int nodeIndex, TrieNode<E> newNode, @Nullable MutabilityOwnership owner) {
    @Nullable Object cell;
    var newNodeBuffer = newNode.buffer;
    if (newNodeBuffer.length == 1 && !(newNodeBuffer[0] instanceof TrieNode<?>)) {
      if (buffer.length == 1) {
        newNode.bitmap = bitmap;
        return newNode;
      }
      cell = newNodeBuffer[0];
    } else {
      cell = newNode;
    }
    return setCellAtIndex(nodeIndex, cell, owner);
  }

  private TrieNode<E> setCellAtIndex(
      int cellIndex, @Nullable Object newCell, @Nullable MutabilityOwnership owner) {
    if (ownedBy != null && ownedBy == owner) {
      buffer[cellIndex] = newCell;
      return this;
    }
    var newBuffer = buffer.clone();
    newBuffer[cellIndex] = newCell;
    return new TrieNode<>(bitmap, newBuffer, owner);
  }

  private TrieNode<E> makeNodeAtIndex(
      int elementIndex, int newElementHash, E newElement, int shift, @Nullable MutabilityOwnership owner) {
    var storedElement = elementAtIndex(elementIndex);
    return makeNode(
        storedElement == null ? 0 : storedElement.hashCode(),
        storedElement,
        newElementHash,
        newElement,
        shift + LOG_MAX_BRANCHING_FACTOR,
        owner);
  }

  private TrieNode<E> moveElementToNode(
      int elementIndex, int newElementHash, E newElement, int shift, @Nullable MutabilityOwnership owner) {
    var node = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, owner);
    return setCellAtIndex(elementIndex, node, owner);
  }

  private TrieNode<E> makeNode(
      int elementHash1,
      E element1,
      int elementHash2,
      E element2,
      int shift,
      @Nullable MutabilityOwnership owner) {
    if (shift > MAX_SHIFT) {
      return new TrieNode<>(0, new @Nullable Object[] {element1, element2}, owner);
    }

    var setBit1 = indexSegment(elementHash1, shift);
    var setBit2 = indexSegment(elementHash2, shift);

    if (setBit1 != setBit2) {
      @Nullable Object[] nodeBuffer =
          setBit1 < setBit2
              ? new @Nullable Object[] {element1, element2}
              : new @Nullable Object[] {element2, element1};
      return new TrieNode<>((1 << setBit1) | (1 << setBit2), nodeBuffer, owner);
    }

    var node =
        makeNode(
            elementHash1,
            element1,
            elementHash2,
            element2,
            shift + LOG_MAX_BRANCHING_FACTOR,
            owner);
    return new TrieNode<>(1 << setBit1, new @Nullable Object[] {node}, owner);
  }

  private TrieNode<E> removeCellAtIndex(
      int cellIndex, int positionMask, @Nullable MutabilityOwnership owner) {
    var newBitmap = bitmap ^ positionMask;
    var newBuffer = removeCellAtIndex(buffer, cellIndex);
    return setProperties(newBitmap, newBuffer, owner);
  }

  private TrieNode<E> collisionRemoveElementAtIndex(
      int index, @Nullable MutabilityOwnership owner) {
    var newBuffer = removeCellAtIndex(buffer, index);
    return setProperties(0, newBuffer, owner);
  }

  private boolean collisionContainsElement(@Nullable Object element) {
    for (var stored : buffer) {
      if (Objects.equals(stored, element)) {
        return true;
      }
    }
    return false;
  }

  private TrieNode<E> collisionAdd(E element) {
    if (collisionContainsElement(element)) {
      return this;
    }
    var newBuffer = addElementAtIndex(buffer, 0, element);
    return setProperties(0, newBuffer, null);
  }

  private TrieNode<E> mutableCollisionAdd(E element, PersistentHashSetBuilder<?> mutator) {
    if (collisionContainsElement(element)) {
      return this;
    }
    mutator.setSize(mutator.size() + 1);
    var newBuffer = addElementAtIndex(buffer, 0, element);
    return setProperties(0, newBuffer, mutator.ownership);
  }

  private TrieNode<E> collisionRemove(@Nullable Object element) {
    for (var index = 0; index < buffer.length; index++) {
      if (Objects.equals(buffer[index], element)) {
        return collisionRemoveElementAtIndex(index, null);
      }
    }
    return this;
  }

  private TrieNode<E> mutableCollisionRemove(
      @Nullable Object element, PersistentHashSetBuilder<?> mutator) {
    for (var index = 0; index < buffer.length; index++) {
      if (Objects.equals(buffer[index], element)) {
        mutator.setSize(mutator.size() - 1);
        return collisionRemoveElementAtIndex(index, mutator.ownership);
      }
    }
    return this;
  }

  private TrieNode<E> mutableCollisionAddAll(
      TrieNode<E> otherNode, DeltaCounter intersectionSizeRef, MutabilityOwnership owner) {
    if (this == otherNode) {
      intersectionSizeRef.count += buffer.length;
      return this;
    }
    var tempBuffer = new @Nullable Object[buffer.length + otherNode.buffer.length];
    System.arraycopy(buffer, 0, tempBuffer, 0, buffer.length);
    var totalWritten = 0;
    for (var element : otherNode.buffer) {
      if (!collisionContainsElement(element)) {
        tempBuffer[buffer.length + totalWritten] = element;
        totalWritten++;
      }
    }
    var totalSize = totalWritten + buffer.length;
    intersectionSizeRef.count += tempBuffer.length - totalSize;
    if (totalSize == buffer.length) {
      return this;
    }
    if (totalSize == otherNode.buffer.length) {
      return otherNode;
    }
    var newBuffer = totalSize == tempBuffer.length ? tempBuffer : copyOf(tempBuffer, totalSize);
    return setProperties(0, newBuffer, owner);
  }

  private @Nullable Object mutableCollisionRetainAll(
      TrieNode<E> otherNode, DeltaCounter intersectionSizeRef, MutabilityOwnership owner) {
    if (this == otherNode) {
      intersectionSizeRef.count += buffer.length;
      return this;
    }
    var tempBuffer =
        owner == ownedBy
            ? buffer
            : new @Nullable Object[Math.min(buffer.length, otherNode.buffer.length)];
    var totalWritten = 0;
    for (var element : buffer) {
      if (otherNode.collisionContainsElement(element)) {
        tempBuffer[totalWritten] = element;
        totalWritten++;
      }
    }
    intersectionSizeRef.count += totalWritten;
    return switch (totalWritten) {
      case 0 -> EMPTY;
      case 1 -> tempBuffer[0];
      default -> {
        if (totalWritten == buffer.length) {
          yield this;
        }
        if (totalWritten == otherNode.buffer.length) {
          yield otherNode;
        }
        yield totalWritten == tempBuffer.length
            ? setProperties(0, tempBuffer, owner)
            : setProperties(0, copyOf(tempBuffer, totalWritten), owner);
      }
    };
  }

  private @Nullable Object mutableCollisionRemoveAll(
      TrieNode<E> otherNode, DeltaCounter intersectionSizeRef, MutabilityOwnership owner) {
    if (this == otherNode) {
      intersectionSizeRef.count += buffer.length;
      return EMPTY;
    }
    var tempBuffer = owner == ownedBy ? buffer : new @Nullable Object[buffer.length];
    var totalWritten = 0;
    for (var element : buffer) {
      if (!otherNode.collisionContainsElement(element)) {
        tempBuffer[totalWritten] = element;
        totalWritten++;
      }
    }
    intersectionSizeRef.count += buffer.length - totalWritten;
    return switch (totalWritten) {
      case 0 -> EMPTY;
      case 1 -> tempBuffer[0];
      default -> {
        if (totalWritten == buffer.length) {
          yield this;
        }
        yield totalWritten == tempBuffer.length
            ? setProperties(0, tempBuffer, owner)
            : setProperties(0, copyOf(tempBuffer, totalWritten), owner);
      }
    };
  }

  private int calculateSize() {
    if (bitmap == 0) {
      return buffer.length;
    }
    var result = 0;
    for (var element : buffer) {
      result += element instanceof TrieNode<?> node ? node.calculateSize() : 1;
    }
    return result;
  }

  private boolean elementsIdentityEquals(TrieNode<E> otherNode) {
    if (this == otherNode) {
      return true;
    }
    if (bitmap != otherNode.bitmap || buffer.length != otherNode.buffer.length) {
      return false;
    }
    for (var index = 0; index < buffer.length; index++) {
      if (buffer[index] != otherNode.buffer[index]) {
        return false;
      }
    }
    return true;
  }

  boolean contains(int elementHash, @Nullable Object element, int shift) {
    var cellPositionMask = 1 << indexSegment(elementHash, shift);
    if (hasNoCellAt(cellPositionMask)) {
      return false;
    }

    var cellIndex = indexOfCellAt(cellPositionMask);
    if (buffer[cellIndex] instanceof TrieNode<?>) {
      var targetNode = nodeAtIndex(cellIndex);
      if (shift == MAX_SHIFT) {
        return targetNode.collisionContainsElement(element);
      }
      return targetNode.contains(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR);
    }
    return Objects.equals(element, buffer[cellIndex]);
  }

  TrieNode<E> mutableAddAll(
      TrieNode<E> otherNode,
      int shift,
      DeltaCounter intersectionSizeRef,
      PersistentHashSetBuilder<?> mutator) {
    if (this == otherNode) {
      intersectionSizeRef.count += calculateSize();
      return this;
    }
    if (shift > MAX_SHIFT) {
      return mutableCollisionAddAll(otherNode, intersectionSizeRef, mutator.ownership);
    }

    var newBitmap = bitmap | otherNode.bitmap;
    TrieNode<E> mutableNode =
        newBitmap == bitmap && ownedBy == mutator.ownership
            ? this
            : new TrieNode<>(
                newBitmap, new @Nullable Object[Integer.bitCount(newBitmap)], mutator.ownership);

    var mask = newBitmap;
    var newNodeIndex = 0;
    while (mask != 0) {
      var positionMask = Integer.lowestOneBit(mask);
      var thisIndex = indexOfCellAt(positionMask);
      var otherNodeIndex = otherNode.indexOfCellAt(positionMask);

      if (hasNoCellAt(positionMask)) {
        mutableNode.buffer[newNodeIndex] = otherNode.buffer[otherNodeIndex];
        newNodeIndex++;
        mask ^= positionMask;
        continue;
      }
      if (otherNode.hasNoCellAt(positionMask)) {
        mutableNode.buffer[newNodeIndex] = buffer[thisIndex];
        newNodeIndex++;
        mask ^= positionMask;
        continue;
      }
      var thisCell = buffer[thisIndex];
      var otherNodeCell = otherNode.buffer[otherNodeIndex];
      var thisIsNode = thisCell instanceof TrieNode<?>;
      var otherIsNode = otherNodeCell instanceof TrieNode<?>;
      if (thisIsNode && otherIsNode) {
        @SuppressWarnings("unchecked")
        var thisNode = (TrieNode<E>) thisCell;
        @SuppressWarnings("unchecked")
        var otherTrieNode = (TrieNode<E>) otherNodeCell;
        mutableNode.buffer[newNodeIndex] =
            thisNode.mutableAddAll(
                otherTrieNode,
                shift + LOG_MAX_BRANCHING_FACTOR,
                intersectionSizeRef,
                mutator);
        newNodeIndex++;
        mask ^= positionMask;
        continue;
      }
      if (thisIsNode) {
        @SuppressWarnings("unchecked")
        var thisNode = (TrieNode<E>) thisCell;
        @SuppressWarnings("unchecked")
        var otherElement = (E) otherNodeCell;
        var oldSize = mutator.size();
        var result =
            thisNode.mutableAdd(
                otherElement == null ? 0 : otherElement.hashCode(),
                otherElement,
                shift + LOG_MAX_BRANCHING_FACTOR,
                mutator);
        if (mutator.size() == oldSize) {
          intersectionSizeRef.count++;
        }
        mutableNode.buffer[newNodeIndex] = result;
        newNodeIndex++;
        mask ^= positionMask;
        continue;
      }
      if (otherIsNode) {
        @SuppressWarnings("unchecked")
        var otherTrieNode = (TrieNode<E>) otherNodeCell;
        @SuppressWarnings("unchecked")
        var thisElement = (E) thisCell;
        var oldSize = mutator.size();
        var result =
            otherTrieNode.mutableAdd(
                thisElement == null ? 0 : thisElement.hashCode(),
                thisElement,
                shift + LOG_MAX_BRANCHING_FACTOR,
                mutator);
        if (mutator.size() == oldSize) {
          intersectionSizeRef.count++;
        }
        mutableNode.buffer[newNodeIndex] = result;
        newNodeIndex++;
        mask ^= positionMask;
        continue;
      }
      if (Objects.equals(thisCell, otherNodeCell)) {
        intersectionSizeRef.count++;
        mutableNode.buffer[newNodeIndex] = thisCell;
        newNodeIndex++;
        mask ^= positionMask;
        continue;
      }
      @SuppressWarnings("unchecked")
      var thisElement = (E) thisCell;
      @SuppressWarnings("unchecked")
      var otherElement = (E) otherNodeCell;
      mutableNode.buffer[newNodeIndex] =
          makeNode(
              thisElement == null ? 0 : thisElement.hashCode(),
              thisElement,
              otherElement == null ? 0 : otherElement.hashCode(),
              otherElement,
              shift + LOG_MAX_BRANCHING_FACTOR,
              mutator.ownership);
      newNodeIndex++;
      mask ^= positionMask;
    }

    if (elementsIdentityEquals(mutableNode)) {
      return this;
    }
    if (otherNode.elementsIdentityEquals(mutableNode)) {
      return otherNode;
    }
    return mutableNode;
  }

  @Nullable Object mutableRetainAll(
      TrieNode<E> otherNode,
      int shift,
      DeltaCounter intersectionSizeRef,
      PersistentHashSetBuilder<?> mutator) {
    if (this == otherNode) {
      intersectionSizeRef.count += calculateSize();
      return this;
    }
    if (shift > MAX_SHIFT) {
      return mutableCollisionRetainAll(otherNode, intersectionSizeRef, mutator.ownership);
    }

    var newBitmap = bitmap & otherNode.bitmap;
    if (newBitmap == 0) {
      return EMPTY;
    }
    var mutableNode =
        ownedBy == mutator.ownership && newBitmap == bitmap
            ? this
            : new TrieNode<E>(
                newBitmap, new @Nullable Object[Integer.bitCount(newBitmap)], mutator.ownership);
    var realBitmap = new int[] {0};

    var mask = newBitmap;
    var newNodeIndex = 0;
    while (mask != 0) {
      var positionMask = Integer.lowestOneBit(mask);
      var thisIndex = indexOfCellAt(positionMask);
      var otherNodeIndex = otherNode.indexOfCellAt(positionMask);
      @Nullable Object newValue;
      var thisCell = buffer[thisIndex];
      var otherNodeCell = otherNode.buffer[otherNodeIndex];
      var thisIsNode = thisCell instanceof TrieNode<?>;
      var otherIsNode = otherNodeCell instanceof TrieNode<?>;
      if (thisIsNode && otherIsNode) {
        @SuppressWarnings("unchecked")
        var thisNode = (TrieNode<E>) thisCell;
        @SuppressWarnings("unchecked")
        var otherTrieNode = (TrieNode<E>) otherNodeCell;
        newValue =
            thisNode.mutableRetainAll(
                otherTrieNode,
                shift + LOG_MAX_BRANCHING_FACTOR,
                intersectionSizeRef,
                mutator);
      } else if (thisIsNode) {
        @SuppressWarnings("unchecked")
        var thisNode = (TrieNode<E>) thisCell;
        @SuppressWarnings("unchecked")
        var otherElement = (E) otherNodeCell;
        if (thisNode.contains(
            otherElement == null ? 0 : otherElement.hashCode(), otherElement, shift + LOG_MAX_BRANCHING_FACTOR)) {
          intersectionSizeRef.count++;
          newValue = otherElement;
        } else {
          newValue = EMPTY;
        }
      } else if (otherIsNode) {
        @SuppressWarnings("unchecked")
        var otherTrieNode = (TrieNode<E>) otherNodeCell;
        @SuppressWarnings("unchecked")
        var thisElement = (E) thisCell;
        if (otherTrieNode.contains(
            thisElement == null ? 0 : thisElement.hashCode(), thisElement, shift + LOG_MAX_BRANCHING_FACTOR)) {
          intersectionSizeRef.count++;
          newValue = thisElement;
        } else {
          newValue = EMPTY;
        }
      } else if (Objects.equals(thisCell, otherNodeCell)) {
        intersectionSizeRef.count++;
        newValue = thisCell;
      } else {
        newValue = EMPTY;
      }
      if (newValue != EMPTY) {
        realBitmap[0] |= positionMask;
      }
      mutableNode.buffer[newNodeIndex] = newValue;
      newNodeIndex++;
      mask ^= positionMask;
    }

    var realSize = Integer.bitCount(realBitmap[0]);
    if (realBitmap[0] == 0) {
      return EMPTY;
    }
    if (realBitmap[0] == newBitmap) {
      if (mutableNode.elementsIdentityEquals(this)) {
        return this;
      }
      if (mutableNode.elementsIdentityEquals(otherNode)) {
        return otherNode;
      }
      return mutableNode;
    }
    if (realSize == 1 && shift != 0) {
      var single = mutableNode.buffer[mutableNode.indexOfCellAt(realBitmap[0])];
      return single instanceof TrieNode<?>
          ? new TrieNode<E>(realBitmap[0], new @Nullable Object[] {single}, mutator.ownership)
          : single;
    }
    var realBuffer = new @Nullable Object[realSize];
    filterNonEmptyTo(mutableNode.buffer, realBuffer, 0);
    return new TrieNode<E>(realBitmap[0], realBuffer, mutator.ownership);
  }

  @Nullable Object mutableRemoveAll(
      TrieNode<E> otherNode,
      int shift,
      DeltaCounter intersectionSizeRef,
      PersistentHashSetBuilder<?> mutator) {
    if (this == otherNode) {
      intersectionSizeRef.count += calculateSize();
      return EMPTY;
    }
    if (shift > MAX_SHIFT) {
      return mutableCollisionRemoveAll(otherNode, intersectionSizeRef, mutator.ownership);
    }

    var removalBitmap = bitmap & otherNode.bitmap;
    if (removalBitmap == 0) {
      return this;
    }
    TrieNode<E> mutableNode =
        ownedBy == mutator.ownership
            ? this
            : new TrieNode<>(bitmap, buffer.clone(), mutator.ownership);
    var realBitmap = new int[] {bitmap};

    var mask = removalBitmap;
    var index = 0;
    while (mask != 0) {
      var positionMask = Integer.lowestOneBit(mask);
      var thisIndex = indexOfCellAt(positionMask);
      var otherNodeIndex = otherNode.indexOfCellAt(positionMask);
      @Nullable Object newValue;
      var thisCell = buffer[thisIndex];
      var otherNodeCell = otherNode.buffer[otherNodeIndex];
      var thisIsNode = thisCell instanceof TrieNode<?>;
      var otherIsNode = otherNodeCell instanceof TrieNode<?>;
      if (thisIsNode && otherIsNode) {
        @SuppressWarnings("unchecked")
        var thisNode = (TrieNode<E>) thisCell;
        @SuppressWarnings("unchecked")
        var otherTrieNode = (TrieNode<E>) otherNodeCell;
        newValue =
            thisNode.mutableRemoveAll(
                otherTrieNode,
                shift + LOG_MAX_BRANCHING_FACTOR,
                intersectionSizeRef,
                mutator);
      } else if (thisIsNode) {
        @SuppressWarnings("unchecked")
        var thisNode = (TrieNode<E>) thisCell;
        @SuppressWarnings("unchecked")
        var otherElement = (E) otherNodeCell;
        var oldSize = mutator.size();
        var removed =
            thisNode.mutableRemove(
                otherElement == null ? 0 : otherElement.hashCode(),
                otherElement,
                shift + LOG_MAX_BRANCHING_FACTOR,
                mutator);
        if (oldSize != mutator.size()) {
          intersectionSizeRef.count++;
          newValue =
              removed.buffer.length == 1 && !(removed.buffer[0] instanceof TrieNode<?>)
                  ? removed.buffer[0]
                  : removed;
        } else {
          newValue = thisCell;
        }
      } else if (otherIsNode) {
        @SuppressWarnings("unchecked")
        var otherTrieNode = (TrieNode<E>) otherNodeCell;
        @SuppressWarnings("unchecked")
        var thisElement = (E) thisCell;
        if (otherTrieNode.contains(
            thisElement == null ? 0 : thisElement.hashCode(), thisElement, shift + LOG_MAX_BRANCHING_FACTOR)) {
          intersectionSizeRef.count++;
          newValue = EMPTY;
        } else {
          newValue = thisCell;
        }
      } else if (Objects.equals(thisCell, otherNodeCell)) {
        intersectionSizeRef.count++;
        newValue = EMPTY;
      } else {
        newValue = thisCell;
      }
      if (newValue == EMPTY) {
        realBitmap[0] ^= positionMask;
      }
      mutableNode.buffer[thisIndex] = newValue;
      index++;
      mask ^= positionMask;
    }

    var realSize = Integer.bitCount(realBitmap[0]);
    if (realBitmap[0] == 0) {
      return EMPTY;
    }
    if (realSize == 1 && shift != 0) {
      var single = mutableNode.buffer[mutableNode.indexOfCellAt(realBitmap[0])];
      return single instanceof TrieNode<?>
          ? new TrieNode<E>(realBitmap[0], new @Nullable Object[] {single}, mutator.ownership)
          : single;
    }
    if (realBitmap[0] == bitmap) {
      return mutableNode.elementsIdentityEquals(this) ? this : mutableNode;
    }
    var realBuffer = new @Nullable Object[realSize];
    filterNonEmptyTo(mutableNode.buffer, realBuffer, 0);
    return new TrieNode<E>(realBitmap[0], realBuffer, mutator.ownership);
  }

  boolean containsAll(TrieNode<E> otherNode, int shift) {
    if (this == otherNode) {
      return true;
    }
    if (shift > MAX_SHIFT) {
      for (var element : otherNode.buffer) {
        if (!collisionContainsElement(element)) {
          return false;
        }
      }
      return true;
    }
    var potentialBitmap = bitmap & otherNode.bitmap;
    if (potentialBitmap != otherNode.bitmap) {
      return false;
    }
    var mask = potentialBitmap;
    var index = 0;
    while (mask != 0) {
      var positionMask = Integer.lowestOneBit(mask);
      var thisIndex = indexOfCellAt(positionMask);
      var otherNodeIndex = otherNode.indexOfCellAt(positionMask);
      var thisCell = buffer[thisIndex];
      var otherNodeCell = otherNode.buffer[otherNodeIndex];
      var thisIsNode = thisCell instanceof TrieNode<?>;
      var otherIsNode = otherNodeCell instanceof TrieNode<?>;
      if (thisIsNode && otherIsNode) {
        @SuppressWarnings("unchecked")
        var thisNode = (TrieNode<E>) thisCell;
        @SuppressWarnings("unchecked")
        var otherTrieNode = (TrieNode<E>) otherNodeCell;
        if (!thisNode.containsAll(otherTrieNode, shift + LOG_MAX_BRANCHING_FACTOR)) {
          return false;
        }
      } else if (thisIsNode) {
        @SuppressWarnings("unchecked")
        var thisNode = (TrieNode<E>) thisCell;
        @SuppressWarnings("unchecked")
        var otherElement = (E) otherNodeCell;
        if (!thisNode.contains(
            otherElement == null ? 0 : otherElement.hashCode(),
            otherElement,
            shift + LOG_MAX_BRANCHING_FACTOR)) {
          return false;
        }
      } else if (otherIsNode) {
        return false;
      } else if (!Objects.equals(thisCell, otherNodeCell)) {
        return false;
      }
      index++;
      mask ^= positionMask;
    }
    return true;
  }

  TrieNode<E> add(int elementHash, E element, int shift) {
    var cellPositionMask = 1 << indexSegment(elementHash, shift);
    if (hasNoCellAt(cellPositionMask)) {
      return addElementAt(cellPositionMask, element, null);
    }

    var cellIndex = indexOfCellAt(cellPositionMask);
    if (buffer[cellIndex] instanceof TrieNode<?>) {
      var targetNode = nodeAtIndex(cellIndex);
      var newNode =
          shift == MAX_SHIFT
              ? targetNode.collisionAdd(element)
              : targetNode.add(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR);
      if (targetNode == newNode) {
        return this;
      }
      return setCellAtIndex(cellIndex, newNode, null);
    }
    if (Objects.equals(element, buffer[cellIndex])) {
      return this;
    }
    return moveElementToNode(cellIndex, elementHash, element, shift, null);
  }

  TrieNode<E> mutableAdd(
      int elementHash, E element, int shift, PersistentHashSetBuilder<?> mutator) {
    var cellPosition = 1 << indexSegment(elementHash, shift);
    if (hasNoCellAt(cellPosition)) {
      mutator.setSize(mutator.size() + 1);
      return addElementAt(cellPosition, element, mutator.ownership);
    }

    var cellIndex = indexOfCellAt(cellPosition);
    if (buffer[cellIndex] instanceof TrieNode<?>) {
      var targetNode = nodeAtIndex(cellIndex);
      var newNode =
          shift == MAX_SHIFT
              ? targetNode.mutableCollisionAdd(element, mutator)
              : targetNode.mutableAdd(
                  elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, mutator);
      if (targetNode == newNode) {
        return this;
      }
      return setCellAtIndex(cellIndex, newNode, mutator.ownership);
    }
    if (Objects.equals(element, buffer[cellIndex])) {
      return this;
    }
    mutator.setSize(mutator.size() + 1);
    return moveElementToNode(cellIndex, elementHash, element, shift, mutator.ownership);
  }

  TrieNode<E> remove(int elementHash, @Nullable Object element, int shift) {
    var cellPositionMask = 1 << indexSegment(elementHash, shift);
    if (hasNoCellAt(cellPositionMask)) {
      return this;
    }

    var cellIndex = indexOfCellAt(cellPositionMask);
    if (buffer[cellIndex] instanceof TrieNode<?>) {
      var targetNode = nodeAtIndex(cellIndex);
      var newNode =
          shift == MAX_SHIFT
              ? targetNode.collisionRemove(element)
              : targetNode.remove(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR);
      if (targetNode == newNode) {
        return this;
      }
      return canonicalizeNodeAtIndex(cellIndex, newNode, null);
    }
    if (Objects.equals(element, buffer[cellIndex])) {
      return removeCellAtIndex(cellIndex, cellPositionMask, null);
    }
    return this;
  }

  TrieNode<E> mutableRemove(
      int elementHash, @Nullable Object element, int shift, PersistentHashSetBuilder<?> mutator) {
    var cellPositionMask = 1 << indexSegment(elementHash, shift);
    if (hasNoCellAt(cellPositionMask)) {
      return this;
    }

    var cellIndex = indexOfCellAt(cellPositionMask);
    if (buffer[cellIndex] instanceof TrieNode<?>) {
      var targetNode = nodeAtIndex(cellIndex);
      var newNode =
          shift == MAX_SHIFT
              ? targetNode.mutableCollisionRemove(element, mutator)
              : targetNode.mutableRemove(
                  elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, mutator);
      if (targetNode.ownedBy != mutator.ownership && targetNode == newNode) {
        return this;
      }
      return canonicalizeNodeAtIndex(cellIndex, newNode, mutator.ownership);
    }
    if (Objects.equals(element, buffer[cellIndex])) {
      mutator.setSize(mutator.size() - 1);
      return removeCellAtIndex(cellIndex, cellPositionMask, mutator.ownership);
    }
    return this;
  }

  private static <E extends @Nullable Object> @Nullable Object[] addElementAtIndex(
      @Nullable Object[] buffer, int index, E element) {
    var newBuffer = new @Nullable Object[buffer.length + 1];
    System.arraycopy(buffer, 0, newBuffer, 0, index);
    System.arraycopy(buffer, index, newBuffer, index + 1, buffer.length - index);
    newBuffer[index] = element;
    return newBuffer;
  }

  private static @Nullable Object[] removeCellAtIndex(@Nullable Object[] buffer, int cellIndex) {
    var newBuffer = new @Nullable Object[buffer.length - 1];
    System.arraycopy(buffer, 0, newBuffer, 0, cellIndex);
    System.arraycopy(buffer, cellIndex + 1, newBuffer, cellIndex, buffer.length - cellIndex - 1);
    return newBuffer;
  }

  private static @Nullable Object[] copyOf(@Nullable Object[] source, int newSize) {
    var copy = new @Nullable Object[newSize];
    System.arraycopy(source, 0, copy, 0, Math.min(source.length, newSize));
    return copy;
  }

  private static int filterNonEmptyTo(
      @Nullable Object[] source, @Nullable Object[] target, int targetOffset) {
    var written = 0;
    for (var element : source) {
      if (element != EMPTY) {
        target[targetOffset + written] = element;
        written++;
      }
    }
    return written;
  }

}
