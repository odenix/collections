/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.implementations.immutableMap.PersistentHashMap;
import org.odenix.collections.implementations.immutableMap.PersistentHashMapBuilder;
import org.odenix.collections.implementations.persistentOrderedMap.PersistentOrderedMap;
import org.odenix.collections.implementations.persistentOrderedMap.PersistentOrderedMapBuilder;

/// A generic persistent collection that holds pairs of objects (keys and values) and supports efficiently retrieving
/// the value corresponding to each key. Map keys are unique; the map holds only one value for each key.
///
/// Modification operations return new instances of the persistent map with the modification applied.
///
/// @param <K> the type of map keys. The map is invariant on its key type.
/// @param <V> the type of map values. The persistent map is covariant on its value type.
public interface PersistentMap<K extends @Nullable Object, V extends @Nullable Object>
    extends ImmutableMap<K, V> {
  /// Returns an empty persistent map.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> of() {
    return PersistentOrderedMap.emptyOf();
  }

  /// Returns a new persistent map with the specified contents, given as a list of pairs
  /// where the first component is the key and the second is the value.
  ///
  /// If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
  ///
  /// Entries of the map are iterated in the order they were specified.
  @SafeVarargs
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> of(
      Map.Entry<? extends K, ? extends V>... entries) {
    PersistentMap<K, V> map = of();
    return map.putAll(entries);
  }

  /// Returns an empty persistent map.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> hashOf() {
    return PersistentHashMap.emptyOf();
  }

  /// Returns a new persistent map with the specified contents, given as a list of pairs
  /// where the first component is the key and the second is the value.
  ///
  /// If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
  ///
  /// Order of the entries in the returned map is unspecified.
  @SafeVarargs
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> hashOf(
      Map.Entry<? extends K, ? extends V>... entries) {
    PersistentMap<K, V> map = hashOf();
    return map.putAll(entries);
  }

  /// Returns a persistent map containing all entries from the specified map.
  ///
  /// If the specified map is already a persistent map, returns it as is.
  /// If the specified map is a persistent map builder, calls `build` on it and returns the result.
  ///
  /// Entries of the returned map are iterated in the same order as in the specified map.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> from(
      ImmutableMap<K, V> map) {
    if (map instanceof PersistentMap<K, V> persistentMap) {
      return persistentMap;
    }
    return PersistentMap.<K, V>of().putAll(map.entries());
  }

  /// Returns a persistent map containing all entries from the specified map.
  ///
  /// If the specified map is already a persistent map, returns it as is.
  /// If the specified map is a persistent map builder, calls `build` on it and returns the result.
  ///
  /// Entries of the returned map are iterated in the same order as in the specified map.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> from(
      Map<K, V> map) {
    if (map instanceof PersistentOrderedMapBuilder<K, V> builder) {
      return builder.build();
    }
    return PersistentMap.<K, V>of().putAll(map);
  }

  /// Returns a persistent map containing all entries from the specified entry sequence.
  ///
  /// Entries of the returned map are iterated in the same order as in the specified entry sequence.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> from(
      Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
    return PersistentMap.<K, V>of().putAll(entries);
  }

  /// Returns a persistent map containing all entries from the specified array.
  ///
  /// Entries of the returned map are iterated in the same order as in the specified array.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> from(
      Map.Entry<? extends K, ? extends V>[] entries) {
    return PersistentMap.<K, V>of().putAll(entries);
  }

  /// Returns a persistent map containing all entries from the specified sequence.
  ///
  /// Entries of the returned map are iterated in the same order as in the specified sequence.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> from(
      Stream<? extends Map.Entry<? extends K, ? extends V>> entries) {
    return PersistentMap.<K, V>of().putAll(entries);
  }

  /// Returns a persistent map containing all entries from the specified map.
  ///
  /// If the specified map is already a persistent hash map, returns it as is.
  /// If the specified map is a persistent hash map builder, calls `build` on it and returns the result.
  ///
  /// Order of the entries in the returned map is unspecified.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> hashFrom(
      Map<K, V> map) {
    if (map instanceof PersistentHashMapBuilder<K, V> builder) {
      return builder.build();
    }
    return PersistentMap.<K, V>hashOf().putAll(map);
  }

  /// Returns a persistent map containing all entries from the specified entry sequence.
  ///
  /// Order of the entries in the returned map is unspecified.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> hashFrom(
      Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
    return PersistentMap.<K, V>hashOf().putAll(entries);
  }

  /// Returns a persistent map containing all entries from the specified array.
  ///
  /// Order of the entries in the returned map is unspecified.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> hashFrom(
      Map.Entry<? extends K, ? extends V>[] entries) {
    return PersistentMap.<K, V>hashOf().putAll(entries);
  }

  /// Returns a persistent map containing all entries from the specified sequence.
  ///
  /// Order of the entries in the returned map is unspecified.
  static <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> hashFrom(
      Stream<? extends Map.Entry<? extends K, ? extends V>> entries) {
    return PersistentMap.<K, V>hashOf().putAll(entries);
  }

  /// Returns the result of associating the specified {@code value} with the specified {@code key} in this map.
  ///
  /// If this map already contains a mapping for the key, the old value is replaced by the specified value.
  ///
  /// @return a new persistent map with the specified {@code value} associated with the specified {@code key};
  /// or this instance if no modifications were made in the result of this operation.
  PersistentMap<K, V> put(K key, V value);

  /// Returns the result of removing the specified {@code key} and its corresponding value from this map.
  ///
  /// @return a new persistent map with the specified {@code key} and its corresponding value removed;
  /// or this instance if it contains no mapping for the key.
  PersistentMap<K, V> remove(K key);

  /// Returns the result of removing the entry that maps the specified {@code key} to the specified {@code value}.
  ///
  /// @return a new persistent map with the entry for the specified {@code key} and {@code value} removed;
  /// or this instance if it contains no entry with the specified key and value.
  PersistentMap<K, V> remove(K key, V value);

  /// Returns the result of merging the specified {@code m} map with this map.
  ///
  /// The effect of this call is equivalent to that of calling `put(k, v)` once for each
  /// mapping from key `k` to value `v` in the specified map.
  ///
  /// @return a new persistent map with keys and values from the specified map {@code m} associated;
  /// or this instance if no modifications were made in the result of this operation.
  PersistentMap<K, V> putAll(Map<? extends K, ? extends V> map);

  /// Returns an empty persistent map.
  PersistentMap<K, V> clear();

  /// Returns a new builder with the same contents as this map.
  ///
  /// The builder can be used to efficiently perform multiple modification operations.
  Builder<K, V> builder();

  default PersistentMap<K, V> mutate(Consumer<? super Map<K, V>> mutator) {
    var builder = builder();
    mutator.accept(builder);
    return builder.build();
  }

  /// Returns the result of replacing or adding entries to this map from the specified key-value pairs.
  ///
  /// @return a new persistent map with entries from the specified key-value pairs added;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentMap<K, V> putAll(Iterable<? extends Map.Entry<? extends K, ? extends V>> pairs) {
    return mutate(map -> Extensions.putAll(map, pairs));
  }

  /// Returns the result of replacing or adding entries to this map from the specified key-value pairs.
  ///
  /// @return a new persistent map with entries from the specified key-value pairs added;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentMap<K, V> putAll(Map.Entry<? extends K, ? extends V>[] pairs) {
    return mutate(map -> Extensions.putAll(map, pairs));
  }

  /// Returns the result of replacing or adding entries to this map from the specified key-value pairs.
  ///
  /// @return a new persistent map with entries from the specified key-value pairs added;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentMap<K, V> putAll(Stream<? extends Map.Entry<? extends K, ? extends V>> pairs) {
    return mutate(map -> Extensions.putAll(map, pairs));
  }

  /// Returns the result of removing the specified {@code keys} and their corresponding values from this map.
  ///
  /// @return a new persistent map with the specified {@code keys} and their corresponding values removed;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentMap<K, V> removeAll(Iterable<? extends K> keys) {
    return mutate(map -> Extensions.minusAssign(map, keys));
  }

  /// Returns the result of removing the specified {@code keys} and their corresponding values from this map.
  ///
  /// @return a new persistent map with the specified {@code keys} and their corresponding values removed;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentMap<K, V> removeAll(K[] keys) {
    return mutate(map -> Extensions.minusAssign(map, keys));
  }

  /// Returns the result of removing the specified {@code keys} and their corresponding values from this map.
  ///
  /// @return a new persistent map with the specified {@code keys} and their corresponding values removed;
  /// or this instance if no modifications were made in the result of this operation.
  default PersistentMap<K, V> removeAll(Stream<? extends K> keys) {
    return mutate(map -> Extensions.minusAssign(map, keys));
  }

  /// A generic builder of the persistent map. Builder exposes its modification operations through the [MutableMap][Map] interface.
  ///
  /// Builders are reusable, that is [build][#build()] method can be called multiple times with modifications between these calls.
  /// However, modifications applied do not affect previously built persistent map instances.
  ///
  /// Builder is backed by the same underlying data structure as the persistent map it was created from.
  /// Thus, [builder][PersistentMap#builder()] and [build][#build()] methods take constant time passing the backing storage to the
  /// new builder and persistent map instances, respectively.
  ///
  /// The builder tracks which nodes in the structure are shared with the persistent map,
  /// and which are owned by it exclusively. It owns the nodes it copied during modification
  /// operations and avoids copying them on subsequent modifications.
  ///
  /// When [build][#build()] is called the builder forgets about all owned nodes it had created.
  interface Builder<K extends @Nullable Object, V extends @Nullable Object> extends Map<K, V> {
    /// Returns a persistent map with the same contents as this builder.
    ///
    /// This method can be called multiple times.
    ///
    /// If operations applied on this builder have caused no modifications:
    /// - on the first call it returns the same persistent map instance this builder was obtained from.
    /// - on subsequent calls it returns the same previously returned persistent map instance.
    PersistentMap<K, V> build();
  }
}
