/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Random;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TrieIteratorTest {
  @Test
  void emptyIteratorTest() {
    var emptyIterator = new TrieIterator<Integer>(new Object[0], 0, 0, 1);

    assertFalse(emptyIterator.hasNext());
    assertThrows(NoSuchElementException.class, emptyIterator::next);
  }

  private static @Nullable Object[] makeRoot(int height, int leafCount) {
    var leaves = new ArrayList<@Nullable Object>();
    for (var index = 0; index < leafCount * Utils.MAX_BUFFER_SIZE; index++) {
      leaves.add(index);
    }

    for (var level = 0; level < height; level++) {
      var newLeaves = new ArrayList<@Nullable Object>();
      for (var i = 0; i < leaves.size(); i += Utils.MAX_BUFFER_SIZE) {
        var buffer = new @Nullable Object[Utils.MAX_BUFFER_SIZE];
        for (var j = i; j < Math.min(leaves.size(), i + Utils.MAX_BUFFER_SIZE); j++) {
          buffer[j - i] = leaves.get(j);
        }
        newLeaves.add(buffer);
      }
      leaves = newLeaves;
    }

    assert leaves.size() == 1;
    return (@Nullable Object[]) leaves.getFirst();
  }

  @Test
  void simpleTest() {
    var random = new Random(0);
    for (var height = 1; height <= 4; height++) {
      var maxCount = (int) Math.pow(Utils.MAX_BUFFER_SIZE, height - 1);
      var minCount = maxCount / 32 + 1;
      var leafCounts = new ArrayList<Integer>();
      leafCounts.add(minCount);
      leafCounts.add(maxCount);
      for (var index = 0; index < 10; index++) {
        leafCounts.add(random.nextInt(minCount, maxCount + 1));
      }
      leafCounts = new ArrayList<>(leafCounts.stream().distinct().toList());
      Collections.sort(leafCounts);

      for (var leafCount : leafCounts) {
        var root = makeRoot(height, leafCount);
        var size = leafCount * Utils.MAX_BUFFER_SIZE;

        var iterator = new TrieIterator<Integer>(root, 0, size, height);
        for (var index = 0; index < size; index++) {
          assertTrue(iterator.hasNext());
          assertEquals(index, iterator.next());
        }

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
      }
    }
  }
}
