/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  private List<ObjectWrapper<Integer>> keys = List.of();
  private List<ObjectWrapper<Integer>> keysToRemove = List.of();
  private PersistentMap<ObjectWrapper<Integer>, String> persistentMap = PersistentMap.of();
  private PersistentMap<ObjectWrapper<Integer>, String> halfHeightPersistentMap = PersistentMap.of();

  @Setup
  public void prepare() {
    keys = HashCodeTypes.generateKeys(hashCodeType, size);
    persistentMap = Utils.persistentMapPut(implementation, keys);

    var entriesToLeave = Utils.sizeForHalfHeight(persistentMap);
    keysToRemove = new ArrayList<>(keys);
    // Mirrors upstream's unseeded shuffled() call; this benchmark is not exactly reproducible across runs.
    Collections.shuffle(keysToRemove);
    keysToRemove = keysToRemove.subList(entriesToLeave, size);

    halfHeightPersistentMap = Utils.persistentMapRemove(persistentMap, keysToRemove);
  }

  @Benchmark
  public PersistentMap<ObjectWrapper<Integer>, String> removeAndPut() {
    var map = Utils.persistentMapRemove(persistentMap, keysToRemove);
    for (var key : keysToRemove) {
      map = map.put(key, "new value");
    }
    return map;
  }

  @Benchmark
  public void removeAndIterateKeys(Blackhole bh) {
    var map = Utils.persistentMapRemove(persistentMap, keysToRemove);
    var count = 0;
    while (count < size) {
      for (var e : map.keys()) {
        bh.consume(e);
        if (++count == size) {
          break;
        }
      }
    }
  }

  @Benchmark
  public PersistentMap<ObjectWrapper<Integer>, String> putAfterRemove() {
    var map = halfHeightPersistentMap;
    for (var index = 0; index < size - halfHeightPersistentMap.size(); index++) {
      map = map.put(keys.get(index), "new value");
    }
    return map;
  }

  @Benchmark
  public void iterateKeysAfterRemove(Blackhole bh) {
    var count = 0;
    while (count < size) {
      for (var e : halfHeightPersistentMap.keys()) {
        bh.consume(e);
        if (++count == size) {
          break;
        }
      }
    }
  }
}
