# Implementation Guidelines

Implementation-level conventions for the Java port. Public API rules live in [porting.md](porting.md).

## Direct Port Rules

1. Prefer a line-by-line upstream port. Preserve names, branch structure, callsite shape, and control flow.

   Keep upstream local names too. Do not rename values such as `elementsToAdd`, `initialElements`, or `allElements` just for local preference.

2. Keep package-private `Extensions.java` focused on minimal bridge methods for missing Java instance methods. General-purpose helpers belong elsewhere, typically under `internal`.

3. When Java lacks a Kotlin stdlib operation used directly upstream, introduce the minimal helper needed to keep usage sites direct.

4. When porting Kotlin stdlib operations into Java helper methods, apply the same direct-port standard used for upstream library code. Keep names, structure, semantics, and performance characteristics aligned as closely as Java allows. Helper layering is acceptable when it mirrors upstream layering or cleanly composes distinct upstream operations.

5. Preserve upstream generic shape as closely as Java allows. When upstream declares a helper or API with explicit type parameters, prefer a correspondingly generic Java signature over wildcard-based erasure unless Java requires otherwise.

6. Preserve Kotlin variance semantics, not just surface syntax. When upstream relies on declaration-site variance, translate it to the corresponding Java wildcard shape so the effective producer/consumer contract stays aligned.

7. Preserve upstream nullness in type-use positions. For arrays, distinguish nullable elements from a nullable array reference: `@Nullable Object[]`, `Object @Nullable []`, or `@Nullable Object @Nullable []`.

8. If a translation would widen or otherwise loosen the upstream type shape, signature shape, or semantic contract for convenience rather than necessity, stop and ask instead of making the change unilaterally. Only do it without asking when Java clearly forces it.

9. After incremental porting, do a whole-method upstream comparison and remove transitional logic not present upstream or in [porting-deviations.md](porting-deviations.md).

10. Temporary stubs should fail immediately with `UnsupportedOperationException("API phase stub")`. Do not invent placeholder behavior, snapshot state, or speculative implementation logic.

11. Fix signatures and declared nullness before adding local non-null assertions. When upstream uses a Kotlin non-null cast such as `as Array<Any?>` or an explicit `!!`, translate that assertion with `Objects.requireNonNull(...)` rather than a plain Java cast.

12. Port upstream KDoc to Markdown JavaDoc using `///` documentation comments. Do not use `/** ... */` for JavaDoc. Preserve wording except for JavaDoc syntax. Convert resolvable program-element references to JavaDoc links, parameters and local names to code text, and leave upstream typos unchanged unless they make JavaDoc invalid. For API members declared only because Java cannot inherit Kotlin collection interfaces, port the inherited Kotlin stdlib KDoc.

## Java Conventions

1. Prefer `Collections.addAll(...)` over a manual array loop when it is the clearest Java equivalent of the upstream operation.

2. Prefer existing JDK utilities, especially `java.util.Objects` and `java.util.Collections`, over local equivalent helpers when they directly match the needed semantics. For null-safe equality, prefer `Objects.equals(left, right)` over a manual comparison.

3. When a port needs an explicit non-null assertion analogous to Kotlin `!!`, use `Objects.requireNonNull(...)` rather than a manual null check followed by `throw new NullPointerException()`.

4. Import top-level classes instead of using fully qualified names inline. This applies to JDK classes such as `java.util.Objects` too.

5. Every `Object.equals(...)` override must declare its parameter as `@Nullable Object`. Shared helpers that model `equals` logic should take `@Nullable Object` too.

6. Prefer `var` over explicit local variable types unless an explicit type materially improves readability, disambiguation, or reviewability.

7. When Java allows the desired generic type directly in an `instanceof` pattern, use it there instead of `<?>` plus a cast and `@SuppressWarnings("unchecked")`.

8. When a generic vararg factory also serves as an empty factory upstream, add a zero-arg overload to avoid repeated generic-vararg workarounds.

9. When porting a Kotlin `if` expression that selects between two values, prefer a Java ternary when it stays readable and keeps the structure closer to upstream.

10. When porting upstream hash-trie lookup, update, removal, collision, or iterator-repair paths that compute the hash of a key or element, preserve upstream's direct `x.hashCode()` call shape as closely as Java null semantics allow. Use the inline Java form `x == null ? 0 : x.hashCode()` rather than `Objects.hashCode(x)` on these hot structural paths. Do not apply this rule to ordinary `hashCode()` implementations, view hash aggregation, `Map.Entry` hash code calculations, or other non-structural aggregate hashing. In those places, `Objects.hashCode(...)` remains the preferred Java equivalent for nullable values.

11. When upstream uses a Kotlin `inline` helper to remove a callback from a hot implementation loop, do not translate it to a Java functional-interface callback. Prefer preserving the primitive loop at the Java usage site. If the loop body is large, it may be extracted into a named helper, but the loop-control abstraction itself should not allocate or box.

12. Do not introduce internal Java helpers whose main purpose is to model an upstream Kotlin `inline` control abstraction. Kotlin inline helpers erase callback boundaries at usage sites; a Java helper taking `Consumer`, `Predicate`, `Function`, or similar functional interfaces usually changes the hot-path cost model. Prefer expanding the upstream inline body at the Java usage site, or extracting only a named non-callback body helper when needed for readability.

## Execution Rules

1. Keep going through the next obvious implementation and test steps. Stop only for real decisions, meaningful checkpoints, or external help.

2. Port tests alongside implementation slices, not as a separate bulk phase. When a behavior slice is implemented, port the corresponding upstream tests for that slice before moving on.

3. Apply the same direct-port standard to test code. Keep test structure, coverage intent, and helper usage aligned with upstream as closely as Java and JUnit allow.

4. In tests, use JUnit assertions, assumptions, and helpers where required for a clean Java translation. Prefer the closest JUnit equivalent of the upstream Kotlin test construct over custom assertion style.

5. The end goal for tests is a strict upstream-shaped port, just like production code. Temporary regrouping during execution is acceptable, but do not let it become the final structure.

6. When an upstream test relies on cross-type equality that this Java API intentionally does not support, rewrite it to the meaningful Java assertion and add a short local comment explaining the deviation.

7. When upstream tests collection views, keep the Java port testing views directly. Do not materialize views into JDK collections unless upstream also materializes them or Java has no direct equivalent. If cross-type equality is unavailable because port types do not extend JDK collection interfaces, translate the assertion into direct size, containment, and iteration checks over the original view.

8. Put permanent downstream-only tests in `test/extra` under `test/extra/src`, using packages under `tests`.

9. Add upstream source links only when the port is non-obvious, when a concrete deviation is being documented, or when a Java helper bridges missing Kotlin stdlib behavior. Do not add per-method links by default.

10. Re-run compile and tests whenever they provide useful feedback for the current slice.

11. Put temporary files and scratch directories under `.agents/`, not in the repo root. Use the repo-relative `.agents/out` directory for Mill output.

12. Keep production and test stdlib-bridge helpers separate. Production code uses `Extensions`; test-only helper bridges should live in `TestExtensions`, not another `Extensions` class.

13. When a Java port type does not extend the corresponding JDK interface, check specialized equality branches for port types before the generic `instanceof Map` / `Set` / `List` fallback.

## Benchmark Porting

1. Preserve upstream benchmark parameters, setup data, benchmark includes, warmup and measurement configuration, fork count, benchmark mode, and time unit. If a Java translation cannot reproduce upstream-generated data exactly, document the mismatch at the usage site and do not use that benchmark for precise upstream/downstream comparisons.

2. Keep Mill benchmark run tasks aligned with the corresponding upstream Gradle benchmark configurations:
   - `benchmarks.runFast` mirrors upstream `benchmark.configurations.fast` for the JVM target.
   - `benchmarks.runFull` mirrors upstream `benchmark.configurations.main` for the JVM target.
   - When upstream changes included benchmark methods, parameter values, warmup or measurement counts, iteration duration, benchmark mode, time unit, or fork count, update the corresponding Mill task in the same port.

3. `benchmarks.compareFast` is downstream-specific comparison tooling. It may run upstream first, reuse upstream's reported JMH JVM for the downstream run, read JSON reports, and print ratios. Keep its benchmark selection and JMH settings aligned with `benchmarks.runFast` and upstream `benchmark.configurations.fast`.

4. `benchmarks.runSmoke` is downstream-only benchmark wiring coverage. It is allowed to use a deliberately tiny subset and short timing settings, but it must not be used for performance conclusions. Keep benchmark run tasks opt-in and out of normal `build`; `runSmoke` may be run by CI as a separate benchmark-smoke job.

## Read Local Sources First

1. Before searching the web for upstream library or JDK behavior, prefer local source bundles and local checkouts.

2. For JDK sources, use the `src.zip` belonging to the JDK relevant to the question. Prefer `$JAVA_HOME/lib/src.zip` when it matches; otherwise locate the matching installed JDK first.

3. Do not assume the active runtime JDK is the one whose sources you need. Choose the source bundle that matches the question.

4. For Kotlin stdlib sources, prefer local `-sources.jar` artifacts from the build/dependency cache, such as Gradle or Coursier caches, before using the web.

5. The `kotlin-stdlib-jdk7` / `kotlin-stdlib-jdk8` artifacts are shim layers and usually not the right place to start. Prefer `kotlin-stdlib` and `kotlin-stdlib-common` first.

6. Useful local archive inspection commands:
   - `jar tf <archive>` to list files in `src.zip` or `*-sources.jar`
   - `jar xf <archive> <path-inside-archive>` to extract a specific source file
   - PowerShell `Expand-Archive` to unpack an archive to a temporary directory when needed

7. A local JetBrains `kotlin` checkout is also acceptable.

8. Fall back to web lookup only after checking the relevant local sources.
