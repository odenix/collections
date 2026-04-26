/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedSet;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.PersistentSet;
import org.odenix.collections.implementations.immutableMap.PersistentHashMapBuilder;
import org.odenix.collections.implementations.immutableSet.PersistentHashSet;
import org.odenix.collections.implementations.immutableSet.PersistentHashSetBuilder;
import org.odenix.collections.internal.EndOfChain;

public final class PersistentOrderedSetBuilder<E extends @Nullable Object> extends AbstractSet<E>
    implements PersistentSet.Builder<E> {
  private @Nullable PersistentOrderedSet<E> builtSet;
  @Nullable Object firstElement;
  private @Nullable Object lastElement;
  final PersistentHashMapBuilder<E, Links> hashMapBuilder;

  public PersistentOrderedSetBuilder(PersistentOrderedSet<E> set) {
    builtSet = set;
    firstElement = set.firstElement;
    lastElement = set.lastElement;
    hashMapBuilder = set.hashMap.builder();
  }

  @Override
  public int size() {
    return hashMapBuilder.size();
  }

  @Override
  public PersistentOrderedSet<E> build() {
    if (builtSet != null) {
      assert hashMapBuilder.builtMap != null : "hashMapBuilder.builtMap != null";
      assert firstElement == builtSet.firstElement : "firstElement == builtSet.firstElement";
      assert lastElement == builtSet.lastElement : "lastElement == builtSet.lastElement";
      return builtSet;
    }

    assert hashMapBuilder.builtMap == null : "hashMapBuilder.builtMap == null";
    var newMap = hashMapBuilder.build();
    var newSet = new PersistentOrderedSet<>(firstElement, lastElement, newMap);
    builtSet = newSet;
    return newSet;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean contains(@Nullable Object element) {
    return hashMapBuilder.containsKey(element);
  }

  @Override
  public boolean add(E element) {
    if (hashMapBuilder.containsKey(element)) {
      return false;
    }
    builtSet = null;
    if (isEmpty()) {
      firstElement = element;
      lastElement = element;
      hashMapBuilder.put(element, new Links());
      return true;
    }

    @SuppressWarnings("unchecked")
    var typedLastElement = (E) lastElement;
    var lastLinks = Objects.requireNonNull(hashMapBuilder.get(typedLastElement));
    hashMapBuilder.put(typedLastElement, lastLinks.withNext(element));
    hashMapBuilder.put(element, new Links(lastElement));
    lastElement = element;
    return true;
  }

  @Override
  public boolean remove(@Nullable Object element) {
    var links = hashMapBuilder.remove(element);
    if (links == null) {
      return false;
    }
    builtSet = null;
    if (links.hasPrevious()) {
      @SuppressWarnings("unchecked")
      var typedPrevious = (E) links.previous;
      var previousLinks = Objects.requireNonNull(hashMapBuilder.get(typedPrevious));
      hashMapBuilder.put(typedPrevious, previousLinks.withNext(links.next));
    } else {
      firstElement = links.next;
    }
    if (links.hasNext()) {
      @SuppressWarnings("unchecked")
      var typedNext = (E) links.next;
      var nextLinks = Objects.requireNonNull(hashMapBuilder.get(typedNext));
      hashMapBuilder.put(typedNext, nextLinks.withPrevious(links.previous));
    } else {
      lastElement = links.previous;
    }
    return true;
  }

  @Override
  public void clear() {
    if (!hashMapBuilder.isEmpty()) {
      builtSet = null;
    }
    hashMapBuilder.clear();
    firstElement = EndOfChain.INSTANCE;
    lastElement = EndOfChain.INSTANCE;
  }

  @Override
  public Iterator<E> iterator() {
    return new PersistentOrderedSetMutableIterator<>(this);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof PersistentOrderedSet<?> persistentOrderedSet) {
      if (size() != persistentOrderedSet.size()) {
        return false;
      }
      return hashMapBuilder.node.equalsWith(persistentOrderedSet.hashMap.node, (a, b) -> true);
    }
    if (other instanceof PersistentOrderedSetBuilder<?> persistentOrderedSetBuilder) {
      if (size() != persistentOrderedSetBuilder.size()) {
        return false;
      }
      return hashMapBuilder.node.equalsWith(
          persistentOrderedSetBuilder.hashMapBuilder.node, (a, b) -> true);
    }
    if (other instanceof PersistentHashSet<?> persistentHashSet) {
      if (size() != persistentHashSet.size()) {
        return false;
      }
      for (var element : persistentHashSet) {
        if (!contains(element)) {
          return false;
        }
      }
      return true;
    }
    if (other instanceof PersistentHashSetBuilder<?> persistentHashSetBuilder) {
      if (size() != persistentHashSetBuilder.size()) {
        return false;
      }
      for (var element : persistentHashSetBuilder) {
        if (!contains(element)) {
          return false;
        }
      }
      return true;
    }
    if (!(other instanceof Set<?> set)) {
      return false;
    }
    if (size() != set.size()) {
      return false;
    }
    for (var element : set) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  /// We provide [equals][#equals(Object)], so as a matter of style, we should also provide [hashCode][#hashCode()].
  /// However, the implementation from [AbstractMutableSet][java.util.AbstractSet] is enough.
  @Override
  public int hashCode() {
    var hashCode = 0;
    for (var element : this) {
      hashCode += Objects.hashCode(element);
    }
    return hashCode;
  }
}
