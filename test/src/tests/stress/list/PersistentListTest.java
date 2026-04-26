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

class PersistentListTest extends ExecutionTimeMeasuringTest {
  @Test
  void isEmptyTests() {
    var vector = PersistentList.<String>of();

    assertTrue(vector.isEmpty());
    assertFalse(vector.add("last").isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_N;
    var elements = TestUtils.distinctStringValues(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(elements.get(index));
      assertFalse(vector.isEmpty());
    }
    for (var index = 0; index < elementsToAdd - 1; index++) {
      vector = vector.removeAt(vector.size() - 1);
      assertFalse(vector.isEmpty());
    }
    vector = vector.removeAt(vector.size() - 1);
    assertTrue(vector.isEmpty());
  }

  @Test
  void sizeTests() {
    var vector = PersistentList.<Integer>of();

    assertTrue(vector.size() == 0);
    assertEquals(1, vector.add(1).size());

    var elementsToAdd = NForAlgorithmComplexity.O_N;
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(index);
      assertEquals(index + 1, vector.size());
    }
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.removeAt(vector.size() - 1);
      assertEquals(elementsToAdd - index - 1, vector.size());
    }
  }

  @Test
  void firstTests() {
    var vector = PersistentList.<Integer>of();

    assertNull(firstOrNull(vector));
    assertEquals(1, vector.add(0, 1).get(0));
    assertEquals(1, vector.add(1).get(0));

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(0, index);
      assertEquals(index, vector.get(0));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index - 1, vector.get(0));
      vector = vector.removeAt(0);
    }
    assertNull(firstOrNull(vector));
  }

  @Test
  void lastTests() {
    var vector = PersistentList.<Integer>of();

    assertNull(lastOrNull(vector));
    assertEquals(1, last(vector.add(0, 1)));
    assertEquals(1, last(vector.add(1)));

    var elementsToAdd = NForAlgorithmComplexity.O_N;
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(index);
      assertEquals(index, last(vector));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index - 1, last(vector));
      vector = vector.removeAt(vector.size() - 1);
    }
    assertNull(lastOrNull(vector));
  }

  @Test
  void toListTest() {
    var vector = PersistentList.<Integer>of();

    assertEquals(List.<Integer>of(), toList(vector));
    assertEquals(List.of(1), toList(vector.add(1)));

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var list = new ArrayList<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      list.add(index);
      vector = vector.add(index);
      assertEquals(list, toList(vector));
    }
  }

  @Test
  void addFirstTests() {
    var vector = PersistentList.<Integer>of();

    assertNull(firstOrNull(vector));
    assertEquals(1, vector.add(0, 1).get(0));
    assertEquals(1, last(vector.add(0, 1)));

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var allElements = new ArrayList<Integer>(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(elementsToAdd - index - 1);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(0, index);

      assertEquals(index, vector.get(0));
      assertEquals(0, last(vector));
      assertEquals(index + 1, vector.size());

      var expectedContent = allElements.subList(elementsToAdd - vector.size(), elementsToAdd);
      assertEquals(expectedContent, toList(vector));
    }
  }

  @Test
  void addLastTests() {
    var vector = PersistentList.<Integer>of();

    assertEquals(1, vector.add(1).get(0));

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var allElements = new ArrayList<Integer>(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(index);

      assertEquals(0, vector.get(0));
      assertEquals(index, vector.get(index));
      assertEquals(index + 1, vector.size());

      assertEquals(allElements.subList(0, vector.size()), toList(vector));
    }
  }

  @Test
  void removeFirstTests() {
    var vector = PersistentList.<Integer>of();

    assertThrows(IndexOutOfBoundsException.class, () -> PersistentList.<Integer>of().removeAt(0));
    assertTrue(vector.add(1).removeAt(0).isEmpty());
    assertTrue(vector.add(0, 1).removeAt(0).isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var allElements = new ArrayList<Integer>(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(index);
      vector = vector.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - 1, last(vector));
      assertEquals(index, vector.get(0));
      assertEquals(elementsToAdd - index, vector.size());
      assertEquals(allElements.subList(index, elementsToAdd), toList(vector));

      vector = vector.removeAt(0);
    }
  }

  @Test
  void removeLastTests() {
    var vector = PersistentList.<Integer>of();

    assertThrows(IndexOutOfBoundsException.class, () -> PersistentList.<Integer>of().removeAt(-1));
    assertTrue(vector.add(1).removeAt(0).isEmpty());
    assertTrue(vector.add(0, 1).removeAt(0).isEmpty());

    var elementsToAdd = NForAlgorithmComplexity.O_NN;
    var allElements = new ArrayList<Integer>(elementsToAdd);
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(elementsToAdd - index - 1);
      vector = vector.add(0, index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(index, last(vector));
      assertEquals(elementsToAdd - 1, vector.get(0));
      assertEquals(elementsToAdd - index, vector.size());
      assertEquals(allElements.subList(0, vector.size()), toList(vector));

      vector = vector.removeAt(vector.size() - 1);
    }

    var linear = NForAlgorithmComplexity.O_N;
    for (var index = 0; index < linear; index++) {
      vector = vector.add(index);
    }
    for (var index = 0; index < linear; index++) {
      assertEquals(linear - 1 - index, last(vector));
      assertEquals(0, vector.get(0));
      assertEquals(linear - index, vector.size());

      vector = vector.removeAt(vector.size() - 1);
    }
  }

  @Test
  void getTests() {
    var vector = PersistentList.<Integer>of();

    assertThrows(IndexOutOfBoundsException.class, () -> PersistentList.<Integer>of().get(0));
    assertEquals(1, vector.add(1).get(0));

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(index);

      for (var i = 0; i <= index; i++) {
        assertEquals(i, vector.get(i));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = index; i < elementsToAdd; i++) {
        assertEquals(i, vector.get(i - index));
      }

      vector = vector.removeAt(0);
    }
  }

  @Test
  void setTests() {
    var vector = PersistentList.<Integer>of();

    assertThrows(IndexOutOfBoundsException.class, () -> PersistentList.<Integer>of().set(0, 0));
    assertEquals(2, vector.add(1).set(0, 2).get(0));

    var elementsToAdd = NForAlgorithmComplexity.O_NNlogN;
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(index * 2);

      for (var i = 0; i <= index; i++) {
        assertEquals(i + index, vector.get(i));
        vector = vector.set(i, i + index + 1);
        assertEquals(i + index + 1, vector.get(i));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = 0; i < elementsToAdd - index; i++) {
        var expected = elementsToAdd + i;

        assertEquals(expected, vector.get(i));
        vector = vector.set(i, expected - 1);
        assertEquals(expected - 1, vector.get(i));
      }

      vector = vector.removeAt(0);
    }
  }

  @Test
  void addAllAtIndexTests() {
    var maxBufferSize = 32;
    for (var initialSize : List.of(0, 1, 10, 31, 32, 33, 64, 65, 100, 1024, 1056, 1057, 10000)) {
      if (initialSize > NForAlgorithmComplexity.O_NN) {
        continue;
      }

      var initialElements = range(0, initialSize);
      var list = PersistentList.<Integer>of().addAll(initialElements);
      var addIndexes = new ArrayList<Integer>();
      addIndexes.add(initialSize);
      if (initialSize > 0) {
        addIndexes.add(0);
        addIndexes.add(RandomHolder.RANDOM.nextInt(initialSize));
      }
      if (initialSize > maxBufferSize) {
        var rootSize = (initialSize - 1) & ~(maxBufferSize - 1);
        var tailSize = initialSize - rootSize;
        addIndexes.add(RandomHolder.RANDOM.nextInt(maxBufferSize));
        addIndexes.add(rootSize + RandomHolder.RANDOM.nextInt(tailSize));
        addIndexes.add(rootSize - RandomHolder.RANDOM.nextInt(maxBufferSize));
        addIndexes.add(rootSize);
      }

      var addSize = RandomHolder.RANDOM.nextInt(maxBufferSize * 2);
      for (var index : addIndexes) {
        for (var size = addSize; size <= addSize + maxBufferSize; size++) {
          var elementsToAdd = range(initialSize, initialSize + size);
          var result = list.addAll(index, elementsToAdd);

          var expected = new ArrayList<>(initialElements);
          expected.addAll(index, elementsToAdd);
          assertEquals(expected, toList(result));
        }
      }
    }
  }

  @Test
  void removeAllTests() {
    var maxBufferSize = 32;
    for (var initialSize : List.of(0, 1, 10, 31, 32, 33, 64, 65, 100, 1024, 1056, 1057, 10000)) {
      if (initialSize > NForAlgorithmComplexity.O_NN) {
        continue;
      }

      var initialElements = range(0, initialSize);
      var list = PersistentList.<Integer>of().addAll(initialElements);
      var removeElements = new ArrayList<List<Integer>>();
      removeElements.add(initialElements);
      if (initialSize > 0) {
        removeElements.add(List.of());
        removeElements.add(List.of(RandomHolder.RANDOM.nextInt(initialSize)));
        removeElements.add(randomIndexes(maxBufferSize, initialSize));
        removeElements.add(randomIndexes(initialSize / 2, initialSize));
        removeElements.add(randomIndexes(initialSize, initialSize));
        removeElements.add(range(0, initialSize / 2));
        removeElements.add(range(initialSize - initialSize / 2 + 1, initialSize + 1));
      }

      for (var elements : removeElements) {
        var expected = new ArrayList<>(initialElements);
        expected.removeAll(elements);

        var result = list.removeAll(elements);
        var hashSet = new HashSet<>(elements);
        var resultPredicate = list.removeAll(hashSet::contains);

        assertEquals(expected, toList(result));
        assertEquals(expected, toList(resultPredicate));
      }
    }
  }

  @Test
  void randomOperationsTests() {
    var lists = new ArrayList<List<Integer>>();
    var vectors = new ArrayList<PersistentList<Integer>>();
    for (var index = 0; index < 20; index++) {
      lists.add(new ArrayList<>());
      vectors.add(PersistentList.of());
    }

    var operationCount = NForAlgorithmComplexity.O_NlogN;
    for (var ignored = 0; ignored < operationCount; ignored++) {
      var index = RandomHolder.RANDOM.nextInt(lists.size());
      var list = lists.get(index);
      var vector = vectors.get(index);

      var operationType = RandomHolder.RANDOM.nextDouble();
      var operationIndex = list.size() > 1 ? RandomHolder.RANDOM.nextInt(list.size()) : 0;

      var shouldRemove = operationType < 0.15;
      var shouldSet = operationType > 0.15 && operationType < 0.3;

      PersistentList<Integer> newVector;
      if (!list.isEmpty() && shouldRemove) {
        list.remove(operationIndex);
        newVector = vector.removeAt(operationIndex);
      } else if (!list.isEmpty() && shouldSet) {
        var value = RandomHolder.RANDOM.nextInt();
        list.set(operationIndex, value);
        newVector = vector.set(operationIndex, value);
      } else {
        var value = RandomHolder.RANDOM.nextInt();
        list.add(operationIndex, value);
        newVector = vector.add(operationIndex, value);
      }

      testAfterOperation(list, newVector, operationIndex);
      vectors.set(index, newVector);
    }

    for (var index = 0; index < lists.size(); index++) {
      assertEquals(lists.get(index), toList(vectors.get(index)));
    }
  }

  private static void testAfterOperation(
      List<Integer> list, PersistentList<Integer> vector, int operationIndex) {
    assertEquals(firstOrNull(list), firstOrNull(vector));
    assertEquals(lastOrNull(list), lastOrNull(vector));
    assertEquals(list.size(), vector.size());
    if (operationIndex < list.size()) {
      assertEquals(list.get(operationIndex), vector.get(operationIndex));
    }
    if (operationIndex > 0) {
      assertEquals(list.get(operationIndex - 1), vector.get(operationIndex - 1));
    }
    if (operationIndex + 1 < list.size()) {
      assertEquals(list.get(operationIndex + 1), vector.get(operationIndex + 1));
    }
  }

  private static <E> ArrayList<E> toList(Iterable<E> elements) {
    var result = new ArrayList<E>();
    for (var element : elements) {
      result.add(element);
    }
    return result;
  }

  private static <E> E firstOrNull(List<E> list) {
    return list.isEmpty() ? null : list.get(0);
  }

  private static <E> E firstOrNull(PersistentList<E> list) {
    return list.isEmpty() ? null : list.get(0);
  }

  private static <E> E lastOrNull(List<E> list) {
    return list.isEmpty() ? null : list.get(list.size() - 1);
  }

  private static <E> E lastOrNull(PersistentList<E> list) {
    return list.isEmpty() ? null : list.get(list.size() - 1);
  }

  private static <E> E last(PersistentList<E> list) {
    return list.get(list.size() - 1);
  }

  private static ArrayList<Integer> range(int startInclusive, int endExclusive) {
    var result = new ArrayList<Integer>();
    for (var index = startInclusive; index < endExclusive; index++) {
      result.add(index);
    }
    return result;
  }

  private static ArrayList<Integer> randomIndexes(int size, int bound) {
    var result = new ArrayList<Integer>();
    for (var index = 0; index < size; index++) {
      result.add(RandomHolder.RANDOM.nextInt(bound));
    }
    return result;
  }

  private static final class RandomHolder {
    private static final Random RANDOM = new Random(0);
  }
}
