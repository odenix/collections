/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableMap;

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
public class PutAll {
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
    HashCodeTypes.COLLISION_HASH_CODE
  })
  public String hashCodeType = "";

  private PersistentMap<ObjectWrapper<Integer>, String> lhs = PersistentMap.of();
  private PersistentMap<ObjectWrapper<Integer>, String> lhsSmall = PersistentMap.of();
  private PersistentMap<ObjectWrapper<Integer>, String> rhs = PersistentMap.of();
  private PersistentMap<ObjectWrapper<Integer>, String> rhsSmall = PersistentMap.of();

  @Setup
  public void prepare() {
    var keys = HashCodeTypes.generateKeys(hashCodeType, 2 * size);
    lhs = Utils.persistentMapPut(implementation, keys.subList(0, size));
    lhsSmall = Utils.persistentMapPut(implementation, keys.subList(0, (size / 1000) + 1));
    rhs = Utils.persistentMapPut(implementation, keys.subList(size, keys.size()));
    rhsSmall = Utils.persistentMapPut(implementation, keys.subList(keys.size() - ((size / 1000) + 1), keys.size()));
  }

  @Benchmark
  public PersistentMap<ObjectWrapper<Integer>, String> putAllEqualSize() {
    // PersistentMap does not extend java.util.Map, so this uses entries() instead of upstream's putAll(rhs).
    return lhs.putAll(rhs.entries());
  }

  @Benchmark
  public PersistentMap<ObjectWrapper<Integer>, String> putAllSmallIntoLarge() {
    // PersistentMap does not extend java.util.Map, so this uses entries() instead of upstream's putAll(rhsSmall).
    return lhs.putAll(rhsSmall.entries());
  }

  @Benchmark
  public PersistentMap<ObjectWrapper<Integer>, String> putAllLargeIntoSmall() {
    // PersistentMap does not extend java.util.Map, so this uses entries() instead of upstream's putAll(rhs).
    return lhsSmall.putAll(rhs.entries());
  }
}
