/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedMap;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.implementations.immutableMap.MapEntry;
import org.odenix.collections.implementations.immutableMap.PersistentHashMap;

class PersistentOrderedMapLinksIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<LinkedValue<V>> {
  @Nullable Object nextKey;
  private final PersistentHashMap<K, LinkedValue<V>> hashMap;
  int index;

  PersistentOrderedMapLinksIterator(
      @Nullable Object nextKey, PersistentHashMap<K, LinkedValue<V>> hashMap) {
    this.nextKey = nextKey;
    this.hashMap = hashMap;
  }

  @Override
  public boolean hasNext() {
    return index < hashMap.size();
  }

  @Override
  public LinkedValue<V> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    @SuppressWarnings("unchecked")
    var typedNextKey = (K) nextKey;
    var result = hashMap.get(typedNextKey);
    if (result == null) {
      throw new ConcurrentModificationException(
          "Hash code of a key (" + nextKey + ") has changed after it was added to the persistent map.");
    }
    index++;
    nextKey = result.next;
    return result;
  }
}

final class PersistentOrderedMapEntriesIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<Map.Entry<K, V>> {
  private final PersistentOrderedMapLinksIterator<K, V> internal;

  PersistentOrderedMapEntriesIterator(PersistentOrderedMap<K, V> map) {
    internal = new PersistentOrderedMapLinksIterator<>(map.firstKey, map.hashMap);
  }

  @Override
  public boolean hasNext() {
    return internal.hasNext();
  }

  @Override
  public Map.Entry<K, V> next() {
    @SuppressWarnings("unchecked")
    var typedNextKey = (K) internal.nextKey;
    var nextValue = internal.next().value;
    return new MapEntry<>(typedNextKey, nextValue);
  }
}

final class PersistentOrderedMapKeysIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<K> {
  private final PersistentOrderedMapLinksIterator<K, V> internal;

  PersistentOrderedMapKeysIterator(PersistentOrderedMap<K, V> map) {
    internal = new PersistentOrderedMapLinksIterator<>(map.firstKey, map.hashMap);
  }

  @Override
  public boolean hasNext() {
    return internal.hasNext();
  }

  @Override
  public K next() {
    @SuppressWarnings("unchecked")
    var typedNextKey = (K) internal.nextKey;
    internal.next();
    return typedNextKey;
  }
}

final class PersistentOrderedMapValuesIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<V> {
  private final PersistentOrderedMapLinksIterator<K, V> internal;

  PersistentOrderedMapValuesIterator(PersistentOrderedMap<K, V> map) {
    internal = new PersistentOrderedMapLinksIterator<>(map.firstKey, map.hashMap);
  }

  @Override
  public boolean hasNext() {
    return internal.hasNext();
  }

  @Override
  public V next() {
    return internal.next().value;
  }
}
