/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableSet;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.ImmutableSet;
import org.odenix.collections.PersistentSet;
import org.odenix.collections.internal.DeltaCounter;
import org.odenix.collections.internal.MutabilityOwnership;

public final class PersistentHashSetBuilder<E extends @Nullable Object> extends AbstractSet<E>
    implements PersistentSet.Builder<E> {
  private @Nullable PersistentHashSet<E> builtSet;
  MutabilityOwnership ownership = new MutabilityOwnership();
  TrieNode<E> node;
  int modCount;
  private int size;

  public PersistentHashSetBuilder(PersistentHashSet<E> set) {
    this.builtSet = set;
    this.node = set.node;
    this.size = set.size();
  }

  void setSize(int size) {
    this.size = size;
    modCount++;
  }

  private void setNode(TrieNode<E> node) {
    if (node != this.node) {
      builtSet = null;
      this.node = node;
    }
  }

  @Override
  public PersistentHashSet<E> build() {
    if (builtSet == null) {
      var newlyBuiltSet = new PersistentHashSet<>(node, size);
      ownership = new MutabilityOwnership();
      builtSet = newlyBuiltSet;
    }
    return builtSet;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean contains(@Nullable Object element) {
    return node.contains(element == null ? 0 : element.hashCode(), element, 0);
  }

  @Override
  public boolean add(E element) {
    var size = this.size;
    var newNode = node.mutableAdd(element == null ? 0 : element.hashCode(), element, 0, this);
    setNode(newNode);
    return size != this.size;
  }

  @Override
  public boolean addAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return false;
    }
    if (elements instanceof PersistentHashSetBuilder<?> builder) {
      @SuppressWarnings("unchecked")
      var persistentBuilder = (PersistentHashSetBuilder<E>) builder;
      var deltaCounter = new DeltaCounter();
      var size = this.size;
      var result = node.mutableAddAll(persistentBuilder.node, 0, deltaCounter, this);
      var newSize = size + elements.size() - deltaCounter.count;
      if (size != newSize) {
        setNode(result);
        setSize(newSize);
      }
      return size != this.size;
    }
    return super.addAll(elements);
  }

  @Override
  public boolean retainAll(Collection<?> elements) {
    if (elements instanceof PersistentHashSetBuilder<?> builder) {
      var deltaCounter = new DeltaCounter();
      var size = this.size;
      @SuppressWarnings("unchecked")
      var persistentBuilder = (PersistentHashSetBuilder<E>) builder;
      var result = node.mutableRetainAll(persistentBuilder.node, 0, deltaCounter, this);
      var newSize = deltaCounter.count;
      if (newSize == 0) {
        clear();
      } else if (newSize != size) {
        @SuppressWarnings("unchecked")
        var newNode = Objects.requireNonNull((TrieNode<E>) result);
        setNode(newNode);
        setSize(newSize);
      }
      return size != this.size;
    }
    return super.retainAll(elements);
  }

  @Override
  public boolean removeAll(Collection<?> elements) {
    if (elements.isEmpty()) {
      return false;
    }
    if (elements instanceof PersistentHashSetBuilder<?> builder) {
      var counter = new DeltaCounter();
      var size = this.size;
      @SuppressWarnings("unchecked")
      var persistentBuilder = (PersistentHashSetBuilder<E>) builder;
      var result = node.mutableRemoveAll(persistentBuilder.node, 0, counter, this);
      var newSize = size - counter.count;
      if (newSize == 0) {
        clear();
      } else if (newSize != size) {
        @SuppressWarnings("unchecked")
        var newNode = Objects.requireNonNull((TrieNode<E>) result);
        setNode(newNode);
        setSize(newSize);
      }
      return size != this.size;
    }
    return super.removeAll(elements);
  }

  @Override
  public boolean containsAll(Collection<?> elements) {
    if (elements instanceof PersistentHashSetBuilder<?> builder) {
      @SuppressWarnings("unchecked")
      var persistentBuilder = (PersistentHashSetBuilder<E>) builder;
      return node.containsAll(persistentBuilder.node, 0);
    }
    return super.containsAll(elements);
  }

  @Override
  public boolean remove(@Nullable Object element) {
    var size = this.size;
    var newNode = node.mutableRemove(element == null ? 0 : element.hashCode(), element, 0, this);
    setNode(newNode);
    return size != this.size;
  }

  @Override
  public void clear() {
    setNode(PersistentHashSet.castEmptyNode());
    setSize(0);
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
        if (!contains(element)) {
          return false;
        }
      }
      return true;
    }
    return super.equals(other);
  }

  @Override
  public Iterator<E> iterator() {
    return new PersistentHashSetMutableIterator<>(this);
  }
}
