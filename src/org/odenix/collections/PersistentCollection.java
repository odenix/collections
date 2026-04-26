/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/// A generic persistent collection of elements that supports adding and removing elements.
///
/// Modification operations return new instances of the persistent collection with the modification applied.
///
/// @param <E> the type of elements contained in the collection. The persistent collection is covariant on its element type.
public interface PersistentCollection<E extends @Nullable Object> extends ImmutableCollection<E> {
  /// Returns the result of adding the specified {@code element} to this collection.
  ///
  /// @return a new persistent collection with the specified {@code element} added;
  /// or this instance if this collection does not support duplicates and it already contains the element.
  PersistentCollection<E> add(E element);

  /// Returns the result of adding all elements of the specified {@code elements} collection to this collection.
  ///
  /// @return a new persistent collection with elements of the specified {@code elements} collection added;
  /// or this instance if no modifications were made in the result of this operation.
  PersistentCollection<E> addAll(Collection<? extends E> elements);

  /// Returns the result of removing a single appearance of the specified {@code element} from this collection.
  ///
  /// @return a new persistent collection with a single appearance of the specified {@code element} removed;
  /// or this instance if there is no such element in this collection.
  PersistentCollection<E> remove(E element);

  /// Returns the result of removing all elements in this collection that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent collection with elements in this collection that are also
  /// contained in the specified {@code elements} collection removed;
  /// or this instance if no modifications were made in the result of this operation.
  PersistentCollection<E> removeAll(Collection<? extends E> elements);

  /// Returns the result of removing all elements in this collection that match the specified {@code predicate}.
  ///
  /// @return a new persistent collection with elements matching the specified {@code predicate} removed;
  /// or this instance if no elements match the predicate.
  PersistentCollection<E> removeAll(Predicate<? super E> predicate);

  /// Returns all elements in this collection that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent set with elements in this set that are also
  /// contained in the specified {@code elements} collection;
  /// or this instance if no modifications were made in the result of this operation.
  PersistentCollection<E> retainAll(Collection<? extends E> elements);

  /// Returns an empty persistent collection.
  PersistentCollection<E> clear();

  /// Returns a new builder with the same contents as this collection.
  ///
  /// The builder can be used to efficiently perform multiple modification operations.
  Builder<E> builder();

  /// Returns the result of adding all elements of the specified {@code elements} collection to this collection.
  ///
  /// @return a new persistent collection with elements of the specified {@code elements} collection added;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentCollection<E> addAll(Iterable<? extends E> elements) {
    if (elements instanceof Collection<? extends E> collection) {
      return addAll(collection);
    }
    var builder = builder();
    Extensions.addAll(builder, elements);
    return builder.build();
  }

  /// Returns the result of adding all elements of the specified {@code elements} array to this collection.
  ///
  /// @return a new persistent collection with elements of the specified {@code elements} array added;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentCollection<E> addAll(E[] elements) {
    var builder = builder();
    Collections.addAll(builder, elements);
    return builder.build();
  }

  /// Returns the result of adding all elements of the specified {@code elements} sequence to this collection.
  ///
  /// @return a new persistent collection with elements of the specified {@code elements} sequence added;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentCollection<E> addAll(Stream<? extends E> elements) {
    var builder = builder();
    Extensions.addAll(builder, elements);
    return builder.build();
  }

  /// Returns the result of removing all elements in this collection that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent collection with elements in this collection that are also
  /// contained in the specified {@code elements} collection removed;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentCollection<E> removeAll(Iterable<? extends E> elements) {
    if (elements instanceof Collection<? extends E> collection) {
      return removeAll(collection);
    }
    var builder = builder();
    Extensions.removeAll(builder, elements);
    return builder.build();
  }

  /// Returns the result of removing all elements in this collection that are also
  /// contained in the specified {@code elements} array.
  ///
  /// @return a new persistent collection with elements in this collection that are also
  /// contained in the specified {@code elements} array removed;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentCollection<E> removeAll(E[] elements) {
    var builder = builder();
    Extensions.removeAll(builder, elements);
    return builder.build();
  }

  /// Returns the result of removing all elements in this collection that are also
  /// contained in the specified {@code elements} sequence.
  ///
  /// @return a new persistent collection with elements in this collection that are also
  /// contained in the specified {@code elements} sequence removed;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentCollection<E> removeAll(Stream<? extends E> elements) {
    var builder = builder();
    Extensions.removeAll(builder, elements);
    return builder.build();
  }

  /// Returns all elements in this collection that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent set with elements in this collection that are also
  /// contained in the specified {@code elements} collection
  default PersistentSet<E> intersect(Iterable<? extends E> elements) {
    return PersistentSet.from(this).intersect(elements);
  }

  /// A generic builder of the persistent collection. Builder exposes its modification operations through the [MutableCollection][Collection] interface.
  ///
  /// Builders are reusable, that is [build][#build()] method can be called multiple times with modifications between these calls.
  /// However, modifications applied do not affect previously built persistent collection instances.
  ///
  /// Builder is backed by the same underlying data structure as the persistent collection it was created from.
  /// Thus, [builder][PersistentCollection#builder()] and [build][#build()] methods take constant time consisting of passing the backing storage to the
  /// new builder and persistent collection instances, respectively.
  ///
  /// The builder tracks which nodes in the structure are shared with the persistent collection,
  /// and which are owned by it exclusively. It owns the nodes it copied during modification
  /// operations and avoids copying them on subsequent modifications.
  ///
  /// When [build][#build()] is called the builder forgets about all owned nodes it had created.
  interface Builder<E extends @Nullable Object> extends Collection<E> {
    /// Returns a persistent collection with the same contents as this builder.
    ///
    /// This method can be called multiple times.
    ///
    /// If operations applied on this builder have caused no modifications:
    /// - on the first call it returns the same persistent collection instance this builder was obtained from.
    /// - on subsequent calls it returns the same previously returned persistent collection instance.
    PersistentCollection<E> build();
  }
}
