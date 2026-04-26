/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

public final class HashCodeTypes {
  public static final String ASCENDING_HASH_CODE = "ascending";
  public static final String RANDOM_HASH_CODE = "random";
  public static final String COLLISION_HASH_CODE = "collision";
  public static final String NON_EXISTING_HASH_CODE = "nonExisting";

  public static final String HASH_IMPL = "hash";
  public static final String ORDERED_IMPL = "ordered";

  private HashCodeTypes() {}

  private static List<ObjectWrapper<Integer>> intWrappers(int size, IntUnaryOperator hashCodeGenerator) {
    var keys = new ArrayList<ObjectWrapper<Integer>>();
    for (var i = 0; i < size; i++) {
      keys.add(new ObjectWrapper<>(i, hashCodeGenerator.applyAsInt(i)));
    }
    return keys;
  }

  private static List<ObjectWrapper<Integer>> generateIntWrappers(String hashCodeType, int size) {
    var random = new XorWowRandom(40, 40 >> 31);
    return switch (hashCodeType) {
      case ASCENDING_HASH_CODE -> intWrappers(size, index -> index);
      case RANDOM_HASH_CODE, NON_EXISTING_HASH_CODE -> intWrappers(size, index -> random.nextInt());
      case COLLISION_HASH_CODE -> intWrappers(size, index -> random.nextInt((size + 1) / 2));
      default -> throw new AssertionError("Unknown hashCodeType: " + hashCodeType);
    };
  }

  public static List<ObjectWrapper<Integer>> generateKeys(String hashCodeType, int size) {
    return generateIntWrappers(hashCodeType, size);
  }

  public static List<ObjectWrapper<Integer>> generateElements(String hashCodeType, int size) {
    return generateIntWrappers(hashCodeType, size);
  }

  /// Mirrors `kotlin.random.Random(seed)` so benchmark data matches upstream.
  private static final class XorWowRandom {
    private int x;
    private int y;
    private int z;
    private int w;
    private int v;
    private int addend;

    XorWowRandom(int seed1, int seed2) {
      this(seed1, seed2, 0, 0, ~seed1, (seed1 << 10) ^ (seed2 >>> 4));
    }

    XorWowRandom(int x, int y, int z, int w, int v, int addend) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
      this.v = v;
      this.addend = addend;
      for (var i = 0; i < 64; i++) {
        nextInt();
      }
    }

    int nextInt() {
      var t = x;
      t = t ^ (t >>> 2);
      x = y;
      y = z;
      z = w;
      var v0 = v;
      w = v0;
      t = (t ^ (t << 1)) ^ v0 ^ (v0 << 4);
      v = t;
      addend += 362437;
      return t + addend;
    }

    int nextInt(int until) {
      var n = until;
      if ((n & -n) == n) {
        var bitCount = 31 - Integer.numberOfLeadingZeros(n);
        return nextBits(bitCount);
      }
      int bits;
      int value;
      do {
        bits = nextInt() >>> 1;
        value = bits % n;
      } while (bits - value + (n - 1) < 0);
      return value;
    }

    private int nextBits(int bitCount) {
      return nextInt() >>> (32 - bitCount);
    }
  }
}
