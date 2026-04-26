/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList;

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

  private List<String> listToAdd = List.of();

  @Setup
  public void prepare() {
    listToAdd = Collections.nCopies(size, "another element");
  }

  @Benchmark
  public ImmutableList<String> addAllLast() {
    return PersistentList.<String>of().addAll(listToAdd);
  }

  @Benchmark
  public ImmutableList<String> addAllLast_Half() {
    var initialSize = size / 2;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    return Utils.persistentListAdd(initialSize).addAll(subListToAdd);
  }

  @Benchmark
  public ImmutableList<String> addAllLast_OneThird() {
    var initialSize = size - size / 3;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    return Utils.persistentListAdd(initialSize).addAll(subListToAdd);
  }

  @Benchmark
  public ImmutableList<String> addAllFirst_Half() {
    var initialSize = size / 2;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    return Utils.persistentListAdd(initialSize).addAll(0, subListToAdd);
  }

  @Benchmark
  public ImmutableList<String> addAllFirst_OneThird() {
    var initialSize = size - size / 3;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    return Utils.persistentListAdd(initialSize).addAll(0, subListToAdd);
  }

  @Benchmark
  public ImmutableList<String> addAllMiddle_Half() {
    var initialSize = size / 2;
    var index = initialSize / 2;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    return Utils.persistentListAdd(initialSize).addAll(index, subListToAdd);
  }

  @Benchmark
  public ImmutableList<String> addAllMiddle_OneThird() {
    var initialSize = size - size / 3;
    var index = initialSize / 2;
    var subListToAdd = listToAdd.subList(0, size - initialSize);
    return Utils.persistentListAdd(initialSize).addAll(index, subListToAdd);
  }
}
