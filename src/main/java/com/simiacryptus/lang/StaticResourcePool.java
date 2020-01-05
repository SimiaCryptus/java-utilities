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
import com.simiacryptus.ref.wrappers.RefCollections;
import com.simiacryptus.ref.wrappers.RefConsumer;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

public @RefAware
class StaticResourcePool<T> extends ReferenceCountingBase {

  @Nonnull
  private final RefList<T> all;
  private final LinkedBlockingQueue<T> pool = new LinkedBlockingQueue<>();

  public StaticResourcePool(@Nonnull final RefList<T> items) {
    super();
    this.all = RefCollections
        .unmodifiableList(new RefArrayList<>(items));
    pool.addAll(getAll());
  }

  @Nonnull
  public RefList<T> getAll() {
    return all;
  }

  public void apply(@Nonnull final RefConsumer<T> f) {
    apply(f, x -> true, false);
  }

  public void apply(@Nonnull final RefConsumer<T> f,
                    final Predicate<T> filter, final boolean exclusive) {
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

  public <U> U run(@Nonnull final Function<T, U> f, final Predicate<T> filter,
                   final boolean exclusive) {
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
    return getAll().size();
  }

  @Nonnull
  private T get(Predicate<T> filter, final boolean exclusive) {
    RefArrayList<T> sampled = new RefArrayList<>();
    try {
      T poll = this.pool.poll();
      while (null != poll) {
        if (filter.test(poll)) {
          return poll;
        } else {
          sampled.add(poll);
          poll = this.pool.poll();
        }
      }
    } finally {
      pool.addAll(sampled);
    }
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
