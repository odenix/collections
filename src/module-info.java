/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
import org.jspecify.annotations.NullMarked;

/// A persistent collection library for Java 21+.
///
/// Persistent collections are immutable collections whose update operations
/// return new collections while sharing storage with the original.
///
/// This library is a Java port of [Kotlin Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable)
/// by JetBrains, closely following its structure and behavior.
/// The public API is in the {@link org.odenix.collections} package.
///
/// The only dependency is JSpecify (~3 KB). It is used for nullness annotations and can be excluded.
@NullMarked
module org.odenix.collections {
  requires static transitive org.jspecify;

  exports org.odenix.collections;
}
