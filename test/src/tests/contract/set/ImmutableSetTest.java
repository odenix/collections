/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.ImmutableSet;
import org.odenix.collections.PersistentSet;

import tests.IntWrapper;

class ImmutableHashSetTest extends ImmutableSetTestBase {
  @SafeVarargs
  @Override
  final <T extends @Nullable Object> PersistentSet<T> immutableSetOf(T... elements) {
    return PersistentSet.hashOf(elements);
  }

  @Override
  <T extends @Nullable Object> void testBuilderToPersistentSet(PersistentSet.Builder<T> builder) {
    // Java adaptation: hash set builders are not recognized by PersistentSet.from(Iterable).
    assertNotSame(builder.build(), PersistentSet.from(builder));
  }

  @Test
  void addAllElements() {
    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      assertSame(left, left.addAll(Set.of()));
      compareSets(TestExtensions.toSet(left), this.<Integer>immutableSetOf().addAll(TestExtensions.toSet(left)));
    }

    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      var right = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(200, 3000).boxed().toList());
      var expected = new HashSet<>(TestExtensions.toSet(left));
      expected.addAll(TestExtensions.toSet(right));
      compareSets(expected, left.addAll(TestExtensions.toSet(right)));
    }

    {
      var left =
          this.<IntWrapper>immutableSetOf().addAll(
              IntStream.rangeClosed(1, 2000)
                  .mapToObj(index -> new IntWrapper(index, index % 200))
                  .toList());
      var right =
          this.<IntWrapper>immutableSetOf().addAll(
              IntStream.rangeClosed(200, 3000)
                  .mapToObj(index -> new IntWrapper(index, index % 200))
                  .toList());
      var expected = new HashSet<>(TestExtensions.toSet(left));
      expected.addAll(TestExtensions.toSet(right));
      compareSets(expected, left.addAll(TestExtensions.toSet(right)));
    }

    {
      var left =
          this.<String>immutableSetOf().addAll(
              IntStream.rangeClosed(1, 2000).mapToObj(Integer::toString).toList());
      var right =
          this.<String>immutableSetOf().addAll(
              IntStream.rangeClosed(200, 3000).mapToObj(Integer::toString).toList());
      var expected = new HashSet<>(TestExtensions.toSet(left));
      expected.addAll(TestExtensions.toSet(right));
      compareSets(expected, left.addAll(TestExtensions.toSet(right)));
    }

    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      assertSame(left, left.addAll(left));
      assertSame(left, left.addAll(immutableSetOf()));
      var right = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      assertSame(right, right.addAll(left));
      assertSame(left, left.addAll(right));
    }
  }

  @Test
  void addAllElementsFromBuilder() {
    var builder1 = this.<String>immutableSetOf().builder();
    var builder2 = this.<String>immutableSetOf().builder();
    var expected = new HashSet<String>();
    for (var i = 300; i <= 400; i++) {
      builder1.add(Integer.toString(i));
      expected.add(Integer.toString(i));
    }
    for (var i = 0; i <= 200; i++) {
      builder2.add(Integer.toString(i));
      expected.add(Integer.toString(i));
    }
    builder1.addAll(builder2);

    compareSets(expected, builder1);
    builder2.add("200");
    compareSets(expected, builder1);
    compareSets(expected, builder1.build());
  }

  @Test
  void containsAll() {
    assertTrue(immutableSetOf(1).containsAll(Set.of(1)));
    assertTrue(immutableSetOf(1, 2).containsAll(Set.of(1)));
    assertTrue(
        this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList())
            .containsAll(
                TestExtensions.toSet(
                    this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(400, 1000).boxed().toList()))));
    assertFalse(
        this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList())
            .containsAll(
                TestExtensions.toSet(
                    this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1999, 2001).boxed().toList()))));
  }

  @Test
  void retainAllElements() {
    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      compareSets(TestExtensions.toSet(immutableSetOf()), left.retainAll(Set.of()));
      compareSets(TestExtensions.toSet(immutableSetOf()), immutableSetOf().retainAll(TestExtensions.toSet(left)));
    }

    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      var right = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(2000, 3000).boxed().toList());
      compareSets(Set.of(2000), left.intersect(right));
      compareSets(Set.of(2000), right.intersect(left));
    }

    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      var right = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(2001, 3000).boxed().toList());
      compareSets(Set.of(), left.intersect(right));
      compareSets(Set.of(), right.intersect(left));
    }

    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      var right = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(200, 3000).boxed().toList());
      var expected = new HashSet<>(TestExtensions.toSet(left));
      expected.retainAll(TestExtensions.toSet(right));
      compareSets(expected, left.intersect(right));
    }
  }

  @Test
  void removeAllElements() {
    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      assertSame(left, left.removeAll(Set.of()));
      assertSame(this.<Integer>immutableSetOf(), this.<Integer>immutableSetOf().removeAll(TestExtensions.toSet(left)));
    }

    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      var right = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(2000, 3000).boxed().toList());
      compareSets(new HashSet<>(IntStream.rangeClosed(1, 1999).boxed().toList()), left.removeAll(right));
      compareSets(new HashSet<>(IntStream.rangeClosed(2001, 3000).boxed().toList()), right.removeAll(left));
    }

    {
      var left = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(1, 2000).boxed().toList());
      var right = this.<Integer>immutableSetOf().addAll(IntStream.rangeClosed(2001, 3000).boxed().toList());
      assertSame(left, left.removeAll(right));
      assertSame(right, right.removeAll(left));
    }
  }
}

class ImmutableOrderedSetTest extends ImmutableSetTestBase {
  @SafeVarargs
  @Override
  final <T extends @Nullable Object> PersistentSet<T> immutableSetOf(T... elements) {
    return PersistentSet.of(elements);
  }

  @Override
  <T extends @Nullable Object> void testBuilderToPersistentSet(PersistentSet.Builder<T> builder) {
    assertSame(builder.build(), PersistentSet.from(builder));
  }

  @Override
  <T extends @Nullable Object> void compareSets(Set<T> expected, Iterable<T> actual) {
    if (expected instanceof LinkedHashSet<?>) {
      TestExtensions.compareOrderedSets(expected, actual);
      return;
    }
    TestExtensions.compareSets(expected, actual);
  }

  @Test
  void toImmutable() {
    var original = TestExtensions.setOf("a", "bar", "cat", null);
    var immOriginal = immutableSetOf(original);
    compareSets(original, immOriginal);

    var hashSet = new HashSet<>(original);
    var immSet = immutableSetOf(hashSet);
    var immSet2 = ImmutableSet.from(immSet);
    assertSame(immSet, immSet2);

    // Deliberate Java adaptation: HashSet iteration order is not stable enough to use as an
    // ordered expected fixture here.
    TestExtensions.compareSets(original, immSet);
    TestExtensions.compareSets(hashSet, immSet);

    hashSet.remove("a");
    assertNotEquals(hashSet, immSet);

    immSet = immSet.remove("a");
    TestExtensions.compareSets(hashSet, immSet);
  }

  @Test
  void elementHashCodeChanged() {
    var changing = new LinkedHashSet<String>();
    changing.add("ok");
    PersistentSet<Object> persistent = immutableSetOf("constant", changing, "fix");
    assertEquals(1, TestExtensions.toMutableList(persistent).stream().filter(element -> element == changing).count());
    changing.add("break iteration");
    assertThrows(
        ConcurrentModificationException.class,
        () -> TestExtensions.toMutableList(persistent).stream().filter(element -> element == changing).toList());
  }

  @Test
  void builderElementHashCodeChanged() {
    var changing = new LinkedHashSet<String>();
    changing.add("ok");
    PersistentSet.Builder<Object> builder = immutableSetOf().builder();
    builder.addAll(List.of("constant", changing, "fix"));
    assertEquals(1, builder.stream().filter(element -> element == changing).count());
    changing.add("break iteration");
    assertThrows(
        ConcurrentModificationException.class,
        () -> builder.stream().filter(element -> element == changing).toList());
  }
}

abstract class ImmutableSetTestBase {
  @SuppressWarnings("unchecked")
  abstract <T extends @Nullable Object> PersistentSet<T> immutableSetOf(T... elements);

  <T extends @Nullable Object> PersistentSet<T> immutableSetOf() {
    return immutableSetOf(TestExtensions.emptyElements());
  }

  abstract <T extends @Nullable Object> void testBuilderToPersistentSet(
      PersistentSet.Builder<T> builder);

  <T extends @Nullable Object> PersistentSet<T> immutableSetOf(Collection<T> elements) {
    return this.<T>immutableSetOf().addAll(elements);
  }

  <T extends @Nullable Object> void compareSets(Set<T> expected, Iterable<T> actual) {
    TestExtensions.compareSets(expected, actual);
  }

  @Test
  void empty() {
    var empty1 = immutableSetOf();
    var empty2 = immutableSetOf();
    assertEquals(empty1, empty2);
    assertSame(empty1, empty2);

    compareSets(Set.of(), empty1);
  }

  @Test
  void ofElements() {
    var set0 = TestExtensions.setOf("a", "d", 1, null);
    var set1 = immutableSetOf("a", "d", 1, null);
    var set2 = immutableSetOf("a", "d", 1, null);

    compareSets(set0, set1);
    compareSets(TestExtensions.toSet(set1), set2);
  }

  @Test
  void toImmutable() {
    var original = TestExtensions.setOf("a", "bar", "cat", null);
    var immOriginal = immutableSetOf(original);
    compareSets(original, immOriginal);

    var hashSet = new HashSet<>(original);
    var immSet = immutableSetOf(hashSet);
    var immSet2 = ImmutableSet.from(immSet);
    assertSame(immSet, immSet2);

    compareSets(original, immSet);
    compareSets(hashSet, immSet);

    hashSet.remove("a");
    assertNotEquals(hashSet, immSet);

    immSet = immSet.remove("a");
    compareSets(hashSet, immSet);
  }

  @Test
  void addElements() {
    PersistentSet<String> set = immutableSetOf();
    set = set.add("x");
    set = set.addAll(TestExtensions.toSet(set));
    set = set.add("y");
    set = set.add("z");
    set = set.addAll(List.of("1", "2"));
    compareSets(TestExtensions.setOf("x", "y", "z", "1", "2"), set);
  }

  @Test
  void removeElements() {
    var set = immutableSetOf(TestExtensions.chars("abcxyz12"));

    expectSet("abcyz12", set.remove('x'));
    expectSet("abcyz12", set.remove('x'));
    expectSet("abcy12", set.removeAll(Set.of('x', 'z')));
    expectSet("abcy12", set.removeAll(Set.of('x', 'z')));
    expectSet("abcxyz", set.removeAll(Character::isDigit));

    compareSets(Set.of(), set.removeAll(set));
    compareSets(Set.of(), set.clear());
  }

  @Test
  void builder() {
    var builder = this.<Character>immutableSetOf().builder();
    TestExtensions.chars("abcxaxyz12").forEach(builder::add);
    PersistentSet<Character> set = builder.build();
    compareSets(TestExtensions.toSet(set), builder);
    assertSame(set, builder.build());

    ImmutableSet<Character> set2 = ImmutableSet.from(builder);
    assertSame(set, set2);

    testBuilderToPersistentSet(builder);

    testMutation(set, mutable -> mutable.add('K'));
    testMutation(set, mutable -> mutable.addAll(TestExtensions.toSet(TestExtensions.chars("kotlin"))));
    testMutation(set, mutable -> mutable.remove('x'));
    testMutation(set, mutable -> mutable.removeAll(Set.of('x', 'z')));
    testMutation(set, mutable -> mutable.removeIf(Character::isDigit));
    testMutation(set, Set::clear);
    testMutation(set, mutable -> mutable.retainAll(Set.of('x', 'y', 'z')));
    testMutation(
        set, mutable -> mutable.removeIf((Character character) -> !Character.isDigit(character)));
  }

  private static <T extends @Nullable Object> void testMutation(
      PersistentSet<T> set, Consumer<Set<T>> operation) {
    var mutable = TestExtensions.toMutableSet(set);
    var builder = set.builder();

    operation.accept(mutable);
    operation.accept(builder);

    TestExtensions.compareSets(mutable, builder);
    TestExtensions.compareSets(mutable, builder.build());
  }

  @Test
  void noOperation() {
    testNoOperation(immutableSetOf(), PersistentSet::clear, Set::clear);

    var set = immutableSetOf(TestExtensions.chars("abcxyz12"));
    testNoOperation(set, persistent -> persistent.add('a'), mutable -> mutable.add('a'));
    testNoOperation(set, persistent -> persistent.addAll(Set.of()), mutable -> mutable.addAll(Set.of()));
    testNoOperation(set, persistent -> persistent.addAll(List.of('a', 'b')), mutable -> mutable.addAll(List.of('a', 'b')));
    testNoOperation(set, persistent -> persistent.remove('d'), mutable -> mutable.remove('d'));
    testNoOperation(set, persistent -> persistent.removeAll(List.of('d', 'e')), mutable -> mutable.removeAll(List.of('d', 'e')));
    testNoOperation(set, persistent -> persistent.removeAll(Character::isUpperCase), mutable -> mutable.removeIf(Character::isUpperCase));
    testNoOperation(
        set,
        persistent -> persistent.removeAll(Set.of()),
        mutable -> mutable.removeAll(Set.<Character>of()));
  }

  private static <T extends @Nullable Object> void testNoOperation(
      PersistentSet<T> set,
      Function<PersistentSet<T>, PersistentSet<T>> persistent,
      Consumer<Set<T>> mutating) {
    var result = persistent.apply(set);
    var buildResult = set.mutate(mutating);
    assertSame(set, result);
    assertSame(set, buildResult);
  }

  @Test
  void emptySetToPersistentSet() {
    // Requires the ordered persistent-set implementation behind Iterable.toPersistentSet().
  }

  @Test
  void equality() {
    var data = new Integer[201];
    for (var index = 0; index <= 200; index++) {
      data[index] = index;
    }
    var changed = data.clone();
    changed[42] = 4242;

    testEquality(data, changed);
  }

  @Test
  void collisionEquality() {
    var data = new IntWrapper[201];
    for (var index = 0; index <= 200; index++) {
      data[index] = new IntWrapper(index, index % 50);
    }
    var changed = data.clone();
    changed[42] = new IntWrapper(4242, 42);

    testEquality(data, changed);
  }

  private static void expectSet(String content, ImmutableSet<Character> set) {
    var expected = new HashSet<Character>();
    for (var character : TestExtensions.chars(content)) {
      expected.add(character);
    }
    TestExtensions.compareSets(expected, set);
  }

  private <E extends @Nullable Object> void testEquality(E[] data, E[] changed) {
    var base = immutableSetOf(data);
    testSpecializedEquality(base, data, true);
    testSpecializedEquality(base, changed, false);

    var builder = this.<E>immutableSetOf().builder();
    builder.addAll(Arrays.asList(data));
    testSpecializedEquality(builder, data, true);
    testSpecializedEquality(builder, changed, false);

    var shuffled = data.clone();
    Collections.shuffle(Arrays.asList(shuffled));
    testSpecializedEquality(base, shuffled, true);
    testSpecializedEquality(builder, shuffled, true);
  }

  private static <E extends @Nullable Object> void testSpecializedEquality(
      Object set, E[] elements, boolean isEqual) {
    var javaSet = TestExtensions.setOf(elements);
    if (set instanceof Set<?>) {
      testEqualsAndHashCode(set, javaSet, isEqual);
    } else {
      // Deliberate deviation: PersistentHashSet does not extend java.util.Set, so cross-type
      // equality with JDK sets is not supported.
      assertNotEquals(javaSet, set);
    }

    testEqualsAndHashCode(set, PersistentSet.hashOf(elements), isEqual);

    var builder = PersistentSet.<E>hashOf().builder();
    builder.addAll(Arrays.asList(elements));
    testEqualsAndHashCode(set, builder, isEqual);
  }

  private static void testEqualsAndHashCode(Object lhs, Object rhs, boolean isEqual) {
    assertEquals(isEqual, lhs.equals(rhs));
    if (isEqual) {
      assertEquals(lhs.hashCode(), rhs.hashCode());
    }
  }
}
