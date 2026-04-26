/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.odenix.collections.PersistentSet;

import tests.TraceKey;
import tests.TraceLog;
import tests.TraceSupport;

class PersistentOrderedSetRandomizedTest {
  private static final String TRACE_NAME = "PersistentOrderedSetRandomizedTest";
  private static final int OPERATIONS_PER_SEED = TraceSupport.operationCount(400);

  @ParameterizedTest
  @MethodSource("tests.TraceSupport#seedChunks")
  void randomizedOperationTracesAgainstLinkedHashSet(int[] seeds) {
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

    PersistentSet<TraceKey> persistent = PersistentSet.of();
    PersistentSet.Builder<TraceKey> builder = PersistentSet.<TraceKey>of().builder();

    var expectedPersistent = new LinkedHashSet<TraceKey>();
    var expectedBuilder = new LinkedHashSet<TraceKey>();

    var persistentSnapshots = new ArrayList<PersistentSet<TraceKey>>();
    var expectedPersistentSnapshots = new ArrayList<LinkedHashSet<TraceKey>>();
    var builderSnapshots = new ArrayList<PersistentSet<TraceKey>>();
    var expectedBuilderSnapshots = new ArrayList<LinkedHashSet<TraceKey>>();

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
          expectedPersistent.add(element);
          persistent = persistent.add(element);
        }
        case 1 -> {
          var element = chooseElement(random, expectedPersistent);
          log.record("persistent.remove(" + element + ")");
          expectedPersistent.remove(element);
          persistent = persistent.remove(element);
        }
        case 2 -> {
          var elements = randomBatch(random, expectedPersistent);
          log.record("persistent.addAll(" + elements + ")");
          expectedPersistent.addAll(elements);
          persistent = persistent.addAll(elements);
        }
        case 3 -> {
          var elements = randomBatch(random, expectedPersistent);
          log.record("persistent.removeAll(" + elements + ")");
          elements.forEach(expectedPersistent::remove);
          persistent = persistent.removeAll(elements);
        }
        case 4 -> {
          var element = randomKey(random);
          log.record("builder.add(" + element + ")");
          builder.add(element);
          expectedBuilder.add(element);
        }
        case 5 -> {
          var element = chooseElement(random, expectedBuilder);
          log.record("builder.remove(" + element + ")");
          builder.remove(element);
          expectedBuilder.remove(element);
        }
        case 6 -> {
          var elements = randomBatch(random, expectedBuilder);
          log.record("builder.addAll(" + elements + ")");
          builder.addAll(elements);
          expectedBuilder.addAll(elements);
        }
        case 7 -> {
          // Upstream-pending: fix-ordered-set-builder-cache-removeall.
          var elements = randomBatch(random, expectedBuilder);
          log.record("builder.removeAll(" + elements + ")");
          //noinspection SlowAbstractSetRemoveAll
          builder.removeAll(elements);
          elements.forEach(expectedBuilder::remove);
        }
        case 8 -> {
          var elements = randomBatch(random, expectedPersistent);
          log.record("persistent.retainAll(" + elements + ")");
          expectedPersistent.retainAll(elements);
          persistent = persistent.retainAll(elements);
        }
        case 9 -> {
          var elements = randomBatch(random, expectedBuilder);
          log.record("builder.retainAll(" + elements + ")");
          builder.retainAll(elements);
          expectedBuilder.retainAll(elements);
        }
        case 10 -> {
          var remainder = random.nextInt(5);
          log.record("builder.removeIf(value % 5 == " + remainder + ")");
          builder.removeIf(element -> Math.floorMod(element.value(), 5) == remainder);
          expectedBuilder.removeIf(element -> Math.floorMod(element.value(), 5) == remainder);
        }
        case 11 -> {
          log.record("builder.clear()");
          builder.clear();
          expectedBuilder.clear();
        }
        case 12 -> {
          log.record("persistent = builder.build()");
          persistent = builder.build();
          expectedPersistent = new LinkedHashSet<>(expectedBuilder);
          persistentSnapshots.add(persistent);
          expectedPersistentSnapshots.add(new LinkedHashSet<>(expectedPersistent));
        }
        case 13 -> {
          log.record("builder = persistent.builder()");
          builder = persistent.builder();
          expectedBuilder = new LinkedHashSet<>(expectedPersistent);
          var builderSnapshot = builder.build();
          builderSnapshots.add(builderSnapshot);
          expectedBuilderSnapshots.add(new LinkedHashSet<>(expectedBuilder));
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
      LinkedHashSet<TraceKey> expectedPersistent,
      List<PersistentSet<TraceKey>> persistentSnapshots,
      List<LinkedHashSet<TraceKey>> expectedPersistentSnapshots,
      PersistentSet<TraceKey> builderSnapshot,
      LinkedHashSet<TraceKey> expectedBuilder,
      List<PersistentSet<TraceKey>> builderSnapshots,
      List<LinkedHashSet<TraceKey>> expectedBuilderSnapshots) {
    persistentSnapshots.add(persistent);
    expectedPersistentSnapshots.add(new LinkedHashSet<>(expectedPersistent));
    builderSnapshots.add(builderSnapshot);
    expectedBuilderSnapshots.add(new LinkedHashSet<>(expectedBuilder));
  }

  private static void assertState(LinkedHashSet<TraceKey> expected, Iterable<TraceKey> actual) {
    assertEquals(expected, TraceSupport.toLinkedHashSet(actual));
    assertEquals(new ArrayList<>(expected), TraceSupport.toList(actual));
  }

  private static void assertSnapshotStates(
      List<LinkedHashSet<TraceKey>> expectedSnapshots, List<PersistentSet<TraceKey>> snapshots) {
    assertEquals(expectedSnapshots.size(), snapshots.size());
    for (var index = 0; index < snapshots.size(); index++) {
      assertEquals(expectedSnapshots.get(index), TraceSupport.toLinkedHashSet(snapshots.get(index)));
      assertEquals(new ArrayList<>(expectedSnapshots.get(index)), TraceSupport.toList(snapshots.get(index)));
    }
  }

  private static TraceKey chooseElement(Random random, LinkedHashSet<TraceKey> existing) {
    if (!existing.isEmpty() && random.nextDouble() < 0.7) {
      return new ArrayList<>(existing).getFirst();
    }
    return randomKey(random);
  }

  private static List<TraceKey> randomBatch(Random random, LinkedHashSet<TraceKey> existing) {
    var size = 1 + random.nextInt(5);
    var batch = new ArrayList<TraceKey>(size);
    for (var index = 0; index < size; index++) {
      batch.add(!existing.isEmpty() && random.nextDouble() < 0.4
          ? new ArrayList<>(existing).getFirst()
          : randomKey(random));
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
