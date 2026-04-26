/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList.builder;

import java.util.Collections;
import java.util.List;

import org.odenix.collections.PersistentList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import benchmarks.BenchmarkSize;
import benchmarks.ImmutablePercentage;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class AddAll {
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

  private List<String> listToAdd = List.of();

  @Setup
  public void prepare() {
    listToAdd = Collections.nCopies(size, "another element");
  }

  @Benchmark
  public PersistentList.Builder<String> addAllLast() {
    var builder = PersistentList.<String>of().builder();
    builder.addAll(listToAdd);
    return builder;
  }

  @Benchmark
  public PersistentList.Builder<String> addAllLast_Half() {
    var initialSize = size / 2;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    var builder = Utils.persistentListBuilderAdd(initialSize, immutablePercentage);
    builder.addAll(subListToAdd);
    return builder;
  }

  @Benchmark
  public PersistentList.Builder<String> addAllLast_OneThird() {
    var initialSize = size - size / 3;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    var builder = Utils.persistentListBuilderAdd(initialSize, immutablePercentage);
    builder.addAll(subListToAdd);
    return builder;
  }

  @Benchmark
  public PersistentList.Builder<String> addAllFirst_Half() {
    var initialSize = size / 2;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    var builder = Utils.persistentListBuilderAdd(initialSize, immutablePercentage);
    builder.addAll(0, subListToAdd);
    return builder;
  }

  @Benchmark
  public PersistentList.Builder<String> addAllFirst_OneThird() {
    var initialSize = size - size / 3;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    var builder = Utils.persistentListBuilderAdd(initialSize, immutablePercentage);
    builder.addAll(0, subListToAdd);
    return builder;
  }

  @Benchmark
  public PersistentList.Builder<String> addAllMiddle_Half() {
    var initialSize = size / 2;
    var index = initialSize / 2;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    var builder = Utils.persistentListBuilderAdd(initialSize, immutablePercentage);
    builder.addAll(index, subListToAdd);
    return builder;
  }

  @Benchmark
  public PersistentList.Builder<String> addAllMiddle_OneThird() {
    var initialSize = size - size / 3;
    var index = initialSize / 2;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    var builder = Utils.persistentListBuilderAdd(initialSize, immutablePercentage);
    builder.addAll(index, subListToAdd);
    return builder;
  }
}
