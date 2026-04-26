/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList;

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
public class Remove {
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
  public ImmutableList<String> removeLast() {
    var list = persistentList;
    for (var i = 0; i < size; i++) {
      list = list.removeAt(list.size() - 1);
    }
    return list;
  }

  /// Removes one element from the beginning.
  ///
  /// Measures (time and memory spent on {@code removeAt} operation) / size.
  ///
  /// Expected time: nearly constant.
  /// Expected memory: nearly constant.
  @Benchmark
  public ImmutableList<String> removeFirst() {
    var list = persistentList;
    return list.removeAt(0);
  }

  /// Removes one element from the middle.
  ///
  /// Measures (time and memory spent on {@code removeAt} operation) / size.
  ///
  /// Expected time: nearly constant.
  /// Expected memory: nearly constant.
  @Benchmark
  public ImmutableList<String> removeMiddle() {
    var list = persistentList;
    return list.removeAt(size / 2);
  }
}
