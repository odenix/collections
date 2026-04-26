/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.stress.set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentSet;

import tests.NForAlgorithmComplexity;
import tests.ObjectWrapper;
import tests.TestUtils;
import tests.stress.ExecutionTimeMeasuringTest;
import tests.stress.WrapperGenerator;

class PersistentHashSetTest extends ExecutionTimeMeasuringTest {
  @Test
  void isEmptyTests() {
    var set = PersistentSet.<String>hashOf();

    assertTrue(set.isEmpty());
    assertFalse(set.add("last").isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;
    var elements = TestUtils.distinctStringValues(elementsToAdd);

    for (var index = 0; index < elementsToAdd; index++) {
      set = set.add(elements.get(index));
      assertFalse(set.isEmpty());
    }
    for (var index = 0; index < elementsToAdd - 1; index++) {
      set = set.remove(elements.get(index));
      assertFalse(set.isEmpty());
    }
    set = set.remove(elements.get(elementsToAdd - 1));
    assertTrue(set.isEmpty());
  }

  @Test
  void sizeTests() {
    var set = PersistentSet.<Integer>hashOf();

    assertTrue(set.size() == 0);
    assertEquals(1, set.add(1).size());

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;

    for (var index = 0; index < elementsToAdd; index++) {
      set = set.add(index);
      assertEquals(index + 1, set.size());

      set = set.add(index);
      assertEquals(index + 1, set.size());
    }
    for (var index = 0; index < elementsToAdd; index++) {
      set = set.remove(index);
      assertEquals(elementsToAdd - index - 1, set.size());

      set = set.remove(index);
      assertEquals(elementsToAdd - index - 1, set.size());
    }
  }

  @Test
  void storedElementsTests() {
    var set = PersistentSet.<Integer>hashOf();
    assertTrue(set.isEmpty());

    var mutableSet = new HashSet<Integer>();

    var elementsToAdd = NForAlgorithmComplexity.O_NN;

    for (var index = 0; index < elementsToAdd; index++) {
      var element = RandomHolder.RANDOM.nextInt();
      mutableSet.add(element);
      set = set.add(element);

      assertEquals(mutableSet, TestExtensions.toSet(set));
    }

    for (var element : new ArrayList<>(mutableSet)) {
      mutableSet.remove(element);
      set = set.remove(element);

      assertEquals(mutableSet, TestExtensions.toSet(set));
    }

    assertTrue(set.isEmpty());
  }

  @Test
  void removeTests() {
    var set = PersistentSet.<Integer>hashOf();
    assertTrue(set.add(0).remove(0).isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;

    for (var index = 0; index < elementsToAdd; index++) {
      set = set.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index, set.size());

      assertTrue(set.contains(index));
      set = set.remove(index);
      assertFalse(set.contains(index));
    }
  }

  @Test
  void containsTests() {
    var set = PersistentSet.<String>hashOf();

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;

    var elements = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      set = set.add(elements.get(index));

      for (var i = 0; i <= index; i++) {
        assertTrue(set.contains(elements.get(i)));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = elementsToAdd - 1; i >= index; i--) {
        assertTrue(set.contains(elements.get(i)));
      }

      set = set.remove(elements.get(index));
    }
  }

  @Test
  void addTests() {
    var set = PersistentSet.<Integer>hashOf();

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;

    for (var index = 0; index < elementsToAdd; index++) {
      set = set.add(index * 2);

      for (var i = index; i >= 0; i--) {
        var element = i + index;

        assertTrue(set.contains(element));
        set = set.remove(element);
        assertFalse(set.contains(element));
        assertFalse(set.contains(element + 1));
        set = set.add(element + 1);
        assertTrue(set.contains(element + 1));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = index; i < elementsToAdd; i++) {
        var element = elementsToAdd - index + i;

        assertTrue(set.contains(element));
        set = set.remove(element);
        assertFalse(set.contains(element));
        assertFalse(set.contains(element - 1));
        set = set.add(element - 1);
        assertTrue(set.contains(element - 1));
      }

      set = set.remove(elementsToAdd - 1);
    }
    assertTrue(set.isEmpty());
  }

  @Test
  void collisionTests() {
    var set = PersistentSet.<ObjectWrapper<Integer>>hashOf();

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;

    var numberOfDistinctHashCodes = elementsToAdd / 5;
    var eGen = new WrapperGenerator<Integer>(numberOfDistinctHashCodes);

    for (var index = 0; index < elementsToAdd; index++) {
      var wrapper = wrapper(eGen, index);
      set = set.add(wrapper);
      assertTrue(set.contains(wrapper));
      assertEquals(index + 1, set.size());

      set = set.add(wrapper);
      assertEquals(index + 1, set.size());

      var collisions = eGen.wrappersByHashCode(wrapper.hashCode());
      assertTrue(collisions.contains(wrapper));

      for (var collision : collisions) {
        assertTrue(set.contains(collision));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      var wrapper = wrapper(eGen, index);
      var collisions = eGen.wrappersByHashCode(wrapper.hashCode());
      assertTrue(collisions.contains(wrapper));

      if (!set.contains(wrapper)) {
        for (var collision : collisions) {
          assertFalse(set.contains(collision));
        }
      } else {
        for (var collision : collisions) {
          assertTrue(set.contains(collision));

          var nonExistingElement = new ObjectWrapper<>(~collision.obj, collision.hashCode());
          var sameSet = set.remove(nonExistingElement);
          assertEquals(set.size(), sameSet.size());
          assertTrue(sameSet.contains(collision));
          set = set.remove(collision);
          assertFalse(set.contains(collision));
        }
      }
    }
    assertTrue(set.isEmpty());
  }

  @Test
  void randomOperationsTests() {
    var setGen = new ArrayList<List<PersistentSet<ObjectWrapper<Integer>>>>();
    setGen.add(new ArrayList<>());
    for (var index = 0; index < 20; index++) {
      setGen.getFirst().add(PersistentSet.hashOf());
    }
    var expected = new ArrayList<List<java.util.Set<ObjectWrapper<Integer>>>>();
    expected.add(new ArrayList<>());
    for (var index = 0; index < 20; index++) {
      expected.getFirst().add(java.util.Set.of());
    }

    for (var times = 0; times < 5; times++) {
      var sets = new ArrayList<java.util.Set<ObjectWrapper<Integer>>>();
      for (var set : setGen.getLast()) {
        sets.add(new HashSet<>(TestExtensions.toSet(set)));
      }
      var persistentSets = new ArrayList<>(setGen.getLast());

      var operationCount = NForAlgorithmComplexity.O_NlogN;

      var numberOfDistinctHashCodes = operationCount / 2;
      var hashCodes = new ArrayList<Integer>(numberOfDistinctHashCodes);
      for (var index = 0; index < numberOfDistinctHashCodes; index++) {
        hashCodes.add(RandomHolder.RANDOM.nextInt());
      }

      for (var ignored = 0; ignored < operationCount; ignored++) {
        var index = RandomHolder.RANDOM.nextInt(sets.size());
        var set = sets.get(index);
        var persistentSet = persistentSets.get(index);

        var shouldRemove = RandomHolder.RANDOM.nextDouble() < 0.3;
        var shouldOperateOnExistingElement =
            !set.isEmpty()
                && (shouldRemove
                    ? RandomHolder.RANDOM.nextDouble() < 0.8
                    : RandomHolder.RANDOM.nextDouble() < 0.001);

        var element =
            shouldOperateOnExistingElement
                ? set.iterator().next()
                : new ObjectWrapper<>(
                    RandomHolder.RANDOM.nextInt(),
                    hashCodes.get(RandomHolder.RANDOM.nextInt(hashCodes.size())));

        if (shouldRemove) {
          set.remove(element);
          persistentSet = persistentSet.remove(element);
        } else {
          set.add(element);
          persistentSet = persistentSet.add(element);
        }

        testAfterOperation(set, persistentSet, element);
        persistentSets.set(index, persistentSet);
      }

      var builtSets = new ArrayList<PersistentSet<ObjectWrapper<Integer>>>();
      for (var set : persistentSets) {
        builtSets.add(set);
      }
      setGen.add(builtSets);
      expected.add(sets);

      var maxSize = 0;
      for (var set : persistentSets) {
        maxSize = Math.max(maxSize, set.size());
      }
      System.out.println("Largest persistent set size: " + maxSize);
    }

    for (var genIndex = 0; genIndex < setGen.size(); genIndex++) {
      var sets = setGen.get(genIndex);
      for (var setIndex = 0; setIndex < sets.size(); setIndex++) {
        var set = sets.get(setIndex);
        var expectedSet = expected.get(genIndex).get(setIndex);
        assertEquals(expectedSet, TestExtensions.toSet(set));
      }
    }
  }

  private static ObjectWrapper<Integer> wrapper(WrapperGenerator<Integer> eGen, int element) {
    return eGen.wrapper(element);
  }

  private static void testAfterOperation(
      java.util.Set<ObjectWrapper<Integer>> expected,
      PersistentSet<ObjectWrapper<Integer>> actual,
      ObjectWrapper<Integer> element) {
    assertEquals(expected.size(), actual.size());
    assertEquals(expected.contains(element), actual.contains(element));
  }

  private static final class RandomHolder {
    private static final Random RANDOM = new Random(0);
  }
}
