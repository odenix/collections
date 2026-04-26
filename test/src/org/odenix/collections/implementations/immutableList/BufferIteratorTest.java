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

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class BufferIteratorTest {
  @Test
  void emptyBufferIteratorTest() {
    var emptyIterator = new BufferIterator<Integer>(new Integer[0], 0, 0);

    assertFalse(emptyIterator.hasNext());
    assertThrows(NoSuchElementException.class, emptyIterator::next);
  }

  @Test
  void simpleTest() {
    var bufferIterator = new BufferIterator<>(new Integer[] {1, 2, 3, 4, 5}, 0, 5);

    for (var index = 0; index < 5; index++) {
      assertTrue(bufferIterator.hasNext());
      assertEquals(index + 1, bufferIterator.next());
    }

    assertFalse(bufferIterator.hasNext());
    assertThrows(NoSuchElementException.class, bufferIterator::next);
  }

  @Test
  void biggerThanSizeBufferTest() {
    var bufferIterator = new BufferIterator<>(new Integer[] {1, 2, 3, 4, 5}, 0, 3);

    for (var index = 0; index < 3; index++) {
      assertTrue(bufferIterator.hasNext());
      assertEquals(index + 1, bufferIterator.next());
    }

    assertFalse(bufferIterator.hasNext());
    assertThrows(NoSuchElementException.class, bufferIterator::next);
  }
}
