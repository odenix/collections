/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/// A generic immutable collection that holds pairs of objects (keys and values) and supports efficiently retrieving
/// the value corresponding to each key. Map keys are unique; the map holds only one value for each key.
/// Methods in this interface support only read-only access to the immutable map.
///
/// Modification operations are supported through the [PersistentMap][PersistentMap] interface.
///
/// Implementors of this interface take responsibility to be immutable.
/// Once constructed they must contain the same elements in the same order.
///
/// @param <K> the type of map keys. The map is invariant on its key type, as it
///          can accept key as a parameter (of [containsKey][#containsKey(Object)] for example) and return it in [keys][#keys()] set.
/// @param <V> the type of map values. The map is covariant on its value type.
@SuppressWarnings("unused")
public interface ImmutableMap<K extends @Nullable Object, V extends @Nullable Object> {
  /// Returns an immutable map containing all entries from the specified map.
  ///
  /// If the specified map is already an immutable map, returns it as is.
  ///
  /// Entries of the returned map are iterated in the same order as in the specified map.
  static <K extends @Nullable Object, V extends @Nullable Object> ImmutableMap<K, V> from(
      ImmutableMap<K, V> map) {
    return map instanceof PersistentMap<K, V> persistentMap ? persistentMap : map;
  }

  /// Returns an immutable map containing all entries from the specified map.
  ///
  /// If the specified map is already an immutable map, returns it as is.
  ///
  /// Entries of the returned map are iterated in the same order as in the specified map.
  static <K extends @Nullable Object, V extends @Nullable Object> ImmutableMap<K, V> from(
      Map<K, V> map) {
    if (map instanceof PersistentMap.Builder<K, V> builder) {
      return builder.build();
    }
    return PersistentMap.from(map);
  }

  /// Returns the number of key/value pairs in the map.
  ///
  /// If a map contains more than {@code Int.MAX_VALUE} elements, the value of this property is unspecified.
  /// For implementations allowing to have more than {@code Int.MAX_VALUE} elements,
  /// it is recommended to explicitly document behavior of this property.
  int size();

  /// Returns `true` if the map is empty (contains no elements), `false` otherwise.
  default boolean isEmpty() {
    return size() == 0;
  }

  /// Returns `true` if the map contains the specified {@code key}.
  boolean containsKey(K key);

  /// Returns `true` if the map maps one or more keys to the specified {@code value}.
  boolean containsValue(V value);

  /// Returns the value corresponding to the given {@code key}, or `null` if such a key is not present in the map.
  ///
  /// Note that for maps supporting `null` values,
  /// the returned `null` value associated with the {@code key} is indistinguishable from the missing {@code key},
  /// so [containsKey][#containsKey(Object)] should be used to check if the map actually contains the {@code key}.
  @Nullable V get(K key);

  /// Returns a read-only [Set][ImmutableSet] of all keys in this map.
  ImmutableSet<K> keys();

  /// Returns a read-only [Collection][ImmutableCollection] of all values in this map. Note that this collection may contain duplicate values.
  ImmutableCollection<V> values();

  /// Returns a read-only [Set][ImmutableSet] of all key/value pairs in this map.
  ImmutableSet<Map.Entry<K, V>> entries();
}
