/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests;

import java.util.ArrayList;
import java.util.List;

public final class TestUtils {
  private static final ArrayList<String> stringValues = new ArrayList<>();

  private TestUtils() {}

  public static List<String> distinctStringValues(int size) {
    if (size <= stringValues.size()) {
      return stringValues.subList(0, size);
    }
    for (var index = stringValues.size(); index < size; index++) {
      stringValues.add(Integer.toString(index));
    }
    return stringValues;
  }
}
