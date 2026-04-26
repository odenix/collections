/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.ImmutableList;
import org.odenix.collections.PersistentList;

class ImmutableListTest {
  @Test
  void empty() {
    var empty1 = PersistentList.<Integer>of();
    var empty2 = PersistentList.<String>of();

    assertEquals(empty1, empty2);
    // Deliberate deviation: ImmutableList does not support cross-type equality with java.util.List.
    assertEquals(List.of(), TestExtensions.toList(empty1));
    assertTrue((Object) empty1 == empty2);

    assertThrows(NoSuchElementException.class, () -> empty1.iterator().next());

    compareLists(List.of(), empty1);
  }

  @Test
  void persistentListFails() {
    var xs = PersistentList.of(rangeBoxed(1, 1885));

    xs = xs.removeAll(rangeList(1, 1837));

    assertEquals(rangeList(1838, 1885), TestExtensions.toList(xs));
  }

  @Test
  void ofElements() {
    var list0 = Arrays.<@Nullable Object>asList("a", "d", 1, null);
    var list1 = PersistentList.<@Nullable Object>of("a", "d", 1, null);
    var list2 = PersistentList.<@Nullable Object>of("a", "d", 1, null);

    compareLists(list0, list1);
    assertEquals(list1, list2);
  }

  @Test
  void toImmutable() {
    var original = Arrays.<@Nullable String>asList("a", "bar", "cat", null);

    var list = new ArrayList<>(original);
    var immList = ImmutableList.from(list);
    var immList2 = ImmutableList.from(immList);
    assertTrue(immList2 == immList);

    compareLists(original, immList);

    list.remove(0);
    // Deliberate deviation: ImmutableList does not support cross-type equality with java.util.List.
    assertNotEquals(list, immList);

    immList = PersistentList.from(immList).removeAt(0);
    compareLists(list, immList);
  }

  @Test
  void emptyListToPersistentList() {
    var empty = List.of();
    var emptyPersistent = PersistentList.from(empty);

    assertSame(emptyPersistent, PersistentList.from(empty));
  }

  @Test
  void addElements() {
    var list = PersistentList.<String>of();
    list = list.add("x");
    list = list.add(0, "a");
    list = list.addAll(TestExtensions.toList(list));
    list = list.addAll(1, List.of("b", "c"));
    list = list.add("y");
    list = list.add("z");
    list = list.addAll(Arrays.asList("1", "2"));

    compareLists("abcxaxyz12".chars().mapToObj(Character::toString).toList(), list);
  }

  @Test
  void replaceElements() {
    var list = PersistentList.from(chars("abcxaxab12"));

    for (var i = 0; i < list.size(); i++) {
      list = list.set(i, (char) (Objects.requireNonNull(list.get(i)) + i));
    }

    assertEquals("ace{e}gi9;", joinChars(list));
    var replacedList = list;
    assertThrows(IndexOutOfBoundsException.class, () -> replacedList.set(-1, '0'));
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> replacedList.set(replacedList.size() + 1, '0'));
  }

  @Test
  void removeElements() {
    var list = PersistentList.from(chars("abcxaxyz12"));

    expectList("bcxaxyz12", list.removeAt(0));
    expectList("abcaxyz12", list.remove('x'));
    expectList("abcaxyz12", list.remove('x'));
    expectList("abcayz12", list.removeAll(List.of('x')));
    expectList("abcayz12", list.removeAll(List.of('x')));
    expectList("abcxaxyz", list.removeAll(Character::isDigit));

    assertEquals(List.of(), TestExtensions.toList(list.removeAll(list)));
    assertEquals(List.of(), TestExtensions.toList(list.clear()));
  }

  @Test
  void smallPersistentListFromMutableBuffer() {
    var list = rangeList(0, 32);
    var vector = PersistentList.<Integer>of().mutate(it -> it.addAll(list));
    vector = vector.removeAt(vector.size() - 1);
    assertEquals(list.subList(0, list.size() - 1), TestExtensions.toList(vector));
  }

  @Test
  void subList() {
    var list = ImmutableList.from(chars("abcxaxyz12"));
    var subList = list.subList(2, 5);

    compareLists(List.of('c', 'x', 'a'), subList);

    assertThrows(IndexOutOfBoundsException.class, () -> list.subList(-1, 2));
    assertThrows(IndexOutOfBoundsException.class, () -> list.subList(0, list.size() + 1));
  }

  @Test
  void builder() {
    var builder = PersistentList.<Character>of().builder();
    builder.addAll(chars("abcxaxyz12"));
    var list = builder.build();
    // Deliberate deviation: ImmutableList does not support cross-type equality with java.util.List.
    assertEquals(TestExtensions.toList(list), builder);
    assertTrue(list == builder.build(), "Building the same list without modifications");

    var list2 = ImmutableList.from(builder);
    assertTrue(list2 == list, "toImmutable calls build()");

    testMutation(list, mutable -> mutable.add('K'));
    testMutation(list, mutable -> mutable.add(0, 'K'));
    testMutation(list, mutable -> mutable.addAll(chars("kotlin")));
    testMutation(list, mutable -> mutable.addAll(0, chars("kotlin")));
    testMutation(list, mutable -> mutable.set(1, (char) (mutable.get(1) + 2)));
    testMutation(list, mutable -> mutable.remove(mutable.size() - 1));
    testMutation(list, mutable -> mutable.remove(Character.valueOf('x')));
    testMutation(list, mutable -> mutable.removeAll(List.of('x')));
    testMutation(list, mutable -> mutable.removeIf(Character::isDigit));
    testMutation(list, List::clear);
    testMutation(list, mutable -> mutable.retainAll(chars("xyz")));
    testMutation(list, mutable -> mutable.removeIf(character -> !Character.isDigit(character)));
  }

  @Test
  void subListOfBuilder() {
    var list = PersistentList.from(chars("abcxaxyz12"));
    var builder = list.builder();

    var subList = builder.subList(2, 5);
    builder.set(4, 'x');
    var staleSubList = subList;
    assertThrows(ConcurrentModificationException.class, () -> joinChars(staleSubList));

    subList = builder.subList(2, 5);
    assertEquals("cxx", joinChars(subList));
    builder.set(4, 'b');
    assertEquals("cxb", joinChars(subList));
    subList.remove(0);
    assertEquals("xb", joinChars(subList));
    assertEquals("abxbxyz12", joinChars(builder));
  }

  @Test
  void noOperation() {
    testNoOperation(
        PersistentList.<Integer>of(), PersistentList::clear, List::clear);

    var list = PersistentList.from(chars("abcxaxyz12"));
    testNoOperation(
        list,
        persistent -> persistent.remove('d'),
        mutable -> mutable.remove(Character.valueOf('d')));
    testNoOperation(
        list,
        persistent -> persistent.removeAll(List.of('d', 'e')),
        mutable -> mutable.removeAll(List.of('d', 'e')));
    testNoOperation(
        list,
        persistent -> persistent.removeAll(Character::isUpperCase),
        mutable -> mutable.removeIf(Character::isUpperCase));
    testNoOperation(
        list, persistent -> persistent.removeAll(List.of()), mutable -> mutable.removeAll(List.of()));
    testNoOperation(
        list, persistent -> persistent.addAll(List.of()), mutable -> mutable.addAll(List.of()));
    testNoOperation(
        list,
        persistent -> persistent.addAll(2, List.of()),
        mutable -> mutable.addAll(2, List.of()));
  }

  @Test
  void covariantTyping() {
    // No direct Java equivalent: the upstream test relies on Kotlin declaration-site variance.
  }

  private static <T extends @Nullable Object> void compareLists(
      List<T> expected, ImmutableList<T> actual) {
    assertEquals(expected, TestExtensions.toList(actual));
  }

  private static void expectList(String content, ImmutableList<Character> list) {
    assertEquals(chars(content), TestExtensions.toList(list));
  }

  private static void testMutation(
      PersistentList<Character> list,
      Consumer<List<Character>> operation) {
    var mutable = new ArrayList<>(TestExtensions.toList(list));
    var builder = list.builder();

    operation.accept(mutable);
    operation.accept(builder);

    assertEquals(mutable, builder);
    assertEquals(mutable, TestExtensions.toList(builder.build()));
  }

  private static <T extends @Nullable Object> void testNoOperation(
      PersistentList<T> list,
      Function<PersistentList<T>, PersistentList<T>> persistent,
      Consumer<List<T>> mutating) {
    var result = persistent.apply(list);
    var buildResult = list.mutate(mutating);
    assertTrue(list == result);
    assertTrue(list == buildResult);
  }

  private static Integer[] rangeBoxed(int start, int endInclusive) {
    var result = new Integer[endInclusive - start + 1];
    for (var index = 0; index < result.length; index++) {
      result[index] = start + index;
    }
    return result;
  }

  private static List<Integer> rangeList(int start, int endInclusive) {
    var result = new ArrayList<Integer>();
    for (var value = start; value <= endInclusive; value++) {
      result.add(value);
    }
    return result;
  }

  private static List<Character> chars(String value) {
    var result = new ArrayList<Character>();
    for (var index = 0; index < value.length(); index++) {
      result.add(value.charAt(index));
    }
    return result;
  }

  private static String joinChars(Iterable<Character> iterable) {
    var result = new StringBuilder();
    for (var element : iterable) {
      result.append(element);
    }
    return result.toString();
  }

}
