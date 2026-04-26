/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.ImmutableCollection;
import org.odenix.collections.ImmutableSet;

final class PersistentOrderedMapEntries<K extends @Nullable Object, V extends @Nullable Object>
    implements ImmutableSet<Map.Entry<K, V>> {
  private final PersistentOrderedMap<K, V> map;

  PersistentOrderedMapEntries(PersistentOrderedMap<K, V> map) {
    this.map = map;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(Map.Entry<K, V> element) {
    var candidate = map.get(element.getKey());
    return candidate != null
        ? candidate.equals(element.getValue())
        : element.getValue() == null && map.containsKey(element.getKey());
  }

  @Override
  public boolean containsAll(Collection<? extends Map.Entry<K, V>> elements) {
    for (var element : elements) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<Map.Entry<K, V>> iterator() {
    return new PersistentOrderedMapEntriesIterator<>(map);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof ImmutableSet<?> set) {
      if (size() != set.size()) {
        return false;
      }
      for (var element : set) {
        if (!(element instanceof Map.Entry<?, ?> entry)) {
          return false;
        }
        @SuppressWarnings("unchecked")
        var typed = (Map.Entry<K, V>) entry;
        if (!contains(typed)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    var hashCode = 0;
    for (var entry : this) {
      hashCode += entry.hashCode();
    }
    return hashCode;
  }
}

final class PersistentOrderedMapKeys<K extends @Nullable Object, V extends @Nullable Object>
    implements ImmutableSet<K> {
  private final PersistentOrderedMap<K, V> map;

  PersistentOrderedMapKeys(PersistentOrderedMap<K, V> map) {
    this.map = map;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(K element) {
    return map.containsKey(element);
  }

  @Override
  public boolean containsAll(Collection<? extends K> elements) {
    for (var element : elements) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<K> iterator() {
    return new PersistentOrderedMapKeysIterator<>(map);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof ImmutableSet<?> set) {
      if (size() != set.size()) {
        return false;
      }
      for (var element : set) {
        @SuppressWarnings("unchecked")
        var typed = (K) element;
        if (!contains(typed)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    var hashCode = 0;
    for (var element : this) {
      hashCode += Objects.hashCode(element);
    }
    return hashCode;
  }
}

final class PersistentOrderedMapValues<K extends @Nullable Object, V extends @Nullable Object>
    implements ImmutableCollection<V> {
  private final PersistentOrderedMap<K, V> map;

  PersistentOrderedMapValues(PersistentOrderedMap<K, V> map) {
    this.map = map;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(V element) {
    return map.containsValue(element);
  }

  @Override
  public boolean containsAll(Collection<? extends V> elements) {
    for (var element : elements) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<V> iterator() {
    return new PersistentOrderedMapValuesIterator<>(map);
  }
}
