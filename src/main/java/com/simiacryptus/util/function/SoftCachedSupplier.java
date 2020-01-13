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

package com.simiacryptus.util.function;

import com.simiacryptus.ref.lang.RefAware;

import javax.annotation.Nullable;
import java.lang.ref.SoftReference;
import java.util.function.Supplier;

public class SoftCachedSupplier<T> implements Supplier<T> {

  private final Supplier<T> fn;
  @Nullable
  private volatile SoftReference<T> cached;

  public SoftCachedSupplier(final Supplier<T> fn) {
    this.fn = fn;
  }

  @Nullable
  @Override
  public T get() {
    @Nullable
    T obj = null == cached ? null : cached.get();
    if (null == obj) {
      synchronized (this) {
        obj = null == cached ? null : cached.get();
        if (null == obj) {
          obj = fn.get();
          if (null != obj) {
            cached = new SoftReference<>(obj);
          }
        }
      }
    }
    return obj;
  }
}
