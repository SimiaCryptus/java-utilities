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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

/**
 * The type Serial array list.
 *
 * @param <U> the type parameter
 */
public class SerialArrayList<U> {
  /**
   * The Unit size.
   */
  public final int unitSize;
  private final SerialType<U> factory;
  private byte[] buffer;
  private int maxByte = 0;

  /**
   * Instantiates a new Serial array list.
   *
   * @param factory the factory
   * @param items   the items
   */
  public SerialArrayList(SerialType<U> factory, SerialArrayList<U>... items) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.maxByte = Arrays.stream(items).mapToInt(item -> item.maxByte).sum();
    this.buffer = new byte[this.maxByte];
    int cursor = 0;
    for (int i = 0; i < items.length; i++) {
      SerialArrayList<U> item = items[i];
      System.arraycopy(item.buffer, 0, this.buffer, cursor, item.maxByte);
      cursor += item.maxByte;
    }
  }

  /**
   * Instantiates a new Serial array list.
   *
   * @param factory the factory
   * @param items   the items
   */
  public SerialArrayList(SerialType<U> factory, Collection<U> items) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.buffer = new byte[items.size() * unitSize];
    int i = 0;
    for (U x : items) set(i++, x);
  }

  /**
   * Instantiates a new Serial array list.
   *
   * @param factory the factory
   * @param items   the items
   */
  public SerialArrayList(SerialType<U> factory, U... items) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.buffer = new byte[items.length * unitSize];
    for (int i = 0; i < items.length; i++) set(i, items[i]);
  }

  /**
   * Instantiates a new Serial array list.
   *
   * @param factory the factory
   */
  public SerialArrayList(SerialType<U> factory) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.buffer = new byte[1024];
  }

  /**
   * Instantiates a new Serial array list.
   *
   * @param factory the factory
   * @param size    the size
   */
  public SerialArrayList(SerialType<U> factory, int size) {
    this.factory = factory;
    this.unitSize = factory.getSize();
    this.buffer = new byte[this.unitSize * size];
  }

  /**
   * Add serial array list.
   *
   * @param right the right
   * @return the serial array list
   */
  public SerialArrayList<U> add(SerialArrayList<U> right) {
    return new SerialArrayList<U>(factory, this, right);
  }

  /**
   * Clear.
   */
  public synchronized void clear() {
    buffer = new byte[]{};
    maxByte = 0;
  }

  /**
   * Length int.
   *
   * @return the int
   */
  public int length() {
    return maxByte / unitSize;
  }

  /**
   * Get u.
   *
   * @param i the
   * @return the u
   */
  public U get(int i) {
    ByteBuffer view = getView(i);
    try {
      return factory.read(view);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Add int.
   *
   * @param value the value
   * @return the int
   */
  public synchronized int add(U value) {
    int length = length();
    set(length, value);
    return length;
  }

  /**
   * Update u.
   *
   * @param i       the
   * @param updater the updater
   * @return the u
   */
  public synchronized U update(int i, Function<U, U> updater) {
    U updated = updater.apply(this.get(i));
    set(i, updated);
    return updated;
  }

  /**
   * Set.
   *
   * @param i     the
   * @param value the value
   */
  public void set(int i, U value) {
    ensureCapacity((i + 1) * unitSize);
    ByteBuffer view = getView(i);
    try {
      factory.write(view, value);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

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
    while (targetBytes < bytes) targetBytes = Math.max(targetBytes * 2, 1);
    if (targetBytes > buffer.length) {
      buffer = Arrays.copyOf(buffer, targetBytes);
    }
  }

  /**
   * Add all int.
   *
   * @param data the data
   * @return the int
   */
  public synchronized int addAll(Collection<U> data) {
    int startIndex = length();
    putAll(data, startIndex);
    return startIndex;
  }

  /**
   * Put all.
   *
   * @param data       the data
   * @param startIndex the start index
   */
  public synchronized void putAll(Collection<U> data, int startIndex) {
    putAll(new SerialArrayList<U>(factory, data), startIndex);
  }

  /**
   * Put all.
   *
   * @param data       the data
   * @param startIndex the start index
   */
  public synchronized void putAll(SerialArrayList<U> data, int startIndex) {
    ensureCapacity((startIndex * unitSize) + data.maxByte);
    System.arraycopy(data.buffer, 0, this.buffer, startIndex * unitSize, data.maxByte);
  }

  /**
   * Gets memory size.
   *
   * @return the memory size
   */
  public int getMemorySize() {
    return buffer.length;
  }

  /**
   * Copy serial array list.
   *
   * @return the serial array list
   */
  public SerialArrayList<U> copy() {
    return new SerialArrayList<U>(factory, this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SerialArrayList<?> that = (SerialArrayList<?>) o;

    if (unitSize != that.unitSize) return false;
    if (maxByte != that.maxByte) return false;
    if (!factory.equals(that.factory)) return false;
    return Arrays.equals(buffer, that.buffer);
  }

  @Override
  public int hashCode() {
    int result = factory.hashCode();
    result = 31 * result + unitSize;
    result = 31 * result + Arrays.hashCode(buffer);
    result = 31 * result + maxByte;
    return result;
  }
}
