# Guidelines

## Primary Goal

Upstream source of truth: the git submodule at `upstream/`.

Port the current upstream API and structure to Java as directly as possible.
Preserve upstream names, distinctions, organization, and surface area wherever Java allows, except for approved deviations in [porting-deviations.md](porting-deviations.md).

## Direct Port Standard

Port upstream code as literally as Java reasonably allows.

Default expectation:

1. Keep names aligned with upstream.
2. Keep usage-site structure aligned with upstream.
3. Keep method-call shape aligned with upstream.
4. Keep branch and control-flow structure aligned with upstream.
5. Prefer line-by-line translations over refactoring.

When Java lacks a Kotlin stdlib operation used by upstream, add the minimal helper needed to keep usage sites direct.

Do not factor out multiple upstream steps into one helper, and do not rewrite code into a different shape just because it is shorter or more idiomatic in Java.

## Source Headers

Java source files use project-owned block license headers. These headers are not part of the upstream translation surface, and may be maintained mechanically.

Ported source and upstream-aligned tests use:

```java
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
```

Downstream-only tests use:

```java
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
```

Do not reformat ported code bodies as part of header maintenance. The formatter may normalize imports by removing unused imports and applying the project import order.

`package-info.java` and `module-info.java` files are excluded from automated license-header replacement. Package documentation must remain before the `package` statement, and module descriptors have no `package` statement for the formatter to use as a safe boundary. Keep their block headers manually aligned with the same header templates.

## API Rules

1. Keep custom `Immutable*` / `Persistent*` interfaces mirroring upstream. Each `Persistent*` extends the corresponding `Immutable*`.
2. Do not make `Immutable*` / `Persistent*` extend `java.util.Collection`, `List`, `Set`, or `Map`.
3. `ImmutableCollection<E>` extends `Iterable<E>`; `ImmutableMap<K, V>` does not extend `Iterable`.
4. Return library types from core APIs, not JDK collection types.
5. In parameter position, translate upstream Kotlin read-only collection types to the corresponding JDK collection types; translate upstream `Iterable` to `java.lang.Iterable`.
6. Keep upstream-aligned conversion helpers as target-type factories such as `ImmutableList.from(...)`, `PersistentSet.from(...)`, and hash-specific `hashFrom(...)` variants.
7. Preserve the immutable vs persistent distinction.
8. Keep Java-view interop such as `toJava()` out of scope for now.
9. Keep convenience out of core interfaces unless upstream has it there.
10. Mirror upstream package structure directly under `org.odenix.collections`. Keep API, adapters, internal code, and implementation clusters in corresponding package tails.

## Approved Exceptions

1. Translate upstream persistent factory functions to interface statics:
   - `persistentListOf(...)` -> `PersistentList.of(...)`
   - `persistentSetOf(...)` -> `PersistentSet.of(...)`
   - `persistentMapOf(...)` -> `PersistentMap.of(...)`
   - `persistentHashSetOf(...)` -> `PersistentSet.hashOf(...)`
   - `persistentHashMapOf(...)` -> `PersistentMap.hashOf(...)`
2. Builders are nested `Persistent*.Builder` interfaces, and in Java they extend the corresponding JDK mutable interfaces.
3. Kotlin `internal` declarations may translate to Java `public` when required for upstream-aligned structure; otherwise package-private is fine. JPMS keeps non-exported packages internal to consumers.
4. The Java port uses the package prefix `org.odenix.collections` instead of upstream's `kotlinx.collections.immutable`.
5. The JPMS descriptor lives at `src/module-info.java` and exports only the public API package.
6. Keep downstream-only tests in `testDownstream` under `testDownstream/src`; keep `test` focused on ported upstream tests.

## Exclusions

1. Do not port deprecated upstream API members by default.
2. When excluding deprecated APIs, follow upstream `ReplaceWith` guidance wherever possible.
3. Do not add new Java-only API surface unless a concrete need arises and it is explicitly approved.

## Nullness And Variance

1. Use `@NullMarked` by default.
2. Keep `@NullMarked` on `module-info.java` and on each source package's `package-info.java`. Package-level annotations preserve nullness defaults for classpath consumers and tools that do not use module metadata.
3. Declare collection type parameters with nullable upper bounds, for example `E extends @Nullable Object`.
4. Translate Kotlin nullable types directly to `@Nullable` at the exact type use.
5. Use Java wildcards to mirror upstream variance mechanically.
6. Treat lookup and equality parameters as nullable where upstream semantics allow arbitrary values.

## Extension Translation

1. Non-factory extensions on `Immutable*` / `Persistent*` receivers become interface instance methods when they translate cleanly.
2. If such a translation collides with an existing core member, treat the extension as satisfied and record the collision in [porting-deviations.md](porting-deviations.md).
3. Conversion extensions on platform receivers become target-type static factories when that is clearer for Java callers.
4. Non-factory extensions on any other receiver become minimal package-private helpers only when needed by ported implementation code.
5. Deprecated extensions are excluded.

## porting-deviations.md

Use [porting-deviations.md](porting-deviations.md) only for concrete, approved translation collisions or required deviations.

