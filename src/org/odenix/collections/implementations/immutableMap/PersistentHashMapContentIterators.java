/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

final class PersistentHashMapContentIterators {
  static final int TRIE_MAX_HEIGHT = 7;

  private PersistentHashMapContentIterators() {}

  @SuppressWarnings("unchecked")
  static <K extends @Nullable Object, V extends @Nullable Object>
      TrieNodeBaseIterator<K, V, Map.Entry<K, V>>[] entriesPath() {
    var path =
        (TrieNodeBaseIterator<K, V, Map.Entry<K, V>>[])
            new TrieNodeBaseIterator<?, ?, ?>[TRIE_MAX_HEIGHT + 1];
    for (var i = 0; i < path.length; i++) {
      path[i] = new TrieNodeEntriesIterator<>();
    }
    return path;
  }

  @SuppressWarnings("unchecked")
  static <K extends @Nullable Object, V extends @Nullable Object> TrieNodeBaseIterator<K, V, K>[] keysPath() {
    var path =
        (TrieNodeBaseIterator<K, V, K>[])
            new TrieNodeBaseIterator<?, ?, ?>[TRIE_MAX_HEIGHT + 1];
    for (var i = 0; i < path.length; i++) {
      path[i] = new TrieNodeKeysIterator<>();
    }
    return path;
  }

  @SuppressWarnings("unchecked")
  static <K extends @Nullable Object, V extends @Nullable Object> TrieNodeBaseIterator<K, V, V>[] valuesPath() {
    var path =
        (TrieNodeBaseIterator<K, V, V>[])
            new TrieNodeBaseIterator<?, ?, ?>[TRIE_MAX_HEIGHT + 1];
    for (var i = 0; i < path.length; i++) {
      path[i] = new TrieNodeValuesIterator<>();
    }
    return path;
  }
}

abstract class TrieNodeBaseIterator<
        K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object>
    implements Iterator<T> {
  @Nullable Object[] buffer = TrieNode.EMPTY.buffer;
  private int dataSize;
  int index;

  void reset(@Nullable Object[] buffer, int dataSize, int index) {
    this.buffer = buffer;
    this.dataSize = dataSize;
    this.index = index;
  }

  void reset(@Nullable Object[] buffer, int dataSize) {
    reset(buffer, dataSize, 0);
  }

  boolean hasNextKey() {
    return index < dataSize;
  }

  K currentKey() {
    assert hasNextKey() : "hasNextKey()";
    @SuppressWarnings("unchecked")
    var key = (K) buffer[index];
    return key;
  }

  void moveToNextKey() {
    assert hasNextKey() : "hasNextKey()";
    index += TrieNode.ENTRY_SIZE;
  }

  boolean hasNextNode() {
    assert index >= dataSize : "index >= dataSize";
    return index < buffer.length;
  }

  TrieNode<K, V> currentNode() {
    assert hasNextNode() : "hasNextNode()";
    @SuppressWarnings("unchecked")
    var node = Objects.requireNonNull((TrieNode<K, V>) buffer[index]);
    return node;
  }

  void moveToNextNode() {
    assert hasNextNode() : "hasNextNode()";
    index++;
  }

  @Override
  public boolean hasNext() {
    return hasNextKey();
  }
}

final class TrieNodeKeysIterator<K extends @Nullable Object, V extends @Nullable Object>
    extends TrieNodeBaseIterator<K, V, K> {
  @Override
  public K next() {
    assert hasNextKey() : "hasNextKey()";
    index += TrieNode.ENTRY_SIZE;
    @SuppressWarnings("unchecked")
    var key = (K) buffer[index - TrieNode.ENTRY_SIZE];
    return key;
  }
}

final class TrieNodeValuesIterator<K extends @Nullable Object, V extends @Nullable Object>
    extends TrieNodeBaseIterator<K, V, V> {
  @Override
  public V next() {
    assert hasNextKey() : "hasNextKey()";
    index += TrieNode.ENTRY_SIZE;
    @SuppressWarnings("unchecked")
    var value = (V) buffer[index - 1];
    return value;
  }
}

final class TrieNodeEntriesIterator<K extends @Nullable Object, V extends @Nullable Object>
    extends TrieNodeBaseIterator<K, V, Map.Entry<K, V>> {
  @Override
  public Map.Entry<K, V> next() {
    assert hasNextKey() : "hasNextKey()";
    index += TrieNode.ENTRY_SIZE;
    @SuppressWarnings("unchecked")
    var key = (K) buffer[index - TrieNode.ENTRY_SIZE];
    @SuppressWarnings("unchecked")
    var value = (V) buffer[index - 1];
    return new MapEntry<>(key, value);
  }
}

abstract class PersistentHashMapBaseIterator<
        K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object>
    implements Iterator<T> {
  protected final TrieNodeBaseIterator<K, V, T>[] path;
  protected int pathLastIndex;
  private boolean hasNext = true;

  PersistentHashMapBaseIterator(TrieNode<K, V> node, TrieNodeBaseIterator<K, V, T>[] path) {
    this.path = path;
    path[0].reset(node.buffer, TrieNode.ENTRY_SIZE * node.entryCount());
    pathLastIndex = 0;
    ensureNextEntryIsReady();
  }

  private int moveToNextNodeWithData(int pathIndex) {
    if (path[pathIndex].hasNextKey()) {
      return pathIndex;
    }
    if (path[pathIndex].hasNextNode()) {
      var node = path[pathIndex].currentNode();
      if (pathIndex == PersistentHashMapContentIterators.TRIE_MAX_HEIGHT - 1) {
        path[pathIndex + 1].reset(node.buffer, node.buffer.length);
      } else {
        path[pathIndex + 1].reset(node.buffer, TrieNode.ENTRY_SIZE * node.entryCount());
      }
      return moveToNextNodeWithData(pathIndex + 1);
    }
    return -1;
  }

  private void ensureNextEntryIsReady() {
    if (path[pathLastIndex].hasNextKey()) {
      return;
    }
    for (var i = pathLastIndex; i >= 0; i--) {
      var result = moveToNextNodeWithData(i);

      if (result == -1 && path[i].hasNextNode()) {
        path[i].moveToNextNode();
        result = moveToNextNodeWithData(i);
      }
      if (result != -1) {
        pathLastIndex = result;
        return;
      }
      if (i > 0) {
        path[i - 1].moveToNextNode();
      }
      path[i].reset(TrieNode.EMPTY.buffer, 0);
    }
    hasNext = false;
  }

  protected K currentKey() {
    checkHasNext();
    return path[pathLastIndex].currentKey();
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public T next() {
    checkHasNext();
    var result = path[pathLastIndex].next();
    ensureNextEntryIsReady();
    return result;
  }

  private void checkHasNext() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
  }
}

final class PersistentHashMapEntriesIterator<K extends @Nullable Object, V extends @Nullable Object>
    extends PersistentHashMapBaseIterator<K, V, Map.Entry<K, V>> {
  PersistentHashMapEntriesIterator(TrieNode<K, V> node) {
    super(node, PersistentHashMapContentIterators.entriesPath());
  }
}

final class PersistentHashMapKeysIterator<K extends @Nullable Object, V extends @Nullable Object>
    extends PersistentHashMapBaseIterator<K, V, K> {
  PersistentHashMapKeysIterator(TrieNode<K, V> node) {
    super(node, PersistentHashMapContentIterators.keysPath());
  }
}

final class PersistentHashMapValuesIterator<K extends @Nullable Object, V extends @Nullable Object>
    extends PersistentHashMapBaseIterator<K, V, V> {
  PersistentHashMapValuesIterator(TrieNode<K, V> node) {
    super(node, PersistentHashMapContentIterators.valuesPath());
  }
}
