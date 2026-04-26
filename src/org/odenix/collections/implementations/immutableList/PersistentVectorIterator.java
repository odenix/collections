/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import org.jspecify.annotations.Nullable;

final class PersistentVectorIterator<T extends @Nullable Object> extends AbstractListIterator<T> {
  private final @Nullable Object[] tail;
  private final TrieIterator<T> trieIterator;

  PersistentVectorIterator(@Nullable Object[] root, @Nullable Object[] tail, int index, int size, int trieHeight) {
    super(index, size);
    this.tail = tail;
    var trieSize = Utils.rootSize(size);
    var trieIndex = Math.min(index, trieSize);
    this.trieIterator = new TrieIterator<>(root, trieIndex, trieSize, trieHeight);
  }

  @Override
  public T next() {
    checkHasNext();
    if (trieIterator.hasNext()) {
      index++;
      return trieIterator.next();
    }
    @SuppressWarnings("unchecked")
    var element = (T) tail[index++ - trieIterator.size];
    return element;
  }

  @Override
  public T previous() {
    checkHasPrevious();
    if (index > trieIterator.size) {
      @SuppressWarnings("unchecked")
      var element = (T) tail[--index - trieIterator.size];
      return element;
    }
    index--;
    return trieIterator.previous();
  }
}
