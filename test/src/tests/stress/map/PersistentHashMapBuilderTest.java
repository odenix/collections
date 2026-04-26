/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.stress.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentMap;

import tests.NForAlgorithmComplexity;
import tests.ObjectWrapper;
import tests.TestUtils;
import tests.stress.ExecutionTimeMeasuringTest;
import tests.stress.WrapperGenerator;

class PersistentHashMapBuilderTest extends ExecutionTimeMeasuringTest {
  @Test
  void isEmptyTests() {
    var builder = PersistentMap.<Integer, String>hashOf().builder();
    assertTrue(builder.isEmpty());
    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    var values = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      builder.put(index, values.get(index));
      assertFalse(builder.isEmpty());
    }
    for (var index = 0; index < elementsToAdd - 1; index++) {
      builder.remove(index);
      assertFalse(builder.isEmpty());
    }
    builder.remove(elementsToAdd - 1);
    assertTrue(builder.isEmpty());
  }

  @Test
  void sizeTests() {
    var builder = PersistentMap.<Integer, Integer>hashOf().builder();
    assertTrue(builder.size() == 0);
    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.put(index, index);
      assertEquals(index + 1, builder.size());
      builder.put(index, index);
      assertEquals(index + 1, builder.size());
      builder.put(index, 7);
      assertEquals(index + 1, builder.size());
    }
    for (var index = 0; index < elementsToAdd; index++) {
      builder.remove(index);
      assertEquals(elementsToAdd - index - 1, builder.size());
      builder.remove(index);
      assertEquals(elementsToAdd - index - 1, builder.size());
    }
  }

  @Test
  void keysValuesEntriesTests() {
    var builder = PersistentMap.<Integer, Integer>hashOf().builder();
    assertTrue(builder.keySet().isEmpty());
    assertTrue(builder.values().isEmpty());
    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    var set = new HashSet<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      var key = RandomHolder.RANDOM.nextInt();
      set.add(key);
      builder.put(key, key);
      testProperties(set, builder);
    }
    for (var key : new HashSet<>(set)) {
      set.remove(key);
      builder.remove(key);
      testProperties(set, builder);
    }
  }

  @Test
  void keysIteratorTests() {
    removeThroughIterator(PersistentMap.<Integer, Integer>hashOf().builder().keySet().iterator());
  }

  @Test
  void valuesIteratorTests() {
    var builder = fullBuilder();
    var iterator = builder.values().iterator();
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }
    assertTrue(builder.isEmpty());
  }

  @Test
  void entriesIteratorTests() {
    var builder = fullBuilder();
    var iterator = builder.entrySet().iterator();
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }
    assertTrue(builder.isEmpty());
  }

  @Test
  void removeTests() {
    var builder = PersistentMap.<Integer, String>hashOf().builder();
    builder.put(0, "0");
    builder.remove(0);
    assertTrue(builder.isEmpty());
    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    var values = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      builder.put(index, values.get(index));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index, builder.size());
      assertEquals(values.get(index), builder.get(index));
      builder.remove(index);
      assertFalse(builder.containsKey(index));
    }
    assertTrue(builder.isEmpty());
  }

  @Test
  void removeBuildTests() {
    var map = PersistentMap.<Integer, Integer>hashOf();
    for (var index = 0; index < NForAlgorithmComplexity.O_NNlogN; index++) {
      map = map.put(index, index);
    }
    var builder = map.builder();
    for (var index = 0; index < NForAlgorithmComplexity.O_NNlogN; index += 2) {
      builder.remove(index);
    }
    var built = builder.build();
    for (var index = 0; index < NForAlgorithmComplexity.O_NNlogN; index++) {
      assertEquals(index % 2 == 1, built.containsKey(index));
    }
  }

  @Test
  void removeEntryTests() {
    var builder = PersistentMap.<Integer, String>hashOf().builder();
    builder.put(0, "0");
    assertTrue(builder.remove(0, "0"));
    builder.put(0, "0");
    assertFalse(builder.remove(0, "x"));
  }

  @Test
  void getTests() {
    var builder = PersistentMap.<Integer, String>hashOf().builder();
    builder.put(1, "1");
    assertEquals("1", builder.get(1));
    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    var values = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      builder.put(index, values.get(index));
      for (var i = 0; i <= index; i++) {
        assertEquals(values.get(i), builder.get(i));
      }
    }
  }

  @Test
  void putTests() {
    var builder = PersistentMap.<Integer, String>hashOf().builder();
    builder.put(1, "1");
    builder.put(1, "2");
    assertEquals("2", builder.get(1));
  }

  @Test
  void collisionTests() {
    var builder = PersistentMap.<ObjectWrapper<Integer>, Integer>hashOf().builder();
    var oneWrapper = new ObjectWrapper<>(1, 1);
    var twoWrapper = new ObjectWrapper<>(2, 1);
    builder.put(oneWrapper, 1);
    builder.put(twoWrapper, 2);
    assertEquals(1, builder.get(oneWrapper));
    assertEquals(2, builder.get(twoWrapper));

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    var maxHashCode = elementsToAdd / 5;
    var keyGen = new WrapperGenerator<Integer>(maxHashCode);
    for (var index = 0; index < elementsToAdd; index++) {
      builder.put(keyGen.wrapper(index), index);
      assertEquals(index, builder.get(keyGen.wrapper(index)));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(index, builder.remove(keyGen.wrapper(index)));
    }
  }

  @Test
  void randomOperationsTests() {
    var mutableMaps = new ArrayList<Map<ObjectWrapper<Integer>, Integer>>();
    var builders = new ArrayList<PersistentMap.Builder<ObjectWrapper<Integer>, Integer>>();
    for (var index = 0; index < 10; index++) {
      mutableMaps.add(new HashMap<>());
      builders.add(PersistentMap.<ObjectWrapper<Integer>, Integer>hashOf().builder());
    }
    var operationCount = NForAlgorithmComplexity.O_NlogN;
    var numberOfDistinctHashCodes = operationCount / 3;
    var hashCodes = new ArrayList<Integer>(numberOfDistinctHashCodes);
    for (var index = 0; index < numberOfDistinctHashCodes; index++) {
      hashCodes.add(RandomHolder.RANDOM.nextInt());
    }
    for (var ignored = 0; ignored < operationCount; ignored++) {
      var index = RandomHolder.RANDOM.nextInt(mutableMaps.size());
      var mutableMap = mutableMaps.get(index);
      var builder = builders.get(index);
      var shouldRemove = RandomHolder.RANDOM.nextDouble() < 0.3;
      var key = !mutableMap.isEmpty() && RandomHolder.RANDOM.nextDouble() < 0.5
          ? mutableMap.keySet().iterator().next()
          : new ObjectWrapper<>(RandomHolder.RANDOM.nextInt(), hashCodes.get(RandomHolder.RANDOM.nextInt(hashCodes.size())));
      if (shouldRemove) {
        mutableMap.remove(key);
        builder.remove(key);
      } else {
        var value = RandomHolder.RANDOM.nextInt();
        mutableMap.put(key, value);
        builder.put(key, value);
      }
      assertEquals(mutableMap, builder);
    }
  }

  private static PersistentMap.Builder<Integer, Integer> fullBuilder() {
    var builder = PersistentMap.<Integer, Integer>hashOf().builder();
    for (var index = 0; index < NForAlgorithmComplexity.O_NNlogN; index++) {
      builder.put(index, index);
    }
    return builder;
  }

  private static void removeThroughIterator(Iterator<?> emptyIterator) {
    var builder = fullBuilder();
    var iterator = builder.keySet().iterator();
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }
    assertTrue(builder.isEmpty());
    assertFalse(emptyIterator.hasNext());
  }

  private static void testProperties(
      Set<Integer> expectedKeys, PersistentMap.Builder<Integer, Integer> actualBuilder) {
    var values = actualBuilder.values();
    var keys = actualBuilder.keySet();
    var entries = actualBuilder.entrySet();

    assertEquals(expectedKeys.size(), values.size());
    assertEquals(expectedKeys.size(), keys.size());
    assertEquals(expectedKeys.size(), entries.size());
    assertTrue(keys.containsAll(expectedKeys));
    assertTrue(keys.containsAll(values));

    for (var entry : entries) {
      assertEquals(entry.getKey(), entry.getValue());
      assertTrue(expectedKeys.contains(entry.getKey()));
    }
  }

  private static final class RandomHolder {
    private static final Random RANDOM = new Random(0);
  }
}
