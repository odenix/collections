/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.ImmutableCollection;
import org.odenix.collections.ImmutableSet;
import org.odenix.collections.PersistentMap;
import org.odenix.collections.implementations.persistentOrderedMap.PersistentOrderedMap;
import org.odenix.collections.implementations.persistentOrderedMap.PersistentOrderedMapBuilder;

public final class PersistentHashMap<K extends @Nullable Object, V extends @Nullable Object>
    implements PersistentMap<K, V> {
  private static final PersistentHashMap<?, ?> EMPTY = new PersistentHashMap<>(emptyNode(), 0);

  public final TrieNode<K, V> node;
  private final int size;

  PersistentHashMap(TrieNode<K, V> node, int size) {
    this.node = node;
    this.size = size;
  }

  public static <K extends @Nullable Object, V extends @Nullable Object> PersistentHashMap<K, V> emptyOf() {
    @SuppressWarnings("unchecked")
    var empty = (PersistentHashMap<K, V>) EMPTY;
    return empty;
  }

  @SuppressWarnings("unchecked")
  private static <K extends @Nullable Object, V extends @Nullable Object> TrieNode<K, V> emptyNode() {
    return (TrieNode<K, V>) TrieNode.EMPTY;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean containsKey(K key) {
    return node.containsKey(key == null ? 0 : key.hashCode(), key, 0);
  }

  @Override
  public boolean containsValue(V value) {
    for (var candidate : values()) {
      if (Objects.equals(candidate, value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable V get(K key) {
    return node.get(key == null ? 0 : key.hashCode(), key, 0);
  }

  @Override
  public ImmutableSet<K> keys() {
    return new PersistentHashMapKeys<>(this);
  }

  @Override
  public ImmutableCollection<V> values() {
    return new PersistentHashMapValues<>(this);
  }

  @Override
  public ImmutableSet<Map.Entry<K, V>> entries() {
    return new PersistentHashMapEntries<>(this);
  }

  @Override
  public PersistentHashMap<K, V> put(K key, V value) {
    var newNodeResult = node.put(key == null ? 0 : key.hashCode(), key, value, 0);
    if (newNodeResult == null) {
      return this;
    }
    return new PersistentHashMap<>(newNodeResult.node, size + newNodeResult.sizeDelta);
  }

  @Override
  public PersistentHashMap<K, V> remove(K key) {
    var newNode = node.remove(key == null ? 0 : key.hashCode(), key, 0);
    if (node == newNode) {
      return this;
    }
    if (newNode == null) {
      return emptyOf();
    }
    return new PersistentHashMap<>(newNode, size - 1);
  }

  @Override
  public PersistentHashMap<K, V> remove(K key, V value) {
    var newNode = node.remove(key == null ? 0 : key.hashCode(), key, value, 0);
    if (node == newNode) {
      return this;
    }
    if (newNode == null) {
      return emptyOf();
    }
    return new PersistentHashMap<>(newNode, size - 1);
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
  public PersistentHashMapBuilder<K, V> builder() {
    return new PersistentHashMapBuilder<>(this);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof PersistentOrderedMap<?, ?> persistentOrderedMap) {
      if (size != persistentOrderedMap.size()) {
        return false;
      }
      return node.equalsWith(persistentOrderedMap.hashMap.node, (a, b) -> Objects.equals(a, b.value));
    }
    if (other instanceof PersistentOrderedMapBuilder<?, ?> persistentOrderedMapBuilder) {
      if (size != persistentOrderedMapBuilder.size()) {
        return false;
      }
      return node.equalsWith(
          persistentOrderedMapBuilder.hashMapBuilder.node, (a, b) -> Objects.equals(a, b.value));
    }
    if (other instanceof PersistentHashMap<?, ?> persistentHashMap) {
      if (size != persistentHashMap.size()) {
        return false;
      }
      return node.equalsWith(persistentHashMap.node, Objects::equals);
    }
    if (other instanceof PersistentHashMapBuilder<?, ?> persistentHashMapBuilder) {
      if (size != persistentHashMapBuilder.size()) {
        return false;
      }
      return node.equalsWith(persistentHashMapBuilder.node, Objects::equals);
    }
    if (!(other instanceof Map<?, ?> otherMap)) {
      return false;
    }
    if (size != otherMap.size()) {
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
