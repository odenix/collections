/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections.implementations.immutableList;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.PersistentList;
import org.odenix.collections.internal.ListImplementation;
import org.odenix.collections.internal.MutabilityOwnership;

public final class PersistentVectorBuilder<E extends @Nullable Object> extends AbstractList<E>
    implements PersistentList.Builder<E> {
  private @Nullable PersistentList<E> builtVector;
  private MutabilityOwnership ownership = new MutabilityOwnership();
  private @Nullable Object @Nullable [] root;
  private @Nullable Object[] tail;
  private int rootShift;
  private int size;

  public PersistentVectorBuilder(
      PersistentList<E> vector,
      @Nullable Object @Nullable [] vectorRoot,
      @Nullable Object[] vectorTail,
      int rootShift) {
    this.builtVector = vector;
    this.root = vectorRoot;
    this.tail = vectorTail;
    this.rootShift = rootShift;
    this.size = vector.size();
  }

  private int rootSize() {
    return size <= Utils.MAX_BUFFER_SIZE ? 0 : Utils.rootSize(size);
  }

  private int tailSize() {
    return size - rootSize();
  }

  private boolean isMutable(@Nullable Object[] buffer) {
    return buffer.length == Utils.MUTABLE_BUFFER_SIZE
        && buffer[Utils.MUTABLE_BUFFER_SIZE - 1] == ownership;
  }

  /// Checks if {@code buffer} is mutable and returns it or its mutable copy.
  private @Nullable Object[] makeMutable(@Nullable Object @Nullable [] buffer) {
    if (buffer == null) {
      return mutableBuffer();
    }
    if (isMutable(buffer)) {
      return buffer;
    }
    var mutableBuffer = mutableBuffer();
    System.arraycopy(buffer, 0, mutableBuffer, 0, Math.min(buffer.length, Utils.MAX_BUFFER_SIZE));
    return mutableBuffer;
  }

  private @Nullable Object[] makeMutableShiftingRight(@Nullable Object[] buffer, int distance) {
    if (isMutable(buffer)) {
      System.arraycopy(buffer, 0, buffer, distance, Utils.MAX_BUFFER_SIZE - distance);
      return buffer;
    }
    var mutableBuffer = mutableBuffer();
    System.arraycopy(buffer, 0, mutableBuffer, distance, Utils.MAX_BUFFER_SIZE - distance);
    return mutableBuffer;
  }

  private @Nullable Object[] mutableBufferWith(@Nullable Object element) {
    var buffer = new @Nullable Object[Utils.MUTABLE_BUFFER_SIZE];
    buffer[0] = element;
    buffer[Utils.MUTABLE_BUFFER_SIZE - 1] = ownership;
    return buffer;
  }

  private @Nullable Object[] mutableBuffer() {
    var buffer = new @Nullable Object[Utils.MUTABLE_BUFFER_SIZE];
    buffer[Utils.MUTABLE_BUFFER_SIZE - 1] = ownership;
    return buffer;
  }

  int getModCount() {
    return modCount;
  }

  @Nullable Object @Nullable [] getRoot() {
    return root;
  }

  @Nullable Object[] getTail() {
    return tail;
  }

  int getRootShift() {
    return rootShift;
  }

  private void modified() {
    builtVector = null;
    modCount++;
  }

  @Override
  public E get(int index) {
    ListImplementation.checkElementIndex(index, size);
    var buffer = bufferFor(index);
    @SuppressWarnings("unchecked")
    var element = (E) buffer[index & Utils.MAX_BUFFER_SIZE_MINUS_ONE];
    return element;
  }

  private @Nullable Object[] bufferFor(int index) {
    if (rootSize() <= index) {
      return tail;
    }
    var buffer = Objects.requireNonNull(root);
    var shift = rootShift;
    while (shift > 0) {
      buffer = (@Nullable Object[]) buffer[Utils.indexSegment(index, shift)];
      shift -= Utils.LOG_MAX_BUFFER_SIZE;
    }
    return buffer;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public E set(int index, E element) {
    ListImplementation.checkElementIndex(index, size);
    if (rootSize() <= index) {
      var mutableTail = makeMutable(tail);
      if (mutableTail != tail) {
        builtVector = null;
        modCount++;
      }
      var tailIndex = index & Utils.MAX_BUFFER_SIZE_MINUS_ONE;
      @SuppressWarnings("unchecked")
      var previous = (E) mutableTail[tailIndex];
      mutableTail[tailIndex] = element;
      tail = mutableTail;
      return previous;
    }

    var oldElementCarry = new ObjectRef(null);
    root = setInRoot(Objects.requireNonNull(root), rootShift, index, element, oldElementCarry);
    @SuppressWarnings("unchecked")
    var previous = (E) oldElementCarry.value;
    return previous;
  }

  private @Nullable Object[] setInRoot(
      @Nullable Object[] root, int shift, int index, E element, ObjectRef oldElementCarry) {
    var bufferIndex = Utils.indexSegment(index, shift);
    var mutableRoot = makeMutable(root);

    if (shift == 0) {
      if (mutableRoot != root) {
        builtVector = null;
        modCount++;
      }
      oldElementCarry.value = mutableRoot[bufferIndex];
      mutableRoot[bufferIndex] = element;
      return mutableRoot;
    }
    mutableRoot[bufferIndex] =
        setInRoot(
            Objects.requireNonNull((@Nullable Object[]) mutableRoot[bufferIndex]),
            shift - Utils.LOG_MAX_BUFFER_SIZE,
            index,
            element,
            oldElementCarry);
    return mutableRoot;
  }

  @Override
  public void add(int index, E element) {
    ListImplementation.checkPositionIndex(index, size);
    if (index == size) {
      add(element);
      return;
    }

    builtVector = null;
    modCount++;

    var rootSize = rootSize();
    if (index >= rootSize) {
      insertIntoTail(root, index - rootSize, element);
      return;
    }

    var elementCarry = new ObjectRef(null);
    var newRoot = insertIntoRoot(Objects.requireNonNull(root), rootShift, index, element, elementCarry);
    insertIntoTail(newRoot, 0, elementCarry.value);
  }

  /// Insert the specified {@code element} into the {@code root} trie at the specified trie {@code index}.
  /// {@code elementCarry} contains the last element of this trie that was popped out by the insertion operation.
  ///
  /// @return new root trie or this modified trie, if it's already mutable
  private @Nullable Object[] insertIntoRoot(
      @Nullable Object[] root, int shift, int index, @Nullable Object element, ObjectRef elementCarry) {
    var bufferIndex = Utils.indexSegment(index, shift);

    if (shift == 0) {
      elementCarry.value = root[Utils.MAX_BUFFER_SIZE_MINUS_ONE];
      var mutableRoot = makeMutable(root);
      System.arraycopy(root, bufferIndex, mutableRoot, bufferIndex + 1, Utils.MAX_BUFFER_SIZE_MINUS_ONE - bufferIndex);
      mutableRoot[bufferIndex] = element;
      return mutableRoot;
    }

    var mutableRoot = makeMutable(root);
    var lowerLevelShift = shift - Utils.LOG_MAX_BUFFER_SIZE;

    mutableRoot[bufferIndex] =
        insertIntoRoot(
            Objects.requireNonNull((@Nullable Object[]) mutableRoot[bufferIndex]),
            lowerLevelShift,
            index,
            element,
            elementCarry);

    for (var i = bufferIndex + 1; i < Utils.MAX_BUFFER_SIZE; i++) {
      if (mutableRoot[i] == null) {
        break;
      }
      mutableRoot[i] =
          insertIntoRoot(
              Objects.requireNonNull((@Nullable Object[]) mutableRoot[i]),
              lowerLevelShift,
              0,
              elementCarry.value,
              elementCarry);
    }

    return mutableRoot;
  }

  private void insertIntoTail(@Nullable Object[] root, int index, @Nullable Object element) {
    var tailSize = tailSize();
    var mutableTail = makeMutable(tail);
    if (tailSize < Utils.MAX_BUFFER_SIZE) {
      System.arraycopy(tail, index, mutableTail, index + 1, tailSize - index);
      mutableTail[index] = element;
      this.root = root;
      this.tail = mutableTail;
      size += 1;
      return;
    }

    var lastElement = tail[Utils.MAX_BUFFER_SIZE_MINUS_ONE];
    System.arraycopy(tail, index, mutableTail, index + 1, Utils.MAX_BUFFER_SIZE_MINUS_ONE - index);
    mutableTail[index] = element;
    pushFilledTail(root, mutableTail, mutableBufferWith(lastElement));
  }

  /// Appends the specified entirely filled {@code tail} as a leaf buffer to the next free position in the {@code root} trie.
  private void pushFilledTail(
      @Nullable Object @Nullable [] root, @Nullable Object[] filledTail, @Nullable Object[] newTail) {
    if (size >> Utils.LOG_MAX_BUFFER_SIZE > 1 << rootShift) {
      builtVector = null;
      modCount++;
      this.root = pushTail(mutableBufferWith(root), filledTail, rootShift + Utils.LOG_MAX_BUFFER_SIZE);
      tail = newTail;
      rootShift += Utils.LOG_MAX_BUFFER_SIZE;
      size += 1;
      return;
    }
    if (root == null) {
      builtVector = null;
      modCount++;
      this.root = filledTail;
      tail = newTail;
      size += 1;
      return;
    }
    builtVector = null;
    modCount++;
    this.root = pushTail(root, filledTail, rootShift);
    tail = newTail;
    size += 1;
  }

  /// Appends the specified entirely filled {@code tail} as a leaf buffer to the next free position in the {@code root} trie.
  ///
  /// The trie must not be filled entirely.
  private @Nullable Object[] pushTail(
      @Nullable Object @Nullable [] root, @Nullable Object[] tail, int shift) {
    var index = Utils.indexSegment(size - 1, shift);
    var mutableRoot = makeMutable(root);

    if (shift == Utils.LOG_MAX_BUFFER_SIZE) {
      mutableRoot[index] = tail;
    } else {
      mutableRoot[index] =
          pushTail(
              (@Nullable Object[]) mutableRoot[index],
              tail,
              shift - Utils.LOG_MAX_BUFFER_SIZE);
    }
    return mutableRoot;
  }

  @Override
  public E remove(int index) {
    ListImplementation.checkElementIndex(index, size);
    var rootSize = rootSize();
    var previous = get(index);
    builtVector = null;
    modCount++;
    var root = this.root;
    if (root == null) {
      var mutableTail = makeMutable(tail);
      System.arraycopy(tail, index + 1, mutableTail, index, size - index - 1);
      mutableTail[--size] = null;
      tail = mutableTail;
      return previous;
    }
    if (index >= rootSize) {
      removeFromTailAt(root, rootSize, rootShift, index - rootSize);
      return previous;
    }
    var elementCarry = new ObjectRef(tail[0]);
    var newRoot = removeFromRootAt(root, rootShift, index, elementCarry);
    removeFromTailAt(newRoot, rootSize, rootShift, 0);
    return previous;
  }

  private void removeFromTailAt(@Nullable Object[] root, int rootSize, int shift, int index) {
    var tailSize = size - rootSize;
    if (tailSize == 1) {
      pullLastBufferFromRoot(root, rootSize, shift);
      return;
    }
    var mutableTail = makeMutable(tail);
    System.arraycopy(tail, index + 1, mutableTail, index, tailSize - index - 1);
    mutableTail[tailSize - 1] = null;
    this.root = root;
    this.tail = mutableTail;
    size = rootSize + tailSize - 1;
    rootShift = shift;
  }

  /// Extracts the last entirely filled leaf buffer from the trie of this vector and makes it a tail in this
  /// Used when there are no elements left in current tail.
  /// Requires the trie to contain at least one leaf buffer.
  private void pullLastBufferFromRoot(@Nullable Object[] root, int rootSize, int shift) {
    if (shift == 0) {
      this.root = null;
      tail = root;
      size = rootSize;
      rootShift = shift;
      return;
    }

    var tailCarry = new ObjectRef(null);
    var newRoot = Objects.requireNonNull(pullLastBuffer(root, shift, rootSize, tailCarry));
    tail = Objects.requireNonNull((@Nullable Object[]) tailCarry.value);
    size = rootSize;

    if (newRoot[1] == null) {
      this.root = (@Nullable Object[]) newRoot[0];
      rootShift = shift - Utils.LOG_MAX_BUFFER_SIZE;
    } else {
      this.root = newRoot;
      rootShift = shift;
    }
  }

  /// Removes last leaf buffer from the given {@code root} trie.
  /// The leaf buffer is stored in the {@code tailCarry}.
  ///
  /// Returns the new root, potentially with decreased height.
  /// Returns `null` if the trie should become empty after removal.
  private @Nullable Object @Nullable [] pullLastBuffer(
      @Nullable Object[] root, int shift, int rootSize, ObjectRef tailCarry) {
    var bufferIndex = Utils.indexSegment(rootSize - 1, shift);

    @Nullable Object @Nullable [] newBufferAtIndex;
    if (shift == Utils.LOG_MAX_BUFFER_SIZE) {
      tailCarry.value = root[bufferIndex];
      newBufferAtIndex = null;
    } else {
      newBufferAtIndex =
          pullLastBuffer(
              Objects.requireNonNull((@Nullable Object[]) root[bufferIndex]),
              shift - Utils.LOG_MAX_BUFFER_SIZE,
              rootSize,
              tailCarry);
    }
    if (newBufferAtIndex == null && bufferIndex == 0) {
      return null;
    }

    var mutableRoot = makeMutable(root);
    mutableRoot[bufferIndex] = newBufferAtIndex;
    return mutableRoot;
  }

  /// Removes an element at the given {@code index} from the {@code root} trie.
  /// {@code tailCarry} contains the first element of the tail buffer, which is moved to the last position of the trie.
  /// The removed element is stored in {@code tailCarry}.
  ///
  /// @return new root trie or this modified trie, if it's already mutable
  private @Nullable Object[] removeFromRootAt(
      @Nullable Object[] root, int shift, int index, ObjectRef tailCarry) {
    var bufferIndex = Utils.indexSegment(index, shift);

    if (shift == 0) {
      var removedElement = root[bufferIndex];
      var mutableRoot = makeMutable(root);
      System.arraycopy(root, bufferIndex + 1, mutableRoot, bufferIndex, Utils.MAX_BUFFER_SIZE - bufferIndex - 1);
      mutableRoot[Utils.MAX_BUFFER_SIZE - 1] = tailCarry.value;
      tailCarry.value = removedElement;
      return mutableRoot;
    }

    var bufferLastIndex = Utils.MAX_BUFFER_SIZE_MINUS_ONE;
    if (root[bufferLastIndex] == null) {
      bufferLastIndex = Utils.indexSegment(rootSize() - 1, shift);
    }

    var mutableRoot = makeMutable(root);
    var lowerLevelShift = shift - Utils.LOG_MAX_BUFFER_SIZE;

    for (var i = bufferLastIndex; i > bufferIndex; i--) {
      mutableRoot[i] =
          removeFromRootAt(
              Objects.requireNonNull((@Nullable Object[]) mutableRoot[i]),
              lowerLevelShift,
              0,
              tailCarry);
    }
    mutableRoot[bufferIndex] =
        removeFromRootAt(
            Objects.requireNonNull((@Nullable Object[]) mutableRoot[bufferIndex]),
            lowerLevelShift,
            index,
            tailCarry);

    return mutableRoot;
  }

  @Override
  public boolean addAll(Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return false;
    }

    builtVector = null;
    modCount++;

    var tailSize = tailSize();
    Iterator<? extends E> elementsIterator = elements.iterator();

    if (Utils.MAX_BUFFER_SIZE - tailSize >= elements.size()) {
      tail = copyToBuffer(makeMutable(tail), tailSize, elementsIterator);
      size += elements.size();
    } else {
      var buffersSize = (elements.size() + tailSize - 1) / Utils.MAX_BUFFER_SIZE;
      var buffers = new @Nullable Object[buffersSize][];

      buffers[0] = copyToBuffer(makeMutable(tail), tailSize, elementsIterator);
      for (var index = 1; index < buffersSize; index++) {
        buffers[index] = copyToBuffer(mutableBuffer(), 0, elementsIterator);
      }

      root = pushBuffersIncreasingHeightIfNeeded(root, rootSize(), buffers);
      tail = copyToBuffer(mutableBuffer(), 0, elementsIterator);
      size += elements.size();
    }
    return true;
  }

  private @Nullable Object[] copyToBuffer(
      @Nullable Object[] buffer, int bufferIndex, Iterator<? extends @Nullable Object> sourceIterator) {
    var index = bufferIndex;
    while (index < Utils.MAX_BUFFER_SIZE && sourceIterator.hasNext()) {
      buffer[index++] = sourceIterator.next();
    }
    return buffer;
  }

  /// Adds all buffers from {@code buffers} as leaf nodes to the {@code root}.
  /// If the {@code root} has less available leaves for the buffers, height of the trie is increased.
  ///
  /// Returns root of the resulting trie.
  private @Nullable Object[] pushBuffersIncreasingHeightIfNeeded(
      @Nullable Object @Nullable [] root, int rootSize, @Nullable Object[][] buffers) {
    var buffersIterator = Arrays.asList(buffers).iterator();

    @Nullable Object[] mutableRoot;
    if (rootSize >> Utils.LOG_MAX_BUFFER_SIZE < 1 << rootShift) {
      mutableRoot = pushBuffers(root, rootSize, rootShift, buffersIterator);
    } else {
      mutableRoot = makeMutable(root);
    }

    while (buffersIterator.hasNext()) {
      rootShift += Utils.LOG_MAX_BUFFER_SIZE;
      mutableRoot = mutableBufferWith(mutableRoot);
      pushBuffers(mutableRoot, 1 << rootShift, rootShift, buffersIterator);
    }

    return mutableRoot;
  }

  /// Adds buffers from the {@code buffersIterator} as leaf nodes.
  /// As the result {@code root} is entirely filled, or all buffers are added.
  ///
  /// Returns the resulting root.
  private @Nullable Object[] pushBuffers(
      @Nullable Object @Nullable [] root,
      int rootSize,
      int shift,
      Iterator<@Nullable Object[]> buffersIterator) {
    if (shift == 0) {
      return buffersIterator.next();
    }

    var mutableRoot = makeMutable(root);
    var index = Utils.indexSegment(rootSize, shift);
    mutableRoot[index] =
        pushBuffers(
            (@Nullable Object @Nullable []) mutableRoot[index],
            rootSize,
            shift - Utils.LOG_MAX_BUFFER_SIZE,
            buffersIterator);

    while (++index < Utils.MAX_BUFFER_SIZE && buffersIterator.hasNext()) {
      mutableRoot[index] =
          pushBuffers(
              (@Nullable Object @Nullable []) mutableRoot[index],
              0,
              shift - Utils.LOG_MAX_BUFFER_SIZE,
              buffersIterator);
    }
    return mutableRoot;
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> elements) {
    ListImplementation.checkPositionIndex(index, size);

    if (index == size) {
      return addAll(elements);
    }
    if (elements.isEmpty()) {
      return false;
    }

    builtVector = null;
    modCount++;

    var unaffectedElementsCount =
        (index >> Utils.LOG_MAX_BUFFER_SIZE) << Utils.LOG_MAX_BUFFER_SIZE;
    var buffersSize =
        (size - unaffectedElementsCount + elements.size() - 1) / Utils.MAX_BUFFER_SIZE;

    if (buffersSize == 0) {
      var startIndex = index & Utils.MAX_BUFFER_SIZE_MINUS_ONE;
      var endIndex =
          (index + elements.size() - 1) & Utils.MAX_BUFFER_SIZE_MINUS_ONE;

      var newTail = makeMutable(tail);
      System.arraycopy(tail, startIndex, newTail, endIndex + 1, tailSize() - startIndex);
      copyToBuffer(newTail, startIndex, elements.iterator());

      tail = newTail;
      size += elements.size();
      return true;
    }

    var buffers = new @Nullable Object[buffersSize][];

    var tailSize = tailSize();
    var newTailSize = tailSize(size + elements.size());

    @Nullable Object[] newTail;
    if (index >= rootSize()) {
      newTail = mutableBuffer();
      splitToBuffers(elements, index, tail, tailSize, buffers, buffersSize, newTail);
    } else if (newTailSize > tailSize) {
      var rightShift = newTailSize - tailSize;
      newTail = makeMutableShiftingRight(tail, rightShift);
      insertIntoRoot(elements, index, rightShift, buffers, buffersSize, newTail);
    } else {
      newTail = mutableBuffer();
      System.arraycopy(tail, tailSize - newTailSize, newTail, 0, newTailSize);

      var rightShift = Utils.MAX_BUFFER_SIZE - (tailSize - newTailSize);
      var lastBuffer = makeMutableShiftingRight(tail, rightShift);
      buffers[buffersSize - 1] = lastBuffer;
      insertIntoRoot(elements, index, rightShift, buffers, buffersSize - 1, lastBuffer);
    }

    root = pushBuffersIncreasingHeightIfNeeded(root, unaffectedElementsCount, buffers);
    tail = newTail;
    size += elements.size();

    return true;
  }

  /// Inserts the {@code elements} into the {@code root} at the given {@code index}.
  /// Affected elements are copied to the {@code buffers} split into {@code nullBuffers} buffers.
  /// Elements that do not fit {@code nullBuffers} buffers are copied to the {@code nextBuffer}.
  private void insertIntoRoot(
      Collection<? extends E> elements,
      int index,
      int rightShift,
      @Nullable Object[][] buffers,
      int nullBuffers,
      @Nullable Object[] nextBuffer) {
    var startLeafIndex = index >> Utils.LOG_MAX_BUFFER_SIZE;
    var startLeaf = shiftLeafBuffers(startLeafIndex, rightShift, buffers, nullBuffers, nextBuffer);

    var lastLeafIndex = (rootSize() >> Utils.LOG_MAX_BUFFER_SIZE) - 1;
    var newNullBuffers = nullBuffers - (lastLeafIndex - startLeafIndex);
    var newNextBuffer = newNullBuffers < nullBuffers ? Objects.requireNonNull(buffers[newNullBuffers]) : nextBuffer;

    splitToBuffers(elements, index, startLeaf, Utils.MAX_BUFFER_SIZE, buffers, newNullBuffers, newNextBuffer);
  }

  /// Shifts elements in the {@code root} to the right by the given {@code rightShift} position starting from the end.
  /// Shifting stops when elements of the leaf at {@code startLeafIndex} are reached.
  /// Last elements whose indexes become bigger than {@code rootSize} are copied to the {@code nextBuffer}.
  /// Shifted leaves are stored in the {@code buffers} starting from the given {@code nullBuffers} index.
  ///
  /// Returns leaf at the {@code startLeafIndex}.
  private @Nullable Object[] shiftLeafBuffers(
      int startLeafIndex,
      int rightShift,
      @Nullable Object[][] buffers,
      int nullBuffers,
      @Nullable Object[] nextBuffer) {
    var leafCount = rootSize() >> Utils.LOG_MAX_BUFFER_SIZE;
    ListIterator<@Nullable Object[]> leafBufferIterator = leafBufferIterator(leafCount);
    var bufferIndex = nullBuffers;
    var buffer = nextBuffer;

    while (leafBufferIterator.previousIndex() != startLeafIndex) {
      var currentBuffer = leafBufferIterator.previous();
      System.arraycopy(
          currentBuffer,
          Utils.MAX_BUFFER_SIZE - rightShift,
          buffer,
          0,
          rightShift);
      buffer = makeMutableShiftingRight(currentBuffer, rightShift);
      buffers[--bufferIndex] = buffer;
    }

    return leafBufferIterator.previous();
  }

  /// Inserts {@code elements} into {@code startBuffer} of size {@code startBufferSize} and splits the result into {@code nullBuffers} buffers.
  /// Elements that do not fit {@code nullBuffers} buffers are copied to the {@code nextBuffer}.
  private void splitToBuffers(
      Collection<? extends E> elements,
      int index,
      @Nullable Object[] startBuffer,
      int startBufferSize,
      @Nullable Object[][] buffers,
      int nullBuffers,
      @Nullable Object[] nextBuffer) {
    var firstBuffer = makeMutable(startBuffer);
    buffers[0] = firstBuffer;

    var newNextBuffer = nextBuffer;
    var newNullBuffers = nullBuffers;

    var startBufferStartIndex = index & Utils.MAX_BUFFER_SIZE_MINUS_ONE;
    var endBufferEndIndex =
        (index + elements.size() - 1) & Utils.MAX_BUFFER_SIZE_MINUS_ONE;

    var elementsToShift = startBufferSize - startBufferStartIndex;

    if (endBufferEndIndex + elementsToShift < Utils.MAX_BUFFER_SIZE) {
      System.arraycopy(
          firstBuffer,
          startBufferStartIndex,
          newNextBuffer,
          endBufferEndIndex + 1,
          elementsToShift);
    } else {
      var toCopyToLast =
          endBufferEndIndex + elementsToShift - Utils.MAX_BUFFER_SIZE + 1;
      if (nullBuffers == 1) {
        newNextBuffer = firstBuffer;
      } else {
        newNextBuffer = mutableBuffer();
        buffers[--newNullBuffers] = newNextBuffer;
      }
      System.arraycopy(
          firstBuffer,
          startBufferSize - toCopyToLast,
          nextBuffer,
          0,
          toCopyToLast);
      System.arraycopy(
          firstBuffer,
          startBufferStartIndex,
          newNextBuffer,
          endBufferEndIndex + 1,
          elementsToShift - toCopyToLast);
    }

    Iterator<? extends E> elementsIterator = elements.iterator();
    copyToBuffer(firstBuffer, startBufferStartIndex, elementsIterator);
    for (var i = 1; i < newNullBuffers; i++) {
      buffers[i] = copyToBuffer(mutableBuffer(), 0, elementsIterator);
    }
    copyToBuffer(newNextBuffer, 0, elementsIterator);
  }

  private int tailSize(int size) {
    if (size <= Utils.MAX_BUFFER_SIZE) {
      return size;
    }
    return size - Utils.rootSize(size);
  }

  @Override
  public boolean add(E element) {
    var tailSize = tailSize();
    if (tailSize < Utils.MAX_BUFFER_SIZE) {
      builtVector = null;
      modCount++;
      var mutableTail = makeMutable(tail);
      mutableTail[tailSize] = element;
      tail = mutableTail;
      size += 1;
      return true;
    }
    builtVector = null;
    pushFilledTail(root, tail, mutableBufferWith(element));
    return true;
  }

  @Override
  public boolean remove(@Nullable Object element) {
    for (var index = 0; index < size; index++) {
      if (Objects.equals(get(index), element)) {
        remove(index);
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> elements) {
    if (elements.isEmpty()) {
      return false;
    }
    return removeAllWithPredicate(elements::contains);
  }

  @Override
  public boolean retainAll(Collection<?> elements) {
    return removeAllWithPredicate(element -> !elements.contains(element));
  }

  boolean removeAllWithPredicate(Predicate<? super E> predicate) {
    var anyRemoved = removeAllInternal(predicate);
    if (anyRemoved) {
      modCount++;
      builtVector = null;
    }
    return anyRemoved;
  }

  private boolean removeAllInternal(Predicate<? super E> predicate) {
    var tailSize = tailSize();
    var bufferRef = new ObjectRef(null);

    if (root == null) {
      return removeAllFromTail(predicate, tailSize, bufferRef) != tailSize;
    }

    var leafIterator = leafBufferIterator(0);
    var bufferSize = Utils.MAX_BUFFER_SIZE;

    while (bufferSize == Utils.MAX_BUFFER_SIZE && leafIterator.hasNext()) {
      bufferSize = removeAllFromBuffer(predicate, leafIterator.next(), Utils.MAX_BUFFER_SIZE, bufferRef);
    }

    if (bufferSize == Utils.MAX_BUFFER_SIZE) {
      var newTailSize = removeAllFromTail(predicate, tailSize, bufferRef);
      if (newTailSize == 0) {
        pullLastBufferFromRoot(Objects.requireNonNull(root), size, rootShift);
      }
      return newTailSize != tailSize;
    }

    var unaffectedElementsCount = leafIterator.previousIndex() << Utils.LOG_MAX_BUFFER_SIZE;

    var buffers = new ArrayList<@Nullable Object[]>();
    var recyclableBuffers = new ArrayList<@Nullable Object[]>();

    while (leafIterator.hasNext()) {
      var leaf = leafIterator.next();
      bufferSize =
          recyclableRemoveAll(
              predicate, leaf, Utils.MAX_BUFFER_SIZE, bufferSize, bufferRef, recyclableBuffers, buffers);
    }

    var newTailSize =
        recyclableRemoveAll(predicate, tail, tailSize, bufferSize, bufferRef, recyclableBuffers, buffers);

    var newTail = Objects.requireNonNull((@Nullable Object[]) bufferRef.value);
    Arrays.fill(newTail, newTailSize, Utils.MAX_BUFFER_SIZE, null);

    var newRoot =
        buffers.isEmpty()
            ? Objects.requireNonNull(root)
            : pushBuffers(Objects.requireNonNull(root), unaffectedElementsCount, rootShift, buffers.iterator());
    var newRootSize = unaffectedElementsCount + (buffers.size() << Utils.LOG_MAX_BUFFER_SIZE);

    root = retainFirst(newRoot, newRootSize);
    tail = newTail;
    size = newRootSize + newTailSize;

    return true;
  }

  /// Retains first {@code size} elements of the {@code root}.
  /// If the height of the root is bigger than needed to store {@code size} elements, it's decreased.
  private @Nullable Object @Nullable [] retainFirst(@Nullable Object[] root, int size) {
    if (size == 0) {
      rootShift = 0;
      return null;
    }

    var lastIndex = size - 1;
    var newRoot = root;
    while (lastIndex >> rootShift == 0) {
      rootShift -= Utils.LOG_MAX_BUFFER_SIZE;
      newRoot = Objects.requireNonNull((@Nullable Object[]) newRoot[0]);
    }
    return nullifyAfter(newRoot, lastIndex, rootShift);
  }

  /// Nullifies nodes cells after the specified {@code index}.
  /// Used to prevent memory leaks after reusing nodes.
  private @Nullable Object[] nullifyAfter(@Nullable Object[] root, int index, int shift) {
    if (shift == 0) {
      return root;
    }

    var lastIndex = Utils.indexSegment(index, shift);
    var newChild =
        nullifyAfter(
            Objects.requireNonNull((@Nullable Object[]) root[lastIndex]),
            index,
            shift - Utils.LOG_MAX_BUFFER_SIZE);

    var newRoot = root;
    if (lastIndex < Utils.MAX_BUFFER_SIZE_MINUS_ONE && newRoot[lastIndex + 1] != null) {
      if (isMutable(newRoot)) {
        Arrays.fill(newRoot, lastIndex + 1, Utils.MAX_BUFFER_SIZE, null);
      }
      var mutableRoot = mutableBuffer();
      System.arraycopy(newRoot, 0, mutableRoot, 0, lastIndex + 1);
      newRoot = mutableRoot;
    }
    if (newChild != newRoot[lastIndex]) {
      newRoot = makeMutable(newRoot);
      newRoot[lastIndex] = newChild;
    }

    return newRoot;
  }

  /// Copies elements of the {@code tail} buffer of size {@code tailSize} that do not match the given {@code predicate} to a new buffer.
  /// If the {@code tail} is mutable, it is reused to store non-matching elements.
  /// If non of the elements match the {@code predicate}, no buffers are created and elements are not copied.
  /// {@code bufferRef} stores the newly created buffer, or the {@code tail} if a new buffer was not created.
  ///
  /// Returns the filled size of the buffer stored in the {@code bufferRef}.
  private int removeAllFromTail(Predicate<? super E> predicate, int tailSize, ObjectRef bufferRef) {
    var newTailSize = removeAllFromBuffer(predicate, tail, tailSize, bufferRef);

    if (newTailSize == tailSize) {
      return tailSize;
    }

    var newTail = Objects.requireNonNull((@Nullable Object[]) bufferRef.value);
    Arrays.fill(newTail, newTailSize, tailSize, null);

    tail = newTail;
    size -= tailSize - newTailSize;

    return newTailSize;
  }

  /// Copies elements of the given {@code buffer} of size {@code bufferSize} that do not match the given {@code predicate} to a new buffer.
  /// If the {@code buffer} is mutable, it is reused to store non-matching elements.
  /// If non of the elements match the {@code predicate}, no buffers are created and elements are not copied.
  /// {@code bufferRef} stores the newly created buffer, or the {@code buffer} if a new buffer was not created.
  ///
  /// Returns the filled size of the buffer stored in the {@code bufferRef}.
  private int removeAllFromBuffer(
      Predicate<? super E> predicate, @Nullable Object[] buffer, int bufferSize, ObjectRef bufferRef) {
    var newBuffer = buffer;
    var newBufferSize = bufferSize;
    var anyRemoved = false;

    for (var index = 0; index < bufferSize; index++) {
      @SuppressWarnings("unchecked")
      var element = (E) buffer[index];

      if (predicate.test(element)) {
        if (!anyRemoved) {
          newBuffer = makeMutable(buffer);
          newBufferSize = index;
          anyRemoved = true;
        }
      } else if (anyRemoved) {
        newBuffer[newBufferSize++] = element;
      }
    }

    bufferRef.value = newBuffer;
    return newBufferSize;
  }

  /// Copied elements of the given {@code buffer} of size {@code bufferSize} that do not match the given {@code predicate}
  /// to the buffer stored in the given {@code bufferRef} starting at {@code toBufferSize}.
  /// If the buffer gets filled entirely, it is added to {@code buffers} and a new buffer is created or
  /// reused from the {@code recyclableBuffers} to hold the rest of the non-matching elements.
  /// {@code bufferRef} stores the newly created buffer if a new buffer was created.
  ///
  /// Returns the filled size of the buffer stored in the {@code bufferRef}.
  private int recyclableRemoveAll(
      Predicate<? super E> predicate,
      @Nullable Object[] buffer,
      int bufferSize,
      int toBufferSize,
      ObjectRef bufferRef,
      ArrayList<@Nullable Object[]> recyclableBuffers,
      ArrayList<@Nullable Object[]> buffers) {
    if (isMutable(buffer)) {
      recyclableBuffers.add(buffer);
    }

    var toBuffer = Objects.requireNonNull((@Nullable Object[]) bufferRef.value);
    var newToBuffer = toBuffer;
    var newToBufferSize = toBufferSize;

    for (var index = 0; index < bufferSize; index++) {
      @SuppressWarnings("unchecked")
      var element = (E) buffer[index];

      if (!predicate.test(element)) {
        if (newToBufferSize == Utils.MAX_BUFFER_SIZE) {
          if (!recyclableBuffers.isEmpty()) {
            newToBuffer = recyclableBuffers.remove(recyclableBuffers.size() - 1);
          } else {
            newToBuffer = mutableBuffer();
          }
          newToBufferSize = 0;
        }

        newToBuffer[newToBufferSize++] = element;
      }
    }

    bufferRef.value = newToBuffer;

    if (toBuffer != bufferRef.value) {
      buffers.add(toBuffer);
    }

    return newToBufferSize;
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    ListImplementation.checkPositionIndex(index, size);
    return new PersistentVectorMutableIterator<>(this, index);
  }

  private ListIterator<@Nullable Object[]> leafBufferIterator(int index) {
    var leafCount = rootSize() >> Utils.LOG_MAX_BUFFER_SIZE;
    ListImplementation.checkPositionIndex(index, leafCount);

    var root = Objects.requireNonNull(this.root);
    if (rootShift == 0) {
      return new SingleElementListIterator<>(root, index);
    }

    var trieHeight = rootShift / Utils.LOG_MAX_BUFFER_SIZE;
    return new TrieIterator<>(root, index, leafCount, trieHeight);
  }

  @Override
  public Iterator<E> iterator() {
    return listIterator();
  }

  @Override
  public boolean contains(@Nullable Object element) {
    for (var index = 0; index < size; index++) {
      if (Objects.equals(get(index), element)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int indexOf(@Nullable Object element) {
    for (var index = 0; index < size; index++) {
      if (Objects.equals(get(index), element)) {
        return index;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(@Nullable Object element) {
    for (var index = size - 1; index >= 0; index--) {
      if (Objects.equals(get(index), element)) {
        return index;
      }
    }
    return -1;
  }

  @Override
  public boolean containsAll(Collection<?> elements) {
    for (var element : elements) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public Object[] toArray() {
    var array = new Object[size];
    for (var index = 0; index < size; index++) {
      array[index] = get(index);
    }
    return array;
  }

  @Override
  public <T extends @Nullable Object> T[] toArray(T[] array) {
    if (array.length < size) {
      return Arrays.copyOf(toArray(), size, (Class<? extends T[]>) array.getClass());
    }
    for (var index = 0; index < size; index++) {
      @SuppressWarnings("unchecked")
      var element = (T) get(index);
      array[index] = element;
    }
    if (array.length > size) {
      array[size] = null;
    }
    return array;
  }

  @Override
  public String toString() {
    return build().toString();
  }

  @Override
  public int hashCode() {
    return build().hashCode();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return build().equals(other);
  }

  @Override
  public void clear() {
    if (size == 0) {
      return;
    }
    modified();
    root = null;
    tail = new @Nullable Object[Utils.MAX_BUFFER_SIZE];
    rootShift = 0;
    size = 0;
  }

  @Override
  public PersistentList<E> build() {
    if (builtVector != null) {
      return builtVector;
    }
    ownership = new MutabilityOwnership();
    if (root == null) {
      if (size == 0) {
        builtVector = Utils.persistentVectorOf();
        return builtVector;
      }
      var newlyBuiltVector = new SmallPersistentVector<E>(Arrays.copyOf(tail, size));
      builtVector = newlyBuiltVector;
      return newlyBuiltVector;
    }
    var newlyBuiltVector = new PersistentVector<E>(Objects.requireNonNull(root), tail, size, rootShift);
    builtVector = newlyBuiltVector;
    return newlyBuiltVector;
  }
}
