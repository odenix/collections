/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import java.util.AbstractSet;
import java.util.Map;

import org.jspecify.annotations.Nullable;

public abstract class AbstractMapBuilderEntries<
        E extends Map.Entry<K, V>, K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractSet<E> {
  @Override
  public final boolean contains(@Nullable Object element) {
    if (!(element instanceof Map.Entry<?, ?> entry)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    var typed = (Map.Entry<K, V>) entry;
    return containsEntry(typed);
  }

  protected abstract boolean containsEntry(Map.Entry<K, V> element);

  @Override
  public final boolean remove(@Nullable Object element) {
    if (!(element instanceof Map.Entry<?, ?> entry)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    var typed = (Map.Entry<K, V>) entry;
    return removeEntry(typed);
  }

  protected abstract boolean removeEntry(Map.Entry<K, V> element);
}
