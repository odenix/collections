/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableSet;

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

  private PersistentSet<ObjectWrapper<Integer>> persistentSet = PersistentSet.of();
  private PersistentSet<ObjectWrapper<Integer>> sameSet = PersistentSet.of();
  private PersistentSet<ObjectWrapper<Integer>> slightlyDifferentSet = PersistentSet.of();
  private PersistentSet<ObjectWrapper<Integer>> veryDifferentSet = PersistentSet.of();

  @Setup
  public void prepare() {
    var keys = HashCodeTypes.generateKeys(hashCodeType, size * 2);
    persistentSet = Utils.persistentSetAdd(implementation, keys.subList(0, size));
    sameSet = Utils.persistentSetAdd(implementation, keys.subList(0, size));
    slightlyDifferentSet = sameSet.add(keys.get(size)).remove(keys.get(0));
    veryDifferentSet = Utils.persistentSetAdd(implementation, keys.subList(size, keys.size()));
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
