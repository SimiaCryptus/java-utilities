/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.util.io;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Spliterator;

public final class BinaryChunkIterator extends RefIteratorBase<byte[]> {

  private final DataInputStream in;
  private final int recordSize;

  public BinaryChunkIterator(final DataInputStream in, final int recordSize) {
    super();
    this.in = in;
    this.recordSize = recordSize;
  }

  public static <T> RefStream<T> toIterator(@Nonnull final RefIterator<T> iterator) {
    RefStream<T> temp_07_0001 = RefStreamSupport
        .stream(RefSpliterators.spliterator(iterator == null ? null : iterator, 1, Spliterator.ORDERED), false);
    return temp_07_0001;
  }

  public static <T> RefStream<T> toStream(@Nonnull final RefIteratorBase<T> iterator) {
    RefStream<T> temp_07_0002 = BinaryChunkIterator.toStream(iterator == null ? null : iterator, 0);
    return temp_07_0002;
  }

  public static <T> RefStream<T> toStream(@Nonnull final @RefAware RefIteratorBase<T> iterator, final int size) {
    return BinaryChunkIterator.toStream(iterator == null ? null : iterator, size, false);
  }

  public static <T> RefStream<T> toStream(@Nonnull final @RefAware RefIteratorBase<T> iterator, final int size,
      final boolean parallel) {
    RefStream<T> temp_07_0004 = RefStreamSupport
        .stream(RefSpliterators.spliterator(iterator == null ? null : iterator, size, Spliterator.ORDERED), parallel);
    return temp_07_0004;
  }

  public static @SuppressWarnings("unused") BinaryChunkIterator[] addRefs(BinaryChunkIterator[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BinaryChunkIterator::addRef)
        .toArray((x) -> new BinaryChunkIterator[x]);
  }

  public static @SuppressWarnings("unused") BinaryChunkIterator[][] addRefs(BinaryChunkIterator[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BinaryChunkIterator::addRefs)
        .toArray((x) -> new BinaryChunkIterator[x][]);
  }

  @Nonnull
  private static byte[] read(@Nonnull final DataInputStream i, final int s) throws IOException {
    @Nonnull
    final byte[] b = new byte[s];
    int pos = 0;
    while (b.length > pos) {
      final int read = i.read(b, pos, b.length - pos);
      if (0 == read) {
        throw new RuntimeException();
      }
      pos += read;
    }
    return b;
  }

  @Override
  public boolean hasNext() {
    try {
      return 0 < in.available();
    } catch (@Nonnull final Throwable e) {
      return false;
    }
  }

  @Nonnull
  @Override
  public byte[] next() {
    assert hasNext();
    try {
      return BinaryChunkIterator.read(in, recordSize);
    } catch (@Nonnull final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public RefStream<byte[]> toStream() {
    return BinaryChunkIterator.toStream(this.addRef());
  }

  public @SuppressWarnings("unused") void _free() {
  }

  public @Override @SuppressWarnings("unused") BinaryChunkIterator addRef() {
    return (BinaryChunkIterator) super.addRef();
  }
}
