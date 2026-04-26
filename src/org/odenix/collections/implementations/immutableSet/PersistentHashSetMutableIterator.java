/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableSet;

import java.util.ConcurrentModificationException;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

final class PersistentHashSetMutableIterator<E extends @Nullable Object>
    extends PersistentHashSetIterator<E> {
  private @Nullable E lastIteratedElement;
  private boolean nextWasInvoked;
  private int expectedModCount;
  private final PersistentHashSetBuilder<E> builder;

  PersistentHashSetMutableIterator(PersistentHashSetBuilder<E> builder) {
    super(builder.node);
    this.builder = builder;
    this.expectedModCount = builder.modCount;
  }

  @Override
  public E next() {
    checkForComodification();
    var next = super.next();
    lastIteratedElement = next;
    nextWasInvoked = true;
    return next;
  }

  @Override
  public void remove() {
    checkNextWasInvoked();
    if (hasNext()) {
      var currentElement = currentElement();
      builder.remove(lastIteratedElement);
      resetPath(currentElement == null ? 0 : currentElement.hashCode(), builder.node, currentElement, 0);
    } else {
      builder.remove(lastIteratedElement);
    }
    lastIteratedElement = null;
    nextWasInvoked = false;
    expectedModCount = builder.modCount;
  }

  private void resetPath(int hashCode, TrieNode<?> node, @Nullable E element, int pathIndex) {
    if (isCollision(node)) {
      var index = indexOf(node.buffer, element);
      assert index != -1 : "index != -1";
      path.get(pathIndex).reset(node.buffer, index);
      pathLastIndex = pathIndex;
      return;
    }

    var position = 1 << TrieNode.indexSegment(hashCode, pathIndex * TrieNode.LOG_MAX_BRANCHING_FACTOR);
    var index = node.indexOfCellAt(position);
    path.get(pathIndex).reset(node.buffer, index);

    var cell = node.buffer[index];
    if (cell instanceof TrieNode<?> child) {
      resetPath(hashCode, child, element, pathIndex + 1);
    } else {
      pathLastIndex = pathIndex;
    }
  }

  private boolean isCollision(TrieNode<?> node) {
    return node.bitmap == 0;
  }
  private static int indexOf(@Nullable Object[] buffer, @Nullable Object element) {
    for (var index = 0; index < buffer.length; index++) {
      if (Objects.equals(buffer[index], element)) {
        return index;
      }
    }
    return -1;
  }

  private void checkNextWasInvoked() {
    if (!nextWasInvoked) {
      throw new IllegalStateException();
    }
  }

  private void checkForComodification() {
    if (builder.modCount != expectedModCount) {
      throw new ConcurrentModificationException();
    }
  }
}
