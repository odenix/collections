/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableSet;

import java.util.List;

import org.odenix.collections.PersistentSet;
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
public class Contains {
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

  private List<ObjectWrapper<Integer>> elements = List.of();
  private PersistentSet<ObjectWrapper<Integer>> persistentSet = PersistentSet.of();

  @Setup
  public void prepare() {
    elements = HashCodeTypes.generateElements(hashCodeType, size);
    persistentSet = Utils.persistentSetAdd(implementation, elements);

    if (hashCodeType.equals(HashCodeTypes.NON_EXISTING_HASH_CODE)) {
      elements = HashCodeTypes.generateElements(hashCodeType, size);
    }
  }

  @Benchmark
  public void contains(Blackhole bh) {
    for (var index = 0; index < size; index++) {
      bh.consume(persistentSet.contains(elements.get(index)));
    }
  }
}
