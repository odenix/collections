/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.stress.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentList;

import tests.NForAlgorithmComplexity;
import tests.TestUtils;
import tests.stress.ExecutionTimeMeasuringTest;

class PersistentListBuilderTest extends ExecutionTimeMeasuringTest {
  @Test
  void isEmptyTests() {
    var builder = PersistentList.<String>of().builder();

    assertTrue(builder.isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_N;
    var elements = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(elements.get(index));
      assertFalse(builder.isEmpty());
    }
    for (var index = 0; index < elementsToAdd - 1; index++) {
      builder.remove(builder.size() - 1);
      assertFalse(builder.isEmpty());
    }
    builder.remove(builder.size() - 1);
    assertTrue(builder.isEmpty());
  }

  @Test
  void sizeTests() {
    var builder = PersistentList.<Integer>of().builder();

    assertTrue(builder.size() == 0);

    var elementsToAdd = NForAlgorithmComplexity.O_N;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
      assertEquals(index + 1, builder.size());
    }
    for (var index = 0; index < elementsToAdd; index++) {
      builder.remove(builder.size() - 1);
      assertEquals(elementsToAdd - index - 1, builder.size());
    }
  }

  @Test
  void firstTests() {
    var builder = PersistentList.<Integer>of().builder();

    assertNull(firstOrNull(builder));
    builder.add(0, 1);
    assertEquals(1, builder.get(0));
    builder.clear();
    builder.add(1);
    assertEquals(1, builder.get(0));
    builder.clear();

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(0, index);
      assertEquals(index, builder.get(0));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index - 1, builder.get(0));
      builder.remove(0);
    }
    assertNull(firstOrNull(builder));
  }

  @Test
  void lastTests() {
    var builder = PersistentList.<Integer>of().builder();

    assertNull(lastOrNull(builder));
    builder.add(0, 1);
    assertEquals(1, last(builder));
    builder.clear();
    builder.add(1);
    assertEquals(1, last(builder));
    builder.clear();

    var elementsToAdd = NForAlgorithmComplexity.O_N;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
      assertEquals(index, last(builder));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index - 1, last(builder));
      builder.remove(builder.size() - 1);
    }
    assertNull(lastOrNull(builder));
  }

  @Test
  void toListTest() {
    var builder = PersistentList.<Integer>of().builder();

    assertEquals(List.<Integer>of(), builder);
    assertEquals(List.of(1), PersistentList.<Integer>of().add(1).builder());

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var list = new ArrayList<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      list.add(index);
      builder.add(index);
      assertEquals(list, builder);
    }
  }

  @Test
  void addFirstTests() {
    var builder = PersistentList.<Integer>of().builder();

    assertNull(firstOrNull(builder));
    builder.add(0, 1);
    assertEquals(1, builder.get(0));
    assertEquals(1, last(builder));
    builder.clear();

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var allElements = new ArrayList<Integer>(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(elementsToAdd - index - 1);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(0, index);
      assertEquals(index, builder.get(0));
      assertEquals(0, last(builder));
      assertEquals(index + 1, builder.size());
      assertEquals(allElements.subList(elementsToAdd - builder.size(), elementsToAdd), builder);
    }
  }

  @Test
  void addLastTests() {
    var builder = PersistentList.<Integer>of().builder();

    builder.add(1);
    assertEquals(1, builder.get(0));
    builder.clear();

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var allElements = range(0, elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
      assertEquals(0, builder.get(0));
      assertEquals(index, builder.get(index));
      assertEquals(index + 1, builder.size());
      assertEquals(allElements.subList(0, builder.size()), builder);
    }
  }

  @Test
  void removeFirstTests() {
    var builder = PersistentList.<Integer>of().builder();

    assertThrows(IndexOutOfBoundsException.class, () -> builder.remove(0));
    builder.add(1);
    builder.remove(0);
    assertTrue(builder.isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var allElements = range(0, elementsToAdd);
    builder.addAll(allElements);
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - 1, last(builder));
      assertEquals(index, builder.get(0));
      assertEquals(elementsToAdd - index, builder.size());
      assertEquals(allElements.subList(index, elementsToAdd), builder);
      builder.remove(0);
    }
  }

  @Test
  void removeLastTests() {
    var builder = PersistentList.<Integer>of().builder();

    assertThrows(IndexOutOfBoundsException.class, () -> builder.remove(builder.size() - 1));
    builder.add(1);
    builder.remove(0);
    assertTrue(builder.isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var allElements = new ArrayList<Integer>(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(elementsToAdd - index - 1);
      builder.add(0, index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(index, last(builder));
      assertEquals(elementsToAdd - 1, builder.get(0));
      assertEquals(elementsToAdd - index, builder.size());
      assertEquals(allElements.subList(0, builder.size()), builder);
      builder.remove(builder.size() - 1);
    }
  }

  @Test
  void getTests() {
    var builder = PersistentList.<Integer>of().builder();

    assertThrows(IndexOutOfBoundsException.class, () -> builder.get(0));
    builder.add(1);
    assertEquals(1, builder.get(0));
    builder.clear();

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
      for (var i = 0; i <= index; i++) {
        assertEquals(i, builder.get(i));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = index; i < elementsToAdd; i++) {
        assertEquals(i, builder.get(i - index));
      }
      builder.remove(0);
    }
  }

  @Test
  void setTests() {
    var builder = PersistentList.<Integer>of().builder();

    assertThrows(IndexOutOfBoundsException.class, () -> builder.set(0, 0));
    builder.add(1);
    assertEquals(1, builder.set(0, 2));
    assertEquals(2, builder.get(0));
    builder.clear();

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index * 2);
      for (var i = 0; i <= index; i++) {
        assertEquals(i + index, builder.get(i));
        builder.set(i, i + index + 1);
        assertEquals(i + index + 1, builder.get(i));
      }
    }
  }

  @Test
  void subListTests() {
    var builder = PersistentList.<Integer>of().builder();
    builder.addAll(range(0, NForAlgorithmComplexity.O_NN));
    for (var index = 0; index < 100; index++) {
      var from = RandomHolder.RANDOM.nextInt(builder.size());
      var to = from + RandomHolder.RANDOM.nextInt(builder.size() - from + 1);
      assertEquals(range(from, to), builder.subList(from, to));
    }
  }

  @Test
  void iterationTests() {
    var builder = PersistentList.<Integer>of().builder();
    builder.addAll(range(0, NForAlgorithmComplexity.O_NN));
    var index = 0;
    for (var element : builder) {
      assertEquals(index++, element);
    }
    assertEquals(builder.size(), index);
  }

  @Test
  void iteratorSetTests() {
    var builder = PersistentList.<Integer>of().builder();
    builder.addAll(range(0, NForAlgorithmComplexity.O_NNlogN));
    var iterator = builder.listIterator();
    while (iterator.hasNext()) {
      var value = iterator.next();
      iterator.set(value + 1);
    }
    for (var index = 0; index < builder.size(); index++) {
      assertEquals(index + 1, builder.get(index));
    }
  }

  @Test
  void iteratorAddTests() {
    var builder = PersistentList.<Integer>of().builder();
    var expected = new ArrayList<Integer>();
    var iterator = builder.listIterator();
    for (var index = 0; index < NForAlgorithmComplexity.O_NNlogN; index++) {
      iterator.add(index);
      expected.add(index);
    }
    assertEquals(expected, builder);
  }

  @Test
  void iteratorRemoveTests() {
    var builder = PersistentList.<Integer>of().builder();
    builder.addAll(range(0, NForAlgorithmComplexity.O_NNlogN));
    var expected = new ArrayList<>(builder);
    var iterator = builder.listIterator();
    while (iterator.hasNext()) {
      var value = iterator.next();
      if (value % 2 == 0) {
        iterator.remove();
      }
    }
    expected.removeIf(value -> value % 2 == 0);
    assertEquals(expected, builder);
  }

  @Test
  void addAllAtIndexTests() {
    var builder = PersistentList.<Integer>of().builder();
    builder.addAll(range(0, 1000));
    var expected = new ArrayList<>(builder);
    var toAdd = range(1000, 1100);
    builder.addAll(500, toAdd);
    expected.addAll(500, toAdd);
    assertEquals(expected, builder);
  }

  @Test
  void removeAllTests() {
    var builder = PersistentList.<Integer>of().builder();
    builder.addAll(range(0, 1000));
    var elements = range(0, 500);
    var expected = new ArrayList<>(builder);
    expected.removeAll(elements);
    builder.removeAll(elements);
    assertEquals(expected, builder);

    var hashSet = new HashSet<>(range(500, 750));
    expected.removeIf(hashSet::contains);
    builder.removeIf(hashSet::contains);
    assertEquals(expected, builder);
  }

  @Test
  void randomOperationsTests() {
    var lists = new ArrayList<List<Integer>>();
    var builders = new ArrayList<PersistentList.Builder<Integer>>();
    for (var index = 0; index < 20; index++) {
      lists.add(new ArrayList<>());
      builders.add(PersistentList.<Integer>of().builder());
    }

    var operationCount = NForAlgorithmComplexity.O_NlogN;
    for (var ignored = 0; ignored < operationCount; ignored++) {
      var index = RandomHolder.RANDOM.nextInt(lists.size());
      var list = lists.get(index);
      var builder = builders.get(index);
      var operationType = RandomHolder.RANDOM.nextDouble();
      var operationIndex = list.size() > 1 ? RandomHolder.RANDOM.nextInt(list.size()) : 0;

      if (!list.isEmpty() && operationType < 0.15) {
        list.remove(operationIndex);
        builder.remove(operationIndex);
      } else if (!list.isEmpty() && operationType < 0.3) {
        var value = RandomHolder.RANDOM.nextInt();
        list.set(operationIndex, value);
        builder.set(operationIndex, value);
      } else {
        var value = RandomHolder.RANDOM.nextInt();
        list.add(operationIndex, value);
        builder.add(operationIndex, value);
      }

      assertEquals(list, builder);
    }
  }

  private static <E> E firstOrNull(List<E> list) {
    return list.isEmpty() ? null : list.get(0);
  }

  private static <E> E lastOrNull(List<E> list) {
    return list.isEmpty() ? null : list.get(list.size() - 1);
  }

  private static <E> E last(List<E> list) {
    return list.get(list.size() - 1);
  }

  private static ArrayList<Integer> range(int startInclusive, int endExclusive) {
    var result = new ArrayList<Integer>();
    for (var index = startInclusive; index < endExclusive; index++) {
      result.add(index);
    }
    return result;
  }

  private static final class RandomHolder {
    private static final Random RANDOM = new Random(0);
  }
}
