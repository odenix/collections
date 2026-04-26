/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks;

import org.jspecify.annotations.Nullable;

public class ObjectWrapper<K extends Comparable<K>> implements Comparable<ObjectWrapper<K>> {
  private final K obj;
  private final int hashCode;

  public ObjectWrapper(K obj, int hashCode) {
    this.obj = obj;
    this.hashCode = hashCode;
  }

  public K obj() {
    return obj;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof ObjectWrapper<?> wrapper)) {
      return false;
    }
    return obj.equals(wrapper.obj);
  }

  @Override
  public int compareTo(ObjectWrapper<K> other) {
    return obj.compareTo(other.obj);
  }
}
