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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrayList;
import com.simiacryptus.ref.wrappers.RefConsumer;
import com.simiacryptus.ref.wrappers.RefHashSet;
import com.simiacryptus.ref.wrappers.RefLinkedBlockingQueue;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Function;
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
    this.all = temp_01_0001 == null ? null : temp_01_0001.addRef();
    if (null != temp_01_0001)
      temp_01_0001.freeRef();
  }

  public static @SuppressWarnings("unused") ResourcePool[] addRefs(ResourcePool[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ResourcePool::addRef).toArray((x) -> new ResourcePool[x]);
  }

  public static @SuppressWarnings("unused") ResourcePool[][] addRefs(ResourcePool[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ResourcePool::addRefs)
        .toArray((x) -> new ResourcePool[x][]);
  }

  public abstract T create();

  public T get() {
    return get(x -> true);
  }

  public T get(Predicate<T> filter) {
    RefArrayList<T> sampled = new RefArrayList<>();
    try {
      T poll = this.pool.poll();
      while (null != poll) {
        if (filter.test(poll)) {
          if (null != sampled)
            sampled.freeRef();
          return poll;
        } else {
          sampled.add(poll);
        }
      }
    } finally {
      pool.addAll(sampled);
    }
    if (null != sampled)
      sampled.freeRef();
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

  public <U> U apply(@Nonnull final Function<T, U> f) {
    return apply(f, x -> true);
  }

  public void apply(@Nonnull final RefConsumer<T> f) {
    apply(f, x -> true);
  }

  public <U> U apply(@Nonnull final Function<T, U> f, final Predicate<T> filter) {
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

  public void apply(@Nonnull final RefConsumer<T> f, final Predicate<T> filter) {
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

  public @SuppressWarnings("unused") void _free() {
    all.freeRef();
  }

  public @Override @SuppressWarnings("unused") ResourcePool<T> addRef() {
    return (ResourcePool<T>) super.addRef();
  }
}
