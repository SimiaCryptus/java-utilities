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

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Spliterator;

public abstract class DataLoader<T> extends ReferenceCountingBase {
  private final RefList<T> queue = RefCollections.synchronizedList(new RefArrayList<>());
  @Nullable
  private volatile Thread thread;


  public void clear() throws InterruptedException {
    if (thread != null) {
      synchronized (this) {
        if (thread != null) {
          thread.interrupt();
          thread.join();
          thread = null;
          queue.clear();
        }
      }
    }
  }

  public void stop() {
    if (thread != null) {
      thread.interrupt();
    }
    try {
      thread.join();
    } catch (@Nonnull final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Nonnull
  public RefStream<T> stream() {
    if (thread == null) {
      synchronized (this) {
        if (thread == null) {
          thread = new Thread(() -> read(queue == null ? null : queue.addRef()));
          thread.setDaemon(true);
          thread.start();
        }
      }
    }
    @Nullable final RefIteratorBase<T> iterator = new AsyncListIterator<>(queue == null ? null : queue.addRef(), thread);
    return RefStreamSupport.stream(
        RefSpliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT),
        false).filter(x -> x != null);
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
  DataLoader<T> addRef() {
    return (DataLoader<T>) super.addRef();
  }

  protected abstract void read(RefList<T> queue);
}
