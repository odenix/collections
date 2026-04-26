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
import org.openjdk.jmh.infra.Blackhole;

import benchmarks.BenchmarkSize;
import benchmarks.HashCodeTypes;
import benchmarks.ObjectWrapper;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class Iterate {
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

  private PersistentMap<ObjectWrapper<Integer>, String> persistentMap = PersistentMap.of();

  @Setup
  public void prepare() {
    persistentMap = Utils.persistentMapPut(implementation, HashCodeTypes.generateKeys(hashCodeType, size));
  }

  @Benchmark
  public void iterateKeys(Blackhole bh) {
    for (var k : persistentMap.keys()) {
      bh.consume(k);
    }
  }

  @Benchmark
  public void iterateValues(Blackhole bh) {
    for (var v : persistentMap.values()) {
      bh.consume(v);
    }
  }

  @Benchmark
  public void iterateEntries(Blackhole bh) {
    for (var e : persistentMap.entries()) {
      bh.consume(e);
    }
  }
}
