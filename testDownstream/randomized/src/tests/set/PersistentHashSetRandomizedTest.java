/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.odenix.collections.PersistentSet;

import tests.TraceKey;
import tests.TraceLog;
import tests.TraceSupport;

class PersistentHashSetRandomizedTest {
  private static final String TRACE_NAME = "PersistentHashSetRandomizedTest";
  private static final int OPERATIONS_PER_SEED = TraceSupport.operationCount(400);

  @ParameterizedTest
  @MethodSource("tests.TraceSupport#seedChunks")
  void randomizedOperationTracesAgainstHashSet(int[] seeds) {
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

    PersistentSet<TraceKey> persistent = PersistentSet.hashOf();
    PersistentSet.Builder<TraceKey> builder = PersistentSet.<TraceKey>hashOf().builder();

    var expectedPersistent = new HashSet<TraceKey>();
    var expectedBuilder = new HashSet<TraceKey>();

    var persistentSnapshots = new ArrayList<PersistentSet<TraceKey>>();
    var expectedPersistentSnapshots = new ArrayList<HashSet<TraceKey>>();
    var builderSnapshots = new ArrayList<PersistentSet<TraceKey>>();
    var expectedBuilderSnapshots = new ArrayList<HashSet<TraceKey>>();

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
      switch (random.nextInt(14)) {
        case 0 -> {
          var element = randomKey(random);
          log.record("persistent.add(" + element + ")");
          var changed = expectedPersistent.add(element);
          var nextPersistent = persistent.add(element);
          assertEquals(changed, nextPersistent != persistent);
          persistent = nextPersistent;
        }
        case 1 -> {
          var element = chooseRemovalTarget(random, expectedPersistent);
          log.record("persistent.remove(" + element + ")");
          var changed = expectedPersistent.remove(element);
          var nextPersistent = persistent.remove(element);
          assertEquals(changed, nextPersistent != persistent);
          persistent = nextPersistent;
        }
        case 2 -> {
          var elements = randomBatch(random, expectedPersistent);
          log.record("persistent.addAll(" + elements + ")");
          var expectedChanged = expectedPersistent.addAll(elements);
          var nextPersistent = persistent.addAll(elements);
          assertEquals(expectedChanged, nextPersistent != persistent);
          persistent = nextPersistent;
        }
        case 3 -> {
          var elements = randomBatch(random, expectedPersistent);
          log.record("persistent.removeAll(" + elements + ")");
          var expectedSize = expectedPersistent.size();
          elements.forEach(expectedPersistent::remove);
          var expectedChanged = expectedSize != expectedPersistent.size();
          var nextPersistent = persistent.removeAll(elements);
          assertEquals(expectedChanged, nextPersistent != persistent);
          persistent = nextPersistent;
        }
        case 4 -> {
          var element = randomKey(random);
          log.record("builder.add(" + element + ")");
          var changed = expectedBuilder.add(element);
          assertEquals(changed, builder.add(element));
        }
        case 5 -> {
          var element = chooseRemovalTarget(random, expectedBuilder);
          log.record("builder.remove(" + element + ")");
          var changed = expectedBuilder.remove(element);
          assertEquals(changed, builder.remove(element));
        }
        case 6 -> {
          var elements = randomBatch(random, expectedBuilder);
          log.record("builder.addAll(" + elements + ")");
          var expectedChanged = expectedBuilder.addAll(elements);
          assertEquals(expectedChanged, builder.addAll(elements));
        }
        case 7 -> {
          var elements = randomBatch(random, expectedBuilder);
          log.record("builder.removeAll(" + elements + ")");
          var expectedSize = expectedBuilder.size();
          elements.forEach(expectedBuilder::remove);
          var expectedChanged = expectedSize != expectedBuilder.size();
          //noinspection SlowAbstractSetRemoveAll
          assertEquals(expectedChanged, builder.removeAll(elements));
        }
        case 8 -> {
          var elements = randomBatch(random, expectedPersistent);
          log.record("persistent.retainAll(" + elements + ")");
          var expectedChanged = expectedPersistent.retainAll(elements);
          var nextPersistent = persistent.retainAll(elements);
          assertEquals(expectedChanged, nextPersistent != persistent);
          persistent = nextPersistent;
        }
        case 9 -> {
          var elements = randomBatch(random, expectedBuilder);
          log.record("builder.retainAll(" + elements + ")");
          var expectedChanged = expectedBuilder.retainAll(elements);
          assertEquals(expectedChanged, builder.retainAll(elements));
        }
        case 10 -> {
          var remainder = random.nextInt(5);
          log.record("builder.removeIf(value % 5 == " + remainder + ")");
          var expectedChanged = expectedBuilder.removeIf(element -> Math.floorMod(element.value(), 5) == remainder);
          assertEquals(expectedChanged, builder.removeIf(element -> Math.floorMod(element.value(), 5) == remainder));
        }
        case 11 -> {
          log.record("builder.clear()");
          expectedBuilder.clear();
          builder.clear();
        }
        case 12 -> {
          log.record("persistent = builder.build()");
          persistent = builder.build();
          expectedPersistent = new HashSet<>(expectedBuilder);
          persistentSnapshots.add(persistent);
          expectedPersistentSnapshots.add(new HashSet<>(expectedPersistent));
        }
        case 13 -> {
          log.record("builder = persistent.builder()");
          builder = persistent.builder();
          expectedBuilder = new HashSet<>(expectedPersistent);
          var builderSnapshot = builder.build();
          builderSnapshots.add(builderSnapshot);
          expectedBuilderSnapshots.add(new HashSet<>(expectedBuilder));
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
      PersistentSet<TraceKey> persistent,
      HashSet<TraceKey> expectedPersistent,
      List<PersistentSet<TraceKey>> persistentSnapshots,
      List<HashSet<TraceKey>> expectedPersistentSnapshots,
      PersistentSet<TraceKey> builderSnapshot,
      HashSet<TraceKey> expectedBuilder,
      List<PersistentSet<TraceKey>> builderSnapshots,
      List<HashSet<TraceKey>> expectedBuilderSnapshots) {
    persistentSnapshots.add(persistent);
    expectedPersistentSnapshots.add(new HashSet<>(expectedPersistent));
    builderSnapshots.add(builderSnapshot);
    expectedBuilderSnapshots.add(new HashSet<>(expectedBuilder));
  }

  private static void assertState(HashSet<TraceKey> expected, Iterable<TraceKey> actual) {
    var actualSet = TraceSupport.toSet(actual);
    assertEquals(expected, actualSet);
    assertEquals(expected.size(), actualSet.size());

    for (var element : expected) {
      assertTrue(actualSet.contains(element));
    }
  }

  private static void assertSnapshotStates(
      List<HashSet<TraceKey>> expectedSnapshots, List<PersistentSet<TraceKey>> snapshots) {
    assertEquals(expectedSnapshots.size(), snapshots.size());
    for (var index = 0; index < snapshots.size(); index++) {
      assertEquals(expectedSnapshots.get(index), TraceSupport.toSet(snapshots.get(index)));
    }
  }

  private static TraceKey chooseRemovalTarget(Random random, HashSet<TraceKey> existing) {
    if (!existing.isEmpty() && random.nextDouble() < 0.7) {
      return TraceSupport.pickElements(existing, 1).getFirst();
    }
    return randomKey(random);
  }

  private static List<TraceKey> randomBatch(Random random, HashSet<TraceKey> existing) {
    var size = 1 + random.nextInt(5);
    var batch = new ArrayList<TraceKey>(size);
    for (var index = 0; index < size; index++) {
      if (!existing.isEmpty() && random.nextDouble() < 0.4) {
        batch.add(TraceSupport.pickElements(existing, 1).getFirst());
      } else {
        batch.add(randomKey(random));
      }
    }
    return batch;
  }

  private static TraceKey randomKey(Random random) {
    var value = random.nextInt(500);
    return new TraceKey(value, hashForValue(value));
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
