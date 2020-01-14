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

import com.simiacryptus.ref.wrappers.RefSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;

public class TimedResult<T> {
  public final T result;
  public final long timeNanos;
  public final long gcMs;

  public TimedResult(final T result, final long timeNanos, long gcMs) {
    this.result = result;
    this.timeNanos = timeNanos;
    this.gcMs = gcMs;
  }

  @Nonnull
  public static <T> TimedResult<T> time(@Nonnull final UncheckedSupplier<T> fn) {
    long priorGcMs = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x -> x.getCollectionTime())
        .sum();
    final long start = RefSystem.nanoTime();
    @Nullable
    T result = null;
    try {
      result = fn.get();
    } catch (@Nonnull final RuntimeException e) {
      throw e;
    } catch (@Nonnull final Exception e) {
      throw new RuntimeException(e);
    }
    long wallClockTime = RefSystem.nanoTime() - start;
    long gcTime = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x -> x.getCollectionTime()).sum()
        - priorGcMs;
    return new TimedResult<T>(result, wallClockTime, gcTime);
  }

  @Nonnull
  public static <T> TimedResult<Void> time(@Nonnull final UncheckedRunnable<T> fn) {
    long priorGcMs = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x -> x.getCollectionTime())
        .sum();
    final long start = RefSystem.nanoTime();
    try {
      fn.get();
    } catch (@Nonnull final RuntimeException e) {
      throw e;
    } catch (@Nonnull final Exception e) {
      throw new RuntimeException(e);
    }
    long wallClockTime = RefSystem.nanoTime() - start;
    long gcTime = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x -> x.getCollectionTime()).sum()
        - priorGcMs;
    return new TimedResult<Void>(null, wallClockTime, gcTime);
  }

  public double seconds() {
    return timeNanos / 1e9;
  }

  public double gc_seconds() {
    return gcMs / 1e3;
  }
}
