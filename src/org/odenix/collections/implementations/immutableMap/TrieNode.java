/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableMap;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.internal.DeltaCounter;
import org.odenix.collections.internal.MutabilityOwnership;

public final class TrieNode<K extends @Nullable Object, V extends @Nullable Object> {
  static final int MAX_BRANCHING_FACTOR = 32;
  static final int LOG_MAX_BRANCHING_FACTOR = 5;
  static final int MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1;
  static final int ENTRY_SIZE = 2;
  static final int MAX_SHIFT = 30;

  /// Gets trie index segment of the specified {@code index} at the level specified by {@code shift}.
  ///
  /// `shift` equal to zero corresponds to the root level.
  /// For each lower level `shift` increments by [LOG_MAX_BRANCHING_FACTOR][#LOG_MAX_BRANCHING_FACTOR].
  static int indexSegment(int index, int shift) {
    return (index >> shift) & MAX_BRANCHING_FACTOR_MINUS_ONE;
  }

  private static @Nullable Object[] insertEntryAtIndex(
      @Nullable Object[] buffer, int keyIndex, @Nullable Object key, @Nullable Object value) {
    var newBuffer = new @Nullable Object[buffer.length + ENTRY_SIZE];
    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex);
    System.arraycopy(buffer, keyIndex, newBuffer, keyIndex + ENTRY_SIZE, buffer.length - keyIndex);
    newBuffer[keyIndex] = key;
    newBuffer[keyIndex + 1] = value;
    return newBuffer;
  }

  private static @Nullable Object[] replaceEntryWithNode(
      @Nullable Object[] buffer, int keyIndex, int nodeIndex, TrieNode<?, ?> newNode) {
    var newNodeIndex = nodeIndex - ENTRY_SIZE;
    var newBuffer = new @Nullable Object[buffer.length - ENTRY_SIZE + 1];
    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex);
    System.arraycopy(buffer, keyIndex + ENTRY_SIZE, newBuffer, keyIndex, nodeIndex - keyIndex - ENTRY_SIZE);
    newBuffer[newNodeIndex] = newNode;
    System.arraycopy(
        buffer, nodeIndex, newBuffer, newNodeIndex + 1, buffer.length - nodeIndex);
    return newBuffer;
  }

  private static @Nullable Object[] replaceNodeWithEntry(
      @Nullable Object[] buffer, int nodeIndex, int keyIndex, @Nullable Object key, @Nullable Object value) {
    var newBuffer = Arrays.copyOf(buffer, buffer.length + 1);
    System.arraycopy(buffer, nodeIndex + 1, newBuffer, nodeIndex + 2, buffer.length - nodeIndex - 1);
    System.arraycopy(buffer, keyIndex, newBuffer, keyIndex + 2, nodeIndex - keyIndex);
    newBuffer[keyIndex] = key;
    newBuffer[keyIndex + 1] = value;
    return newBuffer;
  }

  private static @Nullable Object[] removeEntryAtIndex(@Nullable Object[] buffer, int keyIndex) {
    var newBuffer = new @Nullable Object[buffer.length - ENTRY_SIZE];
    System.arraycopy(buffer, 0, newBuffer, 0, keyIndex);
    System.arraycopy(buffer, keyIndex + ENTRY_SIZE, newBuffer, keyIndex, buffer.length - keyIndex - ENTRY_SIZE);
    return newBuffer;
  }

  private static @Nullable Object[] removeNodeAtIndex(@Nullable Object[] buffer, int nodeIndex) {
    var newBuffer = new @Nullable Object[buffer.length - 1];
    System.arraycopy(buffer, 0, newBuffer, 0, nodeIndex);
    System.arraycopy(buffer, nodeIndex + 1, newBuffer, nodeIndex, buffer.length - nodeIndex - 1);
    return newBuffer;
  }

  static final class ModificationResult<
          K extends @Nullable Object, V extends @Nullable Object> {
    TrieNode<K, V> node;
    final int sizeDelta;

    ModificationResult(TrieNode<K, V> node, int sizeDelta) {
      this.node = node;
      this.sizeDelta = sizeDelta;
    }

  }

  @SuppressWarnings("rawtypes")
  static final TrieNode EMPTY = new TrieNode<>(0, 0, new @Nullable Object[0], null);

  private int dataMap;
  private int nodeMap;
  @Nullable Object[] buffer;
  private final @Nullable MutabilityOwnership ownedBy;

  TrieNode(int dataMap, int nodeMap, @Nullable Object[] buffer) {
    this(dataMap, nodeMap, buffer, null);
  }

  TrieNode(
      int dataMap,
      int nodeMap,
      @Nullable Object[] buffer,
      @Nullable MutabilityOwnership ownedBy) {
    this.dataMap = dataMap;
    this.nodeMap = nodeMap;
    this.buffer = buffer;
    this.ownedBy = ownedBy;
  }

  private ModificationResult<K, V> asInsertResult() {
    return new ModificationResult<>(this, 1);
  }

  private ModificationResult<K, V> asUpdateResult() {
    return new ModificationResult<>(this, 0);
  }

  /// Returns number of entries stored in this trie node (not counting subnodes)
  int entryCount() {
    return Integer.bitCount(dataMap);
  }

  /// Returns true if the data bit map has the bit specified by {@code positionMask} set, indicating there's a data entry in the buffer at that position.
  boolean hasEntryAt(int positionMask) {
    return (dataMap & positionMask) != 0;
  }

  /// Returns true if the node bit map has the bit specified by {@code positionMask} set, indicating there's a subtrie node in the buffer at that position.
  private boolean hasNodeAt(int positionMask) {
    return (nodeMap & positionMask) != 0;
  }

  /// Gets the index in buffer of the data entry key corresponding to the position specified by {@code positionMask}.
  int entryKeyIndex(int positionMask) {
    return ENTRY_SIZE * Integer.bitCount(dataMap & (positionMask - 1));
  }

  /// Gets the index in buffer of the subtrie node entry corresponding to the position specified by {@code positionMask}.
  int nodeIndex(int positionMask) {
    return buffer.length - 1 - Integer.bitCount(nodeMap & (positionMask - 1));
  }

  /// Retrieves the buffer element at the given {@code keyIndex} as key of a data entry.
  private K keyAtIndex(int keyIndex) {
    @SuppressWarnings("unchecked")
    var key = (K) buffer[keyIndex];
    return key;
  }

  /// Retrieves the buffer element next to the given {@code keyIndex} as value of a data entry.
  V valueAtKeyIndex(int keyIndex) {
    @SuppressWarnings("unchecked")
    var value = (V) buffer[keyIndex + 1];
    return value;
  }

  /// Retrieves the buffer element at the given {@code nodeIndex} as subtrie node.
  TrieNode<K, V> nodeAtIndex(int nodeIndex) {
    @SuppressWarnings("unchecked")
    var node = (TrieNode<K, V>) Objects.requireNonNull(buffer[nodeIndex]);
    return node;
  }

  private TrieNode<K, V> insertEntryAt(int positionMask, K key, V value) {
    var keyIndex = entryKeyIndex(positionMask);
    var newBuffer = insertEntryAtIndex(buffer, keyIndex, key, value);
    return new TrieNode<>(dataMap | positionMask, nodeMap, newBuffer);
  }

  private TrieNode<K, V> mutableInsertEntryAt(
      int positionMask, K key, V value, MutabilityOwnership owner) {
    var keyIndex = entryKeyIndex(positionMask);
    if (ownedBy == owner) {
      buffer = insertEntryAtIndex(buffer, keyIndex, key, value);
      dataMap |= positionMask;
      return this;
    }
    var newBuffer = insertEntryAtIndex(buffer, keyIndex, key, value);
    return new TrieNode<>(dataMap | positionMask, nodeMap, newBuffer, owner);
  }

  private TrieNode<K, V> updateValueAtIndex(int keyIndex, V value) {
    var newBuffer = buffer.clone();
    newBuffer[keyIndex + 1] = value;
    return new TrieNode<>(dataMap, nodeMap, newBuffer);
  }

  private TrieNode<K, V> mutableUpdateValueAtIndex(
      int keyIndex, V value, PersistentHashMapBuilder<K, V> mutator) {
    if (ownedBy == mutator.ownership) {
      buffer[keyIndex + 1] = value;
      return this;
    }
    mutator.modCount++;
    var newBuffer = buffer.clone();
    newBuffer[keyIndex + 1] = value;
    return new TrieNode<>(dataMap, nodeMap, newBuffer, mutator.ownership);
  }

  /// The given {@code newNode} must not be a part of any persistent map instance.
  private TrieNode<K, V> updateNodeAtIndex(
      int nodeIndex, int positionMask, TrieNode<K, V> newNode, @Nullable MutabilityOwnership owner) {
    var newNodeBuffer = newNode.buffer;
    if (newNodeBuffer.length == 2 && newNode.nodeMap == 0) {
      if (buffer.length == 1) {
        newNode.dataMap = nodeMap;
        return newNode;
      }

      var keyIndex = entryKeyIndex(positionMask);
      var newBuffer =
          replaceNodeWithEntry(buffer, nodeIndex, keyIndex, newNodeBuffer[0], newNodeBuffer[1]);
      return new TrieNode<>(dataMap ^ positionMask, nodeMap ^ positionMask, newBuffer, owner);
    }

    if (owner != null && ownedBy == owner) {
      buffer[nodeIndex] = newNode;
      return this;
    }

    var newBuffer = buffer.clone();
    newBuffer[nodeIndex] = newNode;
    return new TrieNode<>(dataMap, nodeMap, newBuffer, owner);
  }

  private @Nullable TrieNode<K, V> removeNodeAtIndexInternal(int nodeIndex, int positionMask) {
    if (buffer.length == 1) {
      return null;
    }
    var newBuffer = removeNodeAtIndex(buffer, nodeIndex);
    return new TrieNode<>(dataMap, nodeMap ^ positionMask, newBuffer);
  }

  private @Nullable TrieNode<K, V> mutableRemoveNodeAtIndex(
      int nodeIndex, int positionMask, MutabilityOwnership owner) {
    if (buffer.length == 1) {
      return null;
    }
    if (ownedBy == owner) {
      buffer = removeNodeAtIndex(buffer, nodeIndex);
      nodeMap ^= positionMask;
      return this;
    }
    var newBuffer = removeNodeAtIndex(buffer, nodeIndex);
    return new TrieNode<>(dataMap, nodeMap ^ positionMask, newBuffer, owner);
  }

  private @Nullable Object[] bufferMoveEntryToNode(
      int keyIndex, int positionMask, int newKeyHash, K newKey, V newValue, int shift, @Nullable MutabilityOwnership owner) {
    var storedKey = keyAtIndex(keyIndex);
    var storedKeyHash = storedKey == null ? 0 : storedKey.hashCode();
    var storedValue = valueAtKeyIndex(keyIndex);
    var newNode =
        makeNode(
            storedKeyHash,
            storedKey,
            storedValue,
            newKeyHash,
            newKey,
            newValue,
            shift + LOG_MAX_BRANCHING_FACTOR,
            owner);
    var nodeIndex = nodeIndex(positionMask) + 1;
    return replaceEntryWithNode(buffer, keyIndex, nodeIndex, newNode);
  }

  private TrieNode<K, V> moveEntryToNode(
      int keyIndex, int positionMask, int newKeyHash, K newKey, V newValue, int shift) {
    var newBuffer =
        bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, null);
    return new TrieNode<>(dataMap ^ positionMask, nodeMap | positionMask, newBuffer);
  }

  private TrieNode<K, V> mutableMoveEntryToNode(
      int keyIndex,
      int positionMask,
      int newKeyHash,
      K newKey,
      V newValue,
      int shift,
      MutabilityOwnership owner) {
    if (ownedBy == owner) {
      buffer =
          bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, owner);
      dataMap ^= positionMask;
      nodeMap |= positionMask;
      return this;
    }
    var newBuffer =
        bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, owner);
    return new TrieNode<>(dataMap ^ positionMask, nodeMap | positionMask, newBuffer, owner);
  }

  /// Creates a new TrieNode for holding two given key value entries
  private TrieNode<K, V> makeNode(
      int keyHash1,
      K key1,
      V value1,
      int keyHash2,
      K key2,
      V value2,
      int shift,
      @Nullable MutabilityOwnership owner) {
    if (shift > MAX_SHIFT) {
      return new TrieNode<>(0, 0, new @Nullable Object[] {key1, value1, key2, value2}, owner);
    }

    var setBit1 = indexSegment(keyHash1, shift);
    var setBit2 = indexSegment(keyHash2, shift);

    if (setBit1 != setBit2) {
      @Nullable Object[] nodeBuffer;
      if (setBit1 < setBit2) {
        nodeBuffer = new @Nullable Object[] {key1, value1, key2, value2};
      } else {
        nodeBuffer = new @Nullable Object[] {key2, value2, key1, value1};
      }
      return new TrieNode<>((1 << setBit1) | (1 << setBit2), 0, nodeBuffer, owner);
    }
    var node =
        makeNode(
            keyHash1,
            key1,
            value1,
            keyHash2,
            key2,
            value2,
            shift + LOG_MAX_BRANCHING_FACTOR,
            owner);
    return new TrieNode<>(0, 1 << setBit1, new @Nullable Object[] {node}, owner);
  }

  private @Nullable TrieNode<K, V> removeEntryAtIndexInternal(int keyIndex, int positionMask) {
    if (buffer.length == ENTRY_SIZE) {
      return null;
    }
    var newBuffer = removeEntryAtIndex(buffer, keyIndex);
    return new TrieNode<>(dataMap ^ positionMask, nodeMap, newBuffer);
  }

  private @Nullable TrieNode<K, V> mutableRemoveEntryAtIndex(
      int keyIndex, int positionMask, PersistentHashMapBuilder<K, V> mutator) {
    mutator.size--;
    mutator.operationResult = valueAtKeyIndex(keyIndex);
    if (buffer.length == ENTRY_SIZE) {
      return null;
    }
    if (ownedBy == mutator.ownership) {
      buffer = removeEntryAtIndex(buffer, keyIndex);
      dataMap ^= positionMask;
      return this;
    }
    var newBuffer = removeEntryAtIndex(buffer, keyIndex);
    return new TrieNode<>(dataMap ^ positionMask, nodeMap, newBuffer, mutator.ownership);
  }

  private @Nullable TrieNode<K, V> collisionRemoveEntryAtIndex(int index) {
    if (buffer.length == ENTRY_SIZE) {
      return null;
    }
    var newBuffer = removeEntryAtIndex(buffer, index);
    return new TrieNode<>(0, 0, newBuffer);
  }

  private @Nullable TrieNode<K, V> mutableCollisionRemoveEntryAtIndex(
      int index, PersistentHashMapBuilder<K, V> mutator) {
    mutator.size--;
    mutator.operationResult = valueAtKeyIndex(index);
    if (buffer.length == ENTRY_SIZE) {
      return null;
    }
    if (ownedBy == mutator.ownership) {
      buffer = removeEntryAtIndex(buffer, index);
      return this;
    }
    var newBuffer = removeEntryAtIndex(buffer, index);
    return new TrieNode<>(0, 0, newBuffer, mutator.ownership);
  }

  private int collisionKeyIndex(@Nullable Object key) {
    for (var i = 0; i < buffer.length; i += ENTRY_SIZE) {
      if (Objects.equals(key, keyAtIndex(i))) {
        return i;
      }
    }
    return -1;
  }

  private boolean collisionContainsKey(K key) {
    return collisionKeyIndex(key) != -1;
  }

  private @Nullable V collisionGet(K key) {
    var keyIndex = collisionKeyIndex(key);
    return keyIndex != -1 ? valueAtKeyIndex(keyIndex) : null;
  }

  private @Nullable ModificationResult<K, V> collisionPut(K key, V value) {
    var keyIndex = collisionKeyIndex(key);
    if (keyIndex != -1) {
      if (value == valueAtKeyIndex(keyIndex)) {
        return null;
      }
      var newBuffer = buffer.clone();
      newBuffer[keyIndex + 1] = value;
      return new TrieNode<K, V>(0, 0, newBuffer).asUpdateResult();
    }
    var newBuffer = insertEntryAtIndex(buffer, 0, key, value);
    return new TrieNode<K, V>(0, 0, newBuffer).asInsertResult();
  }

  private TrieNode<K, V> mutableCollisionPut(
      K key, V value, PersistentHashMapBuilder<K, V> mutator) {
    var keyIndex = collisionKeyIndex(key);
    if (keyIndex != -1) {
      mutator.operationResult = valueAtKeyIndex(keyIndex);
      if (ownedBy == mutator.ownership) {
        buffer[keyIndex + 1] = value;
        return this;
      }
      mutator.modCount++;
      var newBuffer = buffer.clone();
      newBuffer[keyIndex + 1] = value;
      return new TrieNode<>(0, 0, newBuffer, mutator.ownership);
    }
    mutator.size++;
    var newBuffer = insertEntryAtIndex(buffer, 0, key, value);
    return new TrieNode<>(0, 0, newBuffer, mutator.ownership);
  }

  private @Nullable TrieNode<K, V> collisionRemove(K key) {
    var keyIndex = collisionKeyIndex(key);
    return keyIndex != -1 ? collisionRemoveEntryAtIndex(keyIndex) : this;
  }

  private @Nullable TrieNode<K, V> mutableCollisionRemove(
      K key, PersistentHashMapBuilder<K, V> mutator) {
    var keyIndex = collisionKeyIndex(key);
    return keyIndex != -1 ? mutableCollisionRemoveEntryAtIndex(keyIndex, mutator) : this;
  }

  private @Nullable TrieNode<K, V> collisionRemove(K key, V value) {
    var keyIndex = collisionKeyIndex(key);
    if (keyIndex != -1 && Objects.equals(value, valueAtKeyIndex(keyIndex))) {
      return collisionRemoveEntryAtIndex(keyIndex);
    }
    return this;
  }

  private @Nullable TrieNode<K, V> mutableCollisionRemove(
      K key, V value, PersistentHashMapBuilder<K, V> mutator) {
    var keyIndex = collisionKeyIndex(key);
    if (keyIndex != -1 && Objects.equals(value, valueAtKeyIndex(keyIndex))) {
      return mutableCollisionRemoveEntryAtIndex(keyIndex, mutator);
    }
    return this;
  }

  private TrieNode<K, V> mutableCollisionPutAll(
      TrieNode<K, V> otherNode, DeltaCounter intersectionCounter, MutabilityOwnership owner) {
    var tempBuffer = Arrays.copyOf(buffer, buffer.length + otherNode.buffer.length);
    var i = buffer.length;
    for (var j = 0; j < otherNode.buffer.length; j += ENTRY_SIZE) {
      @SuppressWarnings("unchecked")
      var otherKey = (K) otherNode.buffer[j];
      if (!collisionContainsKey(otherKey)) {
        tempBuffer[i] = otherNode.buffer[j];
        tempBuffer[i + 1] = otherNode.buffer[j + 1];
        i += ENTRY_SIZE;
      } else {
        intersectionCounter.count++;
      }
    }

    var newSize = i;
    if (newSize == buffer.length) {
      return this;
    }
    if (newSize == otherNode.buffer.length) {
      return otherNode;
    }
    if (newSize == tempBuffer.length) {
      return new TrieNode<>(0, 0, tempBuffer, owner);
    }
    return new TrieNode<>(0, 0, Arrays.copyOf(tempBuffer, newSize), owner);
  }

  /// Updates the cell of this node at {@code positionMask} with entries from the cell of {@code otherNode} at {@code positionMask}.
  private TrieNode<K, V> mutablePutAllFromOtherNodeCell(
      TrieNode<K, V> otherNode,
      int positionMask,
      int shift,
      DeltaCounter intersectionCounter,
      PersistentHashMapBuilder<K, V> mutator) {
    if (hasNodeAt(positionMask)) {
      var targetNode = nodeAtIndex(nodeIndex(positionMask));
      if (otherNode.hasNodeAt(positionMask)) {
        var otherTargetNode = otherNode.nodeAtIndex(otherNode.nodeIndex(positionMask));
        return targetNode.mutablePutAll(
            otherTargetNode, shift + LOG_MAX_BRANCHING_FACTOR, intersectionCounter, mutator);
      }
      if (otherNode.hasEntryAt(positionMask)) {
        var keyIndex = otherNode.entryKeyIndex(positionMask);
        var key = otherNode.keyAtIndex(keyIndex);
        var value = otherNode.valueAtKeyIndex(keyIndex);
        var oldSize = mutator.size;
        var result =
            targetNode.mutablePut(
                key == null ? 0 : key.hashCode(), key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator);
        if (mutator.size == oldSize) {
          intersectionCounter.count++;
        }
        return result;
      }
      return targetNode;
    }

      if (otherNode.hasNodeAt(positionMask)) {
        var otherTargetNode = otherNode.nodeAtIndex(otherNode.nodeIndex(positionMask));
        if (hasEntryAt(positionMask)) {
          var keyIndex = entryKeyIndex(positionMask);
          var key = keyAtIndex(keyIndex);
          if (otherTargetNode.containsKey(
              key == null ? 0 : key.hashCode(), key, shift + LOG_MAX_BRANCHING_FACTOR)) {
            intersectionCounter.count++;
            return otherTargetNode;
          }
        var value = valueAtKeyIndex(keyIndex);
        return otherTargetNode.mutablePut(
            key == null ? 0 : key.hashCode(), key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator);
      }
      return otherTargetNode;
    }

    var thisKeyIndex = entryKeyIndex(positionMask);
    var thisKey = keyAtIndex(thisKeyIndex);
    var thisValue = valueAtKeyIndex(thisKeyIndex);
    var otherKeyIndex = otherNode.entryKeyIndex(positionMask);
    var otherKey = otherNode.keyAtIndex(otherKeyIndex);
    var otherValue = otherNode.valueAtKeyIndex(otherKeyIndex);
    return makeNode(
        thisKey == null ? 0 : thisKey.hashCode(),
        thisKey,
        thisValue,
        otherKey == null ? 0 : otherKey.hashCode(),
        otherKey,
        otherValue,
        shift + LOG_MAX_BRANCHING_FACTOR,
        mutator.ownership);
  }

  private int calculateSize() {
    if (nodeMap == 0) {
      return buffer.length / ENTRY_SIZE;
    }
    var numValues = Integer.bitCount(dataMap);
    var result = numValues;
    for (var i = numValues * ENTRY_SIZE; i < buffer.length; i++) {
      result += nodeAtIndex(i).calculateSize();
    }
    return result;
  }

  private boolean elementsIdentityEquals(TrieNode<K, V> otherNode) {
    if (this == otherNode) {
      return true;
    }
    if (nodeMap != otherNode.nodeMap || dataMap != otherNode.dataMap) {
      return false;
    }
    for (var i = 0; i < buffer.length; i++) {
      if (buffer[i] != otherNode.buffer[i]) {
        return false;
      }
    }
    return true;
  }

  boolean containsKey(int keyHash, K key, int shift) {
    var keyPositionMask = 1 << indexSegment(keyHash, shift);
    if (hasEntryAt(keyPositionMask)) {
      return Objects.equals(key, keyAtIndex(entryKeyIndex(keyPositionMask)));
    }
    if (hasNodeAt(keyPositionMask)) {
      var targetNode = nodeAtIndex(nodeIndex(keyPositionMask));
      if (shift == MAX_SHIFT) {
        return targetNode.collisionContainsKey(key);
      }
      return targetNode.containsKey(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR);
    }
    return false;
  }

  V get(int keyHash, K key, int shift) {
    var keyPositionMask = 1 << indexSegment(keyHash, shift);
    if (hasEntryAt(keyPositionMask)) {
      var keyIndex = entryKeyIndex(keyPositionMask);
      if (Objects.equals(key, keyAtIndex(keyIndex))) {
        return valueAtKeyIndex(keyIndex);
      }
      return null;
    }
    if (hasNodeAt(keyPositionMask)) {
      var targetNode = nodeAtIndex(nodeIndex(keyPositionMask));
      if (shift == MAX_SHIFT) {
        return targetNode.collisionGet(key);
      }
      return targetNode.get(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR);
    }
    return null;
  }

  TrieNode<K, V> mutablePutAll(
      TrieNode<K, V> otherNode,
      int shift,
      DeltaCounter intersectionCounter,
      PersistentHashMapBuilder<K, V> mutator) {
    if (this == otherNode) {
      intersectionCounter.count += calculateSize();
      return this;
    }
    if (shift > MAX_SHIFT) {
      return mutableCollisionPutAll(otherNode, intersectionCounter, mutator.ownership);
    }

    var newNodeMap = nodeMap | otherNode.nodeMap;
    var newDataMap = (dataMap ^ otherNode.dataMap) & ~newNodeMap;
    var mask = dataMap & otherNode.dataMap;
    var index = 0;
    while (mask != 0) {
      var positionMask = Integer.lowestOneBit(mask);
      var leftKey = keyAtIndex(entryKeyIndex(positionMask));
      var rightKey = otherNode.keyAtIndex(otherNode.entryKeyIndex(positionMask));
      if (Objects.equals(leftKey, rightKey)) {
        newDataMap |= positionMask;
      } else {
        newNodeMap |= positionMask;
      }
      index++;
      mask ^= positionMask;
    }

    TrieNode<K, V> mutableNode;
    if (ownedBy == mutator.ownership && dataMap == newDataMap && nodeMap == newNodeMap) {
      mutableNode = this;
    } else {
      var newBuffer =
          new @Nullable Object[Integer.bitCount(newDataMap) * ENTRY_SIZE + Integer.bitCount(newNodeMap)];
      mutableNode = new TrieNode<>(newDataMap, newNodeMap, newBuffer);
    }

    mask = newNodeMap;
    index = 0;
    while (mask != 0) {
      var positionMask = Integer.lowestOneBit(mask);
      var newNodeIndex = mutableNode.buffer.length - 1 - index;
      mutableNode.buffer[newNodeIndex] =
          mutableNode.mutablePutAllFromOtherNodeCell(
              otherNode, positionMask, shift, intersectionCounter, mutator);
      index++;
      mask ^= positionMask;
    }
    mask = newDataMap;
    index = 0;
    while (mask != 0) {
      var positionMask = Integer.lowestOneBit(mask);
      var newKeyIndex = index * ENTRY_SIZE;
      if (!otherNode.hasEntryAt(positionMask)) {
        var oldKeyIndex = entryKeyIndex(positionMask);
        mutableNode.buffer[newKeyIndex] = keyAtIndex(oldKeyIndex);
        mutableNode.buffer[newKeyIndex + 1] = valueAtKeyIndex(oldKeyIndex);
      } else {
        var oldKeyIndex = otherNode.entryKeyIndex(positionMask);
        mutableNode.buffer[newKeyIndex] = otherNode.keyAtIndex(oldKeyIndex);
        mutableNode.buffer[newKeyIndex + 1] = otherNode.valueAtKeyIndex(oldKeyIndex);
        if (hasEntryAt(positionMask)) {
          intersectionCounter.count++;
        }
      }
      index++;
      mask ^= positionMask;
    }

    if (elementsIdentityEquals(mutableNode)) {
      return this;
    }
    if (otherNode.elementsIdentityEquals(mutableNode)) {
      return otherNode;
    }
    return mutableNode;
  }

  @Nullable ModificationResult<K, V> put(int keyHash, K key, V value, int shift) {
    var keyPositionMask = 1 << indexSegment(keyHash, shift);
    if (hasEntryAt(keyPositionMask)) {
      var keyIndex = entryKeyIndex(keyPositionMask);
      if (Objects.equals(key, keyAtIndex(keyIndex))) {
        if (valueAtKeyIndex(keyIndex) == value) {
          return null;
        }
        return updateValueAtIndex(keyIndex, value).asUpdateResult();
      }
      return moveEntryToNode(keyIndex, keyPositionMask, keyHash, key, value, shift).asInsertResult();
    }
    if (hasNodeAt(keyPositionMask)) {
      var nodeIndex = nodeIndex(keyPositionMask);
      var targetNode = nodeAtIndex(nodeIndex);
      var putResult =
          shift == MAX_SHIFT
              ? targetNode.collisionPut(key, value)
              : targetNode.put(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR);
      if (putResult == null) {
        return null;
      }
      putResult.node = updateNodeAtIndex(nodeIndex, keyPositionMask, putResult.node, null);
      return putResult;
    }
    return insertEntryAt(keyPositionMask, key, value).asInsertResult();
  }

  TrieNode<K, V> mutablePut(
      int keyHash, K key, V value, int shift, PersistentHashMapBuilder<K, V> mutator) {
    var keyPositionMask = 1 << indexSegment(keyHash, shift);
    if (hasEntryAt(keyPositionMask)) {
      var keyIndex = entryKeyIndex(keyPositionMask);
      if (Objects.equals(key, keyAtIndex(keyIndex))) {
        mutator.operationResult = valueAtKeyIndex(keyIndex);
        if (valueAtKeyIndex(keyIndex) == value) {
          return this;
        }
        return mutableUpdateValueAtIndex(keyIndex, value, mutator);
      }
      mutator.size++;
      return mutableMoveEntryToNode(
          keyIndex, keyPositionMask, keyHash, key, value, shift, mutator.ownership);
    }
    if (hasNodeAt(keyPositionMask)) {
      var nodeIndex = nodeIndex(keyPositionMask);
      var targetNode = nodeAtIndex(nodeIndex);
      TrieNode<K, V> newNode;
      if (shift == MAX_SHIFT) {
        newNode = targetNode.mutableCollisionPut(key, value, mutator);
      } else {
        newNode = targetNode.mutablePut(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator);
      }
      if (targetNode == newNode) {
        return this;
      }
      return updateNodeAtIndex(nodeIndex, keyPositionMask, newNode, mutator.ownership);
    }
    mutator.size++;
    return mutableInsertEntryAt(keyPositionMask, key, value, mutator.ownership);
  }

  @Nullable TrieNode<K, V> remove(int keyHash, K key, int shift) {
    var keyPositionMask = 1 << indexSegment(keyHash, shift);
    if (hasEntryAt(keyPositionMask)) {
      var keyIndex = entryKeyIndex(keyPositionMask);
      if (Objects.equals(key, keyAtIndex(keyIndex))) {
        return removeEntryAtIndexInternal(keyIndex, keyPositionMask);
      }
      return this;
    }
    if (hasNodeAt(keyPositionMask)) {
      var nodeIndex = nodeIndex(keyPositionMask);
      var targetNode = nodeAtIndex(nodeIndex);
      @Nullable TrieNode<K, V> newNode;
      if (shift == MAX_SHIFT) {
        newNode = targetNode.collisionRemove(key);
      } else {
        newNode = targetNode.remove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR);
      }
      return replaceNode(targetNode, newNode, nodeIndex, keyPositionMask);
    }
    return this;
  }

  private @Nullable TrieNode<K, V> replaceNode(
      TrieNode<K, V> targetNode, @Nullable TrieNode<K, V> newNode, int nodeIndex, int positionMask) {
    if (newNode == null) {
      return removeNodeAtIndexInternal(nodeIndex, positionMask);
    }
    if (targetNode != newNode) {
      return updateNodeAtIndex(nodeIndex, positionMask, newNode, null);
    }
    return this;
  }

  @Nullable TrieNode<K, V> mutableRemove(
      int keyHash, K key, int shift, PersistentHashMapBuilder<K, V> mutator) {
    var keyPositionMask = 1 << indexSegment(keyHash, shift);
    if (hasEntryAt(keyPositionMask)) {
      var keyIndex = entryKeyIndex(keyPositionMask);
      if (Objects.equals(key, keyAtIndex(keyIndex))) {
        return mutableRemoveEntryAtIndex(keyIndex, keyPositionMask, mutator);
      }
      return this;
    }
    if (hasNodeAt(keyPositionMask)) {
      var nodeIndex = nodeIndex(keyPositionMask);
      var targetNode = nodeAtIndex(nodeIndex);
      @Nullable TrieNode<K, V> newNode;
      if (shift == MAX_SHIFT) {
        newNode = targetNode.mutableCollisionRemove(key, mutator);
      } else {
        newNode = targetNode.mutableRemove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR, mutator);
      }
      return mutableReplaceNode(targetNode, newNode, nodeIndex, keyPositionMask, mutator.ownership);
    }
    return this;
  }

  private @Nullable TrieNode<K, V> mutableReplaceNode(
      TrieNode<K, V> targetNode,
      @Nullable TrieNode<K, V> newNode,
      int nodeIndex,
      int positionMask,
      MutabilityOwnership owner) {
    if (newNode == null) {
      return mutableRemoveNodeAtIndex(nodeIndex, positionMask, owner);
    }
    if (targetNode == newNode && (newNode.buffer.length != ENTRY_SIZE || newNode.nodeMap != 0)) {
      return this;
    }
    return updateNodeAtIndex(nodeIndex, positionMask, newNode, owner);
  }

  @Nullable TrieNode<K, V> remove(int keyHash, K key, V value, int shift) {
    var keyPositionMask = 1 << indexSegment(keyHash, shift);
    if (hasEntryAt(keyPositionMask)) {
      var keyIndex = entryKeyIndex(keyPositionMask);
      if (Objects.equals(key, keyAtIndex(keyIndex)) && Objects.equals(value, valueAtKeyIndex(keyIndex))) {
        return removeEntryAtIndexInternal(keyIndex, keyPositionMask);
      }
      return this;
    }
    if (hasNodeAt(keyPositionMask)) {
      var nodeIndex = nodeIndex(keyPositionMask);
      var targetNode = nodeAtIndex(nodeIndex);
      @Nullable TrieNode<K, V> newNode;
      if (shift == MAX_SHIFT) {
        newNode = targetNode.collisionRemove(key, value);
      } else {
        newNode = targetNode.remove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR);
      }
      return replaceNode(targetNode, newNode, nodeIndex, keyPositionMask);
    }
    return this;
  }

  @Nullable TrieNode<K, V> mutableRemove(
      int keyHash, K key, V value, int shift, PersistentHashMapBuilder<K, V> mutator) {
    var keyPositionMask = 1 << indexSegment(keyHash, shift);
    if (hasEntryAt(keyPositionMask)) {
      var keyIndex = entryKeyIndex(keyPositionMask);
      if (Objects.equals(key, keyAtIndex(keyIndex)) && Objects.equals(value, valueAtKeyIndex(keyIndex))) {
        return mutableRemoveEntryAtIndex(keyIndex, keyPositionMask, mutator);
      }
      return this;
    }
    if (hasNodeAt(keyPositionMask)) {
      var nodeIndex = nodeIndex(keyPositionMask);
      var targetNode = nodeAtIndex(nodeIndex);
      @Nullable TrieNode<K, V> newNode;
      if (shift == MAX_SHIFT) {
        newNode = targetNode.mutableCollisionRemove(key, value, mutator);
      } else {
        newNode = targetNode.mutableRemove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator);
      }
      return mutableReplaceNode(targetNode, newNode, nodeIndex, keyPositionMask, mutator.ownership);
    }
    return this;
  }

  public <K1 extends @Nullable Object, V1 extends @Nullable Object> boolean equalsWith(
      TrieNode<K1, V1> that, BiPredicate<V, V1> equalityComparator) {
    if (this == that) {
      return true;
    }
    if (dataMap != that.dataMap || nodeMap != that.nodeMap) {
      return false;
    }
    if (dataMap == 0 && nodeMap == 0) {
      if (buffer.length != that.buffer.length) {
        return false;
      }
      for (var i = 0; i < buffer.length; i += ENTRY_SIZE) {
        var thatKey = that.keyAtIndex(i);
        var thatValue = that.valueAtKeyIndex(i);
        var keyIndex = collisionKeyIndex(thatKey);
        if (keyIndex != -1) {
          var value = valueAtKeyIndex(keyIndex);
          if (!equalityComparator.test(value, thatValue)) {
            return false;
          }
        } else {
          return false;
        }
      }
      return true;
    }

    var valueSize = Integer.bitCount(dataMap) * ENTRY_SIZE;
    for (var i = 0; i < valueSize; i += ENTRY_SIZE) {
      if (!Objects.equals(keyAtIndex(i), that.keyAtIndex(i))) {
        return false;
      }
      if (!equalityComparator.test(valueAtKeyIndex(i), that.valueAtKeyIndex(i))) {
        return false;
      }
    }
    for (var i = valueSize; i < buffer.length; i++) {
      if (!nodeAtIndex(i).equalsWith(that.nodeAtIndex(i), equalityComparator)) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("unused")
  void accept(Visitor<K, V> visitor) {
    accept(visitor, 0, 0);
  }

  private void accept(Visitor<K, V> visitor, int hash, int shift) {
    visitor.visit(this, shift, hash, dataMap, nodeMap);
    var nodePositions = nodeMap;
    while (nodePositions != 0) {
      var mask = Integer.lowestOneBit(nodePositions);
      var hashSegment = Integer.numberOfTrailingZeros(mask);
      var childNode = nodeAtIndex(nodeIndex(mask));
      childNode.accept(visitor, hash + (hashSegment << shift), shift + LOG_MAX_BRANCHING_FACTOR);
      nodePositions -= mask;
    }
  }

  interface Visitor<K extends @Nullable Object, V extends @Nullable Object> {
    void visit(TrieNode<K, V> node, int shift, int hash, int dataMap, int nodeMap);
  }
}
