/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

class PersistentHashSetIterator<E extends @Nullable Object> implements Iterator<E> {
  protected final ArrayList<TrieNodeIterator<E>> path = new ArrayList<>();
  protected int pathLastIndex;
  private boolean hasNext = true;

  PersistentHashSetIterator(TrieNode<E> node) {
    path.add(new TrieNodeIterator<>());
    path.get(0).reset(node.buffer);
    pathLastIndex = 0;
    ensureNextElementIsReady();
  }

  private int moveToNextNodeWithData(int pathIndex) {
    if (path.get(pathIndex).hasNextElement()) {
      return pathIndex;
    }
    if (path.get(pathIndex).hasNextNode()) {
      var node = path.get(pathIndex).currentNode();
      if (pathIndex + 1 == path.size()) {
        path.add(new TrieNodeIterator<>());
      }
      path.get(pathIndex + 1).reset(node.buffer);
      return moveToNextNodeWithData(pathIndex + 1);
    }
    return -1;
  }

  private void ensureNextElementIsReady() {
    if (path.get(pathLastIndex).hasNextElement()) {
      return;
    }
    for (var index = pathLastIndex; index >= 0; index--) {
      var result = moveToNextNodeWithData(index);
      if (result == -1 && path.get(index).hasNextCell()) {
        path.get(index).moveToNextCell();
        result = moveToNextNodeWithData(index);
      }
      if (result != -1) {
        pathLastIndex = result;
        return;
      }
      if (index > 0) {
        path.get(index - 1).moveToNextCell();
      }
      path.get(index).reset(TrieNode.EMPTY.buffer, 0);
    }
    hasNext = false;
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public E next() {
    if (!hasNext) {
      throw new NoSuchElementException();
    }
    var result = path.get(pathLastIndex).nextElement();
    ensureNextElementIsReady();
    return result;
  }

  protected E currentElement() {
    assert hasNext() : "hasNext()";
    return path.get(pathLastIndex).currentElement();
  }
}

final class TrieNodeIterator<E extends @Nullable Object> {
  private @Nullable Object[] buffer = TrieNode.EMPTY.buffer;
  private int index;

  void reset(@Nullable Object[] buffer) {
    reset(buffer, 0);
  }

  void reset(@Nullable Object[] buffer, int index) {
    this.buffer = buffer;
    this.index = index;
  }

  boolean hasNextCell() {
    return index < buffer.length;
  }

  void moveToNextCell() {
    assert hasNextCell() : "hasNextCell()";
    index++;
  }

  boolean hasNextElement() {
    return hasNextCell() && !(buffer[index] instanceof TrieNode<?>);
  }

  E currentElement() {
    assert hasNextElement() : "hasNextElement()";
    @SuppressWarnings("unchecked")
    var element = (E) buffer[index];
    return element;
  }

  E nextElement() {
    assert hasNextElement() : "hasNextElement()";
    @SuppressWarnings("unchecked")
    var element = (E) buffer[index++];
    return element;
  }

  boolean hasNextNode() {
    return hasNextCell() && buffer[index] instanceof TrieNode<?>;
  }

  TrieNode<? extends E> currentNode() {
    assert hasNextNode() : "hasNextNode()";
    @SuppressWarnings("unchecked")
    var node = Objects.requireNonNull((TrieNode<E>) buffer[index]);
    return node;
  }
}
