/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentMap;

class PersistentOrderedMapTest {
  /**
   * Test from issue: https://github.com/Kotlin/kotlinx.collections.immutable/issues/198
   */
  @Test
  void whenRemovingMultipleKeysWithIdenticalHashcodesTheRemainingKeyShouldBeCorrectlyPromoted() {
    class ChosenHashCode {
      private final int hashCode;
      private final String name;

      ChosenHashCode(int hashCode, String name) {
        this.hashCode = hashCode;
        this.name = name;
      }

      @Override
      public boolean equals(@Nullable Object other) {
        return other instanceof ChosenHashCode chosenHashCode && chosenHashCode.name.equals(name);
      }

      @Override
      public int hashCode() {
        return hashCode;
      }

      @Override
      public String toString() {
        return name;
      }
    }

    var a = new ChosenHashCode(123, "A");
    var b = new ChosenHashCode(123, "B");
    var c = new ChosenHashCode(123, "C");

    var abc =
        PersistentMap.of(
            TestExtensions.entry(a, "x"), TestExtensions.entry(b, "y"), TestExtensions.entry(c, "z"));

    var minusAb = abc.removeAll(new ChosenHashCode[] {a, b});
    var cOnly = PersistentMap.of(TestExtensions.entry(c, "z"));

    assertEquals(cOnly.entries(), minusAb.entries());
    assertEquals(cOnly, minusAb);
  }
}
