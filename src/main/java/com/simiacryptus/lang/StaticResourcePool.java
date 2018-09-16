/*
 * Copyright (c) 2018 by Andrew Charneski.
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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The type Static resource pool.
 *
 * @param <T> the type parameter
 */
public class StaticResourcePool<T> {

  @javax.annotation.Nonnull
  private final List<T> all;
  private final java.util.concurrent.LinkedBlockingQueue<T> pool = new java.util.concurrent.LinkedBlockingQueue<>();

  /**
   * Instantiates a new Static resource pool.
   *
   * @param items the items
   */
  public StaticResourcePool(@javax.annotation.Nonnull final List<T> items) {
    super();
    this.all = Collections.unmodifiableList(new ArrayList<>(items));
    pool.addAll(getAll());
  }

  /**
   * With u.
   *
   * @param f the f
   */
  public void apply(@Nonnull final Consumer<T> f) {
    apply(f, x -> true, false);
  }

  /**
   * With u.
   *
   * @param f         the f
   * @param filter    the filter
   * @param exclusive the exclusive
   */
  public void apply(@javax.annotation.Nonnull final Consumer<T> f, final Predicate<T> filter, final boolean exclusive) {
    T poll = get(filter, exclusive);
    try {
      f.accept(poll);
    } finally {
      this.pool.add(poll);
    }
  }

  @Nonnull
  private T get(Predicate<T> filter, final boolean exclusive) {
    ArrayList<T> sampled = new ArrayList<>();
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
        if (null == poll) throw new RuntimeException("Timeout awaiting item from pool");
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

  /**
   * Gets all.
   *
   * @return the all
   */
  @javax.annotation.Nonnull
  public List<T> getAll() {
    return all;
  }

  /**
   * With u.
   *
   * @param <U> the type parameter
   * @param f   the f
   * @return the u
   */
  public <U> U run(@Nonnull final Function<T, U> f) {
    return run(f, x -> true, false);
  }

  /**
   * With u.
   *
   * @param <U>       the type parameter
   * @param f         the f
   * @param filter    the filter
   * @param exclusive the exclusive
   * @return the u
   */
  public <U> U run(@javax.annotation.Nonnull final Function<T, U> f, final Predicate<T> filter, final boolean exclusive) {
    if (all.isEmpty()) throw new IllegalStateException();
    T poll = get(filter, exclusive);
    try {
      return f.apply(poll);
    } finally {
      this.pool.add(poll);
    }
  }

  /**
   * Size int.
   *
   * @return the int
   */
  public int size() {
    return getAll().size();
  }
}
