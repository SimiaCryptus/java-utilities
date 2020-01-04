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

import com.simiacryptus.ref.lang.ReferenceCountingBase;

import java.util.function.Predicate;

public abstract @com.simiacryptus.ref.lang.RefAware
class ResourcePool<T> extends ReferenceCountingBase {

  @javax.annotation.Nonnull
  private final com.simiacryptus.ref.wrappers.RefHashSet<T> all;
  private final ThreadLocal<T> currentValue = new ThreadLocal<>();
  private final int maxItems;
  private final java.util.concurrent.LinkedBlockingQueue<T> pool = new java.util.concurrent.LinkedBlockingQueue<>();

  public ResourcePool(final int maxItems) {
    super();
    this.maxItems = maxItems;
    this.all = new com.simiacryptus.ref.wrappers.RefHashSet<>(this.maxItems);
  }

  public abstract T create();

  public T get() {
    return get(x -> true);
  }

  public T get(Predicate<T> filter) {
    com.simiacryptus.ref.wrappers.RefArrayList<T> sampled = new com.simiacryptus.ref.wrappers.RefArrayList<>();
    try {
      T poll = this.pool.poll();
      while (null != poll) {
        if (filter.test(poll)) {
          return poll;
        } else {
          sampled.add(poll);
        }
      }
    } finally {
      pool.addAll(sampled);
    }
    synchronized (this.all) {
      if (this.all.size() < this.maxItems) {
        T poll = create();
        this.all.add(poll);
        return poll;
      }
    }
    try {
      return this.pool.take();
    } catch (@javax.annotation.Nonnull final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public int size() {
    return all.size();
  }

  public <U> U apply(@javax.annotation.Nonnull final java.util.function.Function<T, U> f) {
    return apply(f, x -> true);
  }

  public void apply(@javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefConsumer<T> f) {
    apply(f, x -> true);
  }

  public <U> U apply(@javax.annotation.Nonnull final java.util.function.Function<T, U> f, final Predicate<T> filter) {
    final T prior = currentValue.get();
    if (null != prior) {
      return f.apply(prior);
    } else {
      final T poll = get(filter);
      try {
        currentValue.set(poll);
        return f.apply(poll);
      } finally {
        this.pool.add(poll);
        currentValue.remove();
      }
    }
  }

  public void apply(@javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefConsumer<T> f,
                    final Predicate<T> filter) {
    final T prior = currentValue.get();
    if (null != prior) {
      f.accept(prior);
    } else {
      final T poll = get(filter);
      try {
        currentValue.set(poll);
        f.accept(poll);
      } finally {
        this.pool.add(poll);
        currentValue.remove();
      }
    }
  }
}
