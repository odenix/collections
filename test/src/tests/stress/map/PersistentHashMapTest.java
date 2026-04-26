/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.stress.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentMap;

import tests.NForAlgorithmComplexity;
import tests.ObjectWrapper;
import tests.TestUtils;
import tests.stress.ExecutionTimeMeasuringTest;
import tests.stress.WrapperGenerator;

class PersistentHashMapTest extends ExecutionTimeMeasuringTest {
  @Test
  void isEmptyTests() {
    var map = PersistentMap.<Integer, String>hashOf();

    assertTrue(map.isEmpty());
    assertFalse(map.put(0, "last").isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    var values = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      map = map.put(index, values.get(index));
      assertFalse(map.isEmpty());
    }
    for (var index = 0; index < elementsToAdd - 1; index++) {
      map = map.remove(index);
      assertFalse(map.isEmpty());
    }
    map = map.remove(elementsToAdd - 1);
    assertTrue(map.isEmpty());
  }

  @Test
  void sizeTests() {
    var map = PersistentMap.<Integer, Integer>hashOf();

    assertTrue(map.size() == 0);
    assertEquals(1, map.put(1, 1).size());

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    for (var index = 0; index < elementsToAdd; index++) {
      map = map.put(index, index);
      assertEquals(index + 1, map.size());
      map = map.put(index, index);
      assertEquals(index + 1, map.size());
      map = map.put(index, 7);
      assertEquals(index + 1, map.size());
    }
    for (var index = 0; index < elementsToAdd; index++) {
      map = map.remove(index);
      assertEquals(elementsToAdd - index - 1, map.size());
      map = map.remove(index);
      assertEquals(elementsToAdd - index - 1, map.size());
    }
  }

  @Test
  void keysValuesEntriesTests() {
    var map = PersistentMap.<Integer, Integer>hashOf();
    assertTrue(map.keys().isEmpty());
    assertTrue(map.values().isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    var set = new HashSet<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      var key = RandomHolder.RANDOM.nextInt();
      set.add(key);
      map = map.put(key, key);
      testProperties(set, map);
    }

    for (var key : new HashSet<>(set)) {
      set.remove(key);
      map = map.remove(key);
      testProperties(set, map);
    }
  }

  @Test
  void removeTests() {
    var map = PersistentMap.<Integer, String>hashOf();
    assertTrue(map.put(0, "0").remove(0).isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    var values = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      map = map.put(index, values.get(index));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index, map.size());
      assertEquals(values.get(index), map.get(index));
      map = map.remove(index);
      assertNull(map.get(index));
    }
    assertTrue(map.isEmpty());
  }

  @Test
  void removeEntryTests() {
    var map = PersistentMap.<Integer, String>hashOf();
    assertTrue(map.put(0, "0").remove(0, "0").isEmpty());
    assertFalse(map.put(0, "0").remove(0, "x").isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    var values = TestUtils.distinctStringValues(elementsToAdd + 1);
    for (var index = 0; index < elementsToAdd; index++) {
      map = map.put(index, values.get(index));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index, map.size());
      assertEquals(values.get(index), map.get(index));
      map = map.remove(index, values.get(index + 1));
      assertEquals(values.get(index), map.get(index));
      map = map.remove(index, values.get(index));
      assertNull(map.get(index));
    }
    assertTrue(map.isEmpty());
  }

  @Test
  void getTests() {
    var map = PersistentMap.<Integer, String>hashOf();
    assertEquals("1", map.put(1, "1").get(1));

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    var values = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      map = map.put(index, values.get(index));
      for (var i = 0; i <= index; i++) {
        assertEquals(values.get(i), map.get(i));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = elementsToAdd - 1; i >= index; i--) {
        assertEquals(values.get(i), map.get(i));
      }
      map = map.remove(index);
    }
  }

  @Test
  void putTests() {
    var map = PersistentMap.<Integer, String>hashOf();
    assertEquals("2", map.put(1, "1").put(1, "2").get(1));

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    var values = TestUtils.distinctStringValues(2 * elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      map = map.put(index, values.get(2 * index));
      for (var i = 0; i <= index; i++) {
        var valueIndex = i + index;
        assertEquals(values.get(valueIndex), map.get(i));
        map = map.put(i, values.get(valueIndex + 1));
        assertEquals(values.get(valueIndex + 1), map.get(i));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = index; i < elementsToAdd; i++) {
        var valueIndex = elementsToAdd - index + i;
        assertEquals(values.get(valueIndex), map.get(i));
        map = map.put(i, values.get(valueIndex - 1));
        assertEquals(values.get(valueIndex - 1), map.get(i));
      }
      map = map.remove(index);
    }
    assertTrue(map.isEmpty());
  }

  @Test
  void collisionTests() {
    var map = PersistentMap.<ObjectWrapper<Integer>, Integer>hashOf();

    var oneWrapper = new ObjectWrapper<>(1, 1);
    var twoWrapper = new ObjectWrapper<>(2, 1);
    assertEquals(1, map.put(oneWrapper, 1).put(twoWrapper, 2).get(oneWrapper));
    assertEquals(2, map.put(oneWrapper, 1).put(twoWrapper, 2).get(twoWrapper));

    for (var removeEntryPredicate = 0; removeEntryPredicate < 2; removeEntryPredicate++) {
      var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
      var maxHashCode = elementsToAdd / 5;
      var keyGen = new WrapperGenerator<Integer>(maxHashCode);

      for (var index = 0; index < elementsToAdd; index++) {
        map = map.put(key(keyGen, index), Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, map.get(key(keyGen, index)));
        assertEquals(index + 1, map.size());

        map = map.put(key(keyGen, index), index);
        assertEquals(index + 1, map.size());

        var collisions = keyGen.wrappersByHashCode(key(keyGen, index).hashCode());
        assertTrue(collisions.contains(key(keyGen, index)));

        for (var key : collisions) {
          assertEquals(key.obj, map.get(key));
        }
      }
      for (var index = 0; index < elementsToAdd; index++) {
        var collisions = keyGen.wrappersByHashCode(key(keyGen, index).hashCode());
        assertTrue(collisions.contains(key(keyGen, index)));

        if (map.get(key(keyGen, index)) == null) {
          for (var key : collisions) {
            assertNull(map.get(key));
          }
        } else {
          for (var key : collisions) {
            assertEquals(key.obj, map.get(key));

            if (removeEntryPredicate == 1) {
              var sameMap = map.remove(key, Integer.MIN_VALUE);
              assertEquals(map.size(), sameMap.size());
              assertEquals(key.obj, sameMap.get(key));
              map = map.remove(key, key.obj);
            } else {
              var nonExistingKey = new ObjectWrapper<>(Integer.MIN_VALUE, key.hashCode());
              var sameMap = map.remove(nonExistingKey);
              assertEquals(map.size(), sameMap.size());
              assertEquals(key.obj, sameMap.get(key));
              map = map.remove(key);
            }
            assertNull(map.get(key));
          }
        }
      }
      assertTrue(map.isEmpty());
    }
  }

  @Test
  void randomOperationsTests() {
    var mutableMaps = new ArrayList<Map<ObjectWrapper<Integer>, Integer>>();
    var immutableMaps = new ArrayList<PersistentMap<ObjectWrapper<Integer>, Integer>>();
    for (var index = 0; index < 10; index++) {
      mutableMaps.add(new HashMap<>());
      immutableMaps.add(PersistentMap.hashOf());
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
      var immutableMap = immutableMaps.get(index);
      var shouldRemove = RandomHolder.RANDOM.nextDouble() < 0.3;
      var shouldOperateOnExistingKey =
          !mutableMap.isEmpty()
              && (shouldRemove ? RandomHolder.RANDOM.nextDouble() < 0.8 : RandomHolder.RANDOM.nextDouble() < 0.2);

      var key =
          shouldOperateOnExistingKey
              ? mutableMap.keySet().iterator().next()
              : new ObjectWrapper<>(
                  RandomHolder.RANDOM.nextInt(), hashCodes.get(RandomHolder.RANDOM.nextInt(hashCodes.size())));

      if (shouldRemove && RandomHolder.RANDOM.nextBoolean()) {
        mutableMap.remove(key);
        immutableMap = immutableMap.remove(key);
      } else if (shouldRemove) {
        var value = shouldOperateOnExistingKey && RandomHolder.RANDOM.nextDouble() < 0.8
            ? mutableMap.get(key)
            : RandomHolder.RANDOM.nextInt();
        mutableMap.remove(key, value);
        immutableMap = immutableMap.remove(key, value);
      } else {
        var value = RandomHolder.RANDOM.nextInt();
        mutableMap.put(key, value);
        immutableMap = immutableMap.put(key, value);
      }

      testAfterOperation(mutableMap, immutableMap, key);
      immutableMaps.set(index, immutableMap);
    }
  }

  static void testProperties(java.util.Set<Integer> expectedKeys, PersistentMap<Integer, Integer> actualMap) {
    var values = actualMap.values();
    var keys = actualMap.keys();
    var entries = actualMap.entries();

    assertEquals(expectedKeys.size(), values.size());
    assertEquals(expectedKeys.size(), keys.size());
    assertEquals(expectedKeys.size(), entries.size());
    assertTrue(keys.containsAll(expectedKeys));
    for (var value : values) {
      assertTrue(keys.contains(value));
    }

    for (var entry : entries) {
      assertEquals(entry.getKey(), entry.getValue());
      assertTrue(expectedKeys.contains(entry.getKey()));
    }
  }

  static ObjectWrapper<Integer> key(WrapperGenerator<Integer> keyGen, int key) {
    return keyGen.wrapper(key);
  }

  static <K, V> void testAfterOperation(
      Map<K, V> expected, PersistentMap<K, V> actual, K operationKey) {
    assertEquals(expected.size(), actual.size());
    assertEquals(expected.get(operationKey), actual.get(operationKey));
    assertEquals(expected.containsKey(operationKey), actual.containsKey(operationKey));
  }

  private static final class RandomHolder {
    private static final Random RANDOM = new Random(0);
  }
}
