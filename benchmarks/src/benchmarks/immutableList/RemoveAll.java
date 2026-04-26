/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.odenix.collections.PersistentList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import benchmarks.BenchmarkSize;

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

  private PersistentList<Integer> persistentList = PersistentList.of();

  @Setup
  public void prepare() {
    var elements = new ArrayList<Integer>(size);
    for (var i = 0; i < size; i++) {
      elements.add(i);
    }
    persistentList = PersistentList.<Integer>of().addAll(elements);
  }

  @Benchmark
  public PersistentList<Integer> removeAll_All() {
    var list = persistentList;
    var elementsToRemove = new ArrayList<Integer>(size);
    for (var i = 0; i < size; i++) {
      elementsToRemove.add(i);
    }
    return list.removeAll(elementsToRemove);
  }

  @Benchmark
  public PersistentList<Integer> removeAll_RandomHalf() {
    var list = persistentList;
    var elementsToRemove = randomIndexes(size / 2);
    return list.removeAll(elementsToRemove);
  }

  @Benchmark
  public PersistentList<Integer> removeAll_RandomTen() {
    var list = persistentList;
    var elementsToRemove = randomIndexes(10);
    return list.removeAll(elementsToRemove);
  }

  @Benchmark
  public PersistentList<Integer> removeAll_Tail() {
    var list = persistentList;
    var elementsToRemove = new ArrayList<Integer>(tailSize());
    for (var i = 0; i < tailSize(); i++) {
      elementsToRemove.add(size - 1 - i);
    }
    return list.removeAll(elementsToRemove);
  }

  @Benchmark
  public PersistentList<Integer> removeAll_NonExisting() {
    var list = persistentList;
    var elementsToRemove = new ArrayList<Integer>(10);
    for (var index : randomIndexes(10)) {
      elementsToRemove.add(size + index);
    }
    return list.removeAll(elementsToRemove);
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
