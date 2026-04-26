/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.list;

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

class PersistentListTest {
  private static final int O_N = 96;
  private static final int O_NLOGN = 128;
  private static final int O_NN = 96;
  private static final int O_NNLOGN = 160;

  @Test
  void isEmptyTests() {
    var vector = PersistentList.<String>of();

    assertTrue(vector.isEmpty());
    assertFalse(vector.add("last").isEmpty());

    var elementsToAdd = O_N;
    var elements = distinctStringValues(elementsToAdd);
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

    assertEquals(0, vector.size());
    assertEquals(1, vector.add(1).size());

    var elementsToAdd = O_N;
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

    assertNull(TestExtensions.firstOrNull(vector));
    assertEquals(1, TestExtensions.first(vector.add(0, 1)));
    assertEquals(1, TestExtensions.first(vector.add(1)));

    var elementsToAdd = O_NN;
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(0, index);
      assertEquals(index, TestExtensions.first(vector));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index - 1, TestExtensions.first(vector));
      vector = vector.removeAt(0);
    }
    assertNull(TestExtensions.firstOrNull(vector));
  }

  @Test
  void lastTests() {
    var vector = PersistentList.<Integer>of();

    assertNull(TestExtensions.lastOrNull(vector));
    assertEquals(1, TestExtensions.last(vector.add(0, 1)));
    assertEquals(1, TestExtensions.last(vector.add(1)));

    var elementsToAdd = O_N;
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(index);
      assertEquals(index, TestExtensions.last(vector));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index - 1, TestExtensions.last(vector));
      vector = vector.removeAt(vector.size() - 1);
    }
    assertNull(TestExtensions.lastOrNull(vector));
  }

  @Test
  void toListTest() {
    var vector = PersistentList.<Integer>of();

    assertEquals(List.of(), TestExtensions.toList(vector));
    assertEquals(List.of(1), TestExtensions.toList(vector.add(1)));
    assertEquals(
        List.of(1, 2, 3, 4, 5, 6),
        TestExtensions.toList(vector.add(1).add(2).add(3).add(4).add(5).add(6)));
    assertEquals(
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20),
        TestExtensions.toList(
            vector
                .add(1)
                .add(2)
                .add(3)
                .add(4)
                .add(5)
                .add(6)
                .add(7)
                .add(8)
                .add(9)
                .add(10)
                .add(11)
                .add(12)
                .add(13)
                .add(14)
                .add(15)
                .add(16)
                .add(17)
                .add(18)
                .add(19)
                .add(20)));

    var elementsToAdd = O_NN;
    var list = new ArrayList<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      list.add(index);
      vector = vector.add(index);
      assertEquals(list, TestExtensions.toList(vector));
    }
  }

  @Test
  void addFirstTests() {
    var vector = PersistentList.<Integer>of();

    assertNull(TestExtensions.firstOrNull(vector));
    assertEquals(1, TestExtensions.first(vector.add(0, 1)));
    assertEquals(1, TestExtensions.last(vector.add(0, 1)));

    var elementsToAdd = O_NN;
    var allElements = new ArrayList<Integer>();
    for (var index = elementsToAdd - 1; index >= 0; index--) {
      allElements.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(0, index);

      assertEquals(index, TestExtensions.first(vector));
      assertEquals(0, TestExtensions.last(vector));
      assertEquals(index + 1, vector.size());

      var expectedContent = allElements.subList(elementsToAdd - vector.size(), elementsToAdd);
      assertEquals(expectedContent, TestExtensions.toList(vector));
    }
  }

  @Test
  void addLastTests() {
    var vector = PersistentList.<Integer>of();

    assertEquals(1, vector.add(1).get(0));

    var elementsToAdd = O_NN;
    var allElements = new ArrayList<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(index);

      assertEquals(0, vector.get(0));
      assertEquals(index, vector.get(index));
      assertEquals(index + 1, vector.size());

      assertEquals(allElements.subList(0, vector.size()), TestExtensions.toList(vector));
    }
  }

  @Test
  void removeFirstTests() {
    var vector = PersistentList.<Integer>of();

    var staleVector = vector;
    assertThrows(IndexOutOfBoundsException.class, () -> staleVector.removeAt(0));
    assertTrue(vector.add(1).removeAt(0).isEmpty());
    assertTrue(vector.add(0, 1).removeAt(0).isEmpty());

    var elementsToAdd = O_NN;
    var allElements = new ArrayList<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(index);
      vector = vector.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - 1, TestExtensions.last(vector));
      assertEquals(index, TestExtensions.first(vector));
      assertEquals(elementsToAdd - index, vector.size());
      assertEquals(allElements.subList(index, elementsToAdd), TestExtensions.toList(vector));

      vector = vector.removeAt(0);
    }
  }

  @Test
  void removeLastTests() {
    var vector = PersistentList.<Integer>of();

    var staleVector = vector;
    assertThrows(
        IndexOutOfBoundsException.class, () -> staleVector.removeAt(staleVector.size() - 1));
    assertTrue(vector.add(1).removeAt(0).isEmpty());
    assertTrue(vector.add(0, 1).removeAt(0).isEmpty());

    var elementsToAdd = O_NN;
    var allElements = new ArrayList<Integer>();
    for (var index = elementsToAdd - 1; index >= 0; index--) {
      allElements.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      vector = vector.add(0, index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(index, TestExtensions.last(vector));
      assertEquals(elementsToAdd - 1, TestExtensions.first(vector));
      assertEquals(elementsToAdd - index, vector.size());
      assertEquals(allElements.subList(0, vector.size()), TestExtensions.toList(vector));

      vector = vector.removeAt(vector.size() - 1);
    }

    var linear = O_N;
    for (var index = 0; index < linear; index++) {
      vector = vector.add(index);
    }
    for (var index = 0; index < linear; index++) {
      assertEquals(linear - index - 1, TestExtensions.last(vector));
      assertEquals(0, TestExtensions.first(vector));
      assertEquals(linear - index, vector.size());

      vector = vector.removeAt(vector.size() - 1);
    }
  }

  @Test
  void getTests() {
    var vector = PersistentList.<Integer>of();

    var staleVector = vector;
    assertThrows(IndexOutOfBoundsException.class, () -> staleVector.get(0));
    assertEquals(1, vector.add(1).get(0));

    var elementsToAdd = O_NNLOGN;
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

    var staleVector = vector;
    assertThrows(IndexOutOfBoundsException.class, () -> staleVector.set(0, 0));
    assertEquals(2, vector.add(1).set(0, 2).get(0));

    var elementsToAdd = O_NNLOGN;
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
    var random = new Random(0L);

    var listSizes = List.of(0, 1, 10, 31, 32, 33, 64, 65, 100, 1024, 1056, 1057, 10000, 100000);
    for (var initialSize : listSizes) {
      if (initialSize > O_NN) {
        continue;
      }

      var initialElements = new ArrayList<Integer>();
      var list = PersistentList.<Integer>of();
      for (var element = 0; element < initialSize; element++) {
        initialElements.add(element);
        list = list.add(element);
      }

      var addIndexes = new ArrayList<Integer>();
      addIndexes.add(initialSize);
      if (initialSize > 0) {
        addIndexes.add(0);
        addIndexes.add(random.nextInt(initialSize));
      }
      if (initialSize > maxBufferSize) {
        var rootSize = (initialSize - 1) & ~(maxBufferSize - 1);
        var tailSize = initialSize - rootSize;
        addIndexes.add(random.nextInt(maxBufferSize));
        addIndexes.add(rootSize + random.nextInt(tailSize));
        addIndexes.add(rootSize - random.nextInt(maxBufferSize));
        addIndexes.add(rootSize);
      }

      var addSize = random.nextInt(maxBufferSize * 2);
      for (var index : addIndexes) {
        for (var size = addSize; size < addSize + maxBufferSize; size++) {
          var elementsToAdd = new ArrayList<Integer>();
          for (var offset = 0; offset < size; offset++) {
            elementsToAdd.add(initialSize + offset);
          }

          var result = list.addAll(index, elementsToAdd);
          var expected = new ArrayList<>(initialElements);
          expected.addAll(index, elementsToAdd);
          assertEquals(expected, TestExtensions.toList(result));
        }
      }
    }
  }

  @Test
  void removeAllTests() {
    var maxBufferSize = 32;
    var random = new Random(1L);

    var listSizes = List.of(0, 1, 10, 31, 32, 33, 64, 65, 100, 1024, 1056, 1057, 10000, 33000);
    for (var initialSize : listSizes) {
      if (initialSize > O_NN) {
        continue;
      }

      var initialElements = new ArrayList<Integer>();
      var list = PersistentList.<Integer>of();
      for (var element = 0; element < initialSize; element++) {
        initialElements.add(element);
        list = list.add(element);
      }

      var removeElements = new ArrayList<List<Integer>>();
      removeElements.add(initialElements);
      if (initialSize > 0) {
        removeElements.add(List.of());
        removeElements.add(randomValues(random, initialSize, 1));
        removeElements.add(randomValues(random, initialSize, maxBufferSize));
        removeElements.add(randomValues(random, initialSize, initialSize / 2));
        removeElements.add(randomValues(random, initialSize, initialSize));
        removeElements.add(prefix(initialSize / 2));
        removeElements.add(suffix(initialSize, initialSize / 2));
      }
      if (initialSize > maxBufferSize) {
        var rootSize = (initialSize - 1) & ~(maxBufferSize - 1);
        var tailSize = initialSize - rootSize;
        removeElements.add(prefix(maxBufferSize));
        removeElements.add(range(rootSize, tailSize));
        removeElements.add(reverseTail(rootSize, maxBufferSize));
        for (var shift = 5; shift < 30; shift += 5) {
          var branches = 1 << shift;
          if (branches > rootSize) {
            break;
          }
          removeElements.add(shuffledTake(initialElements, initialSize - rootSize / branches, random));
        }
      }

      for (var elements : removeElements) {
        var expected = new ArrayList<>(initialElements);
        expected.removeAll(elements);

        var result = list.removeAll(elements);
        var hashSet = new HashSet<>(elements);
        var resultPredicate = list.removeAll(hashSet::contains);

        assertEquals(expected, TestExtensions.toList(result));
        assertEquals(expected, TestExtensions.toList(resultPredicate));
      }
    }
  }

  @Test
  void randomOperationsTests() {
    var random = new Random(0L);
    for (var repeat = 0; repeat < 1; repeat++) {
      var lists = new ArrayList<List<Integer>>();
      var vectors = new ArrayList<PersistentList<Integer>>();
      for (var index = 0; index < 20; index++) {
        lists.add(new ArrayList<>());
        vectors.add(PersistentList.of());
      }

      var operationCount = O_NLOGN;
      for (var operation = 0; operation < operationCount; operation++) {
        var index = random.nextInt(lists.size());
        var list = lists.get(index);
        var vector = vectors.get(index);

        var operationType = random.nextDouble();
        var operationIndex = list.size() > 1 ? random.nextInt(list.size()) : 0;

        var shouldRemove = operationType < 0.15;
        var shouldSet = operationType > 0.15 && operationType < 0.3;

        PersistentList<Integer> newVector;
        if (!list.isEmpty() && shouldRemove) {
          list.remove(operationIndex);
          newVector = vector.removeAt(operationIndex);
        } else if (!list.isEmpty() && shouldSet) {
          var value = random.nextInt();
          list.set(operationIndex, value);
          newVector = vector.set(operationIndex, value);
        } else {
          var value = random.nextInt();
          list.add(operationIndex, value);
          newVector = vector.add(operationIndex, value);
        }

        testAfterOperation(list, newVector, operationIndex);
        vectors.set(index, newVector);
      }

      assertEquals(lists, vectors.stream().map(TestExtensions::toList).toList());

      for (var index = 0; index < lists.size(); index++) {
        var list = lists.get(index);
        var vector = vectors.get(index);

        while (!list.isEmpty()) {
          var removeIndex = random.nextInt(list.size());
          list.remove(removeIndex);
          vector = vector.removeAt(removeIndex);

          testAfterOperation(list, vector, removeIndex);
        }
      }
    }
  }

  private static void testAfterOperation(
      List<Integer> list, PersistentList<Integer> vector, int operationIndex) {
    assertEquals(TestExtensions.firstOrNull(list), TestExtensions.firstOrNull(vector));
    assertEquals(TestExtensions.lastOrNull(list), TestExtensions.lastOrNull(vector));
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

  private static List<String> distinctStringValues(int size) {
    var values = new ArrayList<String>();
    for (var index = 0; index < size; index++) {
      values.add(Integer.toString(index));
    }
    return values;
  }

  private static List<Integer> randomValues(Random random, int bound, int size) {
    var values = new ArrayList<Integer>();
    for (var index = 0; index < size; index++) {
      values.add(random.nextInt(bound));
    }
    return values;
  }

  private static List<Integer> prefix(int size) {
    var values = new ArrayList<Integer>();
    for (var index = 0; index < size; index++) {
      values.add(index);
    }
    return values;
  }

  private static List<Integer> suffix(int initialSize, int size) {
    var values = new ArrayList<Integer>();
    for (var index = 0; index < size; index++) {
      values.add(initialSize - index);
    }
    return values;
  }

  private static List<Integer> range(int start, int size) {
    var values = new ArrayList<Integer>();
    for (var index = 0; index < size; index++) {
      values.add(start + index);
    }
    return values;
  }

  private static List<Integer> reverseTail(int rootSize, int size) {
    var values = new ArrayList<Integer>();
    for (var index = 0; index < size; index++) {
      values.add(rootSize - index);
    }
    return values;
  }

  private static List<Integer> shuffledTake(List<Integer> values, int size, Random random) {
    var shuffled = new ArrayList<>(values);
    for (var index = shuffled.size() - 1; index > 0; index--) {
      var swapIndex = random.nextInt(index + 1);
      var element = shuffled.get(index);
      shuffled.set(index, shuffled.get(swapIndex));
      shuffled.set(swapIndex, element);
    }
    return new ArrayList<>(shuffled.subList(0, size));
  }

}
