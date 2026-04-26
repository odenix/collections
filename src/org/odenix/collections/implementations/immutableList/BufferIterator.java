/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import org.jspecify.annotations.Nullable;

final class BufferIterator<E extends @Nullable Object> extends AbstractListIterator<E> {
  private final E[] buffer;

  BufferIterator(E[] buffer, int index, int size) {
    super(index, size);
    this.buffer = buffer;
  }

  @Override
  public E next() {
    checkHasNext();
    return buffer[index++];
  }

  @Override
  public E previous() {
    checkHasPrevious();
    return buffer[--index];
  }
}
