/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import org.odenix.collections.ImmutableMap;

public final class TraceSupport {
  private TraceSupport() {}

  private static final int[] DEFAULT_SEEDS = {0, 1, 2, 3, 7, 11, 17, 42};
  private static final int DEFAULT_CHUNK_SIZE = 100;

  public static int[] traceSeeds() {
    var seedCount = Integer.getInteger("tests.randomized.seedCount");
    if (seedCount == null) {
      return DEFAULT_SEEDS;
    }
    var startSeed = Integer.getInteger("tests.randomized.startSeed", 0);
    return IntStream.range(startSeed, startSeed + seedCount).toArray();
  }

  public static Stream<Arguments> seedChunks() {
    var seeds = traceSeeds();
    var chunkSize = Math.max(1, intProperty("tests.randomized.chunkSize", DEFAULT_CHUNK_SIZE));
    return IntStream
        .iterate(0, index -> index < seeds.length, index -> index + chunkSize)
        .mapToObj(index -> Arguments.of((Object) Arrays.copyOfRange(
            seeds,
            index,
            Math.min(index + chunkSize, seeds.length))));
  }

  public static int operationCount(int defaultCount) {
    return intProperty("tests.randomized.operations", defaultCount);
  }

  public static void reportSeedProgress(String traceName, int seed) {
    var progressEvery = intProperty("tests.randomized.progressEvery", 0);
    if (progressEvery > 0 && Math.floorMod(seed, progressEvery) == 0) {
      System.out.println(traceName + " seed " + seed);
    }
  }

  private static int intProperty(String name, int defaultValue) {
    var configuredValue = System.getProperty(name);
    if (configuredValue == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(configuredValue);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }

  public static <E> Set<E> toSet(Iterable<E> elements) {
    var result = new HashSet<E>();
    for (var element : elements) {
      result.add(element);
    }
    return result;
  }

  public static <E> List<E> pickElements(Set<E> elements, int count) {
    var picked = new ArrayList<E>(count);
    for (var element : elements) {
      picked.add(element);
      if (picked.size() == count) {
        break;
      }
    }
    return picked;
  }

  public static <K, V> Map<K, V> toMap(ImmutableMap<K, V> map) {
    var result = new HashMap<K, V>();
    for (var entry : map.entries()) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  public static <E> List<E> toList(Iterable<E> elements) {
    var result = new ArrayList<E>();
    for (var element : elements) {
      result.add(element);
    }
    return result;
  }

  public static <K, V> LinkedHashMap<K, V> toLinkedHashMap(ImmutableMap<K, V> map) {
    var result = new LinkedHashMap<K, V>();
    for (var entry : map.entries()) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  public static <E> LinkedHashSet<E> toLinkedHashSet(Iterable<E> elements) {
    var result = new LinkedHashSet<E>();
    for (var element : elements) {
      result.add(element);
    }
    return result;
  }
}
