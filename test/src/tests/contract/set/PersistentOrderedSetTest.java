/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentSet;

class PersistentOrderedSetTest {
  /**
   * Test from issue: https://github.com/Kotlin/kotlinx.collections.immutable/issues/204
   */
  @Test
  void persistentOrderedSetAndTheirBuilderShouldBeEqualBeforeAndAfterModification() {
    var set1 = PersistentSet.of(-486539264, 16777216, 0, 67108864);
    var builder = set1.builder();

    assertTrue(set1.equals(builder));
    assertEquals(set1, builder.build());
    assertEquals(TestExtensions.toSet(set1), TestExtensions.toSet(builder.build()));

    var set2 = set1.remove(0);
    builder.remove(0);

    assertEquals(TestExtensions.toSet(set2), TestExtensions.toSet(builder.build()));
    assertEquals(set2, builder.build());
  }
}
