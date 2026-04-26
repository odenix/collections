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
import org.odenix.collections.internal.EndOfChain;

class PersistentOrderedMapBuilderLinksIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<LinkedValue<V>> {
  private @Nullable Object nextKey;
  final PersistentOrderedMapBuilder<K, V> builder;
  @Nullable Object lastIteratedKey = EndOfChain.INSTANCE;
  private boolean nextWasInvoked;
  private int expectedModCount;
  int index;

  PersistentOrderedMapBuilderLinksIterator(
      @Nullable Object nextKey, PersistentOrderedMapBuilder<K, V> builder) {
    this.nextKey = nextKey;
    this.builder = builder;
    expectedModCount = builder.hashMapBuilder.modCount;
  }

  @Override
  public boolean hasNext() {
    return index < builder.size();
  }

  @Override
  public LinkedValue<V> next() {
    checkForComodification();
    checkHasNext();
    lastIteratedKey = nextKey;
    nextWasInvoked = true;
    index++;
    @SuppressWarnings("unchecked")
    var typedNextKey = (K) nextKey;
    var result = builder.hashMapBuilder.get(typedNextKey);
    if (result == null) {
      throw new ConcurrentModificationException(
          "Hash code of a key (" + nextKey + ") has changed after it was added to the persistent map.");
    }
    nextKey = result.next;
    return result;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public void remove() {
    checkNextWasInvoked();
    builder.remove(lastIteratedKey);
    lastIteratedKey = null;
    nextWasInvoked = false;
    expectedModCount = builder.hashMapBuilder.modCount;
    index--;
  }

  private void checkHasNext() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
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

final class PersistentOrderedMapBuilderEntriesIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<Map.Entry<K, V>> {
  private final PersistentOrderedMapBuilderLinksIterator<K, V> internal;

  PersistentOrderedMapBuilderEntriesIterator(PersistentOrderedMapBuilder<K, V> map) {
    internal = new PersistentOrderedMapBuilderLinksIterator<>(map.firstKey, map);
  }

  @Override
  public boolean hasNext() {
    return internal.hasNext();
  }

  @Override
  public Map.Entry<K, V> next() {
    var links = internal.next();
    @SuppressWarnings("unchecked")
    var typedLastIteratedKey = (K) internal.lastIteratedKey;
    return new MutableMapEntry<>(internal.builder, typedLastIteratedKey, links);
  }

  @Override
  public void remove() {
    internal.remove();
  }
}

final class MutableMapEntry<K extends @Nullable Object, V extends @Nullable Object>
    extends MapEntry<K, V> {
  private final PersistentOrderedMapBuilder<K, V> builder;
  private LinkedValue<V> links;

  MutableMapEntry(PersistentOrderedMapBuilder<K, V> builder, K key, LinkedValue<V> links) {
    super(key, links.value);
    this.builder = builder;
    this.links = links;
  }

  @Override
  public V getValue() {
    return links.value;
  }

  @Override
  public V setValue(V newValue) {
    var result = links.value;
    links = links.withValue(newValue);
    builder.builtMap = null;
    builder.hashMapBuilder.put(getKey(), links);
    return result;
  }
}

final class PersistentOrderedMapBuilderKeysIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<K> {
  private final PersistentOrderedMapBuilderLinksIterator<K, V> internal;

  PersistentOrderedMapBuilderKeysIterator(PersistentOrderedMapBuilder<K, V> map) {
    internal = new PersistentOrderedMapBuilderLinksIterator<>(map.firstKey, map);
  }

  @Override
  public boolean hasNext() {
    return internal.hasNext();
  }

  @Override
  public K next() {
    internal.next();
    @SuppressWarnings("unchecked")
    var typedLastIteratedKey = (K) internal.lastIteratedKey;
    return typedLastIteratedKey;
  }

  @Override
  public void remove() {
    internal.remove();
  }
}

final class PersistentOrderedMapBuilderValuesIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<V> {
  private final PersistentOrderedMapBuilderLinksIterator<K, V> internal;

  PersistentOrderedMapBuilderValuesIterator(PersistentOrderedMapBuilder<K, V> map) {
    internal = new PersistentOrderedMapBuilderLinksIterator<>(map.firstKey, map);
  }

  @Override
  public boolean hasNext() {
    return internal.hasNext();
  }

  @Override
  public V next() {
    return internal.next().value;
  }

  @Override
  public void remove() {
    internal.remove();
  }
}
