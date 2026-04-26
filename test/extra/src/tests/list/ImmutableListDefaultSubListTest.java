/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.ImmutableList;
import org.odenix.collections.internal.ListImplementation;

class ImmutableListDefaultSubListTest {

  @Test
  void subListEqualsAndHashCode() {
    var list = testList('a', 'b', 'c', 'x', 'a', 'x', 'y', 'z', '1', '2');
    var subList = list.subList(2, 5);
    var expected = testList('c', 'x', 'a');

    assertEquals(expected, subList);
    assertEquals(expected.hashCode(), subList.hashCode());
  }

  @Test
  void subListGetAndIndexes() {
    var list = testList('a', 'b', 'c', 'x', 'a', 'x', 'y', 'z', '1', '2');
    var subList = list.subList(2, 8);

    assertEquals('c', subList.get(0));
    assertEquals('x', subList.get(1));
    assertEquals('z', subList.get(5));
    assertThrows(IndexOutOfBoundsException.class, () -> subList.get(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> subList.get(subList.size()));

    assertEquals(1, subList.indexOf('x'));
    assertEquals(3, subList.lastIndexOf('x'));
    assertEquals(-1, subList.indexOf('q'));
    assertEquals(-1, subList.lastIndexOf('q'));
  }

  @Test
  void subListIterator() {
    var list = testList('a', 'b', 'c', 'x', 'a', 'x', 'y', 'z', '1', '2');
    var subList = list.subList(2, 5);
    var iterator = subList.listIterator(1);

    assertTrue(iterator.hasNext());
    assertTrue(iterator.hasPrevious());
    assertEquals(1, iterator.nextIndex());
    assertEquals(0, iterator.previousIndex());

    assertEquals('x', iterator.next());
    assertTrue(iterator.hasNext());
    assertTrue(iterator.hasPrevious());
    assertEquals(2, iterator.nextIndex());
    assertEquals(1, iterator.previousIndex());

    assertEquals('x', iterator.previous());
    assertEquals('x', iterator.next());
    assertEquals('a', iterator.next());
    assertFalse(iterator.hasNext());
    assertTrue(iterator.hasPrevious());
    assertThrows(NoSuchElementException.class, iterator::next);

    assertEquals('a', iterator.previous());
    assertEquals('x', iterator.previous());
    assertEquals('c', iterator.previous());
    assertFalse(iterator.hasPrevious());
    assertThrows(NoSuchElementException.class, iterator::previous);

    assertThrows(IndexOutOfBoundsException.class, () -> subList.listIterator(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> subList.listIterator(subList.size() + 1));
  }

  @Test
  void subListToStringAndFullRangeSubList() {
    var list = testList('a', 'b', 'c', 'x', 'a', 'x', 'y', 'z', '1', '2');
    var subList = list.subList(2, 5);

    assertEquals("[c, x, a]", subList.toString());
    assertEquals(subList, subList.subList(0, subList.size()));
  }

  @SafeVarargs
  private static <E extends @Nullable Object> ImmutableList<E> testList(E... elements) {
    return new TestImmutableList<>(Arrays.asList(elements));
  }

  @SuppressWarnings("ClassCanBeRecord")
  private static final class TestImmutableList<E extends @Nullable Object> implements ImmutableList<E> {
    private final List<E> elements;

    private TestImmutableList(List<E> elements) {
      this.elements = elements;
    }

    @Override
    public E get(int index) {
      return elements.get(index);
    }

    @Override
    public int size() {
      return elements.size();
    }

    @Override
    public boolean contains(E element) {
      return elements.contains(element);
    }

    @Override
    public boolean containsAll(Collection<? extends E> elements) {
      return this.elements.containsAll(elements);
    }

    @Override
    public Iterator<E> iterator() {
      return elements.iterator();
    }

    @Override
    public int indexOf(E element) {
      return elements.indexOf(element);
    }

    @Override
    public int lastIndexOf(E element) {
      return elements.lastIndexOf(element);
    }

    @Override
    public ListIterator<E> listIterator() {
      return elements.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
      return elements.listIterator(index);
    }

    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      return ImmutableList.super.subList(fromIndex, toIndex);
    }

    @Override
    public int hashCode() {
      return ListImplementation.orderedHashCode(this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return ListImplementation.orderedEquals(this, other);
    }
  }
}
