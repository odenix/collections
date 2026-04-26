/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList;

import org.odenix.collections.ImmutableList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import benchmarks.BenchmarkSize;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class Add {
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

  @Benchmark
  public ImmutableList<String> addLast() {
    return Utils.persistentListAdd(size);
  }

  @Benchmark
  public void addLastAndIterate(Blackhole bh) {
    var list = Utils.persistentListAdd(size);
    for (var e : list) {
      bh.consume(e);
    }
  }

  @Benchmark
  public void addLastAndGet(Blackhole bh) {
    var list = Utils.persistentListAdd(size);
    for (var i = 0; i < list.size(); i++) {
      bh.consume(list.get(i));
    }
  }

  /// Adds {@code size} - 1 elements to an empty persistent list
  /// and then inserts one element at the beginning.
  ///
  /// Measures mean time and memory spent per {@code add} operation.
  ///
  /// Expected time: nearly constant.
  /// Expected memory: nearly constant.
  @Benchmark
  public ImmutableList<String> addFirst() {
    return Utils.persistentListAdd(size - 1).add(0, "another element");
  }

  /// Adds {@code size} - 1 elements to an empty persistent list
  /// and then inserts one element at the middle.
  ///
  /// Measures mean time and memory spent per {@code add} operation.
  ///
  /// Expected time: nearly constant.
  /// Expected memory: nearly constant.
  @Benchmark
  public ImmutableList<String> addMiddle() {
    return Utils.persistentListAdd(size - 1).add(size / 2, "another element");
  }
}
