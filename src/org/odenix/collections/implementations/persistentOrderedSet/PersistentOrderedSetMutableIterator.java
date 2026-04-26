/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedSet;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;

final class PersistentOrderedSetMutableIterator<E extends @Nullable Object>
    extends PersistentOrderedSetIterator<E> implements Iterator<E> {
  private final PersistentOrderedSetBuilder<E> builder;
  private @Nullable E lastIteratedElement;
  private boolean nextWasInvoked;
  private int expectedModCount;

  PersistentOrderedSetMutableIterator(PersistentOrderedSetBuilder<E> builder) {
    super(builder.firstElement, builder.hashMapBuilder);
    this.builder = builder;
    expectedModCount = builder.hashMapBuilder.modCount;
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
    builder.remove(lastIteratedElement);
    lastIteratedElement = null;
    nextWasInvoked = false;
    expectedModCount = builder.hashMapBuilder.modCount;
    index--;
  }

  private void checkNextWasInvoked() {
    if (!nextWasInvoked) {
      throw new IllegalStateException();
    }
  }

  private void checkForComodification() {
    if (builder.hashMapBuilder.modCount != expectedModCount) {
      throw new ConcurrentModificationException();
    }
  }
}
