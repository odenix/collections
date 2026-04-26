/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedMap;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.ImmutableCollection;
import org.odenix.collections.ImmutableSet;
import org.odenix.collections.PersistentMap;
import org.odenix.collections.implementations.immutableMap.PersistentHashMap;
import org.odenix.collections.implementations.immutableMap.PersistentHashMapBuilder;
import org.odenix.collections.internal.EndOfChain;

public final class PersistentOrderedMap<K extends @Nullable Object, V extends @Nullable Object>
    implements PersistentMap<K, V> {
  private static final PersistentOrderedMap<?, ?> EMPTY =
      new PersistentOrderedMap<>(EndOfChain.INSTANCE, EndOfChain.INSTANCE, PersistentHashMap.emptyOf());

  final @Nullable Object firstKey;
  final @Nullable Object lastKey;
  public final PersistentHashMap<K, LinkedValue<V>> hashMap;

  PersistentOrderedMap(
      @Nullable Object firstKey,
      @Nullable Object lastKey,
      PersistentHashMap<K, LinkedValue<V>> hashMap) {
    this.firstKey = firstKey;
    this.lastKey = lastKey;
    this.hashMap = hashMap;
  }

  @SuppressWarnings("unchecked")
  public static <K extends @Nullable Object, V extends @Nullable Object>
      PersistentOrderedMap<K, V> emptyOf() {
    return (PersistentOrderedMap<K, V>) EMPTY;
  }

  @Override
  public int size() {
    return hashMap.size();
  }

  @Override
  public ImmutableSet<K> keys() {
    return new PersistentOrderedMapKeys<>(this);
  }

  @Override
  public ImmutableCollection<V> values() {
    return new PersistentOrderedMapValues<>(this);
  }

  @Override
  public ImmutableSet<Map.Entry<K, V>> entries() {
    return new PersistentOrderedMapEntries<>(this);
  }

  @Override
  public boolean containsKey(K key) {
    return hashMap.containsKey(key);
  }

  @Override
  public boolean containsValue(V value) {
    for (var candidate : hashMap.values()) {
      if (Objects.equals(candidate.value, value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable V get(K key) {
    var links = hashMap.get(key);
    return links != null ? links.value : null;
  }

  @Override
  public PersistentOrderedMap<K, V> put(K key, V value) {
    if (isEmpty()) {
      var newMap = hashMap.put(key, new LinkedValue<>(value));
      return new PersistentOrderedMap<>(key, key, newMap);
    }

    var links = hashMap.get(key);
    if (links != null) {
      if (links.value == value) {
        return this;
      }
      var newMap = hashMap.put(key, links.withValue(value));
      return new PersistentOrderedMap<>(firstKey, lastKey, newMap);
    }

    @SuppressWarnings("unchecked")
    var typedLastKey = (K) lastKey;
    var lastLinks = Objects.requireNonNull(hashMap.get(typedLastKey));
    var newMap =
        hashMap
            .put(typedLastKey, lastLinks.withNext(key))
            .put(key, new LinkedValue<>(value, typedLastKey));
    return new PersistentOrderedMap<>(firstKey, key, newMap);
  }

  @Override
  public PersistentOrderedMap<K, V> remove(K key) {
    var links = hashMap.get(key);
    if (links == null) {
      return this;
    }

    var newMap = hashMap.remove(key);
    if (links.hasPrevious()) {
      @SuppressWarnings("unchecked")
      var typedPrevious = (K) links.previous;
      var previousLinks = Objects.requireNonNull(newMap.get(typedPrevious));
      newMap = newMap.put(typedPrevious, previousLinks.withNext(links.next));
    }
    if (links.hasNext()) {
      @SuppressWarnings("unchecked")
      var typedNext = (K) links.next;
      var nextLinks = Objects.requireNonNull(newMap.get(typedNext));
      newMap = newMap.put(typedNext, nextLinks.withPrevious(links.previous));
    }

    var newFirstKey = !links.hasPrevious() ? links.next : firstKey;
    var newLastKey = !links.hasNext() ? links.previous : lastKey;
    return new PersistentOrderedMap<>(newFirstKey, newLastKey, newMap);
  }

  @Override
  public PersistentOrderedMap<K, V> remove(K key, V value) {
    var links = hashMap.get(key);
    if (links == null) {
      return this;
    }
    return Objects.equals(links.value, value) ? remove(key) : this;
  }

  @Override
  public PersistentMap<K, V> putAll(Map<? extends K, ? extends V> map) {
    if (map.isEmpty()) {
      return this;
    }
    return mutate(builder -> builder.putAll(map));
  }

  @Override
  public PersistentMap<K, V> clear() {
    return emptyOf();
  }

  @Override
  public PersistentOrderedMapBuilder<K, V> builder() {
    return new PersistentOrderedMapBuilder<>(this);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof PersistentOrderedMap<?, ?> persistentOrderedMap) {
      if (size() != persistentOrderedMap.size()) {
        return false;
      }
      return hashMap.node.equalsWith(
          persistentOrderedMap.hashMap.node, (a, b) -> Objects.equals(a.value, b.value));
    }
    if (other instanceof PersistentOrderedMapBuilder<?, ?> persistentOrderedMapBuilder) {
      if (size() != persistentOrderedMapBuilder.size()) {
        return false;
      }
      return hashMap.node.equalsWith(
          persistentOrderedMapBuilder.hashMapBuilder.node, (a, b) -> Objects.equals(a.value, b.value));
    }
    if (other instanceof PersistentHashMap<?, ?> persistentHashMap) {
      if (size() != persistentHashMap.size()) {
        return false;
      }
      return hashMap.node.equalsWith(persistentHashMap.node, (a, b) -> Objects.equals(a.value, b));
    }
    if (other instanceof PersistentHashMapBuilder<?, ?> persistentHashMapBuilder) {
      if (size() != persistentHashMapBuilder.size()) {
        return false;
      }
      return hashMap.node.equalsWith(
          persistentHashMapBuilder.node, (a, b) -> Objects.equals(a.value, b));
    }
    if (!(other instanceof Map<?, ?> otherMap)) {
      return false;
    }
    if (size() != otherMap.size()) {
      return false;
    }
    return contentEquals(otherMap);
  }

  /// We provide [equals][#equals(Object)], so as a matter of style, we should also provide [hashCode][#hashCode()].
  /// However, the implementation from [AbstractMap][java.util.AbstractMap] is enough.
  @Override
  public int hashCode() {
    return entries().hashCode();
  }

  private boolean contentEquals(Map<?, ?> otherMap) {
    for (var entry : otherMap.entrySet()) {
      @SuppressWarnings("unchecked")
      var typedKey = (K) entry.getKey();
      var candidate = get(typedKey);
      if (candidate != null) {
        if (!candidate.equals(entry.getValue())) {
          return false;
        }
      } else if (entry.getValue() != null || !containsKey(typedKey)) {
        return false;
      }
    }
    return true;
  }
}
