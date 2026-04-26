/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests;

import org.jspecify.annotations.Nullable;

public final class TraceKey {
  private final int value;
  private final int hashCode;

  public TraceKey(int value, int hashCode) {
    this.value = value;
    this.hashCode = hashCode;
  }

  public int value() {
    return value;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof TraceKey traceKey && traceKey.value == value;
  }

  @Override
  public String toString() {
    return "TraceKey(" + value + ", hashCode = " + hashCode + ")";
  }
}
