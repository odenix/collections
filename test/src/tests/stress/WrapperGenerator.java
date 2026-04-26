/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.stress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import tests.ObjectWrapper;

public final class WrapperGenerator<K extends Comparable<K>> {
  private final int hashCodeUpperBound;
  private final HashMap<K, ObjectWrapper<K>> elementMap = new HashMap<>();
  private final HashMap<Integer, ArrayList<ObjectWrapper<K>>> hashCodeMap = new HashMap<>();

  public WrapperGenerator(int hashCodeUpperBound) {
    this.hashCodeUpperBound = hashCodeUpperBound;
  }

  public ObjectWrapper<K> wrapper(K element) {
    var existing = elementMap.get(element);
    if (existing != null) {
      return existing;
    }
    var hashCode = RandomHolder.RANDOM.nextInt(hashCodeUpperBound);
    var wrapper = new ObjectWrapper<>(element, hashCode);
    elementMap.put(element, wrapper);

    var wrappers = hashCodeMap.get(hashCode);
    if (wrappers == null) {
      wrappers = new ArrayList<>();
      hashCodeMap.put(hashCode, wrappers);
    }
    wrappers.add(wrapper);

    return wrapper;
  }

  public List<ObjectWrapper<K>> wrappersByHashCode(int hashCode) {
    var wrappers = hashCodeMap.get(hashCode);
    return wrappers == null ? List.of() : wrappers;
  }

  private static final class RandomHolder {
    private static final Random RANDOM = new Random(0);
  }
}
