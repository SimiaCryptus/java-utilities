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
import com.simiacryptus.ref.wrappers.RefSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;

public class TimedResult<T> extends ReferenceCountingBase {
  public final long timeNanos;
  public final long gcMs;
  private final T result;

  public TimedResult(final T result, final long timeNanos, long gcMs) {
    this.result = result;
    this.timeNanos = timeNanos;
    this.gcMs = gcMs;
  }

  public @RefAware T getResult() {
    return RefUtil.addRef(result);
  }

  @Nonnull
  public static <T> TimedResult<T> time(@Nonnull @RefAware final UncheckedSupplier<T> fn) {
    try {
      final long priorGcMs = gcTime();
      final long start = RefSystem.nanoTime();
      @Nullable
      T result = fn.get();
      return new TimedResult<T>(result, RefSystem.nanoTime() - start, gcTime() - priorGcMs);
    } catch (@Nonnull final RuntimeException e) {
      throw e;
    } catch (@Nonnull final Exception e) {
      throw new RuntimeException(e);
    } finally {
      RefUtil.freeRef(fn);
    }
  }

  public static long gcTime() {
    return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x -> x.getCollectionTime()).sum();
  }

  @Nonnull
  public static <T> TimedResult<Void> time(@Nonnull @RefAware final UncheckedRunnable<T> fn) {
    try {
      final long priorGcMs = gcTime();
      final long start = RefSystem.nanoTime();
      fn.get();
      return new TimedResult<Void>(null, RefSystem.nanoTime() - start, gcTime() - priorGcMs);
    } catch (@Nonnull final RuntimeException e) {
      throw e;
    } catch (@Nonnull final Exception e) {
      throw new RuntimeException(e);
    } finally {
      RefUtil.freeRef(fn);
    }
  }

  public double seconds() {
    return timeNanos / 1e9;
  }

  public double gc_seconds() {
    return gcMs / 1e3;
  }

  @Override
  protected void _free() {
    RefUtil.freeRef(result);
    super._free();
  }
}
