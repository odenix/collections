/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.list;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentList;

class PersistentVectorTest {

  @Test
  void firstTrieVectorReadBehavior() {
    var list = PersistentList.<Integer>of();
    for (var i = 0; i < 33; i++) {
      list = list.add(i);
    }

    assertEquals(33, list.size());
    assertEquals(0, list.get(0));
    assertEquals(31, list.get(31));
    assertEquals(32, list.get(32));
    assertEquals(31, list.indexOf(31));
    assertEquals(32, list.lastIndexOf(32));
    assertEquals(List.of(30, 31, 32), TestExtensions.toList(list.subList(30, 33)));
  }

  @Test
  void trieVectorAppendAndReplaceElements() {
    var list = PersistentList.<Integer>of();
    for (var i = 0; i < 65; i++) {
      list = list.add(i);
    }

    assertEquals(65, list.size());
    assertEquals(List.of(30, 31, 32, 33, 34), TestExtensions.toList(list.subList(30, 35)));

    list = list.set(0, -1);
    list = list.set(31, -31);
    list = list.set(32, -32);
    list = list.set(64, -64);

    assertEquals(-1, list.get(0));
    assertEquals(-31, list.get(31));
    assertEquals(-32, list.get(32));
    assertEquals(-64, list.get(64));
  }

  @Test
  void trieVectorBuilderAppendAndReplaceElements() {
    var list = PersistentList.<Integer>of();
    for (var i = 0; i < 64; i++) {
      list = list.add(i);
    }

    var builder = list.builder();
    builder.add(64);
    builder.set(0, -1);
    builder.set(31, -31);
    builder.set(32, -32);
    builder.set(64, -64);

    var built = builder.build();
    assertEquals(65, built.size());
    assertEquals(-1, built.get(0));
    assertEquals(-31, built.get(31));
    assertEquals(-32, built.get(32));
    assertEquals(-64, built.get(64));
  }

  @Test
  void trieVectorRemoveElements() {
    var list = PersistentList.<Integer>of();
    for (var i = 0; i < 65; i++) {
      list = list.add(i);
    }

    assertEquals(List.of(0, 1, 2, 3, 4), TestExtensions.toList(list.removeAt(64).subList(0, 5)));
    assertEquals(64, list.removeAt(64).size());
    assertEquals(63, list.removeAt(64).get(63));

    var withoutFirst = list.removeAt(0);
    assertEquals(64, withoutFirst.size());
    assertEquals(1, withoutFirst.get(0));
    assertEquals(64, withoutFirst.get(63));

    var withoutMiddle = list.removeAt(32);
    assertEquals(64, withoutMiddle.size());
    assertEquals(31, withoutMiddle.get(31));
    assertEquals(33, withoutMiddle.get(32));

    var withoutEvens = list.removeAll(value -> value % 2 == 0);
    assertEquals(32, withoutEvens.size());
    assertEquals(1, withoutEvens.get(0));
    assertEquals(63, withoutEvens.get(31));
  }

  @Test
  void trieVectorBuilderRemoveElements() {
    var list = PersistentList.<Integer>of();
    for (var i = 0; i < 65; i++) {
      list = list.add(i);
    }

    var builder = list.builder();
    builder.remove(64);
    builder.remove(0);
    builder.remove(Integer.valueOf(32));

    var built = builder.build();
    assertEquals(62, built.size());
    assertEquals(1, built.get(0));
    assertEquals(33, built.get(31));
    assertEquals(63, built.get(61));
  }
}
