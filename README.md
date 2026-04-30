# Odenix Collections

A persistent collection library for Java 21+.

Persistent collections are immutable collections whose update operations return new collections that share storage with the original.

This library is an idiomatic Java port of [Kotlin Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable) by JetBrains.

## Contents

- [Example](#example)
- [Installation](#installation)
- [Why This Library](#why-this-library)
- [API Overview](#api-overview)
- [Development](#development)

## Example

```java
import org.odenix.collections.*;

// Create a persistent list
var list = PersistentList.of(1, 2, 3);

// Update operations return new collections
var updated = list.add(4);

// The original list is unchanged
System.out.println(list);    // [1, 2, 3]
System.out.println(updated); // [1, 2, 3, 4]

// Use mutate() for efficient batched updates
var mutated = list.mutate(builder -> {
  builder.add(4);
  builder.add(5);
});

System.out.println(mutated); // [1, 2, 3, 4, 5]

// Maps preserve insertion order by default
var map = PersistentMap.of("a", 1, "b", 2);
var map2 = map.put("c", 3);

System.out.println(map2); // {a=1, b=2, c=3}
```

## Installation

Releases are available from Maven Central: [org.odenix:odenix-collections](https://central.sonatype.com/artifact/org.odenix/odenix-collections/versions)

Gradle:

```kotlin
implementation("org.odenix:odenix-collections:{version}")
```

Maven:

```xml
<dependency>
  <groupId>org.odenix</groupId>
  <artifactId>odenix-collections</artifactId>
  <version>{version}</version>
</dependency>
```

Mill:

```scala
mvn"org.odenix:odenix-collections:{version}"
```

## Why This Library

The Java ecosystem lacks a modern, full-featured, production-ready persistent collection library.
This library aims to fill this gap by porting Kotlin Immutable Collections to Java.

Kotlin Immutable Collections is a strong baseline:

- Modern data structures with efficient small and large representations
- Hash-based and insertion-ordered sets/maps
- Thoroughly tested and benchmarked
- Proven in production despite its experimental status

This Java port adds:

- An idiomatic Java API
- No dependency on the Kotlin standard library
- Java nullness annotations via [JSpecify](https://jspecify.dev/) (only dependency, can be excluded)

## API Overview

API reference: [latest Javadoc](https://collections.odenix.org/)

The public API is organized around immutable and persistent collection types.
Unlike Kotlin Immutable Collections, immutable interfaces extend only `Iterable`, not `Collection`, `List`, `Set`, or `Map`.

### Immutable interfaces

Provide read-only access:

| Interface             | Bases                 |
|-----------------------|-----------------------|
| `ImmutableCollection` | `Iterable`            |
| `ImmutableList`       | `ImmutableCollection` |
| `ImmutableSet`        | `ImmutableCollection` |
| `ImmutableMap`        | —                     |

### Persistent interfaces

Extend immutable interfaces with update operations that return new collections:

| Interface              | Bases                                   |
|------------------------|-----------------------------------------|
| `PersistentCollection` | `ImmutableCollection`                   |
| `PersistentList`       | `PersistentCollection`, `ImmutableList` |
| `PersistentSet`        | `PersistentCollection`, `ImmutableSet`  |
| `PersistentMap`        | `ImmutableMap`                          |

### Builders

Expose JDK mutable collection interfaces for efficient batched updates:

| Interface                      | Bases                                  |
|--------------------------------|----------------------------------------|
| `PersistentCollection.Builder` | `Collection`                           |
| `PersistentList.Builder`       | `PersistentCollection.Builder`, `List` |
| `PersistentSet.Builder`        | `PersistentCollection.Builder`, `Set`  |
| `PersistentMap.Builder`        | `Map`                                  |

### Creating Collections

- `PersistentList.of()`
- `PersistentSet.of()`, `PersistentMap.of()` to preserve insertion order
- `PersistentSet.hashOf()`, `PersistentMap.hashOf()` when insertion order does not matter

### Conversion

- `ImmutableList.from()`, `ImmutableSet.from()`, `ImmutableMap.from()` for read-only access
- `PersistentList.from()`, `PersistentSet.from()`, `PersistentMap.from()` for persistent updates
- `PersistentSet.hashFrom()`, `PersistentMap.hashFrom()` when insertion order does not matter

### Updates

- `add()`
- `addAll()`
- `remove()`
- `removeAll()`
- `mutate()`

Returned collections may share storage with the original.

### Streams

- `stream()`
- `parallelStream()` 

To keep the result persistent, pass the stream to `from()`:

```java
var lengths = PersistentList.from(names.stream().map(String::length));
```

## Development

Build, test, repository layout, and porting instructions are in [DEVELOPMENT.md](DEVELOPMENT.md).
