/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList.builder;

import org.odenix.collections.PersistentList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import benchmarks.BenchmarkSize;
import benchmarks.ImmutablePercentage;

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
  public PersistentList.Builder<String> addAndRemoveLast() {
    var builder = Utils.persistentListBuilderAdd(size, immutablePercentage);
    for (var i = 0; i < size; i++) {
      builder.remove(builder.size() - 1);
    }
    return builder;
  }

  @Benchmark
  public String addAndRemoveFirst() {
    var builder = Utils.persistentListBuilderAdd(size, immutablePercentage);
    return builder.remove(0);
  }

  @Benchmark
  public String addAndRemoveMiddle() {
    var builder = Utils.persistentListBuilderAdd(size, immutablePercentage);
    return builder.remove(size / 2);
  }
}
