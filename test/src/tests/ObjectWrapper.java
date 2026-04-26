/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

public class ObjectWrapper<K extends Comparable<K>> implements Comparable<ObjectWrapper<K>> {
  public final K obj;
  private final int hashCode;

  public ObjectWrapper(K obj, int hashCode) {
    this.obj = obj;
    this.hashCode = hashCode;
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
    assert !Objects.equals(obj, wrapper.obj) || hashCode == wrapper.hashCode();
    return Objects.equals(obj, wrapper.obj);
  }

  @Override
  public int compareTo(ObjectWrapper<K> other) {
    return obj.compareTo(other.obj);
  }

  @Override
  public String toString() {
    return "ObjectWrapper(" + obj + ", hashCode = " + hashCode + ")";
  }
}
