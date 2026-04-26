/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.internal.MapImplementation;

final class PersistentHashMapBuilderEntries<
        K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMapBuilderEntries<Map.Entry<K, V>, K, V> {
  private final PersistentHashMapBuilder<K, V> builder;

  PersistentHashMapBuilderEntries(PersistentHashMapBuilder<K, V> builder) {
    this.builder = builder;
  }

  @Override
  public boolean add(Map.Entry<K, V> element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    builder.clear();
  }

  @Override
  public Iterator<Map.Entry<K, V>> iterator() {
    return new PersistentHashMapBuilderEntriesIterator<>(builder);
  }

  @Override
  protected boolean removeEntry(Map.Entry<K, V> element) {
    return builder.remove(element.getKey(), element.getValue());
  }

  @Override
  public int size() {
    return builder.size();
  }

  @Override
  protected boolean containsEntry(Map.Entry<K, V> element) {
    return MapImplementation.containsEntry(builder, element);
  }
}

final class PersistentHashMapBuilderKeys<
        K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractSet<K> {
  private final PersistentHashMapBuilder<K, V> builder;

  PersistentHashMapBuilderKeys(PersistentHashMapBuilder<K, V> builder) {
    this.builder = builder;
  }

  @Override
  public boolean add(K element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    builder.clear();
  }

  @Override
  public Iterator<K> iterator() {
    return new PersistentHashMapBuilderKeysIterator<>(builder);
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean remove(@Nullable Object element) {
    if (builder.containsKey(element)) {
      builder.remove(element);
      return true;
    }
    return false;
  }

  @Override
  public int size() {
    return builder.size();
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean contains(@Nullable Object element) {
    return builder.containsKey(element);
  }
}

final class PersistentHashMapBuilderValues<
        K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractCollection<V> {
  private final PersistentHashMapBuilder<K, V> builder;

  PersistentHashMapBuilderValues(PersistentHashMapBuilder<K, V> builder) {
    this.builder = builder;
  }

  @Override
  public int size() {
    return builder.size();
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean contains(@Nullable Object element) {
    return builder.containsValue(element);
  }

  @Override
  public void clear() {
    builder.clear();
  }

  @Override
  public Iterator<V> iterator() {
    return new PersistentHashMapBuilderValuesIterator<>(builder);
  }
}
