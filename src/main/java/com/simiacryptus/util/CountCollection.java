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

package com.simiacryptus.util;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefMaps.EntryTransformer;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public @com.simiacryptus.ref.lang.RefAware
class CountCollection<T, C extends com.simiacryptus.ref.wrappers.RefMap<T, AtomicInteger>> extends ReferenceCountingBase {

  protected final C map;

  public CountCollection(final C collection) {
    super();
    this.map = collection;
  }

  public com.simiacryptus.ref.wrappers.RefList<T> getList() {
    final com.simiacryptus.ref.wrappers.RefArrayList<T> list = new com.simiacryptus.ref.wrappers.RefArrayList<T>();
    for (final Entry<T, AtomicInteger> e : this.map.entrySet()) {
      for (int i = 0; i < e.getValue().get(); i++) {
        list.add(e.getKey());
      }
    }
    return list;
  }

  public com.simiacryptus.ref.wrappers.RefMap<T, Integer> getMap() {
    return com.simiacryptus.ref.wrappers.RefMaps.transformEntries(this.map,
        new EntryTransformer<T, AtomicInteger, Integer>() {
          @Override
          public Integer transformEntry(final T key, final AtomicInteger value) {
            return value.get();
          }
        });
  }

  public int add(final T bits) {
    return this.getCounter(bits).incrementAndGet();
  }

  public int add(final T bits, final int count) {
    return this.getCounter(bits).addAndGet(count);
  }

  protected int count(final T key) {
    final AtomicInteger counter = this.map.get(key);
    if (null == counter) {
      return 0;
    }
    return counter.get();
  }

  private AtomicInteger getCounter(final T bits) {
    AtomicInteger counter = this.map.get(bits);
    if (null == counter) {
      counter = new AtomicInteger();
      this.map.put(bits, counter);
    }
    return counter;
  }

}