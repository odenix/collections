/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.odenix.collections.PersistentMap;

import tests.TraceKey;
import tests.TraceLog;
import tests.TraceSupport;

class PersistentHashMapRandomizedTest {
  private static final String TRACE_NAME = "PersistentHashMapRandomizedTest";
  private static final int OPERATIONS_PER_SEED = TraceSupport.operationCount(450);

  @ParameterizedTest
  @MethodSource("tests.TraceSupport#seedChunks")
  void randomizedOperationTracesAgainstHashMap(int[] seeds) {
    for (var seed : seeds) {
      TraceSupport.reportSeedProgress(TRACE_NAME, seed);
      var log = new TraceLog(TRACE_NAME, seed);
      try {
        runTrace(seed, log);
      } catch (AssertionError | RuntimeException failure) {
        throw log.failure(failure);
      }
    }
  }

  private static void runTrace(int seed, TraceLog log) {
    var random = new Random(seed);

    PersistentMap<TraceKey, @Nullable Integer> persistent = PersistentMap.hashOf();
    PersistentMap.Builder<TraceKey, @Nullable Integer> builder =
        PersistentMap.<TraceKey, @Nullable Integer>hashOf().builder();

    var expectedPersistent = new HashMap<TraceKey, @Nullable Integer>();
    var expectedBuilder = new HashMap<TraceKey, @Nullable Integer>();

    var persistentSnapshots = new ArrayList<PersistentMap<TraceKey, @Nullable Integer>>();
    var expectedPersistentSnapshots = new ArrayList<HashMap<TraceKey, @Nullable Integer>>();
    var builderSnapshots = new ArrayList<PersistentMap<TraceKey, @Nullable Integer>>();
    var expectedBuilderSnapshots = new ArrayList<HashMap<TraceKey, @Nullable Integer>>();

    recordSnapshots(
        persistent,
        expectedPersistent,
        persistentSnapshots,
        expectedPersistentSnapshots,
        builder.build(),
        expectedBuilder,
        builderSnapshots,
        expectedBuilderSnapshots);

    for (var ignored = 0; ignored < OPERATIONS_PER_SEED; ignored++) {
      switch (random.nextInt(16)) {
        case 0 -> {
          var key = randomKey(random);
          var value = randomValue(random);
          log.record("persistent.put(" + key + ", " + value + ")");
          expectedPersistent.put(key, value);
          persistent = persistent.put(key, value);
        }
        case 1 -> {
          var key = chooseKey(random, expectedPersistent);
          log.record("persistent.remove(" + key + ")");
          expectedPersistent.remove(key);
          persistent = persistent.remove(key);
        }
        case 2 -> {
          var key = chooseKey(random, expectedPersistent);
          var value = random.nextBoolean() && expectedPersistent.containsKey(key)
              ? expectedPersistent.get(key)
              : randomValue(random);
          log.record("persistent.remove(" + key + ", " + value + ")");
          var changed =
              expectedPersistent.containsKey(key) && Objects.equals(expectedPersistent.get(key), value);
          if (changed) {
            expectedPersistent.remove(key);
          }
          persistent = persistent.remove(key, value);
        }
        case 3 -> {
          var entries = randomBatch(random, expectedPersistent);
          log.record("persistent.putAll(" + entries + ")");
          expectedPersistent.putAll(entries);
          persistent = persistent.putAll(entries);
        }
        case 4 -> {
          var key = randomKey(random);
          var value = randomValue(random);
          log.record("builder.put(" + key + ", " + value + ")");
          assertEquals(expectedBuilder.put(key, value), builder.put(key, value));
        }
        case 5 -> {
          var key = chooseKey(random, expectedBuilder);
          log.record("builder.remove(" + key + ")");
          assertEquals(expectedBuilder.remove(key), builder.remove(key));
        }
        case 6 -> {
          var key = chooseKey(random, expectedBuilder);
          var value = random.nextBoolean() && expectedBuilder.containsKey(key)
              ? expectedBuilder.get(key)
              : randomValue(random);
          log.record("builder.remove(" + key + ", " + value + ")");
          var expectedChanged = expectedBuilder.containsKey(key) && Objects.equals(expectedBuilder.get(key), value);
          if (expectedChanged) {
            expectedBuilder.remove(key);
          }
          assertEquals(expectedChanged, builder.remove(key, value));
        }
        case 7 -> {
          var entries = randomBatch(random, expectedBuilder);
          log.record("builder.putAll(" + entries + ")");
          expectedBuilder.putAll(entries);
          builder.putAll(entries);
        }
        case 8 -> {
          var key = chooseKey(random, expectedBuilder);
          log.record("builder.keySet().remove(" + key + ")");
          var expectedChanged = expectedBuilder.containsKey(key);
          expectedBuilder.remove(key);
          //noinspection RedundantCollectionOperation
          assertEquals(expectedChanged, builder.keySet().remove(key));
        }
        case 9 -> {
          var entry = chooseEntry(random, expectedBuilder);
          log.record("builder.entrySet().remove(" + entry + ")");
          assertEquals(expectedBuilder.entrySet().remove(entry), builder.entrySet().remove(entry));
        }
        case 10 -> {
          var value = chooseValue(random, expectedBuilder);
          log.record("builder.values().remove(" + value + ")");
          assertEquals(expectedBuilder.values().remove(value), builder.values().remove(value));
        }
        case 11 -> {
          var keys = randomKeyBatch(random, expectedBuilder);
          log.record("builder.keySet().retainAll(" + keys + ")");
          assertEquals(expectedBuilder.keySet().retainAll(keys), builder.keySet().retainAll(keys));
        }
        case 12 -> {
          log.record("builder.clear()");
          expectedBuilder.clear();
          builder.clear();
        }
        case 13 -> {
          log.record("persistent = builder.build()");
          persistent = builder.build();
          expectedPersistent = new HashMap<>(expectedBuilder);
          persistentSnapshots.add(persistent);
          expectedPersistentSnapshots.add(new HashMap<>(expectedPersistent));
        }
        case 14 -> {
          log.record("builder = persistent.builder()");
          builder = persistent.builder();
          expectedBuilder = new HashMap<>(expectedPersistent);
          var builderSnapshot = builder.build();
          builderSnapshots.add(builderSnapshot);
          expectedBuilderSnapshots.add(new HashMap<>(expectedBuilder));
        }
        case 15 -> {
          log.record("persistent.clear()");
          expectedPersistent.clear();
          persistent = persistent.clear();
        }
        default -> throw new AssertionError();
      }

      assertState(expectedPersistent, persistent);
      assertState(expectedBuilder, builder);
      assertSnapshotStates(expectedPersistentSnapshots, persistentSnapshots);
      assertSnapshotStates(expectedBuilderSnapshots, builderSnapshots);

      if (random.nextInt(6) == 0) {
        recordSnapshots(
            persistent,
            expectedPersistent,
            persistentSnapshots,
            expectedPersistentSnapshots,
            builder.build(),
            expectedBuilder,
            builderSnapshots,
            expectedBuilderSnapshots);
      }
    }
  }

  private static void recordSnapshots(
      PersistentMap<TraceKey, @Nullable Integer> persistent,
      HashMap<TraceKey, @Nullable Integer> expectedPersistent,
      List<PersistentMap<TraceKey, @Nullable Integer>> persistentSnapshots,
      List<HashMap<TraceKey, @Nullable Integer>> expectedPersistentSnapshots,
      PersistentMap<TraceKey, @Nullable Integer> builderSnapshot,
      HashMap<TraceKey, @Nullable Integer> expectedBuilder,
      List<PersistentMap<TraceKey, @Nullable Integer>> builderSnapshots,
      List<HashMap<TraceKey, @Nullable Integer>> expectedBuilderSnapshots) {
    persistentSnapshots.add(persistent);
    expectedPersistentSnapshots.add(new HashMap<>(expectedPersistent));
    builderSnapshots.add(builderSnapshot);
    expectedBuilderSnapshots.add(new HashMap<>(expectedBuilder));
  }

  private static void assertState(
      HashMap<TraceKey, @Nullable Integer> expected, PersistentMap<TraceKey, @Nullable Integer> actual) {
    var actualMap = TraceSupport.toMap(actual);
    assertEquals(expected, actualMap);
    assertEquals(expected.size(), actual.size());
    for (var entry : expected.entrySet()) {
      assertEquals(entry.getValue(), actual.get(entry.getKey()));
      assertEquals(expected.containsKey(entry.getKey()), actual.containsKey(entry.getKey()));
    }
  }

  private static void assertState(
      HashMap<TraceKey, @Nullable Integer> expected, Map<TraceKey, @Nullable Integer> actual) {
    assertEquals(expected, actual);
    assertEquals(expected.size(), actual.size());
    for (var entry : expected.entrySet()) {
      assertEquals(entry.getValue(), actual.get(entry.getKey()));
      assertEquals(expected.containsKey(entry.getKey()), actual.containsKey(entry.getKey()));
    }
  }

  private static void assertSnapshotStates(
      List<HashMap<TraceKey, @Nullable Integer>> expectedSnapshots,
      List<PersistentMap<TraceKey, @Nullable Integer>> snapshots) {
    assertEquals(expectedSnapshots.size(), snapshots.size());
    for (var index = 0; index < snapshots.size(); index++) {
      assertEquals(expectedSnapshots.get(index), TraceSupport.toMap(snapshots.get(index)));
    }
  }

  private static TraceKey chooseKey(Random random, HashMap<TraceKey, @Nullable Integer> existing) {
    if (!existing.isEmpty() && random.nextDouble() < 0.7) {
      return new ArrayList<>(existing.keySet()).getFirst();
    }
    return randomKey(random);
  }

  private static Map.Entry<TraceKey, @Nullable Integer> chooseEntry(
      Random random, HashMap<TraceKey, @Nullable Integer> existing) {
    var key = chooseKey(random, existing);
    var value = existing.containsKey(key) && random.nextDouble() < 0.7
        ? existing.get(key)
        : randomValue(random);
    return new AbstractMap.SimpleImmutableEntry<>(key, value);
  }

  private static @Nullable Integer chooseValue(Random random, HashMap<TraceKey, @Nullable Integer> existing) {
    if (!existing.isEmpty() && random.nextDouble() < 0.7) {
      for (var value : existing.values()) {
        if (valueFrequency(existing, value) == 1) {
          return value;
        }
      }
    }
    return 200 + random.nextInt(200);
  }

  private static int valueFrequency(HashMap<TraceKey, @Nullable Integer> map, @Nullable Integer value) {
    var count = 0;
    for (var candidate : map.values()) {
      if (Objects.equals(candidate, value)) {
        count++;
      }
    }
    return count;
  }

  private static List<TraceKey> randomKeyBatch(Random random, HashMap<TraceKey, @Nullable Integer> existing) {
    var size = 1 + random.nextInt(5);
    var keys = new ArrayList<TraceKey>(size);
    for (var index = 0; index < size; index++) {
      keys.add(chooseKey(random, existing));
    }
    return keys;
  }

  private static Map<TraceKey, @Nullable Integer> randomBatch(
      Random random, HashMap<TraceKey, @Nullable Integer> existing) {
    var size = 1 + random.nextInt(5);
    var entries = new HashMap<TraceKey, @Nullable Integer>(size);
    for (var index = 0; index < size; index++) {
      var key = !existing.isEmpty() && random.nextDouble() < 0.4
          ? new ArrayList<>(existing.keySet()).getFirst()
          : randomKey(random);
      entries.put(key, randomValue(random));
    }
    return entries;
  }

  private static TraceKey randomKey(Random random) {
    var value = random.nextInt(500);
    return new TraceKey(value, hashForValue(value));
  }

  private static @Nullable Integer randomValue(Random random) {
    return random.nextInt(5) == 0 ? null : random.nextInt(200);
  }

  private static int hashForValue(int value) {
    return switch (Math.floorMod(value, 10)) {
      case 0, 1, 2 -> 0;
      case 3, 4, 5 -> 13 | ((value & 31) << 5);
      case 6, 7 -> 13 | (7 << 5) | ((value & 31) << 10);
      default -> Integer.rotateLeft(value * 0x9E3779B9, 7);
    };
  }
}
