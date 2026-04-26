/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

public final class TraceLog {
  private static final int INLINE_OPERATION_LIMIT = 200;
  private static final String TRACE_DIRECTORY_PROPERTY = "tests.randomized.traceDir";

  private final String name;
  private final int seed;
  private final List<String> operations = new ArrayList<>();

  public TraceLog(String name, int seed) {
    this.name = name;
    this.seed = seed;
  }

  public void record(String operation) {
    operations.add(operations.size() + ": " + operation);
  }

  public AssertionError failure(Throwable cause) {
    var message = new StringBuilder("Randomized trace failed for seed ")
        .append(seed)
        .append("\n");
    var tracePath = writeTraceFile();
    if (tracePath != null) {
      message.append("Full trace: ").append(tracePath.normalize()).append("\n");
    }
    var firstInlineOperation = Math.max(0, operations.size() - INLINE_OPERATION_LIMIT);
    if (firstInlineOperation > 0) {
      message
          .append("Showing last ")
          .append(INLINE_OPERATION_LIMIT)
          .append(" of ")
          .append(operations.size())
          .append(" operations.\n");
    }
    for (var index = firstInlineOperation; index < operations.size(); index++) {
      message.append(operations.get(index)).append("\n");
    }
    return new AssertionError(message.toString(), cause);
  }

  private @Nullable Path writeTraceFile() {
    var configuredDirectory = System.getProperty(TRACE_DIRECTORY_PROPERTY);
    if (configuredDirectory == null || configuredDirectory.isBlank()) {
      return null;
    }
    var outputDirectory = Path.of(configuredDirectory);
    try {
      Files.createDirectories(outputDirectory);
      var tracePath = outputDirectory.resolve(sanitizedName() + "-seed-" + seed + ".trace");
      Files.writeString(tracePath, traceText(), StandardCharsets.UTF_8);
      return tracePath;
    } catch (RuntimeException | java.io.IOException ignored) {
      return null;
    }
  }

  private String traceText() {
    var text = new StringBuilder()
        .append("name: ")
        .append(name)
        .append("\nseed: ")
        .append(seed)
        .append("\noperations:\n");
    for (var operation : operations) {
      text.append(operation).append("\n");
    }
    return text.toString();
  }

  private String sanitizedName() {
    return name.replaceAll("[^A-Za-z0-9_.-]", "_");
  }
}
