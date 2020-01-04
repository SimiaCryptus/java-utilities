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

import javax.annotation.Nullable;
import java.util.Spliterator;

public abstract @com.simiacryptus.ref.lang.RefAware
class DataLoader<T> extends ReferenceCountingBase {
  private final com.simiacryptus.ref.wrappers.RefList<T> queue = com.simiacryptus.ref.wrappers.RefCollections
      .synchronizedList(new com.simiacryptus.ref.wrappers.RefArrayList<>());
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
    } catch (@javax.annotation.Nonnull final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public com.simiacryptus.ref.wrappers.RefStream<T> stream() {
    if (thread == null) {
      synchronized (this) {
        if (thread == null) {
          thread = new Thread(() -> read(queue));
          thread.setDaemon(true);
          thread.start();
        }
      }
    }
    @Nullable final com.simiacryptus.ref.wrappers.RefIteratorBase<T> iterator = new AsyncListIterator<>(queue, thread);
    return com.simiacryptus.ref.wrappers.RefStreamSupport
        .stream(com.simiacryptus.ref.wrappers.RefSpliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT),
            false)
        .filter(x -> x != null);
  }

  protected abstract void read(com.simiacryptus.ref.wrappers.RefList<T> queue);
}
