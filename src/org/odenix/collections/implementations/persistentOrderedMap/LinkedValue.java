/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedMap;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.internal.EndOfChain;

public final class LinkedValue<V extends @Nullable Object> {
  public final V value;
  public final @Nullable Object previous;
  public final @Nullable Object next;

  LinkedValue(V value, @Nullable Object previous, @Nullable Object next) {
    this.value = value;
    this.previous = previous;
    this.next = next;
  }

  /// Constructs LinkedValue for a new single entry
  LinkedValue(V value) {
    this(value, EndOfChain.INSTANCE, EndOfChain.INSTANCE);
  }

  /// Constructs LinkedValue for a new last entry
  LinkedValue(V value, @Nullable Object previous) {
    this(value, previous, EndOfChain.INSTANCE);
  }

  LinkedValue<V> withValue(V newValue) {
    return new LinkedValue<>(newValue, previous, next);
  }

  LinkedValue<V> withPrevious(@Nullable Object newPrevious) {
    return new LinkedValue<>(value, newPrevious, next);
  }

  LinkedValue<V> withNext(@Nullable Object newNext) {
    return new LinkedValue<>(value, previous, newNext);
  }

  boolean hasNext() {
    return next != EndOfChain.INSTANCE;
  }

  boolean hasPrevious() {
    return previous != EndOfChain.INSTANCE;
  }
}
