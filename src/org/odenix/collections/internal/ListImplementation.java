/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.internal;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.ImmutableList;

public final class ListImplementation {
  private ListImplementation() {}

  public static void checkElementIndex(int index, int size) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
    }
  }

  public static void checkPositionIndex(int index, int size) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
    }
  }

  public static void checkRangeIndexes(int fromIndex, int toIndex, int size) {
    if (fromIndex < 0 || toIndex > size) {
      throw new IndexOutOfBoundsException(
          "fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", size: " + size);
    }
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex: " + fromIndex + " > toIndex: " + toIndex);
    }
  }

  public static int orderedHashCode(ImmutableList<?> list) {
    var hashCode = 1;
    for (var element : list) {
      hashCode = 31 * hashCode + (element != null ? element.hashCode() : 0);
    }
    return hashCode;
  }

  public static boolean orderedEquals(ImmutableList<?> list, @Nullable Object other) {
    if (!(other instanceof ImmutableList<?> otherList)) {
      return false;
    }
    if (list.size() != otherList.size()) {
      return false;
    }

    var otherIterator = otherList.iterator();
    for (var element : list) {
      if (!Objects.equals(element, otherIterator.next())) {
        return false;
      }
    }
    return true;
  }
}
