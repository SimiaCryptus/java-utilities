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

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

public @com.simiacryptus.ref.lang.RefAware
class StaticResourcePool<T> extends ReferenceCountingBase {

  @javax.annotation.Nonnull
  private final com.simiacryptus.ref.wrappers.RefList<T> all;
  private final java.util.concurrent.LinkedBlockingQueue<T> pool = new java.util.concurrent.LinkedBlockingQueue<>();

  public StaticResourcePool(@javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<T> items) {
    super();
    this.all = com.simiacryptus.ref.wrappers.RefCollections
        .unmodifiableList(new com.simiacryptus.ref.wrappers.RefArrayList<>(items));
    pool.addAll(getAll());
  }

  @javax.annotation.Nonnull
  public com.simiacryptus.ref.wrappers.RefList<T> getAll() {
    return all;
  }

  public void apply(@Nonnull final com.simiacryptus.ref.wrappers.RefConsumer<T> f) {
    apply(f, x -> true, false);
  }

  public void apply(@javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefConsumer<T> f,
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

  public <U> U run(@javax.annotation.Nonnull final Function<T, U> f, final Predicate<T> filter,
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
    com.simiacryptus.ref.wrappers.RefArrayList<T> sampled = new com.simiacryptus.ref.wrappers.RefArrayList<>();
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
    } catch (@javax.annotation.Nonnull final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
