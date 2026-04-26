/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/// A generic immutable unordered collection of elements that does not support duplicate elements.
/// Methods in this interface support only read-only access to the immutable set.
///
/// Modification operations are supported through the [PersistentSet][PersistentSet] interface.
///
/// Implementors of this interface take responsibility to be immutable.
/// Once constructed they must contain the same elements in the same order.
///
/// @param <E> the type of elements contained in the set. The set is covariant on its element type.
@SuppressWarnings("unused")
public interface ImmutableSet<E extends @Nullable Object> extends ImmutableCollection<E> {
  /// Returns an immutable set of all elements of the specified collection.
  ///
  /// If the specified collection is already an immutable set, returns it as is.
  ///
  /// Elements of the returned set are iterated in the same order as in the specified collection.
  static <T extends @Nullable Object> ImmutableSet<T> from(Iterable<T> iterable) {
    if (iterable instanceof ImmutableSet<T> set) {
      return set;
    }
    if (iterable instanceof PersistentSet.Builder<T> builder) {
      return builder.build();
    }
    return PersistentSet.from(iterable);
  }

  /// Returns an immutable set of all elements of the specified array.
  ///
  /// Elements of the returned set are iterated in the same order as in the specified array.
  static <T extends @Nullable Object> ImmutableSet<T> from(T[] array) {
    return PersistentSet.from(array);
  }

  /// Returns an immutable set of all elements of the specified sequence.
  ///
  /// Elements of the returned set are iterated in the same order as in the specified sequence.
  static <T extends @Nullable Object> ImmutableSet<T> from(Stream<? extends T> stream) {
    return PersistentSet.from(stream);
  }

  /// Returns an immutable set of all characters.
  ///
  /// Elements of the returned set are iterated in the same order as in the specified char sequence.
  static ImmutableSet<Character> from(CharSequence chars) {
    return PersistentSet.from(chars);
  }
}
