/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.ImmutableSet;
import org.odenix.collections.PersistentSet;

public final class PersistentHashSet<E extends @Nullable Object>
    implements PersistentSet<E> {
  private static final PersistentHashSet<?> EMPTY =
      new PersistentHashSet<>(castEmptyNode(), 0);

  final TrieNode<E> node;
  private final int size;

  PersistentHashSet(TrieNode<E> node, int size) {
    this.node = node;
    this.size = size;
  }

  public static <E extends @Nullable Object> PersistentHashSet<E> emptyOf() {
    @SuppressWarnings("unchecked")
    var empty = (PersistentHashSet<E>) EMPTY;
    return empty;
  }

  @SuppressWarnings("unchecked")
  static <E extends @Nullable Object> TrieNode<E> castEmptyNode() {
    return (TrieNode<E>) TrieNode.EMPTY;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean contains(E element) {
    return node.contains(element == null ? 0 : element.hashCode(), element, 0);
  }

  @Override
  public boolean containsAll(Collection<? extends E> elements) {
    if (elements instanceof PersistentHashSetBuilder<?> builder) {
      @SuppressWarnings("unchecked")
      var persistentBuilder = (PersistentHashSetBuilder<E>) builder;
      return node.containsAll(persistentBuilder.node, 0);
    }
    for (var element : elements) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public PersistentHashSet<E> add(E element) {
    var newNode = node.add(element == null ? 0 : element.hashCode(), element, 0);
    if (node == newNode) {
      return this;
    }
    return new PersistentHashSet<>(newNode, size + 1);
  }

  @Override
  public PersistentHashSet<E> addAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return this;
    }
    var builder = builder();
    builder.addAll(elements);
    return builder.build();
  }

  @Override
  public PersistentHashSet<E> remove(E element) {
    var newNode = node.remove(element == null ? 0 : element.hashCode(), element, 0);
    if (node == newNode) {
      return this;
    }
    return new PersistentHashSet<>(newNode, size - 1);
  }

  @Override
  public PersistentHashSet<E> removeAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return this;
    }
    var builder = builder();
    builder.removeAll(elements);
    return builder.build();
  }

  @Override
  public PersistentHashSet<E> removeAll(Predicate<? super E> predicate) {
    var builder = builder();
    builder.removeIf(predicate);
    return builder.build();
  }

  @Override
  public PersistentHashSet<E> retainAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return emptyOf();
    }
    var builder = builder();
    builder.retainAll(elements);
    return builder.build();
  }

  @Override
  public PersistentHashSet<E> clear() {
    return emptyOf();
  }

  @Override
  public Iterator<E> iterator() {
    return new PersistentHashSetIterator<>(node);
  }

  @Override
  public PersistentHashSetBuilder<E> builder() {
    return new PersistentHashSetBuilder<>(this);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof ImmutableSet<?> set) {
      if (size != set.size()) {
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
    if (other instanceof PersistentHashSetBuilder<?> builder) {
      if (size != builder.size()) {
        return false;
      }
      for (var element : builder) {
        @SuppressWarnings("unchecked")
        var typed = (E) element;
        if (!contains(typed)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    var hashCode = 0;
    for (var element : this) {
      hashCode += Objects.hashCode(element);
    }
    return hashCode;
  }
}
