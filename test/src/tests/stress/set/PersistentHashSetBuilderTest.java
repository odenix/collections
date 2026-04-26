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
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentSet;

import tests.NForAlgorithmComplexity;
import tests.ObjectWrapper;
import tests.TestUtils;
import tests.stress.ExecutionTimeMeasuringTest;
import tests.stress.WrapperGenerator;

class PersistentHashSetBuilderTest extends ExecutionTimeMeasuringTest {
  @Test
  void isEmptyTests() {
    var builder = PersistentSet.<Integer>hashOf().builder();

    assertTrue(builder.isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;

    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
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
    var builder = PersistentSet.<Integer>hashOf().builder();

    assertTrue(builder.size() == 0);

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;

    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
      assertEquals(index + 1, builder.size());

      builder.add(index);
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
  void storedElementsTests() {
    var builder = PersistentSet.<Integer>hashOf().builder();
    assertTrue(builder.isEmpty());

    var mutableSet = new java.util.HashSet<Integer>();

    var elementsToAdd = NForAlgorithmComplexity.O_NN;

    for (var index = 0; index < elementsToAdd; index++) {
      var element = RandomHolder.RANDOM.nextInt();
      mutableSet.add(element);
      builder.add(element);

      assertEquals(mutableSet, builder);
    }

    for (var element : new ArrayList<>(mutableSet)) {
      mutableSet.remove(element);
      builder.remove(element);

      assertEquals(mutableSet, builder);
    }

    assertTrue(builder.isEmpty());
  }

  @Test
  void iteratorTests() {
    var builder = PersistentSet.<Integer>hashOf().builder();
    assertFalse(builder.iterator().hasNext());

    var mutableSet = new java.util.HashSet<Integer>();

    var elementsToAdd = NForAlgorithmComplexity.O_NN;

    for (var index = 0; index < elementsToAdd; index++) {
      var element = RandomHolder.RANDOM.nextInt();
      mutableSet.add(element);
      builder.add(element);
    }

    var iterator = builder.iterator();
    for (var element : new ArrayList<>(mutableSet)) {
      mutableSet.remove(element);

      var didRemove = false;
      for (var i = 0; i <= 1; i++) {
        while (!didRemove && iterator.hasNext()) {
          if (iterator.next().equals(element)) {
            iterator.remove();
            didRemove = true;
            break;
          }
        }
        if (!didRemove) {
          iterator = builder.iterator();
        }
      }
      assertTrue(didRemove);

      assertEquals(mutableSet.size(), builder.size());
      assertEquals(mutableSet, builder);
    }

    assertTrue(builder.isEmpty());
  }

  @Test
  void removeTests() {
    var builder = PersistentSet.<Integer>hashOf().builder();

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;

    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index, builder.size());

      assertTrue(builder.contains(index));
      builder.remove(index);
      assertFalse(builder.contains(index));
    }
  }

  @Test
  void containsTests() {
    var builder = PersistentSet.<String>hashOf().builder();

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;

    var elements = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(elements.get(index));

      for (var i = 0; i <= index; i++) {
        assertTrue(builder.contains(elements.get(i)));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = elementsToAdd - 1; i >= index; i--) {
        assertTrue(builder.contains(elements.get(i)));
      }

      builder.remove(elements.get(index));
    }
  }

  @Test
  void addTests() {
    var builder = PersistentSet.<Integer>hashOf().builder();

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;

    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index * 2);

      for (var i = index; i >= 0; i--) {
        var element = i + index;

        assertTrue(builder.contains(element));
        builder.remove(element);
        assertFalse(builder.contains(element));
        assertFalse(builder.contains(element + 1));
        builder.add(element + 1);
        assertTrue(builder.contains(element + 1));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = index; i < elementsToAdd; i++) {
        var element = elementsToAdd - index + i;

        assertTrue(builder.contains(element));
        builder.remove(element);
        assertFalse(builder.contains(element));
        assertFalse(builder.contains(element - 1));
        builder.add(element - 1);
        assertTrue(builder.contains(element - 1));
      }

      builder.remove(elementsToAdd - 1);
    }
    assertTrue(builder.isEmpty());
  }

  @Test
  void collisionTests() {
    var builder = PersistentSet.<ObjectWrapper<Integer>>hashOf().builder();

    var elementsToAdd = NForAlgorithmComplexity.O_NlogN;

    var numberOfDistinctHashCodes = elementsToAdd / 5;
    var eGen = new WrapperGenerator<Integer>(numberOfDistinctHashCodes);

    for (var index = 0; index < elementsToAdd; index++) {
      var wrapper = wrapper(eGen, index);
      builder.add(wrapper);
      assertTrue(builder.contains(wrapper));
      assertEquals(index + 1, builder.size());

      builder.add(wrapper);
      assertEquals(index + 1, builder.size());

      var collisions = eGen.wrappersByHashCode(wrapper.hashCode());
      assertTrue(collisions.contains(wrapper));

      for (var collision : collisions) {
        assertTrue(builder.contains(collision));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      var wrapper = wrapper(eGen, index);
      var collisions = eGen.wrappersByHashCode(wrapper.hashCode());
      assertTrue(collisions.contains(wrapper));

      if (!builder.contains(wrapper)) {
        for (var collision : collisions) {
          assertFalse(builder.contains(collision));
        }
      } else {
        for (var collision : collisions) {
          assertTrue(builder.contains(collision));

          var nonExistingElement = new ObjectWrapper<>(~collision.obj, collision.hashCode());
          assertFalse(builder.remove(nonExistingElement));
          assertTrue(builder.contains(collision));
          assertTrue(builder.remove(collision));
          assertFalse(builder.contains(collision));
        }
      }
    }
    assertTrue(builder.isEmpty());
  }

  @Test
  void randomOperationsTests() {
    var setGen = new ArrayList<List<PersistentSet<ObjectWrapper<Integer>>>>();
    setGen.add(List.of(
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf(),
        PersistentSet.hashOf()));
    var expected = new ArrayList<List<java.util.Set<ObjectWrapper<Integer>>>>();
    expected.add(List.of(
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of(),
        java.util.Set.of()));

    for (var times = 0; times < 5; times++) {
      var builders = new ArrayList<PersistentSet.Builder<ObjectWrapper<Integer>>>();
      for (var set : setGen.getLast()) {
        builders.add(set.builder());
      }
      var sets = new ArrayList<java.util.Set<ObjectWrapper<Integer>>>();
      for (var builder : builders) {
        sets.add(new java.util.HashSet<>(builder));
      }

      var operationCount = NForAlgorithmComplexity.O_NlogN;

      var numberOfDistinctHashCodes = operationCount / 2;
      var hashCodes = new ArrayList<Integer>(numberOfDistinctHashCodes);
      for (var index = 0; index < numberOfDistinctHashCodes; index++) {
        hashCodes.add(RandomHolder.RANDOM.nextInt());
      }

      for (var ignored = 0; ignored < operationCount; ignored++) {
        var index = RandomHolder.RANDOM.nextInt(sets.size());
        var set = sets.get(index);
        var builder = builders.get(index);

        var shouldRemove = RandomHolder.RANDOM.nextDouble() < 0.3;
        var shouldOperateOnExistingElement =
            !set.isEmpty() && (shouldRemove ? RandomHolder.RANDOM.nextDouble() < 0.8 : RandomHolder.RANDOM.nextDouble() < 0.001);

        var element =
            shouldOperateOnExistingElement
                ? set.iterator().next()
                : new ObjectWrapper<>(
                    RandomHolder.RANDOM.nextInt(),
                    hashCodes.get(RandomHolder.RANDOM.nextInt(hashCodes.size())));

        if (shouldRemove) {
          assertEquals(set.remove(element), builder.remove(element));
        } else {
          assertEquals(set.add(element), builder.add(element));
        }

        testAfterOperation(set, builder, element);
      }

      assertEquals(sets, builders);

      var builtSets = new ArrayList<PersistentSet<ObjectWrapper<Integer>>>();
      for (var builder : builders) {
        builtSets.add(builder.build());
      }
      setGen.add(builtSets);
      expected.add(sets);

      var maxSize = 0;
      for (var builder : builders) {
        maxSize = Math.max(maxSize, builder.size());
      }
      System.out.println("Largest persistent set builder size: " + maxSize);
    }

    for (var genIndex = 0; genIndex < setGen.size(); genIndex++) {
      var sets = setGen.get(genIndex);
      for (var setIndex = 0; setIndex < sets.size(); setIndex++) {
        var set = sets.get(setIndex);
        var expectedSet = expected.get(genIndex).get(setIndex);
        // Deliberate deviation: PersistentHashSet does not extend java.util.Set, so compare via a
        // Java set snapshot instead of cross-type equality.
        assertEquals(
            expectedSet,
            TestExtensions.toSet(set),
            "The persistent set of "
                + genIndex
                + " generation was modified.\nExpected: "
                + expectedSet
                + "\nActual: "
                + set);
      }
    }
  }

  private static ObjectWrapper<Integer> wrapper(WrapperGenerator<Integer> eGen, int element) {
    return eGen.wrapper(element);
  }

  private static void testAfterOperation(
      java.util.Set<ObjectWrapper<Integer>> expected,
      java.util.Set<ObjectWrapper<Integer>> actual,
      ObjectWrapper<Integer> element) {
    assertEquals(expected.size(), actual.size());
    assertEquals(expected.contains(element), actual.contains(element));
  }

  private static final class RandomHolder {
    private static final Random RANDOM = new Random(0);
  }
}
