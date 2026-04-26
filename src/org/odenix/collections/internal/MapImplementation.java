/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.internal;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/// This should not be needed after KT-30016 and KT-45673 are fixed
public final class MapImplementation {
  private MapImplementation() {}

  public static <K extends @Nullable Object, V extends @Nullable Object> boolean containsEntry(
      Map<K, V> map, Map.Entry<K, V> element) {
    var candidate = map.get(element.getKey());
    return candidate != null
        ? candidate.equals(element.getValue())
        : element.getValue() == null && map.containsKey(element.getKey());
  }

  public static <K extends @Nullable Object, V extends @Nullable Object> boolean equals(
      Map<K, V> thisMap, Map<?, ?> otherMap) {
    if (thisMap.size() != otherMap.size()) {
      throw new IllegalArgumentException();
    }
    for (var entry : otherMap.entrySet()) {
      @SuppressWarnings("unchecked")
      var castEntry = (Map.Entry<K, V>) entry;
      if (!containsEntry(thisMap, castEntry)) {
        return false;
      }
    }
    return true;
  }

  public static <K extends @Nullable Object, V extends @Nullable Object> int hashCode(Map<K, V> map) {
    return map.entrySet().hashCode();
  }
}
