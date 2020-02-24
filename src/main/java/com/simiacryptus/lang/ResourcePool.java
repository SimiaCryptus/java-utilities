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

package com.simiacryptus.lang;

import com.simiacryptus.ref.lang.*;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;

public abstract class ResourcePool<T> extends ReferenceCountingBase {

  @Nonnull
  private final RefHashSet<T> all;
  private final RefThreadLocal<T> currentValue = new RefThreadLocal<>();
  private final int maxItems;
  private final RefLinkedBlockingQueue<T> pool = new RefLinkedBlockingQueue<>();

  public ResourcePool(final int maxItems) {
    super();
    this.maxItems = maxItems;
    RefHashSet<T> temp_01_0001 = new RefHashSet<>(this.maxItems);
    this.all = temp_01_0001.addRef();
    temp_01_0001.freeRef();
  }

  public abstract T create();

  public T get() {
    assertAlive();
    {
      T poll = this.pool.poll();
      if (null != poll) {
        RefUtil.assertAlive(poll);
        return poll;
      }
    }
    synchronized (this.all) {
      if (this.all.size() < this.maxItems) {
        T poll = create();
        RefUtil.assertAlive(poll);
        this.all.add(RefUtil.addRef(poll));
        return poll;
      }
    }
    try {
      T take = this.pool.take();
      RefUtil.assertAlive(take);
      return take;
    } catch (@Nonnull final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public int size() {
    return all.size();
  }

  @Nonnull
  public <U> U apply(@Nonnull @RefAware final RefFunction<T, U> fn) {
    assertAlive();
    final T prior = currentValue.get();
    try {
      if (null != prior) {
        RefUtil.assertAlive(prior);
        return fn.apply(prior);
      } else {
        final T poll = get();
        RefUtil.assertAlive(poll);
        try {
          currentValue.set(RefUtil.addRef(poll));
          return fn.apply(RefUtil.addRef(poll));
        } finally {
          RefUtil.assertAlive(poll);
          this.pool.add(poll);
          currentValue.remove();
        }
      }
    } finally {
      RefUtil.freeRef(fn);
    }
  }

  public void apply(@Nonnull @RefAware final RefConsumer<T> f) {
    assert assertAlive();
    assert this.pool.assertAlive();
    final T prior = currentValue.get();
    try {
      if (null != prior) {
        f.accept(prior);
      } else {
        final T poll = get();
        try {
          currentValue.set(RefUtil.addRef(poll));
          f.accept(RefUtil.addRef(poll));
        } finally {
          RefUtil.assertAlive(poll);
          this.pool.add(poll);
          currentValue.remove();
        }
      }
    } finally {
      RefUtil.freeRef(f);
    }
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
    all.freeRef();
    pool.freeRef();
    currentValue.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ResourcePool<T> addRef() {
    return (ResourcePool<T>) super.addRef();
  }
}
