/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableSet.builder;

import org.odenix.collections.PersistentSet;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import benchmarks.BenchmarkSize;
import benchmarks.HashCodeTypes;
import benchmarks.ObjectWrapper;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class Equals {
  @Param({
    BenchmarkSize.BM_1,
    BenchmarkSize.BM_10,
    BenchmarkSize.BM_100,
    BenchmarkSize.BM_1000,
    BenchmarkSize.BM_10000,
    BenchmarkSize.BM_100000,
    BenchmarkSize.BM_1000000
  })
  public int size;

  @Param({HashCodeTypes.HASH_IMPL, HashCodeTypes.ORDERED_IMPL})
  public String implementation = "";

  @Param({
    HashCodeTypes.ASCENDING_HASH_CODE,
    HashCodeTypes.RANDOM_HASH_CODE,
    HashCodeTypes.COLLISION_HASH_CODE,
    HashCodeTypes.NON_EXISTING_HASH_CODE
  })
  public String hashCodeType = "";

  private PersistentSet.Builder<ObjectWrapper<Integer>> persistentSet = PersistentSet.<ObjectWrapper<Integer>>of().builder();
  private PersistentSet.Builder<ObjectWrapper<Integer>> sameSet = PersistentSet.<ObjectWrapper<Integer>>of().builder();
  private PersistentSet.Builder<ObjectWrapper<Integer>> slightlyDifferentSet = PersistentSet.<ObjectWrapper<Integer>>of().builder();
  private PersistentSet.Builder<ObjectWrapper<Integer>> veryDifferentSet = PersistentSet.<ObjectWrapper<Integer>>of().builder();

  @Setup
  public void prepare() {
    var keys = HashCodeTypes.generateKeys(hashCodeType, size * 2);
    persistentSet = Utils.persistentSetBuilderAdd(implementation, keys.subList(0, size), 0.0);
    sameSet = Utils.persistentSetBuilderAdd(implementation, keys.subList(0, size), 0.0);
    slightlyDifferentSet = sameSet.build().builder();
    slightlyDifferentSet.add(keys.get(size));
    slightlyDifferentSet.remove(keys.get(0));
    veryDifferentSet = Utils.persistentSetBuilderAdd(implementation, keys.subList(size, keys.size()), 0.0);
  }

  @Benchmark
  public boolean equalsTrue() {
    return persistentSet.equals(sameSet);
  }

  @Benchmark
  public boolean nearlyEquals() {
    return persistentSet.equals(slightlyDifferentSet);
  }

  @Benchmark
  public boolean notEquals() {
    return persistentSet.equals(veryDifferentSet);
  }
}
