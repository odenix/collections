/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

final class TrieNodeMutableEntriesIterator<K extends @Nullable Object, V extends @Nullable Object>
    extends TrieNodeBaseIterator<K, V, Map.Entry<K, V>> {
  private final PersistentHashMapBuilderEntriesIterator<K, V> parentIterator;

  TrieNodeMutableEntriesIterator(PersistentHashMapBuilderEntriesIterator<K, V> parentIterator) {
    this.parentIterator = parentIterator;
  }

  @Override
  public Map.Entry<K, V> next() {
    assert hasNextKey() : "hasNextKey()";
    index += TrieNode.ENTRY_SIZE;
    @SuppressWarnings("unchecked")
    var key = (K) buffer[index - TrieNode.ENTRY_SIZE];
    @SuppressWarnings("unchecked")
    var value = (V) buffer[index - 1];
    return new MutableMapEntry<>(parentIterator, key, value);
  }
}

final class MutableMapEntry<K extends @Nullable Object, V extends @Nullable Object>
    extends MapEntry<K, V> {
  private final PersistentHashMapBuilderEntriesIterator<K, V> parentIterator;
  private V value;

  MutableMapEntry(
      PersistentHashMapBuilderEntriesIterator<K, V> parentIterator, K key, V value) {
    super(key, value);
    this.parentIterator = parentIterator;
    this.value = value;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V newValue) {
    var result = value;
    value = newValue;
    parentIterator.setValue(getKey(), newValue);
    return result;
  }
}

class PersistentHashMapBuilderBaseIterator<
        K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object>
    extends PersistentHashMapBaseIterator<K, V, T> {
  private final PersistentHashMapBuilder<K, V> builder;
  private @Nullable K lastIteratedKey;
  private boolean nextWasInvoked;
  private int expectedModCount;

  PersistentHashMapBuilderBaseIterator(
      PersistentHashMapBuilder<K, V> builder, TrieNodeBaseIterator<K, V, T>[] path) {
    super(builder.node, path);
    this.builder = builder;
    expectedModCount = builder.modCount;
  }

  @Override
  public T next() {
    checkForComodification();
    lastIteratedKey = currentKey();
    nextWasInvoked = true;
    return super.next();
  }

  @Override
  public void remove() {
    checkForComodification();
    checkNextWasInvoked();
    if (hasNext()) {
      var currentKey = currentKey();
      builder.remove(lastIteratedKey);
      resetPath(
          currentKey == null ? 0 : currentKey.hashCode(),
          builder.node,
          currentKey,
          0,
          lastIteratedKey == null ? 0 : lastIteratedKey.hashCode(),
          true);
    } else {
      builder.remove(lastIteratedKey);
    }

    lastIteratedKey = null;
    nextWasInvoked = false;
    expectedModCount = builder.modCount;
  }

  void setValue(K key, V newValue) {
    checkForComodification();
    if (!builder.containsKey(key)) {
      return;
    }

    if (hasNext()) {
      var currentKey = currentKey();
      builder.put(key, newValue);
      resetPath(currentKey == null ? 0 : currentKey.hashCode(), builder.node, currentKey, 0, 0, false);
    } else {
      builder.put(key, newValue);
    }

    expectedModCount = builder.modCount;
  }

  private void resetPath(
      int keyHash,
      TrieNode<?, ?> node,
      K key,
      int pathIndex,
      int removedKeyHash,
      boolean afterRemove) {
    var shift = pathIndex * TrieNode.LOG_MAX_BRANCHING_FACTOR;

    if (shift > TrieNode.MAX_SHIFT) {
      path[pathIndex].reset(node.buffer, node.buffer.length, 0);
      while (!Objects.equals(path[pathIndex].currentKey(), key)) {
        path[pathIndex].moveToNextKey();
      }
      pathLastIndex = pathIndex;
      return;
    }

    var keyPositionMask = 1 << TrieNode.indexSegment(keyHash, shift);

    if (node.hasEntryAt(keyPositionMask)) {
      var keyIndex = node.entryKeyIndex(keyPositionMask);
      var removedKeyPositionMask =
          afterRemove ? 1 << TrieNode.indexSegment(removedKeyHash, shift) : 0;

      if (keyPositionMask == removedKeyPositionMask && pathIndex < pathLastIndex) {
        path[pathLastIndex].reset(
            new @Nullable Object[] {node.buffer[keyIndex], node.buffer[keyIndex + 1]},
            TrieNode.ENTRY_SIZE);
        return;
      }

      path[pathIndex].reset(node.buffer, TrieNode.ENTRY_SIZE * node.entryCount(), keyIndex);
      pathLastIndex = pathIndex;
      return;
    }

    var nodeIndex = node.nodeIndex(keyPositionMask);
    var targetNode = node.nodeAtIndex(nodeIndex);
    path[pathIndex].reset(node.buffer, TrieNode.ENTRY_SIZE * node.entryCount(), nodeIndex);
    resetPath(keyHash, targetNode, key, pathIndex + 1, removedKeyHash, afterRemove);
  }

  private void checkNextWasInvoked() {
    if (!nextWasInvoked) {
      throw new IllegalStateException();
    }
  }

  private void checkForComodification() {
    if (builder.modCount != expectedModCount) {
      throw new ConcurrentModificationException();
    }
  }
}

final class PersistentHashMapBuilderEntriesIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements Iterator<Map.Entry<K, V>> {
  private final PersistentHashMapBuilderBaseIterator<K, V, Map.Entry<K, V>> base;

  PersistentHashMapBuilderEntriesIterator(PersistentHashMapBuilder<K, V> builder) {
    this.base = new PersistentHashMapBuilderBaseIterator<>(builder, entriesPath());
  }

  @SuppressWarnings("unchecked")
  private TrieNodeBaseIterator<K, V, Map.Entry<K, V>>[] entriesPath() {
    var path =
        (TrieNodeBaseIterator<K, V, Map.Entry<K, V>>[])
            new TrieNodeBaseIterator<?, ?, ?>[PersistentHashMapContentIterators.TRIE_MAX_HEIGHT
                + 1];
    for (var i = 0; i < path.length; i++) {
      path[i] = new TrieNodeMutableEntriesIterator<>(this);
    }
    return path;
  }

  @Override
  public boolean hasNext() {
    return base.hasNext();
  }

  @Override
  public Map.Entry<K, V> next() {
    return Objects.requireNonNull(base.next());
  }

  @Override
  public void remove() {
    base.remove();
  }

  void setValue(K key, V newValue) {
    base.setValue(key, newValue);
  }
}

final class PersistentHashMapBuilderKeysIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    extends PersistentHashMapBuilderBaseIterator<K, V, K> {
  PersistentHashMapBuilderKeysIterator(PersistentHashMapBuilder<K, V> builder) {
    super(builder, PersistentHashMapContentIterators.keysPath());
  }
}

final class PersistentHashMapBuilderValuesIterator<
        K extends @Nullable Object, V extends @Nullable Object>
    extends PersistentHashMapBuilderBaseIterator<K, V, V> {
  PersistentHashMapBuilderValuesIterator(PersistentHashMapBuilder<K, V> builder) {
    super(builder, PersistentHashMapContentIterators.valuesPath());
  }
}
