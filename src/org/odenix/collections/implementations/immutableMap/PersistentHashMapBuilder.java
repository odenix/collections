/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.PersistentMap;
import org.odenix.collections.implementations.persistentOrderedMap.PersistentOrderedMap;
import org.odenix.collections.implementations.persistentOrderedMap.PersistentOrderedMapBuilder;
import org.odenix.collections.internal.DeltaCounter;
import org.odenix.collections.internal.MapImplementation;
import org.odenix.collections.internal.MutabilityOwnership;

public final class PersistentHashMapBuilder<K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMap<K, V> implements PersistentMap.Builder<K, V> {
  public @Nullable PersistentHashMap<K, V> builtMap;
  MutabilityOwnership ownership = new MutabilityOwnership();
  public TrieNode<K, V> node;
  @Nullable V operationResult;
  public int modCount;
  int size;

  public PersistentHashMapBuilder(PersistentHashMap<K, V> map) {
    builtMap = map;
    node = map.node;
    size = map.size();
  }

  @Override
  public int size() {
    return size;
  }

  void setSize(int size) {
    this.size = size;
    modCount++;
  }

  private void setNode(TrieNode<K, V> node) {
    if (node != this.node) {
      this.node = node;
      builtMap = null;
    }
  }

  @Override
  public PersistentHashMap<K, V> build() {
    if (builtMap != null) {
      return builtMap;
    }
    var newlyBuiltMap = new PersistentHashMap<>(node, size);
    builtMap = newlyBuiltMap;
    ownership = new MutabilityOwnership();
    return newlyBuiltMap;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new PersistentHashMapBuilderEntries<>(this);
  }

  @Override
  public Set<K> keySet() {
    return new PersistentHashMapBuilderKeys<>(this);
  }

  @Override
  public Collection<V> values() {
    return new PersistentHashMapBuilderValues<>(this);
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    @SuppressWarnings("unchecked")
    var typedKey = (K) key;
    return node.containsKey(typedKey == null ? 0 : typedKey.hashCode(), typedKey, 0);
  }

  @Override
  public @Nullable V get(@Nullable Object key) {
    @SuppressWarnings("unchecked")
    var typedKey = (K) key;
    return node.get(typedKey == null ? 0 : typedKey.hashCode(), typedKey, 0);
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    @SuppressWarnings("unchecked")
    var typedValue = (V) value;
    for (var candidate : values()) {
      if (Objects.equals(candidate, typedValue)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable V put(K key, V value) {
    operationResult = null;
    var oldModCount = modCount;
    var oldSize = size;
    var newNode = node.mutablePut(key == null ? 0 : key.hashCode(), key, value, 0, this);
    setNode(newNode);
    if (modCount == oldModCount && size != oldSize) {
      modCount++;
    }
    return operationResult;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> from) {
    if (from.isEmpty()) {
      return;
    }
    PersistentHashMap<K, V> map = null;
    if (from instanceof PersistentHashMapBuilder<?, ?> persistentHashMapBuilder) {
      @SuppressWarnings("unchecked")
      var typedBuilder = (PersistentHashMapBuilder<K, V>) persistentHashMapBuilder;
      map = typedBuilder.build();
    }

    if (map != null) {
      var intersectionCounter = new DeltaCounter();
      var oldSize = size;
      var newNode = node.mutablePutAll(map.node, 0, intersectionCounter, this);
      setNode(newNode);
      var newSize = oldSize + map.size() - intersectionCounter.count;
      if (oldSize != newSize) {
        setSize(newSize);
      }
      return;
    }
    super.putAll(from);
  }

  @Override
  public @Nullable V remove(@Nullable Object key) {
    @SuppressWarnings("unchecked")
    var typedKey = (K) key;
    operationResult = null;
    var oldModCount = modCount;
    var oldSize = size;
    var newNode = node.mutableRemove(typedKey == null ? 0 : typedKey.hashCode(), typedKey, 0, this);
    if (newNode == null) {
      newNode = emptyNode();
    }
    setNode(newNode);
    if (modCount == oldModCount && size != oldSize) {
      modCount++;
    }
    return operationResult;
  }

  @Override
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    @SuppressWarnings("unchecked")
    var typedKey = (K) key;
    @SuppressWarnings("unchecked")
    var typedValue = (V) value;
    var oldModCount = modCount;
    var oldSize = size;
    var newNode = node.mutableRemove(typedKey == null ? 0 : typedKey.hashCode(), typedKey, typedValue, 0, this);
    if (newNode == null) {
      newNode = emptyNode();
    }
    setNode(newNode);
    if (modCount == oldModCount && size != oldSize) {
      modCount++;
    }
    return oldSize != size;
  }

  @SuppressWarnings("unchecked")
  private TrieNode<K, V> emptyNode() {
    return (TrieNode<K, V>) TrieNode.EMPTY;
  }

  @Override
  public void clear() {
    setNode(emptyNode());
    setSize(0);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
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
    if (!(other instanceof Map<?, ?> otherMap)) {
      return false;
    }
    if (size != otherMap.size()) {
      return false;
    }
    return MapImplementation.equals(this, otherMap);
  }

  /// We provide [equals][#equals(Object)], so as a matter of style, we should also provide [hashCode][#hashCode()].
  ///
  /// Should be super.hashCode(), but <https://youtrack.jetbrains.com/issue/KT-45673>
  @Override
  public int hashCode() {
    return MapImplementation.hashCode(this);
  }
}
