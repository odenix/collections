/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.persistentOrderedSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.PersistentSet;
import org.odenix.collections.implementations.immutableMap.PersistentHashMap;
import org.odenix.collections.implementations.immutableSet.PersistentHashSet;
import org.odenix.collections.implementations.immutableSet.PersistentHashSetBuilder;
import org.odenix.collections.internal.EndOfChain;

final class Links {
  final @Nullable Object previous;
  final @Nullable Object next;

  Links(@Nullable Object previous, @Nullable Object next) {
    this.previous = previous;
    this.next = next;
  }

  /// Constructs Links for a new single element
  Links() {
    this(EndOfChain.INSTANCE, EndOfChain.INSTANCE);
  }

  /// Constructs Links for a new last element
  Links(@Nullable Object previous) {
    this(previous, EndOfChain.INSTANCE);
  }

  Links withNext(@Nullable Object newNext) {
    return new Links(previous, newNext);
  }

  Links withPrevious(@Nullable Object newPrevious) {
    return new Links(newPrevious, next);
  }

  boolean hasNext() {
    return next != EndOfChain.INSTANCE;
  }

  boolean hasPrevious() {
    return previous != EndOfChain.INSTANCE;
  }
}

public final class PersistentOrderedSet<E extends @Nullable Object>
    implements PersistentSet<E> {
  private static final PersistentOrderedSet<?> EMPTY =
      new PersistentOrderedSet<>(EndOfChain.INSTANCE, EndOfChain.INSTANCE, PersistentHashMap.emptyOf());

  final @Nullable Object firstElement;
  final @Nullable Object lastElement;
  final PersistentHashMap<E, Links> hashMap;

  PersistentOrderedSet(
      @Nullable Object firstElement, @Nullable Object lastElement, PersistentHashMap<E, Links> hashMap) {
    this.firstElement = firstElement;
    this.lastElement = lastElement;
    this.hashMap = hashMap;
  }

  @SuppressWarnings("unchecked")
  public static <E extends @Nullable Object> PersistentOrderedSet<E> emptyOf() {
    return (PersistentOrderedSet<E>) EMPTY;
  }

  @Override
  public int size() {
    return hashMap.size();
  }

  @Override
  public boolean contains(E element) {
    return hashMap.containsKey(element);
  }

  @Override
  public boolean containsAll(Collection<? extends E> elements) {
    for (var element : elements) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public PersistentOrderedSet<E> add(E element) {
    if (hashMap.containsKey(element)) {
      return this;
    }
    if (isEmpty()) {
      var newMap = hashMap.put(element, new Links());
      return new PersistentOrderedSet<>(element, element, newMap);
    }
    @SuppressWarnings("unchecked")
    var typedLastElement = (E) lastElement;
    var lastLinks = Objects.requireNonNull(hashMap.get(typedLastElement));
    var newMap =
        hashMap.put(typedLastElement, lastLinks.withNext(element)).put(element, new Links(typedLastElement));
    return new PersistentOrderedSet<>(firstElement, element, newMap);
  }

  @Override
  public PersistentOrderedSet<E> addAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return this;
    }
    return (PersistentOrderedSet<E>) mutate(set -> set.addAll(elements));
  }

  @Override
  public PersistentOrderedSet<E> remove(E element) {
    var links = hashMap.get(element);
    if (links == null) {
      return this;
    }

    var newMap = hashMap.remove(element);
    if (links.hasPrevious()) {
      @SuppressWarnings("unchecked")
      var typedPrevious = (E) links.previous;
      var previousLinks = Objects.requireNonNull(newMap.get(typedPrevious));
      newMap = newMap.put(typedPrevious, previousLinks.withNext(links.next));
    }
    if (links.hasNext()) {
      @SuppressWarnings("unchecked")
      var typedNext = (E) links.next;
      var nextLinks = Objects.requireNonNull(newMap.get(typedNext));
      newMap = newMap.put(typedNext, nextLinks.withPrevious(links.previous));
    }
    var newFirstElement = !links.hasPrevious() ? links.next : firstElement;
    var newLastElement = !links.hasNext() ? links.previous : lastElement;
    return new PersistentOrderedSet<>(newFirstElement, newLastElement, newMap);
  }

  @Override
  public PersistentOrderedSet<E> removeAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return this;
    }
    return (PersistentOrderedSet<E>) mutate(set -> set.removeAll(elements));
  }

  @Override
  public PersistentOrderedSet<E> removeAll(Predicate<? super E> predicate) {
    return (PersistentOrderedSet<E>) mutate(set -> set.removeIf(predicate));
  }

  @Override
  public PersistentOrderedSet<E> retainAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return emptyOf();
    }
    return (PersistentOrderedSet<E>) mutate(set -> set.retainAll(elements));
  }

  @Override
  public PersistentOrderedSet<E> clear() {
    return emptyOf();
  }

  @Override
  public Iterator<E> iterator() {
    return new PersistentOrderedSetIterator<>(firstElement, hashMap);
  }

  @Override
  public PersistentOrderedSetBuilder<E> builder() {
    return new PersistentOrderedSetBuilder<>(this);
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
      return hashMap.node.equalsWith(persistentOrderedSet.hashMap.node, (a, b) -> true);
    }
    if (other instanceof PersistentOrderedSetBuilder<?> persistentOrderedSetBuilder) {
      if (size() != persistentOrderedSetBuilder.size()) {
        return false;
      }
      return hashMap.node.equalsWith(persistentOrderedSetBuilder.hashMapBuilder.node, (a, b) -> true);
    }
    if (other instanceof PersistentHashSet<?> persistentHashSet) {
      if (size() != persistentHashSet.size()) {
        return false;
      }
      for (var element : persistentHashSet) {
        @SuppressWarnings("unchecked")
        var typed = (E) element;
        if (!contains(typed)) {
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
        @SuppressWarnings("unchecked")
        var typed = (E) element;
        if (!contains(typed)) {
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
      @SuppressWarnings("unchecked")
      var typed = (E) element;
      if (!contains(typed)) {
        return false;
      }
    }
    return true;
  }

  /// We provide [equals][#equals(Object)], so as a matter of style, we should also provide [hashCode][#hashCode()].
  /// However, the implementation from [AbstractSet][java.util.AbstractSet] is enough.
  @Override
  public int hashCode() {
    var hashCode = 0;
    for (var element : this) {
      hashCode += Objects.hashCode(element);
    }
    return hashCode;
  }
}
