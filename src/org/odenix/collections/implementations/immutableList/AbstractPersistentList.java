/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.ImmutableList;
import org.odenix.collections.PersistentList;
import org.odenix.collections.internal.ListImplementation;

abstract class AbstractPersistentList<E extends @Nullable Object>
    implements PersistentList<E> {
  @Override
  public ImmutableList<E> subList(int fromIndex, int toIndex) {
    return PersistentList.super.subList(fromIndex, toIndex);
  }

  @Override
  public PersistentList<E> addAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return this;
    }
    return mutate(list -> list.addAll(elements));
  }

  @Override
  public PersistentList<E> addAll(int index, Collection<? extends E> elements) {
    ListImplementation.checkPositionIndex(index, size());
    if (elements.isEmpty()) {
      return this;
    }
    return mutate(list -> list.addAll(index, elements));
  }

  @Override
  public PersistentList<E> remove(E element) {
    var index = indexOf(element);
    if (index != -1) {
      return removeAt(index);
    }
    return this;
  }

  @Override
  public PersistentList<E> removeAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return this;
    }
    return removeAll(elements::contains);
  }

  @Override
  public PersistentList<E> retainAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return Utils.persistentVectorOf();
    }
    return removeAll(element -> !elements.contains(element));
  }

  @Override
  public PersistentList<E> clear() {
    return Utils.persistentVectorOf();
  }

  @Override
  public boolean contains(E element) {
    return indexOf(element) != -1;
  }

  @Override
  public boolean containsAll(Collection<? extends E> elements) {
    for (var element : elements) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<E> iterator() {
    return listIterator();
  }

  @Override
  public ListIterator<E> listIterator() {
    return listIterator(0);
  }

  @Override
  public int hashCode() {
    return ListImplementation.orderedHashCode(this);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return ListImplementation.orderedEquals(this, other);
  }

  @Override
  public String toString() {
    var result = new StringJoiner(", ", "[", "]");
    for (var element : this) {
      result.add(String.valueOf(element));
    }
    return result.toString();
  }
}
