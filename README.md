# Odenix Collections

A persistent collection library for Java 21+.

Persistent collections are immutable collections whose update operations return new collections while sharing storage with the original.

This library is a Java port of [Kotlin Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable) by JetBrains, closely following its structure and behavior.
The public API is in the `org.odenix.collections` package.

The only dependency is JSpecify (~3 KB). It is used for nullness annotations and can be excluded.

## Example

```java
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

## Why This Port Exists

- Java lacks a modern, actively maintained persistent collection library.
- A Java port of Kotlin Immutable Collections is easier to use from Java and avoids the Kotlin standard library (~2 MB).
- Modern coding agents reduce the maintenance effort for the port.

## API Overview

This library provides immutable and persistent collection interfaces.
Unlike the upstream Kotlin implementation, immutable collections do not extend JDK collection interfaces (except `Iterable`).

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

To keep the result persistent, pass the stream to `from()`

```java
var lengths = PersistentList.from(names.stream().map(String::length));
```

## Development

Build, test, repository layout, and porting instructions are in [DEVELOPMENT.md](DEVELOPMENT.md).
