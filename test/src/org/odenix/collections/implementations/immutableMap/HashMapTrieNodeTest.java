/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tests.IntWrapper;

class HashMapTrieNodeTest {
  private static void testEmptyMap(PersistentHashMap<IntWrapper, Integer> map) {
    map.node.accept(
        (node, shift, hash, dataMap, nodeMap) -> {
          assertEquals(0, shift);
          assertEquals(0, hash);
          assertEquals(0, dataMap);
          assertEquals(0, nodeMap);
          assertEquals(0, node.buffer.length);
        });
  }

  @Test
  void addSingle() {
    var map = PersistentHashMap.<IntWrapper, Integer>emptyOf();

    testEmptyMap(map);

    var wrapper1 = new IntWrapper(1, 0b100_01101);
    map = map.put(wrapper1, 1);

    map.node.accept(
        (node, shift, hash, dataMap, nodeMap) -> {
          assertEquals(0, shift);
          assertEquals(0, hash);
          assertEquals(1 << 0b01101, dataMap);
          assertEquals(0b0, nodeMap);
          assertArrayEquals(new Object[] {wrapper1, 1}, node.buffer);
        });

    map = map.remove(wrapper1);

    testEmptyMap(map);
  }

  @Test
  void canonicalization() {
    var wrapper1 = new IntWrapper(1, 0b1);
    var wrapper33 = new IntWrapper(33, 0b1_00001);
    var map = PersistentHashMap.<IntWrapper, Integer>emptyOf().put(wrapper1, 1).put(wrapper33, 33);

    map.node.accept(
        (node, shift, hash, dataMap, nodeMap) -> {
          if (shift == 0) {
            assertEquals(0b0, dataMap);
            assertEquals(0b10, nodeMap);
          } else {
            assertEquals(1, hash);
            assertEquals(TrieNode.LOG_MAX_BRANCHING_FACTOR, shift);
            assertEquals(0b11, dataMap);
            assertEquals(0b0, nodeMap);
            assertArrayEquals(new Object[] {wrapper1, 1, wrapper33, 33}, node.buffer);
          }
        });

    map.remove(wrapper1)
        .node
        .accept(
            (node, shift, hash, dataMap, nodeMap) -> {
              assertEquals(0, shift);
              assertEquals(0, hash);
              assertEquals(0b10, dataMap);
              assertEquals(0b0, nodeMap);
              assertArrayEquals(new Object[] {wrapper33, 33}, node.buffer);
            });

    testEmptyMap(map.remove(wrapper1).remove(wrapper33));
  }

  @Test
  void canonicalization1() {
    var wrapper1 = new IntWrapper(1, 0b1);
    var wrapper33 = new IntWrapper(33, 0b1_00001);
    var wrapper1057 = new IntWrapper(1057, 0b1_00001_00001);
    var map =
        PersistentHashMap.<IntWrapper, Integer>emptyOf()
            .put(wrapper1, 1)
            .put(wrapper33, 33)
            .put(wrapper1057, 1057);

    map.node.accept(
        (node, shift, hash, dataMap, nodeMap) -> {
          if (shift == 0) {
            assertEquals(0b0, dataMap);
            assertEquals(0b10, nodeMap);
          } else if (shift == TrieNode.LOG_MAX_BRANCHING_FACTOR) {
            assertEquals(1, hash);
            assertEquals(0b1, dataMap);
            assertEquals(0b10, nodeMap);
            assertArrayEquals(new Object[] {wrapper1, 1}, Arrays.copyOf(node.buffer, 2));
          } else {
            assertEquals(33, hash);
            assertEquals(2 * TrieNode.LOG_MAX_BRANCHING_FACTOR, shift);
            assertEquals(0b11, dataMap);
            assertEquals(0b0, nodeMap);
            assertArrayEquals(new Object[] {wrapper33, 33, wrapper1057, 1057}, node.buffer);
          }
        });

    map.remove(wrapper1057)
        .node
        .accept(
            (node, shift, hash, dataMap, nodeMap) -> {
              if (shift == 0) {
                assertEquals(0b0, dataMap);
                assertEquals(0b10, nodeMap);
              } else {
                assertEquals(1, hash);
                assertEquals(TrieNode.LOG_MAX_BRANCHING_FACTOR, shift);
                assertEquals(0b11, dataMap);
                assertEquals(0b0, nodeMap);
                assertArrayEquals(new Object[] {wrapper1, 1, wrapper33, 33}, node.buffer);
              }
            });
  }

  @Test
  void canonicalization2() {
    var wrapper1 = new IntWrapper(1, 0b1);
    var wrapper33 = new IntWrapper(33, 0b1_00001);
    var wrapper1057 = new IntWrapper(1057, 0b1_00001_00001);
    var map =
        PersistentHashMap.<IntWrapper, Integer>emptyOf()
            .put(wrapper1, 1)
            .put(wrapper33, 33)
            .put(wrapper1057, 1057);

    map.remove(wrapper1)
        .node
        .accept(
            (node, shift, hash, dataMap, nodeMap) -> {
              if (shift == 0) {
                assertEquals(0b0, dataMap);
                assertEquals(0b10, nodeMap);
              } else if (shift == TrieNode.LOG_MAX_BRANCHING_FACTOR) {
                assertEquals(1, hash);
                assertEquals(0b0, dataMap);
                assertEquals(0b10, nodeMap);
              } else {
                assertEquals(33, hash);
                assertEquals(2 * TrieNode.LOG_MAX_BRANCHING_FACTOR, shift);
                assertEquals(0b11, dataMap);
                assertEquals(0b0, nodeMap);
                assertArrayEquals(new Object[] {wrapper33, 33, wrapper1057, 1057}, node.buffer);
              }
            });

    map.remove(wrapper1)
        .remove(wrapper1057)
        .node
        .accept(
            (node, shift, hash, dataMap, nodeMap) -> {
              assertEquals(0, shift);
              assertEquals(0, hash);
              assertEquals(0b10, dataMap);
              assertEquals(0b0, nodeMap);
              assertArrayEquals(new Object[] {wrapper33, 33}, node.buffer);
            });

    testEmptyMap(map.remove(wrapper1).remove(wrapper1057).remove(wrapper33));
  }

  @Test
  void collision() {
    var wrapper1 = new IntWrapper(1, 0b1);
    var wrapper2 = new IntWrapper(2, 0b1);
    var map = PersistentHashMap.<IntWrapper, Integer>emptyOf().put(wrapper1, 1).put(wrapper2, 2);

    map.node.accept(
        (node, shift, hash, dataMap, nodeMap) -> {
          if (shift > TrieNode.MAX_SHIFT) {
            assertEquals(1, hash);
            assertEquals(0b0, dataMap);
            assertEquals(0b0, nodeMap);
            assertEquals(Arrays.asList(wrapper1, 1, wrapper2, 2), Arrays.asList(node.buffer));
          } else {
            assertEquals(0b0, dataMap);
            var mask = shift == 0 ? 0b10 : 0b1;
            assertEquals(mask, nodeMap);
          }
        });

    map.remove(wrapper1)
        .node
        .accept(
            (node, shift, hash, dataMap, nodeMap) -> {
              assertEquals(0, shift);
              assertEquals(0, hash);
              assertEquals(0b10, dataMap);
              assertEquals(0b0, nodeMap);
              assertArrayEquals(new Object[] {wrapper2, 2}, node.buffer);
            });

    testEmptyMap(map.remove(wrapper1).remove(wrapper2));
  }

  @Test
  void collision1() {
    var wrapper1 = new IntWrapper(1, 0b1);
    var wrapper2 = new IntWrapper(2, 0b1);
    var wrapper3 = new IntWrapper(3, 0b1_00001);
    var map =
        PersistentHashMap.<IntWrapper, Integer>emptyOf()
            .put(wrapper1, 1)
            .put(wrapper2, 2)
            .put(wrapper3, 3);

    map.node.accept(
        (node, shift, hash, dataMap, nodeMap) -> {
          if (shift == 0) {
            assertEquals(0, hash);
            assertEquals(0b0, dataMap);
            assertEquals(0b10, nodeMap);
          } else if (shift == TrieNode.LOG_MAX_BRANCHING_FACTOR) {
            assertEquals(1, hash);
            assertEquals(0b10, dataMap);
            assertEquals(0b1, nodeMap);
            assertArrayEquals(new Object[] {wrapper3, 3}, Arrays.copyOf(node.buffer, 2));
          } else if (shift <= TrieNode.MAX_SHIFT) {
            assertEquals(1, hash);
            assertEquals(0b0, dataMap);
            assertEquals(0b1, nodeMap);
          } else {
            assertEquals(1, hash);
            assertEquals(0b0, dataMap);
            assertEquals(0b0, nodeMap);
            assertEquals(Arrays.asList(wrapper1, 1, wrapper2, 2), Arrays.asList(node.buffer));
          }
        });

    map.remove(wrapper1)
        .node
        .accept(
            (node, shift, hash, dataMap, nodeMap) -> {
              if (shift == 0) {
                assertEquals(0b0, dataMap);
                assertEquals(0b10, nodeMap);
              } else {
                assertEquals(TrieNode.LOG_MAX_BRANCHING_FACTOR, shift);
                assertEquals(1, hash);
                assertEquals(0b11, dataMap);
                assertEquals(0b0, nodeMap);
                assertArrayEquals(new Object[] {wrapper2, 2, wrapper3, 3}, node.buffer);
              }
            });
  }

  @Test
  void collision2() {
    var wrapper1 = new IntWrapper(1, 0b1);
    var wrapper2 = new IntWrapper(2, 0b1);
    var wrapper3 = new IntWrapper(3, 0b1_00001);
    var map =
        PersistentHashMap.<IntWrapper, Integer>emptyOf()
            .put(wrapper1, 1)
            .put(wrapper2, 2)
            .put(wrapper3, 3);

    map.remove(wrapper3)
        .node
        .accept(
            (node, shift, hash, dataMap, nodeMap) -> {
              if (shift <= TrieNode.MAX_SHIFT) {
                var code = shift == 0 ? 0 : 1;
                assertEquals(code, hash);
                assertEquals(0b0, dataMap);
                var mask = shift == 0 ? 0b10 : 0b1;
                assertEquals(mask, nodeMap);
              } else {
                assertEquals(1, hash);
                assertEquals(0b0, dataMap);
                assertEquals(0b0, nodeMap);
                assertArrayEquals(new Object[] {wrapper1, 1, wrapper2, 2}, node.buffer);
              }
            });

    map.remove(wrapper3)
        .remove(wrapper1)
        .node
        .accept(
            (node, shift, hash, dataMap, nodeMap) -> {
              assertEquals(0, shift);
              assertEquals(0, hash);
              assertEquals(0b10, dataMap);
              assertEquals(0b0, nodeMap);
              assertArrayEquals(new Object[] {wrapper2, 2}, node.buffer);
            });
  }
}
