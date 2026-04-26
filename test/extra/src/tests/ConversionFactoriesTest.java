/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.odenix.collections.ImmutableList;
import org.odenix.collections.ImmutableSet;
import org.odenix.collections.PersistentList;
import org.odenix.collections.PersistentSet;

class ConversionFactoriesTest {
  @Test
  void arrayAndStreamConversionsMirrorUpstreamExtensions() {
    assertEquals(PersistentList.of("a", "b"), ImmutableList.from(new String[] {"a", "b"}));
    assertEquals(PersistentList.of("a", "b"), PersistentList.from(Stream.of("a", "b")));

    assertEquals(PersistentSet.of("a", "b"), ImmutableSet.from(new String[] {"a", "b"}));
    assertEquals(PersistentSet.of("a", "b"), PersistentSet.from(Stream.of("a", "b")));
    assertEquals(PersistentSet.hashOf("a", "b"), PersistentSet.hashFrom(Stream.of("a", "b")));
  }

  @Test
  void charSequenceConversionsUseCharsAsElements() {
    assertEquals(PersistentList.of('a', 'b', 'a'), ImmutableList.from("aba"));
    assertEquals(PersistentSet.of('a', 'b'), ImmutableSet.from("aba"));
    assertEquals(PersistentSet.hashOf('a', 'b'), PersistentSet.hashFrom("aba"));
  }

  @Test
  void immutableCollectionsExposeStreamsForTransformations() {
    var list = PersistentList.of("a", "bb", "ccc");
    assertEquals(PersistentList.of(1, 2, 3), PersistentList.from(list.stream().map(String::length)));
    assertEquals(PersistentList.of(1, 2, 3), PersistentList.from(list.parallelStream().map(String::length)));

    var set = PersistentSet.of("a", "bb", "ccc");
    assertEquals(PersistentSet.of(1, 2, 3), PersistentSet.from(set.stream().map(String::length)));
    assertEquals(PersistentSet.of(1, 2, 3), PersistentSet.from(set.parallelStream().map(String::length)));
  }
}
