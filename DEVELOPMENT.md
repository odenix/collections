# Development

## Status

This project tracks the upstream Kotlin implementation.

Correctness is checked with:

- ported upstream tests
- downstream Java tests
- randomized trace tests that compare persistent collection behavior against JDK collection models

Performance parity is checked with `benchmarks.compareFast`.

## Repository Layout

- `src/` contains the Java port.
- `test/` contains tests ported from upstream. `test/extra/` contains downstream-only tests, including opt-in randomized trace tests.
- `upstream/` is a git submodule pointing at the original Kotlin project.
- `docs/porting.md`, `docs/porting-implementation.md`, and `docs/porting-deviations.md` document the porting rules.

## Build And Test

Common tasks:

```sh
./millw compile
./millw test
./millw test.extra
./millw build
./millw publishLocal
./millw initUpstream
```

`test` runs the ported upstream tests. `test.extra` runs downstream-only tests.

Run longer randomized downstream tests explicitly:

```sh
./millw test.extra.randomized
```

Randomized test sizing is configured in `test/extra/randomized/package.mill`.
Failure traces are written under the randomized module's Mill output directory.

## Benchmarks

Benchmarks are JMH benchmarks under `benchmarks/src`. They are opt-in and are not part of `build`.

Benchmark tasks:

```sh
./millw benchmarks.runSmoke
./millw benchmarks.runFast
./millw benchmarks.compareFast
./millw benchmarks.runFull
```

`runSmoke` is a short wiring check. `runFast` is the upstream-aligned fast JVM suite.
`compareFast` runs upstream first, then downstream with the JVM reported by upstream JMH.
`runFull` mirrors the upstream full JVM benchmark configuration and can take much longer.

## Porting Rules

Before porting, read:

- `docs/porting.md`
- `docs/porting-implementation.md`
- `docs/porting-deviations.md`

The short version: prefer direct translation over Java-specific redesign.
Keep upstream naming, structure, control flow, and documentation unless Java requires a narrow adaptation.
