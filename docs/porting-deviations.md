# Deviations

Approved translation collisions and required deviations from upstream.

## Package Namespace

- Upstream: public API and implementations live under `kotlinx.collections.immutable`.
- Java port: public API and implementations live under `org.odenix.collections`.
- Reason: the downstream Java port must use an Odenix-owned namespace and must not publish classes in the KotlinX namespace.

## `Sequence` Signatures

- Upstream: extension signatures that take `kotlin.sequences.Sequence`.
- Java port: the corresponding signatures take `java.util.stream.Stream`.
- Reason: Java has no direct standard-library `Sequence` equivalent. `Stream` is the approved stand-in.

## Collection Streams

- Upstream: Kotlin immutable and persistent collections inherit standard transformation operations from Kotlin collection interfaces.
- Java port: `ImmutableCollection` exposes `stream()` and `parallelStream()` for Java-native transformation pipelines.
- Reason: Java users need direct stream access because the port intentionally does not extend JDK collection interfaces.

## Operator Extension Names

- Upstream: `plus(...)` and `minus(...)` extension functions support Kotlin `+` and `-` operators.
- Java port: the corresponding bulk operations use Java names such as `addAll(...)`, `removeAll(...)`, and `putAll(...)`.
- Reason: Java has no operator overloading, so Kotlin operator names are not useful API parity.

## `Pair`-Based Map APIs

- Upstream: map factory and extension APIs use Kotlin `Pair<K, V>`.
- Java port: the corresponding APIs use `Map.Entry<? extends K, ? extends V>`.
- Reason: Java has no standard `Pair` type. `Map.Entry` is the approved stand-in.

## `PersistentMap.putAll(Map)` Collision

- Upstream: there is a `PersistentMap.putAll(map: Map<...>)` member and also an extension-based `plus(map)` path that delegates to it.
- Java port: the core `PersistentMap.putAll(Map<? extends K, ? extends V>)` member satisfies that behavior directly, and no extra extension-shaped method was added.
- Reason: direct member/extension collision; the core member already covers the upstream behavior.

## Map Conversion Factory Type Checks

- Upstream: `toPersistentMap()` checks concrete ordered map types and ordered map builders; `toPersistentHashMap()` checks concrete hash map types and hash map builders.
- Java port: [PersistentMap.from(...)](../src/org/odenix/collections/PersistentMap.java) and `PersistentMap.hashFrom(...)` keep the corresponding builder checks, but `Map`-receiver conversions cannot check or return concrete persistent map instances.
- Reason: the Java parameter type is `Map<? extends K, ? extends V>`, while library persistent map types do not extend `java.util.Map`. The exact upstream checks are not expressible without breaking the core API rules.

## Cross-Type Equality With JDK Collections

- Upstream: Kotlin immutable/persistent collection types inherit `List` / `Set` / `Map` equality behavior from the corresponding Kotlin collection interfaces.
- Java port: `ImmutableList`, `ImmutableSet`, and `ImmutableMap` do not extend the corresponding JDK interfaces, so cross-type equality with `java.util.List`, `java.util.Set`, and `java.util.Map` is not part of the contract.
- Reason: the port keeps the custom `Immutable*` / `Persistent*` API separate from the JDK collection hierarchy. Symmetric equality within that hierarchy is preferred over JDK cross-type equality.

## Implementation Test Packages

- Upstream: implementation tests live under `tests.implementations.list` and `tests.implementations.map`.
- Java port: tests that need package-private implementation access live in the corresponding implementation packages:
  - `tests.implementations.list.BufferIteratorTest` -> `org.odenix.collections.implementations.immutableList.BufferIteratorTest`
  - `tests.implementations.list.TrieIteratorTest` -> `org.odenix.collections.implementations.immutableList.TrieIteratorTest`
  - `tests.implementations.map.HashMapTrieNodeTest` -> `org.odenix.collections.implementations.immutableMap.HashMapTrieNodeTest`
- Reason: Java package-private access is package-scoped. Placing these tests in matching implementation packages preserves upstream internal coverage without widening production visibility.
