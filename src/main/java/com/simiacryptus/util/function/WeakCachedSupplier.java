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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefSupplier;
import com.simiacryptus.ref.wrappers.RefWeakReference;

import javax.annotation.Nullable;

public class WeakCachedSupplier<T> extends ReferenceCountingBase implements RefSupplier<T> {

  private final RefSupplier<T> fn;
  @Nullable
  private volatile RefWeakReference<T> cached;

  public WeakCachedSupplier(@RefAware final RefSupplier<T> fn) {
    this.fn = fn;
  }

  @Nullable
  @Override
  @RefAware
  public T get() {
    @Nullable
    T obj = null == cached ? null : cached.get();
    if (null == obj) {
      synchronized (this) {
        obj = null == cached ? null : cached.get();
        if (null == obj) {
          obj = fn.get();
          if (null != obj) {
            RefUtil.freeRef(cached);
            cached = new RefWeakReference<>(obj);
          }
        }
      }
    }
    return RefUtil.addRef(obj);
  }

  @Override
  protected void _free() {
    super._free();
    RefUtil.freeRef(fn);
    RefUtil.freeRef(cached);
  }
}
