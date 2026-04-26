/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks;

public final class ImmutablePercentage {
  public static final String IP_100 = "100.0";
  public static final String IP_99_09 = "99.09";
  public static final String IP_95 = "95.0";
  public static final String IP_70 = "70.0";
  public static final String IP_50 = "50.0";
  public static final String IP_30 = "30.0";
  public static final String IP_0 = "0.0";

  private ImmutablePercentage() {}

  public static int immutableSize(int size, double immutablePercentage) {
    return (int) Math.floor(size * immutablePercentage / 100.0);
  }
}
