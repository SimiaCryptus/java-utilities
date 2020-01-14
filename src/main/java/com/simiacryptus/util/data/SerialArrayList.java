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

package com.simiacryptus.util.data;

import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefCollection;
import com.simiacryptus.ref.wrappers.RefSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SerialArrayList<U> {
  public final int unitSize;
  @Nonnull
  private final SerialType<U> factory;
  private byte[] buffer;
  private int maxByte = 0;

  public SerialArrayList(@Nonnull SerialType<U> factory, @Nonnull SerialArrayList<U>... items) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.maxByte = RefArrays.stream(items).mapToInt(item -> item.maxByte).sum();
    this.buffer = new byte[this.maxByte];
    int cursor = 0;
    for (int i = 0; i < items.length; i++) {
      SerialArrayList<U> item = items[i];
      RefSystem.arraycopy(item.buffer, 0, this.buffer, cursor, item.maxByte);
      cursor += item.maxByte;
    }
  }

  public SerialArrayList(@Nonnull SerialType<U> factory, @Nonnull RefCollection<U> items) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.buffer = new byte[items.size() * unitSize];
    AtomicInteger i = new AtomicInteger();
    items.forEach(x -> set(i.getAndIncrement(), x));
    items.freeRef();
  }

  public SerialArrayList(@Nonnull SerialType<U> factory, @Nonnull U... items) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.buffer = new byte[items.length * unitSize];
    for (int i = 0; i < items.length; i++)
      set(i, items[i]);
  }

  public SerialArrayList(@Nonnull SerialType<U> factory) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.buffer = new byte[1024];
  }

  public SerialArrayList(@Nonnull SerialType<U> factory, int size) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.buffer = new byte[this.unitSize * size];
  }

  public int getMemorySize() {
    return buffer.length;
  }

  @Nonnull
  public SerialArrayList<U> add(SerialArrayList<U> right) {
    return new SerialArrayList<U>(factory, this, right);
  }

  public synchronized void clear() {
    buffer = new byte[]{};
    maxByte = 0;
  }

  public int length() {
    return maxByte / unitSize;
  }

  @Nonnull
  public U get(int i) {
    ByteBuffer view = getView(i);
    try {
      return factory.read(view);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized int add(U value) {
    int length = length();
    set(length, value);
    return length;
  }

  public synchronized U update(int i, @Nonnull Function<U, U> updater) {
    U updated = updater.apply(this.get(i));
    set(i, updated);
    return updated;
  }

  public void set(int i, U value) {
    ensureCapacity((i + 1) * unitSize);
    ByteBuffer view = getView(i);
    try {
      factory.write(view, value);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized int addAll(@Nullable RefCollection<U> data) {
    int startIndex = length();
    putAll(data == null ? null : data.addRef(), startIndex);
    if (null != data)
      data.freeRef();
    return startIndex;
  }

  public synchronized void putAll(@Nullable RefCollection<U> data, int startIndex) {
    putAll(new SerialArrayList<U>(factory, data == null ? null : data.addRef()), startIndex);
    if (null != data)
      data.freeRef();
  }

  public synchronized void putAll(@Nonnull SerialArrayList<U> data, int startIndex) {
    ensureCapacity((startIndex * unitSize) + data.maxByte);
    RefSystem.arraycopy(data.buffer, 0, this.buffer, startIndex * unitSize, data.maxByte);
  }

  @Nonnull
  public SerialArrayList<U> copy() {
    return new SerialArrayList<U>(factory, this);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    SerialArrayList<?> that = (SerialArrayList<?>) o;

    if (unitSize != that.unitSize)
      return false;
    if (maxByte != that.maxByte)
      return false;
    if (!factory.equals(that.factory))
      return false;
    return RefArrays.equals(buffer, that.buffer);
  }

  @Override
  public int hashCode() {
    int result = factory.hashCode();
    result = 31 * result + unitSize;
    result = 31 * result + RefArrays.hashCode(buffer);
    result = 31 * result + maxByte;
    return result;
  }

  @Nonnull
  private ByteBuffer getView(int i) {
    ByteBuffer duplicate = ByteBuffer.wrap(buffer);
    duplicate.position(unitSize * i);
    return duplicate;
  }

  private synchronized void ensureCapacity(int bytes) {
    if (maxByte < bytes) {
      maxByte = bytes;
    }
    int targetBytes = buffer.length;
    while (targetBytes < bytes)
      targetBytes = Math.max(targetBytes * 2, 1);
    if (targetBytes > buffer.length) {
      buffer = RefArrays.copyOf(buffer, targetBytes);
    }
  }
}
