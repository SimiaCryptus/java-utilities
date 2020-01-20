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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public abstract class ResourcePool<T> extends ReferenceCountingBase {

  @Nonnull
  private final RefHashSet<T> all;
  private final ThreadLocal<T> currentValue = new ThreadLocal<>();
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
    return get(x -> true);
  }

  public T get(@Nonnull Predicate<T> filter) {
    RefArrayList<T> sampled = new RefArrayList<>();
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
    } catch (@Nonnull final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public int size() {
    return all.size();
  }

  @Nonnull
  public <U> U apply(@Nonnull @RefAware final RefFunction<T, U> f) {
    return apply(f, x -> true);
  }

  public void apply(@Nonnull @RefAware final RefConsumer<T> f) {
    apply(f, x -> true);
  }

  @Nonnull
  public <U> U apply(@Nonnull @RefAware final RefFunction<T, U> f, @Nonnull final Predicate<T> filter) {
    final T prior = currentValue.get();
    try {
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
    } finally {
      RefUtil.freeRef(f);
    }
  }

  public void apply(@Nonnull @RefAware final RefConsumer<T> f, @Nonnull final Predicate<T> filter) {
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
    RefUtil.freeRef(f);
  }

  public @SuppressWarnings("unused")
  void _free() {
    all.freeRef();
    pool.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ResourcePool<T> addRef() {
    return (ResourcePool<T>) super.addRef();
  }
}
