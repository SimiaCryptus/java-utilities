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

/**
 * The interface Serial type.
 *
 * @param <T> the type parameter
 */
public interface SerialType<T> {
  /**
   * New list serial array list.
   *
   * @return the serial array list
   */
  default SerialArrayList<T> newList() {
    return new SerialArrayList<T>(this);
  }

  /**
   * New list serial array list.
   *
   * @param size the size
   * @return the serial array list
   */
  default SerialArrayList<T> newList(int size) {
    return new SerialArrayList<T>(this, size);
  }

  /**
   * New list serial array list.
   *
   * @param items the items
   * @return the serial array list
   */
  default SerialArrayList<T> newList(T... items) {
    return new SerialArrayList<T>(this, items);
  }

  /**
   * Gets size.
   *
   * @return the size
   */
  int getSize();

  /**
   * Read t.
   *
   * @param input the input
   * @return the t
   * @throws IOException the io exception
   */
  T read(ByteBuffer input) throws IOException;

  /**
   * Read t.
   *
   * @param input the input
   * @return the t
   * @throws IOException the io exception
   */
  default T read(byte[] input) throws IOException {
    assert (input.length == getSize());
    return read(ByteBuffer.wrap(input));
  }

  /**
   * Write.
   *
   * @param output the output
   * @param value  the value
   * @throws IOException the io exception
   */
  void write(ByteBuffer output, T value) throws IOException;

  /**
   * Write byte [ ].
   *
   * @param value the value
   * @return the byte [ ]
   * @throws IOException the io exception
   */
  default byte[] write(T value) throws IOException {
    byte[] buffer = new byte[getSize()];
    write(ByteBuffer.wrap(buffer), value);
    return buffer;
  }
}
