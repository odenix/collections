/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

public class MapEntry<K extends @Nullable Object, V extends @Nullable Object>
    implements Map.Entry<K, V> {
  private final K key;
  private final V value;

  public MapEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(key) ^ Objects.hashCode(value);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Map.Entry<?, ?> entry
        && Objects.equals(entry.getKey(), key)
        && Objects.equals(entry.getValue(), value);
  }

  @Override
  public String toString() {
    return key + "=" + value;
  }
}
