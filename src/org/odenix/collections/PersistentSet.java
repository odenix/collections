/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.implementations.immutableSet.PersistentHashSet;
import org.odenix.collections.implementations.immutableSet.PersistentHashSetBuilder;
import org.odenix.collections.implementations.persistentOrderedSet.PersistentOrderedSet;
import org.odenix.collections.implementations.persistentOrderedSet.PersistentOrderedSetBuilder;

/// A generic persistent unordered collection of elements that does not support duplicate elements, and supports
/// adding and removing elements.
///
/// Modification operations return new instances of the persistent set with the modification applied.
///
/// @param <E> the type of elements contained in the set. The persistent set is covariant on its element type.
public interface PersistentSet<E extends @Nullable Object> extends ImmutableSet<E>, PersistentCollection<E> {
  /// Returns an empty persistent set.
  static <E extends @Nullable Object> PersistentSet<E> of() {
    return PersistentOrderedSet.emptyOf();
  }

  /// Returns a new persistent set with the given elements.
  ///
  /// Elements of the returned set are iterated in the order they were specified.
  @SafeVarargs
  static <E extends @Nullable Object> PersistentSet<E> of(E... elements) {
    PersistentSet<E> set = of();
    return set.addAll(elements);
  }

  /// Returns an empty persistent set.
  static <E extends @Nullable Object> PersistentSet<E> hashOf() {
    return PersistentHashSet.emptyOf();
  }

  /// Returns a new persistent set with the given elements.
  ///
  /// Order of the elements in the returned set is unspecified.
  @SafeVarargs
  static <E extends @Nullable Object> PersistentSet<E> hashOf(E... elements) {
    PersistentSet<E> set = hashOf();
    return set.addAll(elements);
  }

  /// Returns a persistent set of all elements of the specified collection.
  ///
  /// If the specified collection is already a persistent set, returns it as is.
  /// If the specified collection is a persistent set builder, calls `build` on it and returns the result.
  ///
  /// Elements of the returned set are iterated in the same order as in the specified collection.
  static <T extends @Nullable Object> PersistentSet<T> from(Iterable<T> iterable) {
    if (iterable instanceof PersistentOrderedSet) {
      return (PersistentSet<T>) iterable;
    }
    if (iterable instanceof PersistentOrderedSetBuilder) {
      return ((PersistentOrderedSetBuilder<T>) iterable).build();
    }
    return PersistentSet.<T>of().addAll(iterable);
  }

  /// Returns a persistent set of all elements of the specified array.
  ///
  /// Elements of the returned set are iterated in the same order as in the specified array.
  static <T extends @Nullable Object> PersistentSet<T> from(T[] array) {
    return PersistentSet.<T>of().addAll(array);
  }

  /// Returns a persistent set of all elements of the specified sequence.
  ///
  /// Elements of the returned set are iterated in the same order as in the specified sequence.
  static <T extends @Nullable Object> PersistentSet<T> from(Stream<? extends T> stream) {
    return PersistentSet.<T>of().addAll(stream);
  }

  /// Returns a persistent set of all characters.
  ///
  /// Elements of the returned set are iterated in the same order as in the specified char sequence.
  static PersistentSet<Character> from(CharSequence chars) {
    var builder = PersistentSet.<Character>of().builder();
    for (var i = 0; i < chars.length(); i++) {
      builder.add(chars.charAt(i));
    }
    return builder.build();
  }

  /// Returns a persistent set containing all elements from the specified collection.
  ///
  /// If the specified collection is already a persistent hash set, returns it as is.
  /// If the specified collection is a persistent hash set builder, calls `build` on it and returns the result.
  ///
  /// Order of the elements in the returned set is unspecified.
  static <T extends @Nullable Object> PersistentSet<T> hashFrom(Iterable<T> iterable) {
    if (iterable instanceof PersistentHashSet<T> set) {
      return set;
    }
    if (iterable instanceof PersistentHashSetBuilder<T> builder) {
      return builder.build();
    }
    return PersistentSet.<T>hashOf().addAll(iterable);
  }

  /// Returns a persistent set of all elements of the specified array.
  ///
  /// Order of the elements in the returned set is unspecified.
  static <T extends @Nullable Object> PersistentSet<T> hashFrom(T[] array) {
    return PersistentSet.<T>hashOf().addAll(array);
  }

  /// Returns a persistent set of all elements of the specified sequence.
  ///
  /// Order of the elements in the returned set is unspecified.
  static <T extends @Nullable Object> PersistentSet<T> hashFrom(Stream<? extends T> stream) {
    return PersistentSet.<T>hashOf().addAll(stream);
  }

  /// Returns a persistent set of all characters.
  ///
  /// Order of the elements in the returned set is unspecified.
  static PersistentSet<Character> hashFrom(CharSequence chars) {
    var builder = PersistentSet.<Character>hashOf().builder();
    for (var i = 0; i < chars.length(); i++) {
      builder.add(chars.charAt(i));
    }
    return builder.build();
  }

  /// Returns the result of adding the specified {@code element} to this set.
  ///
  /// @return a new persistent set with the specified {@code element} added;
  /// or this instance if it already contains the element.
  @Override
  PersistentSet<E> add(E element);

  /// Returns the result of adding all elements of the specified {@code elements} collection to this set.
  ///
  /// @return a new persistent set with elements of the specified {@code elements} collection added;
  /// or this instance if it already contains every element of the specified collection.
  @Override
  PersistentSet<E> addAll(Collection<? extends E> elements);

  /// Returns the result of removing the specified {@code element} from this set.
  ///
  /// @return a new persistent set with the specified {@code element} removed;
  /// or this instance if there is no such element in this set.
  @Override
  PersistentSet<E> remove(E element);

  /// Returns the result of removing all elements in this set that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent set with elements in this set that are also
  /// contained in the specified {@code elements} collection removed;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  PersistentSet<E> removeAll(Collection<? extends E> elements);

  /// Returns the result of removing all elements in this set that match the specified {@code predicate}.
  ///
  /// @return a new persistent set with elements matching the specified {@code predicate} removed;
  /// or this instance if no elements match the predicate.
  @Override
  PersistentSet<E> removeAll(Predicate<? super E> predicate);

  /// Returns all elements in this set that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent set with elements in this set that are also
  /// contained in the specified {@code elements} collection;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  PersistentSet<E> retainAll(Collection<? extends E> elements);

  /// Returns an empty persistent set.
  @Override
  PersistentSet<E> clear();

  @Override
  Builder<E> builder();

  default PersistentSet<E> mutate(Consumer<? super Set<E>> mutator) {
    var builder = builder();
    mutator.accept(builder);
    return builder.build();
  }

  /// Returns the result of adding all elements of the specified {@code elements} collection to this set.
  ///
  /// @return a new persistent set with elements of the specified {@code elements} collection added;
  /// or this instance if it already contains every element of the specified collection.
  @Override
  default PersistentSet<E> addAll(Iterable<? extends E> elements) {
    if (elements instanceof Collection<? extends E> collection) {
      return addAll(collection);
    }
    return mutate(set -> Extensions.addAll(set, elements));
  }

  /// Returns the result of adding all elements of the specified {@code elements} array to this set.
  ///
  /// @return a new persistent set with elements of the specified {@code elements} array added;
  /// or this instance if it already contains every element of the specified array.
  @Override
  default PersistentSet<E> addAll(E[] elements) {
    return mutate(set -> Collections.addAll(set, elements));
  }

  /// Returns the result of adding all elements of the specified {@code elements} sequence to this set.
  ///
  /// @return a new persistent set with elements of the specified {@code elements} sequence added;
  /// or this instance if it already contains every element of the specified sequence.
  @Override
  default PersistentSet<E> addAll(Stream<? extends E> elements) {
    return mutate(set -> Extensions.addAll(set, elements));
  }

  /// Returns the result of removing all elements in this set that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent set with elements in this set that are also
  /// contained in the specified {@code elements} collection removed;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  default PersistentSet<E> removeAll(Iterable<? extends E> elements) {
    if (elements instanceof Collection<? extends E> collection) {
      return removeAll(collection);
    }
    return mutate(set -> Extensions.removeAll(set, elements));
  }

  /// Returns the result of removing all elements in this set that are also
  /// contained in the specified {@code elements} array.
  ///
  /// @return a new persistent set with elements in this set that are also
  /// contained in the specified {@code elements} array removed;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  default PersistentSet<E> removeAll(E[] elements) {
    return mutate(set -> Extensions.removeAll(set, elements));
  }

  /// Returns the result of removing all elements in this set that are also
  /// contained in the specified {@code elements} sequence.
  ///
  /// @return a new persistent set with elements in this set that are also
  /// contained in the specified {@code elements} sequence removed;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  default PersistentSet<E> removeAll(Stream<? extends E> elements) {
    return mutate(set -> Extensions.removeAll(set, elements));
  }

  /// Returns all elements in this set that are also
  /// contained in the specified {@code elements} collection.
  ///
  /// @return a new persistent set with elements in this set that are also
  /// contained in the specified {@code elements} collection;
  /// or this instance if no modifications were made in the result of this operation.
  @Override
  default PersistentSet<E> intersect(Iterable<? extends E> elements) {
    if (elements instanceof Collection<? extends E> collection) {
      return retainAll(collection);
    }
    return mutate(set -> Extensions.retainAll(set, elements));
  }

  /// A generic builder of the persistent set. Builder exposes its modification operations through the [MutableSet][Set] interface.
  ///
  /// Builders are reusable, that is [build][#build()] method can be called multiple times with modifications between these calls.
  /// However, modifications applied do not affect previously built persistent set instances.
  ///
  /// Builder is backed by the same underlying data structure as the persistent set it was created from.
  /// Thus, [builder][PersistentSet#builder()] and [build][#build()] methods take constant time consisting of passing the backing storage to the
  /// new builder and persistent set instances, respectively.
  ///
  /// The builder tracks which nodes in the structure are shared with the persistent set,
  /// and which are owned by it exclusively. It owns the nodes it copied during modification
  /// operations and avoids copying them on subsequent modifications.
  ///
  /// When [build][#build()] is called the builder forgets about all owned nodes it had created.
  interface Builder<E extends @Nullable Object>
      extends Set<E>, PersistentCollection.Builder<E> {
    @Override
    PersistentSet<E> build();
  }
}
