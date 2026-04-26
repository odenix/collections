/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

final class TrieIterator<E extends @Nullable Object> extends AbstractListIterator<E> {
  private int height;
  private @Nullable Object[] path;
  private boolean isInRightEdge;

  TrieIterator(@Nullable Object[] root, int index, int size, int height) {
    super(index, size);
    this.height = height;
    this.path = new @Nullable Object[height];
    this.isInRightEdge = index == size;
    path[0] = root;
    fillPath(index - (isInRightEdge ? 1 : 0), 1);
  }

  void reset(@Nullable Object[] root, int index, int size, int height) {
    this.index = index;
    this.size = size;
    this.height = height;
    if (path.length < height) {
      path = new @Nullable Object[height];
    }
    path[0] = root;
    isInRightEdge = index == size;
    fillPath(index - (isInRightEdge ? 1 : 0), 1);
  }

  private void fillPath(int index, int startLevel) {
    var shift = (height - startLevel) * Utils.LOG_MAX_BUFFER_SIZE;
    for (var level = startLevel; level < height; level++) {
      path[level] =
          Objects.requireNonNull((@Nullable Object[]) path[level - 1])
              [Utils.indexSegment(index, shift)];
      shift -= Utils.LOG_MAX_BUFFER_SIZE;
    }
  }

  private void fillPathIfNeeded(int indexPredicate) {
    var shift = 0;
    while (Utils.indexSegment(index, shift) == indexPredicate) {
      shift += Utils.LOG_MAX_BUFFER_SIZE;
    }

    if (shift > 0) {
      var level = height - 1 - shift / Utils.LOG_MAX_BUFFER_SIZE;
      fillPath(index, level + 1);
    }
  }

  private E elementAtCurrentIndex() {
    var leafBufferIndex = index & Utils.MAX_BUFFER_SIZE_MINUS_ONE;
    @SuppressWarnings("unchecked")
    var element = (E) Objects.requireNonNull((@Nullable Object[]) path[height - 1])[leafBufferIndex];
    return element;
  }

  @Override
  public E next() {
    checkHasNext();

    var result = elementAtCurrentIndex();
    index += 1;

    if (index == size) {
      isInRightEdge = true;
      return result;
    }

    fillPathIfNeeded(0);
    return result;
  }

  @Override
  public E previous() {
    checkHasPrevious();

    index -= 1;

    if (isInRightEdge) {
      isInRightEdge = false;
      return elementAtCurrentIndex();
    }

    fillPathIfNeeded(Utils.MAX_BUFFER_SIZE_MINUS_ONE);
    return elementAtCurrentIndex();
  }
}
