/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableMap.builder;

import java.util.List;

import org.odenix.collections.PersistentMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import benchmarks.BenchmarkSize;
import benchmarks.HashCodeTypes;
import benchmarks.ImmutablePercentage;
import benchmarks.ObjectWrapper;

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

  private List<ObjectWrapper<Integer>> keys = List.of();
  private List<ObjectWrapper<Integer>> keysToRemove = List.of();

  @Setup
  public void prepare() {
    keys = HashCodeTypes.generateKeys(hashCodeType, size);
    keysToRemove =
        hashCodeType.equals(HashCodeTypes.NON_EXISTING_HASH_CODE)
            ? HashCodeTypes.generateKeys(hashCodeType, size)
            : keys;
  }

  @Benchmark
  public PersistentMap.Builder<ObjectWrapper<Integer>, String> putAndRemove() {
    var builder = Utils.persistentMapBuilderPut(implementation, keys, immutablePercentage);
    for (var index = 0; index < size; index++) {
      builder.remove(keysToRemove.get(index));
    }
    return builder;
  }
}
