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
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentList;

class PersistentListBuilderTest {
  @Test
  void isEmptyTests() {
    var builder = PersistentList.<String>of().builder();

    assertTrue(builder.isEmpty());

    var elementsToAdd = 8;
    var elements = List.of("a", "b", "c", "d", "e", "f", "g", "h");
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
    var builder = PersistentList.of().builder();

    assertTrue(builder.size() == 0);

    var elementsToAdd = 96;
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
    var builder = PersistentList.of().builder();

    assertNull(TestExtensions.firstOrNull(builder));

    var elementsToAdd = 96;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(0, index);
      assertEquals(index, TestExtensions.first(builder));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index - 1, TestExtensions.first(builder));
      builder.remove(0);
    }
    assertNull(TestExtensions.firstOrNull(builder));
  }

  @Test
  void lastTests() {
    var builder = PersistentList.of().builder();

    assertNull(TestExtensions.lastOrNull(builder));

    var elementsToAdd = 96;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
      assertEquals(index, TestExtensions.last(builder));
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - index - 1, TestExtensions.last(builder));
      builder.remove(builder.size() - 1);
    }
    assertNull(TestExtensions.lastOrNull(builder));
  }

  @Test
  void toListTest() {
    var builder = PersistentList.of().builder();

    assertEquals(List.of(), TestExtensions.toList(builder));

    var elementsToAdd = 96;
    var list = new ArrayList<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      list.add(index);
      builder.add(index);
      assertEquals(list, TestExtensions.toList(builder));
    }
  }

  @Test
  void addFirstTests() {
    var builder = PersistentList.of().builder();

    assertNull(TestExtensions.firstOrNull(builder));

    var elementsToAdd = 96;
    var allElements = new ArrayList<Integer>();
    for (var index = elementsToAdd - 1; index >= 0; index--) {
      allElements.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(0, index);

      assertEquals(index, TestExtensions.first(builder));
      assertEquals(0, TestExtensions.last(builder));
      assertEquals(index + 1, builder.size());

      var expectedContent = allElements.subList(elementsToAdd - builder.size(), elementsToAdd);
      assertEquals(expectedContent, builder);
    }
  }

  @Test
  void addLastTests() {
    var builder = PersistentList.of().builder();

    var elementsToAdd = 96;
    var allElements = new ArrayList<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(index);
      builder.add(index);

      assertEquals(0, builder.get(0));
      assertEquals(index, builder.get(index));
      assertEquals(index + 1, builder.size());
      assertEquals(allElements.subList(0, builder.size()), builder);
    }
  }

  @Test
  void removeFirstTests() {
    var builder = PersistentList.of().builder();

    assertThrows(IndexOutOfBoundsException.class, () -> builder.remove(0));

    var elementsToAdd = 96;
    var allElements = new ArrayList<Integer>();
    for (var index = 0; index < elementsToAdd; index++) {
      allElements.add(index);
      builder.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(elementsToAdd - 1, TestExtensions.last(builder));
      assertEquals(index, TestExtensions.first(builder));
      assertEquals(elementsToAdd - index, builder.size());
      assertEquals(allElements.subList(index, elementsToAdd), builder);

      builder.remove(0);
    }
  }

  @Test
  void removeLastTests() {
    var builder = PersistentList.of().builder();

    assertThrows(IndexOutOfBoundsException.class, () -> builder.remove(builder.size() - 1));

    var elementsToAdd = 96;
    var allElements = new ArrayList<Integer>();
    for (var index = elementsToAdd - 1; index >= 0; index--) {
      allElements.add(index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(0, index);
    }
    for (var index = 0; index < elementsToAdd; index++) {
      assertEquals(index, TestExtensions.last(builder));
      assertEquals(elementsToAdd - 1, TestExtensions.first(builder));
      assertEquals(elementsToAdd - index, builder.size());
      assertEquals(allElements.subList(0, builder.size()), builder);

      builder.remove(builder.size() - 1);
    }

    var linear = 64;
    for (var index = 0; index < linear; index++) {
      builder.add(index);
    }
    for (var index = 0; index < linear; index++) {
      assertEquals(linear - 1 - index, TestExtensions.last(builder));
      assertEquals(0, TestExtensions.first(builder));
      assertEquals(linear - index, builder.size());

      builder.remove(builder.size() - 1);
    }
  }

  @Test
  void getTests() {
    var builder = PersistentList.of().builder();

    assertThrows(IndexOutOfBoundsException.class, () -> builder.get(0));

    var elementsToAdd = 160;
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
    var builder = PersistentList.of().builder();

    assertThrows(IndexOutOfBoundsException.class, () -> builder.set(0, 0));

    var elementsToAdd = 160;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index * 2);

      for (var i = 0; i <= index; i++) {
        assertEquals(i + index, builder.get(i));
        builder.set(i, i + index + 1);
        assertEquals(i + index + 1, builder.get(i));
      }
    }
    for (var index = 0; index < elementsToAdd; index++) {
      for (var i = 0; i < elementsToAdd - index; i++) {
        var expected = elementsToAdd + i;

        assertEquals(expected, builder.get(i));
        builder.set(i, expected - 1);
        assertEquals(expected - 1, builder.get(i));
      }

      builder.remove(0);
    }
  }

  @Test
  void subListTests() {
    var builder = PersistentList.of().builder();

    var elementsToAdd = 5000;
    for (var index = 0; index < elementsToAdd; index++) {
      builder.add(index);
    }

    var beginIndex = 1234;
    var endIndex = 4321;
    var subList = builder.subList(beginIndex, endIndex);

    builder.set(beginIndex, 0);
    assertEquals(endIndex - beginIndex, subList.size());
    assertEquals(0, subList.get(0));
    assertEquals(elementsToAdd, builder.size());
    assertEquals(0, builder.get(beginIndex));

    subList.add(beginIndex, 0);
    assertEquals(endIndex - beginIndex + 1, subList.size());
    assertEquals(0, subList.get(beginIndex));
    assertEquals(elementsToAdd + 1, builder.size());
    assertEquals(0, builder.get(beginIndex * 2));

    builder.add(0);
    assertThrows(ConcurrentModificationException.class, () -> subList.add(0));
  }

  @Test
  void iterationTests() {
    var elementsToAdd = 160;
    var list = PersistentList(elementsToAdd, index -> index);
    var builder = list.builder();
    var expected = TestExtensions.toMutableList(list);

    var builderIterator = builder.listIterator();
    var expectedIterator = expected.listIterator();
    compare(expectedIterator, builderIterator);

    var random = new Random(0L);
    for (var iteration = 0; iteration < 100; iteration++) {
      var createNew = random.nextDouble() < 0.2;
      if (createNew) {
        var index = random.nextInt(expected.size());
        builderIterator = builder.listIterator(index);
        expectedIterator = expected.listIterator(index);
        compare(expectedIterator, builderIterator);
      }

      var maxIterationCount = expected.size() / 3;
      iterateWith(random, expectedIterator, builderIterator, maxIterationCount, () -> {});
    }
  }

  @Test
  void iteratorSetTests() {
    var elementsToAdd = 160;
    var list = PersistentList(elementsToAdd, index -> index);
    var builder = list.builder();
    var expected = TestExtensions.toMutableList(list);

    var builderIterator = builder.listIterator();
    var expectedIterator = expected.listIterator();
    compare(expectedIterator, builderIterator);

    var random = new Random(1L);
    for (var iteration = 0; iteration < 100; iteration++) {
      var createNew = random.nextDouble() < 0.1;
      if (createNew) {
        var index = random.nextInt(expected.size());
        builderIterator = builder.listIterator(index);
        expectedIterator = expected.listIterator(index);
        compare(expectedIterator, builderIterator);
      }

      var maxIterationCount = expected.size() / 3;
      var finalExpectedIterator = expectedIterator;
      var finalBuilderIterator = builderIterator;
      iterateWith(
          random,
          expectedIterator,
          builderIterator,
          maxIterationCount,
          () -> {
            var shouldSet = random.nextBoolean();
            if (shouldSet) {
              var elementToSet = random.nextInt();
              finalExpectedIterator.set(elementToSet);
              finalBuilderIterator.set(elementToSet);
            }
          });
    }
  }

  @Test
  void iteratorAddTests() {
    var elementsToAdd = 96;
    var list = PersistentList(elementsToAdd, index -> index);
    var builder = list.builder();
    var expected = TestExtensions.toMutableList(list);

    var builderIterator = builder.listIterator(builder.size());
    var expectedIterator = expected.listIterator(builder.size());
    compare(expectedIterator, builderIterator);

    var random = new Random(2L);
    for (var iteration = 0; iteration < 100; iteration++) {
      var createNew = random.nextDouble() < 0.1;
      if (createNew) {
        var index = random.nextInt(expected.size());
        builderIterator = builder.listIterator(index);
        expectedIterator = expected.listIterator(index);
        compare(expectedIterator, builderIterator);
      }

      var shouldAdd = random.nextBoolean();
      if (shouldAdd) {
        var addCount = random.nextInt(32);
        for (var i = 0; i < addCount; i++) {
          var elementToAdd = random.nextInt();
          expectedIterator.add(elementToAdd);
          builderIterator.add(elementToAdd);
          compare(expectedIterator, builderIterator);
        }
      } else {
        var maxIterationCount = expected.size() / 3;
        iterateWith(random, expectedIterator, builderIterator, maxIterationCount, () -> {});
      }
    }
  }

  @Test
  void iteratorRemoveTests() {
    var elementsToAdd = 96;
    var list = PersistentList(elementsToAdd, index -> index);
    var builder = list.builder();
    var expected = TestExtensions.toMutableList(list);

    var builderIterator = builder.listIterator();
    var expectedIterator = expected.listIterator();
    compare(expectedIterator, builderIterator);

    var random = new Random(3L);
    for (var iteration = 0; iteration < 100; iteration++) {
      var createNew = random.nextDouble() < 0.1;
      if (createNew) {
        var index = random.nextInt(expected.size() + 1);
        builderIterator = builder.listIterator(index);
        expectedIterator = expected.listIterator(index);
        compare(expectedIterator, builderIterator);
      }

      var shouldAddOrRemove = random.nextBoolean();
      if (shouldAddOrRemove) {
        var actionCount = random.nextInt(32);
        var shouldAdd = random.nextBoolean();

        if (shouldAdd) {
          for (var i = 0; i < actionCount; i++) {
            var elementToAdd = random.nextInt();
            expectedIterator.add(elementToAdd);
            builderIterator.add(elementToAdd);
            compare(expectedIterator, builderIterator);
          }
        } else {
          var finalExpectedIterator = expectedIterator;
          var finalBuilderIterator = builderIterator;
          iterateWith(
              random,
              expectedIterator,
              builderIterator,
              actionCount,
              () -> {
                finalExpectedIterator.remove();
                finalBuilderIterator.remove();
                compare(finalExpectedIterator, finalBuilderIterator);
              });
        }
      } else {
        var maxIterationCount = expected.size() / 3 + 1;
        iterateWith(random, expectedIterator, builderIterator, maxIterationCount, () -> {});
      }
    }
  }

  @Test
  void addAllAtIndexTests() {
    var maxBufferSize = 32;
    var listSizes = List.of(0, 1, 10, 31, 32, 33, 64, 65, 100, 1024, 1056, 1057);
    var random = new Random(0L);

    for (var initialSize : listSizes) {
      var initialElements = List.copyOf(TestExtensions.toList(PersistentList(initialSize, index -> index)));
      var list = PersistentList(initialSize, index -> index);

      var addIndex = new ArrayList<Integer>();
      addIndex.add(initialSize);
      if (initialSize > 0) {
        addIndex.add(0);
        addIndex.add(random.nextInt(initialSize));
      }
      if (initialSize > maxBufferSize) {
        var rootSize = (initialSize - 1) & ~(maxBufferSize - 1);
        var tailSize = initialSize - rootSize;
        addIndex.add(random.nextInt(maxBufferSize));
        addIndex.add(rootSize + random.nextInt(tailSize));
        addIndex.add(rootSize - random.nextInt(maxBufferSize));
        addIndex.add(rootSize);
      }

      var addSize = random.nextInt(maxBufferSize * 2);
      for (var index : addIndex) {
        for (var size = addSize; size <= addSize + maxBufferSize; size++) {
          var elementsToAdd = new ArrayList<Integer>();
          for (var value = 0; value < size; value++) {
            elementsToAdd.add(initialSize + value);
          }

          var builder = list.builder();
          builder.addAll(index, elementsToAdd);

          var expected = TestExtensions.toMutableList(initialElements);
          expected.addAll(index, elementsToAdd);
          assertEquals(expected, builder);
        }
      }
    }
  }

  @Test
  void removeAllTests() {
    var maxBufferSize = 32;
    var listSizes = List.of(0, 1, 10, 31, 32, 33, 64, 65, 100, 1024, 1056, 1057);
    var random = new Random(1L);

    for (var initialSize : listSizes) {
      var initialElements = List.copyOf(TestExtensions.toList(PersistentList(initialSize, index -> index)));
      var list = PersistentList(initialSize, index -> index);

      var removeElements = new ArrayList<List<Integer>>();
      removeElements.add(initialElements);
      if (initialSize > 0) {
        removeElements.add(Collections.emptyList());
        removeElements.add(randomValues(random, initialSize, 1));
        removeElements.add(randomValues(random, initialSize, maxBufferSize));
        removeElements.add(randomValues(random, initialSize, initialSize / 2));
        removeElements.add(randomValues(random, initialSize, initialSize));
      }
      if (initialSize > maxBufferSize) {
        var rootSize = (initialSize - 1) & ~(maxBufferSize - 1);
        var tailSize = initialSize - rootSize;

        var firstLeaf = new ArrayList<Integer>();
        for (var value = 0; value < maxBufferSize; value++) {
          firstLeaf.add(value);
        }
        removeElements.add(firstLeaf);

        var tail = new ArrayList<Integer>();
        for (var value = 0; value < tailSize; value++) {
          tail.add(rootSize + value);
        }
        removeElements.add(tail);

        var lastLeaf = new ArrayList<Integer>();
        for (var value = 0; value < maxBufferSize; value++) {
          lastLeaf.add(rootSize - value);
        }
        removeElements.add(lastLeaf);
      }

      for (var elements : removeElements) {
        var expected = TestExtensions.toMutableList(initialElements);
        expected.removeAll(elements);

        var builder = list.builder();
        builder.removeAll(elements);

        var builderPredicate = list.builder();
        var hashSet = new HashSet<>(elements);
        builderPredicate.removeIf(hashSet::contains);

        assertEquals(expected, builder);
        assertEquals(expected, builderPredicate);
      }
    }
  }

  @Test
  void randomOperationsTests() {
    var random = new Random(0L);
    var vectorGen = new ArrayList<List<PersistentList<Integer>>>();
    var expected = new ArrayList<List<List<Integer>>>();

    vectorGen.add(TestExtensions.map(Collections.nCopies(20, 0), ignored -> PersistentList.of()));
    expected.add(TestExtensions.map(Collections.nCopies(20, 0), ignored -> List.of()));

    for (var generation = 0; generation < 5; generation++) {
      var builders =
          TestExtensions.map(
              Objects.requireNonNull(TestExtensions.last(vectorGen)),
              PersistentList::builder);
      var lists = TestExtensions.map(builders, TestExtensions::toMutableList);

      var operationCount = 160;
      for (var operation = 0; operation < operationCount; operation++) {
        var index = random.nextInt(lists.size());
        var list = lists.get(index);
        var builder = builders.get(index);

        var operationType = random.nextDouble();
        var operationIndex = list.size() > 1 ? random.nextInt(list.size()) : 0;

        var shouldRemove = operationType < 0.15;
        var shouldSet = operationType > 0.15 && operationType < 0.3;

        if (!list.isEmpty() && shouldRemove) {
          assertEquals(list.remove(operationIndex), builder.remove(operationIndex));
        } else if (!list.isEmpty() && shouldSet) {
          var value = random.nextInt();
          assertEquals(list.set(operationIndex, value), builder.set(operationIndex, value));
        } else {
          var value = random.nextInt();
          list.add(operationIndex, value);
          builder.add(operationIndex, value);
        }

        testAfterOperation(list, builder, operationIndex);
      }

      assertEquals(lists, builders);

      vectorGen.add(TestExtensions.map(builders, PersistentList.Builder::build));
      expected.add(lists);

      var maxSize = 0;
      for (var builder : builders) {
        maxSize = Math.max(maxSize, builder.size());
      }
      System.out.println("Largest persistent list builder size: " + maxSize);
    }

    for (var genIndex = 0; genIndex < vectorGen.size(); genIndex++) {
      var vectors = vectorGen.get(genIndex);
      for (var vectorIndex = 0; vectorIndex < vectors.size(); vectorIndex++) {
        var vector = vectors.get(vectorIndex);
        var expectedList = expected.get(genIndex).get(vectorIndex);
        // Deliberate deviation: ImmutableList does not support cross-type equality with java.util.List.
        assertEquals(
            expectedList,
            TestExtensions.toList(vector),
            "The persistent list of "
                + genIndex
                + " generation was modified.\nExpected: "
                + expectedList
                + "\nActual: "
                + vector);
      }
    }
  }

  @SuppressWarnings("MethodName")
  private static <E extends @Nullable Object> PersistentList<E> PersistentList(
      int size, IntFunction<E> producer) {
    var list = PersistentList.<E>of();
    for (var index = 0; index < size; index++) {
      list = list.add(producer.apply(index));
    }
    return list;
  }

  private static <E extends @Nullable Object> void iterateWith(
      Random random,
      ListIterator<E> expectedIterator,
      ListIterator<E> actualIterator,
      int maxIterationCount,
      Runnable afterIteration) {
    var towardStart = random.nextBoolean();
    var iterationCount = random.nextInt(maxIterationCount + 1);

    if (towardStart) {
      for (var iteration = 0; iteration < iterationCount; iteration++) {
        if (!expectedIterator.hasPrevious()) {
          return;
        }
        assertEquals(expectedIterator.previous(), actualIterator.previous());
        afterIteration.run();
        compare(expectedIterator, actualIterator);
      }
      return;
    }

    for (var iteration = 0; iteration < iterationCount; iteration++) {
      if (!expectedIterator.hasNext()) {
        return;
      }
      assertEquals(expectedIterator.next(), actualIterator.next());
      afterIteration.run();
      compare(expectedIterator, actualIterator);
    }
  }

  private static <E extends @Nullable Object> void compare(
      ListIterator<E> expectedIterator, ListIterator<E> actualIterator) {
    assertEquals(expectedIterator.hasNext(), actualIterator.hasNext());
    assertEquals(expectedIterator.hasPrevious(), actualIterator.hasPrevious());
    assertEquals(expectedIterator.nextIndex(), actualIterator.nextIndex());
    assertEquals(expectedIterator.previousIndex(), actualIterator.previousIndex());
  }

  private static List<Integer> randomValues(Random random, int initialSize, int size) {
    var values = new ArrayList<Integer>();
    for (var index = 0; index < size; index++) {
      values.add(random.nextInt(initialSize));
    }
    return values;
  }

  private static void testAfterOperation(List<Integer> list1, List<Integer> list2, int operationIndex) {
    assertEquals(TestExtensions.firstOrNull(list1), TestExtensions.firstOrNull(list2));
    assertEquals(TestExtensions.lastOrNull(list1), TestExtensions.lastOrNull(list2));
    assertEquals(list1.size(), list2.size());
    if (operationIndex < list1.size()) {
      assertEquals(list1.get(operationIndex), list2.get(operationIndex));
    }
    if (operationIndex > 0) {
      assertEquals(list1.get(operationIndex - 1), list2.get(operationIndex - 1));
    }
    if (operationIndex + 1 < list1.size()) {
      assertEquals(list1.get(operationIndex + 1), list2.get(operationIndex + 1));
    }
  }
}
