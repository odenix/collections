/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;

abstract class AbstractListIterator<E extends @Nullable Object> implements ListIterator<E> {
  protected int index;
  protected int size;

  protected AbstractListIterator(int index, int size) {
    this.index = index;
    this.size = size;
  }

  @Override
  public boolean hasNext() {
    return index < size;
  }

  @Override
  public boolean hasPrevious() {
    return index > 0;
  }

  @Override
  public int nextIndex() {
    return index;
  }

  @Override
  public int previousIndex() {
    return index - 1;
  }

  protected void checkHasNext() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
  }

  protected void checkHasPrevious() {
    if (!hasPrevious()) {
      throw new NoSuchElementException();
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(E element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(E element) {
    throw new UnsupportedOperationException();
  }
}

final class SingleElementListIterator<E extends @Nullable Object> extends AbstractListIterator<E> {
  private final E element;

  SingleElementListIterator(E element, int index) {
    super(index, 1);
    this.element = element;
  }

  @Override
  public E next() {
    checkHasNext();
    index++;
    return element;
  }

  @Override
  public E previous() {
    checkHasPrevious();
    index--;
    return element;
  }
}
