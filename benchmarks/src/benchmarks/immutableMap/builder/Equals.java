/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableMap.builder;

import org.odenix.collections.PersistentMap;
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

  private PersistentMap.Builder<ObjectWrapper<Integer>, String> persistentMap = PersistentMap.<ObjectWrapper<Integer>, String>of().builder();
  private PersistentMap.Builder<ObjectWrapper<Integer>, String> sameMap = PersistentMap.<ObjectWrapper<Integer>, String>of().builder();
  private PersistentMap.Builder<ObjectWrapper<Integer>, String> slightlyDifferentMap = PersistentMap.<ObjectWrapper<Integer>, String>of().builder();
  private PersistentMap.Builder<ObjectWrapper<Integer>, String> veryDifferentMap = PersistentMap.<ObjectWrapper<Integer>, String>of().builder();

  @Setup
  public void prepare() {
    var keys = HashCodeTypes.generateKeys(hashCodeType, size * 2);
    persistentMap = Utils.persistentMapBuilderPut(implementation, keys.subList(0, size), 0.0);
    sameMap = Utils.persistentMapBuilderPut(implementation, keys.subList(0, size), 0.0);
    slightlyDifferentMap = sameMap.build().builder();
    slightlyDifferentMap.put(keys.get(size), "different value");
    slightlyDifferentMap.remove(keys.get(0));
    veryDifferentMap = Utils.persistentMapBuilderPut(implementation, keys.subList(size, keys.size()), 0.0);
  }

  @Benchmark
  public boolean equalsTrue() {
    return persistentMap.equals(sameMap);
  }

  @Benchmark
  public boolean nearlyEquals() {
    return persistentMap.equals(slightlyDifferentMap);
  }

  @Benchmark
  public boolean notEquals() {
    return persistentMap.equals(veryDifferentMap);
  }
}
