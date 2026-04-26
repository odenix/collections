/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.odenix.collections.ImmutableList;
import org.odenix.collections.PersistentList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import benchmarks.BenchmarkSize;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class Set {
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
  private List<Integer> randomIndices = List.of();

  @Setup
  public void prepare() {
    persistentList = Utils.persistentListAdd(size);
    randomIndices = new ArrayList<>();
    for (var i = 0; i < size; i++) {
      randomIndices.add(i);
    }
    // Mirrors upstream's unseeded shuffled() call; this benchmark is not exactly reproducible across runs.
    Collections.shuffle(randomIndices);
  }

  @Benchmark
  public ImmutableList<String> setByIndex() {
    for (var index = 0; index < size; index++) {
      persistentList = persistentList.set(index, "another element");
    }
    return persistentList;
  }

  @Benchmark
  public ImmutableList<String> setByRandomIndex() {
    for (var index = 0; index < size; index++) {
      persistentList = persistentList.set(randomIndices.get(index), "another element");
    }
    return persistentList;
  }
}
