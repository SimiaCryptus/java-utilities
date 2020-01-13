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
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

public class StaticResourcePool<T> extends ReferenceCountingBase {

  @Nonnull
  private final RefList<T> all;
  private final RefLinkedBlockingQueue<T> pool = new RefLinkedBlockingQueue<>();

  public StaticResourcePool(@Nonnull final RefList<T> items) {
    super();
    RefList<T> temp_03_0001 = RefCollections
        .unmodifiableList(new RefArrayList<>(items == null ? null : items.addRef()));
    this.all = temp_03_0001 == null ? null : temp_03_0001.addRef();
    if (null != temp_03_0001)
      temp_03_0001.freeRef();
    items.freeRef();
    RefList<T> temp_03_0002 = getAll();
    pool.addAll(temp_03_0002);
    if (null != temp_03_0002)
      temp_03_0002.freeRef();
  }

  @Nonnull
  public RefList<T> getAll() {
    return all == null ? null : all.addRef();
  }

  public static @SuppressWarnings("unused") StaticResourcePool[] addRefs(StaticResourcePool[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(StaticResourcePool::addRef)
        .toArray((x) -> new StaticResourcePool[x]);
  }

  public static @SuppressWarnings("unused") StaticResourcePool[][] addRefs(StaticResourcePool[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(StaticResourcePool::addRefs)
        .toArray((x) -> new StaticResourcePool[x][]);
  }

  public void apply(@Nonnull final RefConsumer<T> f) {
    apply(f, x -> true, false);
  }

  public void apply(@Nonnull final RefConsumer<T> f, final Predicate<T> filter, final boolean exclusive) {
    T poll = get(filter, exclusive);
    try {
      f.accept(poll);
    } finally {
      this.pool.add(poll);
    }
  }

  public <U> U run(@Nonnull final Function<T, U> f) {
    return run(f, x -> true, false);
  }

  public <U> U run(@Nonnull final Function<T, U> f, final Predicate<T> filter, final boolean exclusive) {
    if (all.isEmpty())
      throw new IllegalStateException();
    T poll = get(filter, exclusive);
    try {
      return f.apply(poll);
    } finally {
      this.pool.add(poll);
    }
  }

  public int size() {
    RefList<T> temp_03_0004 = getAll();
    int temp_03_0003 = temp_03_0004.size();
    if (null != temp_03_0004)
      temp_03_0004.freeRef();
    return temp_03_0003;
  }

  public @SuppressWarnings("unused") void _free() {
    all.freeRef();
  }

  public @Override @SuppressWarnings("unused") StaticResourcePool<T> addRef() {
    return (StaticResourcePool<T>) super.addRef();
  }

  @Nonnull
  private T get(Predicate<T> filter, final boolean exclusive) {
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
          poll = this.pool.poll();
        }
      }
    } finally {
      pool.addAll(sampled);
    }
    if (null != sampled)
      sampled.freeRef();
    try {
      while (true) {
        final T poll;
        poll = this.pool.poll(5, TimeUnit.MINUTES);
        if (null == poll)
          throw new RuntimeException("Timeout awaiting item from pool");
        if (exclusive && !filter.test(poll)) {
          this.pool.add(poll);
          Thread.sleep(0);
        } else {
          return poll;
        }
      }
    } catch (@Nonnull final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
