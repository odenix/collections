/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.internal;

/// The mutability ownership token of a persistent collection builder.
///
/// Used to mark persistent data structures, that are owned by a collection builder and can be mutated by it.
@SuppressWarnings("unused")
public final class MutabilityOwnership {}
