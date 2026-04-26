/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import org.odenix.collections.PersistentList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import benchmarks.BenchmarkSize;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class RemoveAllPredicate {
  @Param({
    BenchmarkSize.BM_1,
    BenchmarkSize.BM_10,
    BenchmarkSize.BM_100,
    BenchmarkSize.BM_1000,
    BenchmarkSize.BM_10000,
    BenchmarkSize.BM_100000,
    BenchmarkSize.BM_1000000,
    BenchmarkSize.BM_10000000
  })
  public int size;

  private PersistentList<String> persistentList = PersistentList.of();
  private final Predicate<String> truePredicate = element -> true;
  private final Predicate<String> falsePredicate = element -> false;
  private Predicate<String> randomHalfElementsPredicate = truePredicate;
  private Predicate<String> randomTenElementsPredicate = truePredicate;
  private Predicate<String> randomOneElementPredicate = truePredicate;
  private Predicate<String> tailElementsPredicate = truePredicate;

  @Setup
  public void prepare() {
    var randomHalfElements = toStringSet(randomIndexes(size / 2));
    randomHalfElementsPredicate = randomHalfElements::contains;

    var randomTenElements = toStringSet(randomIndexes(10));
    randomTenElementsPredicate = randomTenElements::contains;

    // Mirrors upstream's unseeded Random.nextInt call; this benchmark is not exactly reproducible across runs.
    var randomOneElement = Integer.toString(ThreadLocalRandom.current().nextInt(size));
    randomOneElementPredicate = element -> element.equals(randomOneElement);

    var tailElements = new HashSet<String>();
    for (var i = 0; i < tailSize(); i++) {
      tailElements.add(Integer.toString(size - 1 - i));
    }
    tailElementsPredicate = tailElements::contains;

    var allElements = new ArrayList<String>(size);
    for (var i = 0; i < size; i++) {
      allElements.add(Integer.toString(i));
    }
    persistentList = PersistentList.<String>of().addAll(allElements);
  }

  @Benchmark
  public PersistentList<String> removeAll_All() {
    return persistentList.removeAll(truePredicate);
  }

  @Benchmark
  public PersistentList<String> removeAll_Non() {
    return persistentList.removeAll(falsePredicate);
  }

  @Benchmark
  public PersistentList<String> removeAll_RandomHalf() {
    return persistentList.removeAll(randomHalfElementsPredicate);
  }

  @Benchmark
  public PersistentList<String> removeAll_RandomTen() {
    return persistentList.removeAll(randomTenElementsPredicate);
  }

  @Benchmark
  public PersistentList<String> removeAll_RandomOne() {
    return persistentList.removeAll(randomOneElementPredicate);
  }

  @Benchmark
  public PersistentList<String> removeAll_Tail() {
    return persistentList.removeAll(tailElementsPredicate);
  }

  private Set<String> toStringSet(List<Integer> indexes) {
    var result = new HashSet<String>();
    for (var index : indexes) {
      result.add(Integer.toString(index));
    }
    return result;
  }

  private List<Integer> randomIndexes(int count) {
    // Mirrors upstream's unseeded Random.nextInt calls; this benchmark is not exactly reproducible across runs.
    var result = new ArrayList<Integer>(count);
    for (var i = 0; i < count; i++) {
      result.add(ThreadLocalRandom.current().nextInt(size));
    }
    return result;
  }

  private int tailSize() {
    var bufferSize = 32;
    var remainder = size & (bufferSize - 1);
    return remainder == 0 ? bufferSize : remainder;
  }
}
