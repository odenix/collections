/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedMap;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.PersistentMap;
import org.odenix.collections.implementations.immutableMap.PersistentHashMap;
import org.odenix.collections.implementations.immutableMap.PersistentHashMapBuilder;
import org.odenix.collections.internal.EndOfChain;
import org.odenix.collections.internal.MapImplementation;

public final class PersistentOrderedMapBuilder<
        K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMap<K, V> implements PersistentMap.Builder<K, V> {
  @Nullable PersistentOrderedMap<K, V> builtMap;
  @Nullable Object firstKey;
  private @Nullable Object lastKey;
  public final PersistentHashMapBuilder<K, LinkedValue<V>> hashMapBuilder;

  public PersistentOrderedMapBuilder(PersistentOrderedMap<K, V> map) {
    builtMap = map;
    firstKey = map.firstKey;
    lastKey = map.lastKey;
    hashMapBuilder = map.hashMap.builder();
  }

  @Override
  public int size() {
    return hashMapBuilder.size();
  }

  @Override
  public PersistentOrderedMap<K, V> build() {
    if (builtMap != null) {
      assert hashMapBuilder.builtMap != null : "hashMapBuilder.builtMap != null";
      assert firstKey == builtMap.firstKey : "firstKey == builtMap.firstKey";
      assert lastKey == builtMap.lastKey : "lastKey == builtMap.lastKey";
      return builtMap;
    }
    assert hashMapBuilder.builtMap == null : "hashMapBuilder.builtMap == null";
    var newHashMap = hashMapBuilder.build();
    var newOrdered = new PersistentOrderedMap<>(firstKey, lastKey, newHashMap);
    builtMap = newOrdered;
    return newOrdered;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new PersistentOrderedMapBuilderEntries<>(this);
  }

  @Override
  public Set<K> keySet() {
    return new PersistentOrderedMapBuilderKeys<>(this);
  }

  @Override
  public Collection<V> values() {
    return new PersistentOrderedMapBuilderValues<>(this);
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return hashMapBuilder.containsKey(key);
  }

  @Override
  public @Nullable V get(@Nullable Object key) {
    var links = hashMapBuilder.get(key);
    return links != null ? links.value : null;
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    for (var candidate : values()) {
      if (Objects.equals(candidate, value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable V put(K key, V value) {
    var links = hashMapBuilder.get(key);
    if (links != null) {
      if (links.value == value) {
        return value;
      }
      builtMap = null;
      hashMapBuilder.put(key, links.withValue(value));
      return links.value;
    }

    builtMap = null;
    if (isEmpty()) {
      firstKey = key;
      lastKey = key;
      hashMapBuilder.put(key, new LinkedValue<>(value));
    } else {
      @SuppressWarnings("unchecked")
      var typedLastKey = (K) lastKey;
      var lastLinks = hashMapBuilder.get(typedLastKey);
      assert lastLinks != null && !lastLinks.hasNext() : "lastLinks != null && !lastLinks.hasNext()";
      hashMapBuilder.put(typedLastKey, lastLinks.withNext(key));
      hashMapBuilder.put(key, new LinkedValue<>(value, typedLastKey));
      lastKey = key;
    }
    return null;
  }

  @Override
  public @Nullable V remove(@Nullable Object key) {
    var links = hashMapBuilder.remove(key);
    if (links == null) {
      return null;
    }
    builtMap = null;
    if (links.hasPrevious()) {
      @SuppressWarnings("unchecked")
      var typedPrevious = (K) links.previous;
      var previousLinks = Objects.requireNonNull(hashMapBuilder.get(typedPrevious));
      hashMapBuilder.put(typedPrevious, previousLinks.withNext(links.next));
    } else {
      firstKey = links.next;
    }
    if (links.hasNext()) {
      @SuppressWarnings("unchecked")
      var typedNext = (K) links.next;
      var nextLinks = Objects.requireNonNull(hashMapBuilder.get(typedNext));
      hashMapBuilder.put(typedNext, nextLinks.withPrevious(links.previous));
    } else {
      lastKey = links.previous;
    }
    return links.value;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    var links = hashMapBuilder.get(key);
    if (links == null) {
      return false;
    }
    if (!Objects.equals(links.value, value)) {
      return false;
    }
    remove(key);
    return true;
  }

  @Override
  public void clear() {
    if (!hashMapBuilder.isEmpty()) {
      builtMap = null;
    }
    hashMapBuilder.clear();
    firstKey = EndOfChain.INSTANCE;
    lastKey = EndOfChain.INSTANCE;
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
      return hashMapBuilder.node.equalsWith(
          persistentOrderedMap.hashMap.node, (a, b) -> Objects.equals(a.value, b.value));
    }
    if (other instanceof PersistentOrderedMapBuilder<?, ?> persistentOrderedMapBuilder) {
      if (size() != persistentOrderedMapBuilder.size()) {
        return false;
      }
      return hashMapBuilder.node.equalsWith(
          persistentOrderedMapBuilder.hashMapBuilder.node,
          (a, b) -> Objects.equals(a.value, b.value));
    }
    if (other instanceof PersistentHashMap<?, ?> persistentHashMap) {
      if (size() != persistentHashMap.size()) {
        return false;
      }
      return hashMapBuilder.node.equalsWith(
          persistentHashMap.node, (a, b) -> Objects.equals(a.value, b));
    }
    if (other instanceof PersistentHashMapBuilder<?, ?> persistentHashMapBuilder) {
      if (size() != persistentHashMapBuilder.size()) {
        return false;
      }
      return hashMapBuilder.node.equalsWith(
          persistentHashMapBuilder.node, (a, b) -> Objects.equals(a.value, b));
    }
    if (!(other instanceof Map<?, ?> otherMap)) {
      return false;
    }
    if (size() != otherMap.size()) {
      return false;
    }
    return MapImplementation.equals(this, otherMap);
  }

  /// We provide [equals][#equals(Object)], so as a matter of style, we should also provide [hashCode][#hashCode()].
  ///
  /// Should be `super.hashCode()`, but <https://youtrack.jetbrains.com/issue/KT-45673>
  @Override
  public int hashCode() {
    return MapImplementation.hashCode(this);
  }
}
