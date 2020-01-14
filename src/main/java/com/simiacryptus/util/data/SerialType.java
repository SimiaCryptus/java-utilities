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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface SerialType<T> {
  int getSize();

  @Nonnull
  default SerialArrayList<T> newList() {
    return new SerialArrayList<T>(this);
  }

  @Nonnull
  default SerialArrayList<T> newList(int size) {
    return new SerialArrayList<T>(this, size);
  }

  @Nonnull
  default SerialArrayList<T> newList(T... items) {
    return new SerialArrayList<T>(this, items);
  }

  @Nonnull
  T read(ByteBuffer input) throws IOException;

  @Nonnull
  default T read(@Nonnull byte[] input) throws IOException {
    assert (input.length == getSize());
    return read(ByteBuffer.wrap(input));
  }

  void write(ByteBuffer output, T value) throws IOException;

  @Nonnull
  default byte[] write(T value) throws IOException {
    byte[] buffer = new byte[getSize()];
    write(ByteBuffer.wrap(buffer), value);
    return buffer;
  }
}
