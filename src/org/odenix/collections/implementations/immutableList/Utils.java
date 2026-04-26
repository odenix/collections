/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.PersistentList;

final class Utils {
  static final int MAX_BUFFER_SIZE = 32;
  static final int LOG_MAX_BUFFER_SIZE = 5;
  static final int MAX_BUFFER_SIZE_MINUS_ONE = MAX_BUFFER_SIZE - 1;
  static final int MUTABLE_BUFFER_SIZE = MAX_BUFFER_SIZE + 1;

  private Utils() {}

  static <E extends @Nullable Object> PersistentList<E> persistentVectorOf() {
    return SmallPersistentVector.emptyOf();
  }

  /// Creates new buffer of [MAX_BUFFER_SIZE][#MAX_BUFFER_SIZE] capacity having first element initialized with the specified {@code element}.
  static @Nullable Object[] presizedBufferWith(@Nullable Object element) {
    var buffer = new @Nullable Object[MAX_BUFFER_SIZE];
    buffer[0] = element;
    return buffer;
  }

  /// Gets trie index segment of the specified {@code index} at the level specified by {@code shift}.
  ///
  /// `shift` equal to zero corresponds to the bottommost (leaf) level.
  /// For each upper level `shift` increments by [LOG_MAX_BUFFER_SIZE][#LOG_MAX_BUFFER_SIZE].
  static int indexSegment(int index, int shift) {
    return (index >> shift) & MAX_BUFFER_SIZE_MINUS_ONE;
  }

  /// Returns the size of trie part of a persistent vector of the specified {@code vectorSize}.
  static int rootSize(int vectorSize) {
    return (vectorSize - 1) & ~MAX_BUFFER_SIZE_MINUS_ONE;
  }
}

final class ObjectRef {
  @Nullable Object value;

  ObjectRef(@Nullable Object value) {
    this.value = value;
  }
}
