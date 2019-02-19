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

import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * The type Resource pool.
 *
 * @param <T> the type parameter
 */
public abstract class ResourcePool<T> {

  @javax.annotation.Nonnull
  private final java.util.HashSet<T> all;
  private final ThreadLocal<T> currentValue = new ThreadLocal<>();
  private final int maxItems;
  private final java.util.concurrent.LinkedBlockingQueue<T> pool = new java.util.concurrent.LinkedBlockingQueue<>();

  /**
   * Instantiates a new Resource pool.
   *
   * @param maxItems the max items
   */
  public ResourcePool(final int maxItems) {
    super();
    this.maxItems = maxItems;
    this.all = new java.util.HashSet<>(this.maxItems);
  }

  /**
   * Create t.
   *
   * @return the t
   */
  public abstract T create();

  /**
   * Get t.
   *
   * @return the t
   */
  public T get() {
    return get(x -> true);
  }

  /**
   * Get t.
   *
   * @param filter the filter
   * @return the t
   */
  public T get(Predicate<T> filter) {
    ArrayList<T> sampled = new ArrayList<>();
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

  /**
   * Size int.
   *
   * @return the int
   */
  public int size() {
    return all.size();
  }

  /**
   * With.
   *
   * @param <U> the type parameter
   * @param f   the f
   * @return the u
   */
  public <U> U apply(@javax.annotation.Nonnull final java.util.function.Function<T, U> f) {
    return apply(f, x -> true);
  }

  /**
   * With.
   *
   * @param f the f
   */
  public void apply(@javax.annotation.Nonnull final java.util.function.Consumer<T> f) {
    apply(f, x -> true);
  }

  /**
   * With.
   *
   * @param <U>    the type parameter
   * @param f      the f
   * @param filter the filter
   * @return the u
   */
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

  /**
   * With.
   *
   * @param f      the f
   * @param filter the filter
   */
  public void apply(@javax.annotation.Nonnull final java.util.function.Consumer<T> f, final Predicate<T> filter) {
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
