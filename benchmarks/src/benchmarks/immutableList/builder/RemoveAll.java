/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.odenix.collections.PersistentList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import benchmarks.BenchmarkSize;
import benchmarks.ImmutablePercentage;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class RemoveAll {
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

  @Param({
    ImmutablePercentage.IP_100,
    ImmutablePercentage.IP_99_09,
    ImmutablePercentage.IP_95,
    ImmutablePercentage.IP_70,
    ImmutablePercentage.IP_50,
    ImmutablePercentage.IP_30,
    ImmutablePercentage.IP_0
  })
  public double immutablePercentage;

  @Benchmark
  public boolean addAndRemoveAll_All() {
    var builder = persistentListBuilderAddIndexes();
    var elementsToRemove = new ArrayList<Integer>(size);
    for (var i = 0; i < size; i++) {
      elementsToRemove.add(i);
    }
    return builder.removeAll(elementsToRemove);
  }

  @Benchmark
  public boolean addAndRemoveAll_RandomHalf() {
    var builder = persistentListBuilderAddIndexes();
    var elementsToRemove = randomIndexes(size / 2);
    return builder.removeAll(elementsToRemove);
  }

  @Benchmark
  public boolean addAndRemoveAll_RandomTen() {
    var builder = persistentListBuilderAddIndexes();
    var elementsToRemove = randomIndexes(10);
    return builder.removeAll(elementsToRemove);
  }

  @Benchmark
  public boolean addAndRemoveAll_Tail() {
    var builder = persistentListBuilderAddIndexes();
    var elementsToRemove = new ArrayList<Integer>(tailSize());
    for (var i = 0; i < tailSize(); i++) {
      elementsToRemove.add(size - 1 - i);
    }
    return builder.removeAll(elementsToRemove);
  }

  @Benchmark
  public boolean addAndRemoveAll_NonExisting() {
    var builder = persistentListBuilderAddIndexes();
    var elementsToRemove = new ArrayList<Integer>(10);
    for (var index : randomIndexes(10)) {
      elementsToRemove.add(size + index);
    }
    return builder.removeAll(elementsToRemove);
  }

  private PersistentList.Builder<Integer> persistentListBuilderAddIndexes() {
    var immutableSize = ImmutablePercentage.immutableSize(size, immutablePercentage);
    var list = PersistentList.<Integer>of();
    for (var i = 0; i < immutableSize; i++) {
      list = list.add(i);
    }
    var builder = list.builder();
    for (var i = immutableSize; i < size; i++) {
      builder.add(i);
    }
    return builder;
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
