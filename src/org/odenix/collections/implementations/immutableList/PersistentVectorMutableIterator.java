/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import java.util.ConcurrentModificationException;

import org.jspecify.annotations.Nullable;

/// The class responsible for iterating over elements of the [PersistentVectorBuilder].
///
/// There are two parts where the elements of the builder are located: root and tail.
/// [TrieIterator] is responsible for iterating over elements located at root,
/// whereas tail elements are iterated directly from this class.
final class PersistentVectorMutableIterator<T extends @Nullable Object> extends AbstractListIterator<T> {
  private final PersistentVectorBuilder<T> builder;
  /// The modCount this iterator is aware of.
  /// Used to check if the [PersistentVectorBuilder] was modified outside this iterator.
  private int expectedModCount;
  /// Iterates over leaves of the builder.root trie.
  /// This property is equal to null if builder.root is null.
  private @Nullable TrieIterator<T> trieIterator;
  /// Index of the element this iterator returned from last invocation of next() or previous().
  /// Used to remove or set new value at this index.
  /// This property is set to -1 when method `add(element: T)` or `remove()` gets invoked.
  private int lastIteratedIndex = -1;

  PersistentVectorMutableIterator(PersistentVectorBuilder<T> builder, int index) {
    super(index, builder.size());
    this.builder = builder;
    this.expectedModCount = builder.getModCount();
    setupTrieIterator();
  }

  @Override
  public T previous() {
    checkForComodification();
    checkHasPrevious();

    lastIteratedIndex = index - 1;

    var trieIterator = this.trieIterator;
    if (trieIterator == null) {
      @SuppressWarnings("unchecked")
      var element = (T) builder.getTail()[--index];
      return element;
    }
    if (index > trieIterator.size) {
      @SuppressWarnings("unchecked")
      var element = (T) builder.getTail()[--index - trieIterator.size];
      return element;
    }
    index--;
    return trieIterator.previous();
  }

  @Override
  public T next() {
    checkForComodification();
    checkHasNext();

    lastIteratedIndex = index;

    var trieIterator = this.trieIterator;
    if (trieIterator == null) {
      @SuppressWarnings("unchecked")
      var element = (T) builder.getTail()[index++];
      return element;
    }
    if (trieIterator.hasNext()) {
      index++;
      return trieIterator.next();
    }
    @SuppressWarnings("unchecked")
    var element = (T) builder.getTail()[index++ - trieIterator.size];
    return element;
  }

  private void reset() {
    size = builder.size();
    expectedModCount = builder.getModCount();
    lastIteratedIndex = -1;
    setupTrieIterator();
  }

  private void setupTrieIterator() {
    var root = builder.getRoot();
    if (root == null) {
      trieIterator = null;
      return;
    }

    var trieSize = Utils.rootSize(builder.size());
    var trieIndex = Math.min(index, trieSize);
    var trieHeight = builder.getRootShift() / Utils.LOG_MAX_BUFFER_SIZE + 1;
    if (trieIterator == null) {
      trieIterator = new TrieIterator<>(root, trieIndex, trieSize, trieHeight);
    } else {
      trieIterator.reset(root, trieIndex, trieSize, trieHeight);
    }
  }

  @Override
  public void add(T element) {
    checkForComodification();
    builder.add(index, element);
    index++;
    reset();
  }

  @Override
  public void remove() {
    checkForComodification();
    checkHasIterated();

    builder.remove(lastIteratedIndex);
    if (lastIteratedIndex < index) {
      index = lastIteratedIndex;
    }
    reset();
  }

  @Override
  public void set(T element) {
    checkForComodification();
    checkHasIterated();

    builder.set(lastIteratedIndex, element);
    expectedModCount = builder.getModCount();
    setupTrieIterator();
  }

  private void checkForComodification() {
    if (expectedModCount != builder.getModCount()) {
      throw new ConcurrentModificationException();
    }
  }

  private void checkHasIterated() {
    if (lastIteratedIndex == -1) {
      throw new IllegalStateException();
    }
  }
}
