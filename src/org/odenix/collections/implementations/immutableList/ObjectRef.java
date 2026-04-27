/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import org.jspecify.annotations.Nullable;

final class ObjectRef {
  @Nullable Object value;

  ObjectRef(@Nullable Object value) {
    this.value = value;
  }
}
