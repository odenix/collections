/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableSet;

import java.util.ArrayList;
import java.util.Collections;
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
public class Canonicalization {
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

  private List<ObjectWrapper<Integer>> elements = List.of();
  private List<ObjectWrapper<Integer>> elementsToRemove = List.of();
  private PersistentSet<ObjectWrapper<Integer>> persistentSet = PersistentSet.of();
  private PersistentSet<ObjectWrapper<Integer>> halfHeightPersistentSet = PersistentSet.of();

  @Setup
  public void prepare() {
    elements = HashCodeTypes.generateElements(hashCodeType, size);
    persistentSet = Utils.persistentSetAdd(implementation, elements);

    var elementsToLeave = Utils.sizeForHalfHeight(persistentSet);
    elementsToRemove = new ArrayList<>(elements);
    // Mirrors upstream's unseeded shuffled() call; this benchmark is not exactly reproducible across runs.
    Collections.shuffle(elementsToRemove);
    elementsToRemove = elementsToRemove.subList(elementsToLeave, size);

    halfHeightPersistentSet = Utils.persistentSetRemove(persistentSet, elementsToRemove);
  }

  @Benchmark
  public PersistentSet<ObjectWrapper<Integer>> removeAndAdd() {
    var set = Utils.persistentSetRemove(persistentSet, elementsToRemove);
    for (var element : elementsToRemove) {
      set = set.add(element);
    }
    return set;
  }

  @Benchmark
  public void removeAndIterate(Blackhole bh) {
    var set = Utils.persistentSetRemove(persistentSet, elementsToRemove);
    var count = 0;
    while (count < size) {
      for (var e : set) {
        bh.consume(e);
        if (++count == size) {
          break;
        }
      }
    }
  }

  @Benchmark
  public PersistentSet<ObjectWrapper<Integer>> addAfterRemove() {
    var set = halfHeightPersistentSet;
    for (var element : elementsToRemove) {
      set = set.add(element);
    }
    return set;
  }

  @Benchmark
  public void iterateAfterRemove(Blackhole bh) {
    var count = 0;
    while (count < size) {
      for (var e : halfHeightPersistentSet) {
        bh.consume(e);
        if (++count == size) {
          break;
        }
      }
    }
  }
}
