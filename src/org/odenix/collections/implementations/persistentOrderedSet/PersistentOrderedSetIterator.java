/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedSet;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.implementations.immutableMap.PersistentHashMap;
import org.odenix.collections.implementations.immutableMap.PersistentHashMapBuilder;

class PersistentOrderedSetIterator<E extends @Nullable Object> implements Iterator<E> {
  private @Nullable Object nextElement;
  private final @Nullable PersistentHashMap<E, Links> map;
  private final @Nullable PersistentHashMapBuilder<E, Links> mapBuilder;
  int index = 0;

  PersistentOrderedSetIterator(@Nullable Object nextElement, PersistentHashMap<E, Links> map) {
    this.nextElement = nextElement;
    this.map = map;
    mapBuilder = null;
  }

  PersistentOrderedSetIterator(
      @Nullable Object nextElement, PersistentHashMapBuilder<E, Links> mapBuilder) {
    this.nextElement = nextElement;
    map = null;
    this.mapBuilder = mapBuilder;
  }

  @Override
  public boolean hasNext() {
    return index < size();
  }

  @Override
  public E next() {
    checkHasNext();

    @SuppressWarnings("unchecked")
    var result = (E) nextElement;
    index++;
    var links = get(result);
    if (links == null) {
      throw new ConcurrentModificationException(
          "Hash code of an element (" + result + ") has changed after it was added to the persistent set.");
    }
    nextElement = links.next;
    return result;
  }

  private int size() {
    return map != null ? map.size() : Objects.requireNonNull(mapBuilder).size();
  }

  private @Nullable Links get(E key) {
    return map != null ? map.get(key) : Objects.requireNonNull(mapBuilder).get(key);
  }

  private void checkHasNext() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
  }
}
