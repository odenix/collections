/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList;

import org.odenix.collections.PersistentList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import benchmarks.BenchmarkSize;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class Iterate {
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

  @Setup
  public void prepare() {
    persistentList = Utils.persistentListAdd(size);
  }

  @Benchmark
  public void firstToLast(Blackhole bh) {
    for (var e : persistentList) {
      bh.consume(e);
    }
  }

  @Benchmark
  public void lastToFirst(Blackhole bh) {
    var iterator = persistentList.listIterator(size);

    while (iterator.hasPrevious()) {
      bh.consume(iterator.previous());
    }
  }
}
