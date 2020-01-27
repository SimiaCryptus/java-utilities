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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefIteratorBase;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class AsyncListIterator<T> extends RefIteratorBase<T> {
  @Nullable
  private final RefList<T> queue;
  private final Thread thread;
  int index = -1;

  public AsyncListIterator(@Nullable final RefList<T> queue, final Thread thread) {
    this.thread = thread;
    RefList<T> temp_02_0001 = queue == null ? null : queue.addRef();
    this.queue = temp_02_0001 == null ? null : temp_02_0001.addRef();
    if (null != temp_02_0001)
      temp_02_0001.freeRef();
    if (null != queue)
      queue.freeRef();
  }

  @Override
  public boolean hasNext() {
    assert queue != null;
    return index < queue.size() || thread.isAlive();
  }

  @Nullable
  @Override
  public T next() {
    try {
      while (hasNext()) {
        assert queue != null;
        if (++index < queue.size()) {
          return queue.get(index);
        } else {
          Thread.sleep(100);
        }
      }
      return null;
    } catch (@Nonnull final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
    if (null != queue)
      queue.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  AsyncListIterator<T> addRef() {
    return (AsyncListIterator<T>) super.addRef();
  }
}
