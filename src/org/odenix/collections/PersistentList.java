/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.implementations.immutableList.PersistentVector;

/// A generic persistent ordered collection of elements that supports adding and removing elements.
///
/// Modification operations return new instances of the persistent list with the modification applied.
///
/// @param <E> the type of elements contained in the list. The persistent list is covariant on its element type.
public interface PersistentList<E extends @Nullable Object>
    extends ImmutableList<E>, PersistentCollection<E> {
  /// Returns an empty persistent list.
  static <E extends @Nullable Object> PersistentList<E> of() {
    return PersistentVector.emptyOf();
  }

  /// Returns a new persistent list of the specified elements.
  @SafeVarargs
  static <E extends @Nullable Object> PersistentList<E> of(E... elements) {
    return PersistentList.<E>of().addAll(Arrays.asList(elements));
  }

  /// Returns a persistent list containing all elements of the specified collection.
  ///
  /// If the specified collection is already a persistent list, returns it as is.
  /// If the specified collection is a persistent list builder, calls `build` on it and returns the result.
  static <T extends @Nullable Object> PersistentList<T> from(Iterable<T> iterable) {
    if (iterable instanceof PersistentList<T> list) {
      return list;
    }
    if (iterable instanceof PersistentList.Builder<T> builder) {
      return builder.build();
    }
    return PersistentList.<T>of().addAll(iterable);
  }

  /// Returns a persistent list containing all elements of the specified array.
  static <T extends @Nullable Object> PersistentList<T> from(T[] array) {
    return PersistentList.<T>of().addAll(array);
  }

  /// Returns a persistent list containing all elements of the specified sequence.
  static <T extends @Nullable Object> PersistentList<T> from(Stream<? extends T> stream) {
    return PersistentList.<T>of().addAll(stream);
  }

  /// Returns a persistent list containing all characters.
  static PersistentList<Character> from(CharSequence chars) {
    var builder = PersistentList.<Character>of().builder();
    for (var i = 0; i < chars.length(); i++) {
      builder.add(chars.charAt(i));
    }
    return builder.build();
  }

  /// Returns a new persistent list with the specified {@code element} appended.
  @Override
  PersistentList<E> add(E element);

  /// Returns the result of appending all elements of the specified {@code elements} collection to this list.
  ///
  /// The elements are appended in the order they appear in the specified collection.
  ///
  /// @return a new persistent list with elements of the specified {@code elements} collection appended;
  /// or this instance if the specified collection is empty.
  @Override
  PersistentList<E> addAll(Collection<? extends E> elements);

  /// Returns the result of removing the first appearance of the specified {@code element} from this list.
  ///
  /// @return a new persistent list with the first appearance of the specified {@code element} removed;
  /// or this instance if there is no such element in this list.
  @Override
  PersistentList<E> remove(E element);

  /// Returns the result of removing all elements in this list that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent list with elements in this list that are also
  /// contained in the specified {@code elements} collection removed;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  PersistentList<E> removeAll(Collection<? extends E> elements);

  /// Returns the result of removing all elements in this list that match the specified {@code predicate}.
  ///
  /// @return a new persistent list with elements matching the specified {@code predicate} removed;
  /// or this instance if no elements match the predicate.
  @Override
  PersistentList<E> removeAll(Predicate<? super E> predicate);

  /// Returns all elements in this list that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent list with elements in this list that are also
  /// contained in the specified {@code elements} collection;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  PersistentList<E> retainAll(Collection<? extends E> elements);

  /// Returns an empty persistent list.
  @Override
  PersistentList<E> clear();

  /// Returns the result of inserting the specified {@code c} collection at the specified {@code index}.
  ///
  /// @return a new persistent list with the specified {@code c} collection inserted at the specified {@code index};
  /// or this instance if the specified collection is empty.
  ///
  /// @throws IndexOutOfBoundsException if {@code index} is out of bounds of this list.
  PersistentList<E> addAll(int index, Collection<? extends E> elements);

  /// Returns a new persistent list with the element at the specified {@code index} replaced with the specified {@code element}.
  ///
  /// @throws IndexOutOfBoundsException if {@code index} is out of bounds of this list.
  PersistentList<E> set(int index, E element);

  /// Returns a new persistent list with the specified {@code element} inserted at the specified {@code index}.
  ///
  /// @throws IndexOutOfBoundsException if {@code index} is out of bounds of this list.
  PersistentList<E> add(int index, E element);

  /// Returns a new persistent list with the element at the specified {@code index} removed.
  ///
  /// @throws IndexOutOfBoundsException if {@code index} is out of bounds of this list.
  PersistentList<E> removeAt(int index);

  @Override
  Builder<E> builder();

  default PersistentList<E> mutate(Consumer<? super List<E>> mutator) {
    var builder = builder();
    mutator.accept(builder);
    return builder.build();
  }

  /// Returns the result of appending all elements of the specified {@code elements} collection to this list.
  ///
  /// The elements are appended in the order they appear in the specified collection.
  ///
  /// @return a new persistent list with elements of the specified {@code elements} collection appended;
  /// or this instance if the specified collection is empty.
  @Override
  default PersistentList<E> addAll(Iterable<? extends E> elements) {
    if (elements instanceof Collection<? extends E> collection) {
      return addAll(collection);
    }
    return mutate(list -> Extensions.addAll(list, elements));
  }

  /// Returns the result of appending all elements of the specified {@code elements} array to this list.
  ///
  /// The elements are appended in the order they appear in the specified array.
  ///
  /// @return a new persistent list with elements of the specified {@code elements} array appended;
  /// or this instance if the specified array is empty.
  @Override
  default PersistentList<E> addAll(E[] elements) {
    return mutate(list -> Collections.addAll(list, elements));
  }

  /// Returns the result of appending all elements of the specified {@code elements} sequence to this list.
  ///
  /// The elements are appended in the order they appear in the specified sequence.
  ///
  /// @return a new persistent list with elements of the specified {@code elements} sequence appended;
  /// or this instance if the specified sequence is empty.
  @Override
  default PersistentList<E> addAll(Stream<? extends E> elements) {
    return mutate(list -> Extensions.addAll(list, elements));
  }

  /// Returns the result of removing all elements in this list that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent list with elements in this list that are also
  /// contained in the specified {@code elements} collection removed;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  default PersistentList<E> removeAll(Iterable<? extends E> elements) {
    if (elements instanceof Collection<? extends E> collection) {
      return removeAll(collection);
    }
    return mutate(list -> Extensions.removeAll(list, elements));
  }

  /// Returns the result of removing all elements in this list that are also
  /// contained in the specified {@code elements} array.
  ///
  /// @return a new persistent list with elements in this list that are also
  /// contained in the specified {@code elements} array removed;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  default PersistentList<E> removeAll(E[] elements) {
    return mutate(list -> Extensions.removeAll(list, elements));
  }

  /// Returns the result of removing all elements in this list that are also
  /// contained in the specified {@code elements} sequence.
  ///
  /// @return a new persistent list with elements in this list that are also
  /// contained in the specified {@code elements} sequence removed;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  default PersistentList<E> removeAll(Stream<? extends E> elements) {
    return mutate(list -> Extensions.removeAll(list, elements));
  }

  /// A generic builder of the persistent list. Builder exposes its modification operations through the [MutableList][List] interface.
  ///
  /// Builders are reusable, that is [build][#build()] method can be called multiple times with modifications between these calls.
  /// However, modifications applied do not affect previously built persistent list instances.
  ///
  /// Builder is backed by the same underlying data structure as the persistent list it was created from.
  /// Thus, [builder][PersistentList#builder()] and [build][#build()] methods take constant time consisting of passing the backing storage to the
  /// new builder and persistent list instances, respectively.
  ///
  /// The builder tracks which nodes in the structure are shared with the persistent list,
  /// and which are owned by it exclusively. It owns the nodes it copied during modification
  /// operations and avoids copying them on subsequent modifications.
  ///
  /// When [build][#build()] is called the builder forgets about all owned nodes it had created.
  interface Builder<E extends @Nullable Object>
      extends List<E>, PersistentCollection.Builder<E> {
    @Override
    PersistentList<E> build();
  }
}
