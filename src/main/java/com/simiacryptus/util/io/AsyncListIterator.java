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
import com.simiacryptus.ref.wrappers.RefIteratorBase;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class AsyncListIterator<T> extends RefIteratorBase<T> {
  private final RefList<T> queue;
  private final Thread thread;
  int index = -1;

  public AsyncListIterator(final RefList<T> queue, final Thread thread) {
    this.thread = thread;
    RefList<T> temp_02_0001 = queue == null ? null : queue.addRef();
    this.queue = temp_02_0001 == null ? null : temp_02_0001.addRef();
    if (null != temp_02_0001)
      temp_02_0001.freeRef();
    if (null != queue)
      queue.freeRef();
  }

  public static @SuppressWarnings("unused") AsyncListIterator[] addRefs(AsyncListIterator[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(AsyncListIterator::addRef)
        .toArray((x) -> new AsyncListIterator[x]);
  }

  public static @SuppressWarnings("unused") AsyncListIterator[][] addRefs(AsyncListIterator[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(AsyncListIterator::addRefs)
        .toArray((x) -> new AsyncListIterator[x][]);
  }

  @Override
  public boolean hasNext() {
    return index < queue.size() || thread.isAlive();
  }

  @Nullable
  @Override
  public T next() {
    try {
      while (hasNext()) {
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

  public @SuppressWarnings("unused") void _free() {
    if (null != queue)
      queue.freeRef();
  }

  public @Override @SuppressWarnings("unused") AsyncListIterator<T> addRef() {
    return (AsyncListIterator<T>) super.addRef();
  }
}
