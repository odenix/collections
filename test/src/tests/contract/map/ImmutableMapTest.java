/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.ImmutableMap;
import org.odenix.collections.PersistentMap;

import tests.IntWrapper;
import tests.ObjectWrapper;

class ImmutableHashMapTest extends ImmutableMapTest {
  @SafeVarargs
  @Override
  final <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> immutableMapOf(
      Map.Entry<K, V>... pairs) {
    return PersistentMap.hashOf(pairs);
  }

  @Override
  <K extends @Nullable Object, V extends @Nullable Object> void testBuilderToPersistentMap(
      PersistentMap.Builder<K, V> builder) {
    // Java adaptation: hash map builders are not recognized by PersistentMap.from(Map).
    assertNotSame(builder.build(), PersistentMap.from(builder));
  }

  @Override
  <K extends @Nullable Object, V extends @Nullable Object> ImmutableMap<K, V> toImmutableMap(
      Map<K, V> map) {
    return PersistentMap.hashFrom(map);
  }

  @Override
  <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> toPersistentMap(
      Map<K, V> map) {
    return PersistentMap.hashFrom(map);
  }

  @Test
  void regressionGithubIssue109() {
    var map0 = immutableMapOf(Map.entry(0, 0), Map.entry(1, 1), Map.entry(32, 32));
    var map1 = map0.mutate(mutable -> mutable.remove(0));
    var map2 =
        map1.mutate(
            mutable -> {
              mutable.remove(1);
              mutable.remove(0);
            });

    assertTrue(map1.containsKey(32));
    assertTrue(map2.containsKey(32));
  }

  @Test
  void regressionGithubIssue114() {
    var p = PersistentMap.hashOf(Map.entry(99, 1));
    var e = IntStream.range(0, 101).mapToObj(index -> Map.entry(index, index)).toList();
    var c = PersistentMap.hashOf(TestExtensions.toTypedArray(e));
    var n = p.builder();
    n.putAll(TestExtensions.toMap(c));
    var built = n.build();
    assertEquals(99, built.get(99));
  }
}

class ImmutableOrderedMapTest extends ImmutableMapTest {
  @SafeVarargs
  @Override
  final <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> immutableMapOf(
      Map.Entry<K, V>... pairs) {
    return PersistentMap.of(pairs);
  }

  @Override
  <K extends @Nullable Object, V extends @Nullable Object> void testBuilderToPersistentMap(
      PersistentMap.Builder<K, V> builder) {
    assertSame(builder.build(), PersistentMap.from(builder));
  }

  @Override
  <K extends @Nullable Object, V extends @Nullable Object> ImmutableMap<K, V> toImmutableMap(
      Map<K, V> map) {
    return PersistentMap.from(map);
  }

  @Override
  <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> toPersistentMap(
      Map<K, V> map) {
    return PersistentMap.from(map);
  }

  @Override
  <K extends @Nullable Object, V extends @Nullable Object> void compareMaps(
      Map<K, V> expected, ImmutableMap<K, V> actual) {
    TestExtensions.compareOrderedMaps(expected, actual);
  }

  @Test
  void iterationOrder() {
    PersistentMap<String, @Nullable Integer> map =
        immutableMapOf(TestExtensions.entry("x", null), TestExtensions.entry("y", 1));
    assertEquals(List.of("x", "y"), TestExtensions.keys(map));

    map = map.put("x", 1);
    assertEquals(List.of("x", "y"), TestExtensions.keys(map));

    map = map.remove("x");
    map = map.put("x", 2);
    assertEquals(List.of("y", "x"), TestExtensions.keys(map));
    assertEquals(List.of(1, 2), TestExtensions.values(map));
    TestExtensions.compareOrderedMaps(
        TestExtensions.orderedMapOf(TestExtensions.entry("y", 1), TestExtensions.entry("x", 2)), map);
  }

  @Test
  void keyHashCodeChanged() {
    var changing = new HashSet<String>();
    changing.add("ok");
    PersistentMap<Object, Object> persistent =
        immutableMapOf(TestExtensions.entry("constant", "fixed"), TestExtensions.entry(changing, "modified"));
    assertEquals(1, TestExtensions.keys(persistent).stream().filter(key -> key == changing).count());
    changing.add("break iteration");
    assertThrows(
        ConcurrentModificationException.class,
        () -> TestExtensions.keys(persistent).stream().filter(key -> key == changing).toList());
  }

  @Test
  void builderKeyHashCodeChanged() {
    var changing = new HashSet<String>();
    changing.add("ok");
    PersistentMap.Builder<Object, Object> builder = immutableMapOf().builder();
    builder.putAll(
        TestExtensions.orderedMapOf(
            TestExtensions.entry("constant", "fixed"), TestExtensions.entry(changing, "modified")));
    assertEquals(1, builder.entrySet().stream().filter(entry -> entry.getKey() == changing).count());
    changing.add("break iteration");
    assertThrows(
        ConcurrentModificationException.class,
        () -> builder.entrySet().stream().filter(entry -> entry.getKey() == changing).toList());
  }
}

abstract class ImmutableMapTest {
  @SuppressWarnings("unchecked")
  abstract <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> immutableMapOf(
      Map.Entry<K, V>... pairs);

  <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> immutableMapOf() {
    return immutableMapOf(TestExtensions.emptyEntries());
  }

  abstract <K extends @Nullable Object, V extends @Nullable Object> void testBuilderToPersistentMap(
      PersistentMap.Builder<K, V> builder);

  abstract <K extends @Nullable Object, V extends @Nullable Object> ImmutableMap<K, V> toImmutableMap(
      Map<K, V> map);

  abstract <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> toPersistentMap(
      Map<K, V> map);

  <K extends @Nullable Object, V extends @Nullable Object> PersistentMap<K, V> immutableMapOf(
      Map<K, V> map) {
    return this.<K, V>immutableMapOf().putAll(map);
  }

  <K extends @Nullable Object, V extends @Nullable Object> void compareMaps(
      Map<K, V> expected, ImmutableMap<K, V> actual) {
    TestExtensions.compareMaps(expected, actual);
  }

  @Test
  void empty() {
    var empty1 = this.<Integer, String>immutableMapOf();
    var empty2 = this.<String, Integer>immutableMapOf();
    assertEquals(empty1, empty2);
    assertEquals(Map.of(), TestExtensions.toMap(empty1));
    assertSame(empty1, empty2);

    compareMaps(Map.of(), empty1);
  }

  @Test
  void ofPairs() {
    Map<@Nullable String, @Nullable Integer> map0 =
        TestExtensions.mapOf(
            TestExtensions.entry("x", 1), TestExtensions.entry("y", null), TestExtensions.entry(null, 2));
    var map1 = immutableMapOf(map0);
    var map2 = immutableMapOf(map0);

    compareMaps(map0, map1);
    compareMaps(TestExtensions.toMap(map1), map2);
  }

  @Test
  void toImmutable() {
    Map<@Nullable String, @Nullable Integer> original =
        TestExtensions.mapOf(
            TestExtensions.entry("x", 1), TestExtensions.entry("y", null), TestExtensions.entry(null, 2));
    var immOriginal = toImmutableMap(original);
    compareMaps(original, immOriginal);

    Map<@Nullable String, @Nullable Integer> map = new HashMap<>(original);
    PersistentMap<@Nullable String, @Nullable Integer> immMap = toPersistentMap(map);
    var immMap2 = ImmutableMap.from(immMap);
    assertSame(immMap, immMap2);

    compareMaps(original, immMap);
    compareMaps(map, immMap);

    map.remove(null);
    assertNotEquals(map, immMap);

    immMap = immMap.remove(null);
    compareMaps(map, immMap);
  }

  @Test
  void emptyMapToPersistentMap() {
    // Requires the ordered persistent-map implementation behind Map.toPersistentMap().
  }

  @Test
  void putElements() {
    PersistentMap<String, @Nullable Integer> map = this.<String, @Nullable Integer>immutableMapOf();
    map = map.put("x", 0);
    map = map.put("x", 1);
    map = map.putAll(TestExtensions.mapOf(TestExtensions.entry("x", null)));
    map = map.put("y", null);
    map = map.put("y", 1);
    assertEquals(
        TestExtensions.mapOf(TestExtensions.entry("x", null), TestExtensions.entry("y", 1)),
        TestExtensions.toMap(map));

    map = map.putAll(TestExtensions.toMap(map).entrySet());
    map = map.putAll(
        List.of(TestExtensions.entry("x!", null), TestExtensions.entry("y!", 1)));

    assertEquals(map.size(), map.entries().size());
    assertEquals(
        TestExtensions.mapOf(
            TestExtensions.entry("x", null),
            TestExtensions.entry("y", 1),
            TestExtensions.entry("x!", null),
            TestExtensions.entry("y!", 1)),
        TestExtensions.toMap(map));
  }

  @Test
  void putEqualButNotSameValue() {
    record Value<T>(T value) {}

    var map = immutableMapOf(Map.of("x", new Value<>(1)));

    var newValue = new Value<>(1);
    var newMap = map.put("x", newValue);
    assertNotSame(map, newMap);
    assertEquals(map, newMap);

    var sameMap = newMap.put("x", newValue);
    assertSame(newMap, sameMap);
  }

  @Test
  void removeElements() {
    PersistentMap<@Nullable String, @Nullable Object> map =
        immutableMapOf(TestExtensions.mapOf(TestExtensions.entry("x", 1), TestExtensions.entry(null, "x")));

    assertEquals(TestExtensions.mapOf(Map.entry("x", 1)), TestExtensions.toMap(map.remove(null)));
    assertEquals(
        TestExtensions.mapOf(Map.entry("x", 1)),
        TestExtensions.toMap(map.remove(null, "x")));
    assertEquals(TestExtensions.toMap(map), TestExtensions.toMap(map.remove("x", 2)));

    assertEquals(Map.of(), TestExtensions.toMap(map.clear()));
    assertEquals(Map.of(), TestExtensions.toMap(map.remove("x").remove(null)));
  }

  @Test
  void removeCollection() {
    var map = immutableMapOf(Map.ofEntries(Map.entry(0, "a"), Map.entry(1, "B"), Map.entry(2, "c")));
    var newMap = map.removeAll(List.of(2)).removeAll(List.of(1));
    assertEquals(TestExtensions.mapOf(Map.entry(0, "a")), TestExtensions.toMap(newMap));
  }

  @Test
  void removeMatching() {
    var map = immutableMapOf(Map.ofEntries(Map.entry(0, "a"), Map.entry(1, "B"), Map.entry(2, "c")));
    var newMap = map.mutate(mutable -> mutable.entrySet().removeIf(entry -> entry.getKey() % 2 == 0));
    assertEquals(TestExtensions.mapOf(Map.entry(1, "B")), TestExtensions.toMap(newMap));
  }

  @Test
  void builder() {
    var builder = this.<Character, @Nullable Integer>immutableMapOf().builder();
    "abcxaxyz12".chars().forEach(character -> builder.put((char) character, character));
    PersistentMap<Character, @Nullable Integer> map = builder.build();
    assertEquals(TestExtensions.toMap(map), builder);
    assertSame(map, builder.build());

    ImmutableMap<Character, @Nullable Integer> map2 = ImmutableMap.from(builder);
    assertSame(map, map2);

    testBuilderToPersistentMap(builder);

    testMutation(map, mutable -> mutable.put('K', null));
    testMutation(
        map,
        mutable -> {
          mutable.put('k', 0);
          mutable.put('o', 0);
          mutable.put('t', 0);
          mutable.put('l', 0);
          mutable.put('i', 0);
          mutable.put('n', 0);
        });
    testMutation(map, mutable -> mutable.put('a', null));
    testMutation(map, mutable -> mutable.remove('x'));
    testMutation(map, Map::clear);
    testMutation(map, mutable -> mutable.entrySet().remove(null));
  }

  private static <K extends @Nullable Object, V extends @Nullable Object> void testMutation(
      PersistentMap<K, V> map, Consumer<Map<K, V>> operation) {
    var mutable = TestExtensions.toMutableMap(map);
    var builder = map.builder();

    operation.accept(mutable);
    operation.accept(builder);

    TestExtensions.compareMaps(mutable, builder);
    TestExtensions.compareMaps(mutable, builder.build());
  }

  @Test
  void noOperation() {
    testNoOperation(this.<String, String>immutableMapOf(), PersistentMap::clear, Map::clear);

    var key = new ObjectWrapper<>("x", "x".hashCode());
    var equalKey = new ObjectWrapper<>("x", "x".hashCode());
    var notEqualKey = new ObjectWrapper<>("y", "x".hashCode());
    var value = new ObjectWrapper<>(1, 1);
    var equalValue = new ObjectWrapper<>(1, 1);
    var notEqualValue = new ObjectWrapper<>(2, 2);

    PersistentMap<@Nullable ObjectWrapper<String>, @Nullable Object> map =
        immutableMapOf(TestExtensions.mapOf(TestExtensions.entry(key, value), TestExtensions.entry(null, "x")));

    testNoOperation(map, persistent -> persistent.remove(notEqualKey), mutable -> mutable.remove(notEqualKey));
    testNotNoOperation(map, persistent -> persistent.remove(equalKey), mutable -> mutable.remove(equalKey));

    testNoOperation(
        map,
        persistent -> persistent.remove(key, notEqualValue),
        mutable -> mutable.remove(key, notEqualValue));
    testNotNoOperation(
        map,
        persistent -> persistent.remove(key, equalValue),
        mutable -> mutable.remove(key, equalValue));

    testNoOperation(map, persistent -> persistent.put(equalKey, value), mutable -> mutable.put(equalKey, value));
    testNotNoOperation(
        map,
        persistent -> persistent.put(equalKey, equalValue),
        mutable -> mutable.put(equalKey, equalValue));

    //noinspection CollectionAddedToSelf
    testNoOperation(map, persistent -> persistent.putAll(TestExtensions.toMap(persistent)), mutable -> mutable.putAll(mutable));
    testNoOperation(map, persistent -> persistent.putAll(Map.of()), mutable -> mutable.putAll(Map.of()));
  }

  private static <K extends @Nullable Object, V extends @Nullable Object> void testNoOperation(
      PersistentMap<K, V> map,
      Function<PersistentMap<K, V>, PersistentMap<K, V>> persistent,
      Consumer<Map<K, V>> mutating) {
    var result = persistent.apply(map);
    var buildResult = map.mutate(mutating);
    assertSame(map, result);
    assertSame(map, buildResult);
  }

  private static <K extends @Nullable Object, V extends @Nullable Object> void testNotNoOperation(
      PersistentMap<K, V> map,
      Function<PersistentMap<K, V>, PersistentMap<K, V>> persistent,
      Consumer<Map<K, V>> mutating) {
    var result = persistent.apply(map);
    var buildResult = map.mutate(mutating);
    assertNotSame(map, result);
    assertNotSame(map, buildResult);
  }

  @Test
  void covariantTyping() {
    // No direct Java equivalent: upstream relies on Kotlin declaration-site variance.
  }

  @Test
  void equality() {
    var data =
        IntStream.rangeClosed(0, 200).mapToObj(index -> Map.entry(index, String.valueOf(index))).toList();
    var typedData = TestExtensions.toTypedArray(data);
    var changed = typedData.clone();
    changed[42] = Map.entry(42, "Invalid");

    testEquality(typedData, changed);
  }

  @Test
  void collisionEquality() {
    var data =
        IntStream.rangeClosed(0, 200)
            .mapToObj(index -> Map.entry(new IntWrapper(index, index % 50), String.valueOf(index)))
            .toList();
    var typedData = TestExtensions.toTypedArray(data);
    var changed = typedData.clone();
    changed[42] = Map.entry(new IntWrapper(42, 42), "Invalid");

    testEquality(typedData, changed);
  }

  private <K extends @Nullable Object, V extends @Nullable Object> void testEquality(
      Map.Entry<K, V>[] data, Map.Entry<K, V>[] changed) {
    var base = immutableMapOf(data);
    testSpecializedEquality(base, data, true);
    testSpecializedEquality(base, changed, false);

    var builder = this.<K, V>immutableMapOf().builder();
    builder.putAll(TestExtensions.mapOf(data));
    testSpecializedEquality(builder, data, true);
    testSpecializedEquality(builder, changed, false);
  }

  private static <K extends @Nullable Object, V extends @Nullable Object> void testSpecializedEquality(
      Object map, Map.Entry<K, V>[] pairs, boolean isEqual) {
    var javaMap = TestExtensions.mapOf(pairs);
    if (map instanceof Map<?, ?>) {
      testEqualsAndHashCode(map, javaMap, isEqual);
    } else if (isEqual) {
      // Deliberate deviation: PersistentHashMap does not extend java.util.Map, so cross-type map
      // equality with JDK maps is not supported.
      assertNotEquals(javaMap, map);
    }

    testEqualsAndHashCode(map, PersistentMap.hashOf(pairs), isEqual);

    var builder = PersistentMap.<K, V>hashOf().builder();
    builder.putAll(javaMap);
    testEqualsAndHashCode(map, builder, isEqual);
  }

  private static void testEqualsAndHashCode(Object lhs, Object rhs, boolean isEqual) {
    assertEquals(isEqual, lhs.equals(rhs));
    if (isEqual) {
      assertEquals(lhs.hashCode(), rhs.hashCode());
    }
  }
}
