/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.Nullable;

/// A generic immutable collection of elements. Methods in this interface support only read-only access to the collection.
///
/// Modification operations are supported through the [PersistentCollection][PersistentCollection] interface.
///
/// Implementors of this interface take responsibility to be immutable.
/// Once constructed they must contain the same elements in the same order.
///
/// @param <E> the type of elements contained in the collection. The immutable collection is covariant on its element type.
@SuppressWarnings("unused")
public interface ImmutableCollection<E extends @Nullable Object> extends Iterable<E> {
  /// Returns the number of elements in this collection.
  int size();

  /// Returns {@code true} if this collection contains no elements.
  default boolean isEmpty() {
    return size() == 0;
  }

  /// Returns {@code true} if this collection contains the specified element.
  ///
  /// More formally, returns {@code true} if and only if this collection
  /// contains at least one element {@code e} such that {@code Objects.equals(o, e)}.
  boolean contains(E element);

  /// Returns {@code true} if this collection contains all elements
  /// in the specified collection.
  boolean containsAll(Collection<? extends E> elements);

  /// Returns an iterator over the elements of this collection.
  @Override
  Iterator<E> iterator();

  /// Returns a sequential [Stream] with this collection as its source.
  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /// Returns a possibly parallel [Stream] with this collection as its source.
  default Stream<E> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
  }
}
