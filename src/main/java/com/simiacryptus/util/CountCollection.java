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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrayList;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefMaps;
import com.simiacryptus.ref.wrappers.RefMaps.EntryTransformer;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public @RefAware class CountCollection<T, C extends RefMap<T, AtomicInteger>> extends ReferenceCountingBase {

  protected final C map;

  public CountCollection(final C collection) {
    super();
    {
      C temp_00_0001 = RefUtil.addRef(collection);
      this.map = RefUtil.addRef(temp_00_0001);
      if (null != temp_00_0001)
        temp_00_0001.freeRef();
    }
    if (null != collection)
      collection.freeRef();
  }

  public RefList<T> getList() {
    final RefArrayList<T> list = new RefArrayList<T>();
    for (final Entry<T, AtomicInteger> e : this.map.entrySet()) {
      for (int i = 0; i < e.getValue().get(); i++) {
        list.add(e.getKey());
      }
    }
    return list;
  }

  public RefMap<T, Integer> getMap() {
    return RefMaps.transformEntries(RefUtil.addRef(this.map), new EntryTransformer<T, AtomicInteger, Integer>() {
      @Override
      public Integer transformEntry(final @com.simiacryptus.ref.lang.RefAware T key,
          final @com.simiacryptus.ref.lang.RefAware AtomicInteger value) {
        return value.get();
      }
    });
  }

  public static @SuppressWarnings("unused") CountCollection[] addRefs(CountCollection[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(CountCollection::addRef)
        .toArray((x) -> new CountCollection[x]);
  }

  public static @SuppressWarnings("unused") CountCollection[][] addRefs(CountCollection[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(CountCollection::addRefs)
        .toArray((x) -> new CountCollection[x][]);
  }

  public int add(final T bits) {
    return this.getCounter(bits).incrementAndGet();
  }

  public int add(final T bits, final int count) {
    return this.getCounter(bits).addAndGet(count);
  }

  public @SuppressWarnings("unused") void _free() {
    if (null != map)
      map.freeRef();
  }

  public @Override @SuppressWarnings("unused") CountCollection<T, C> addRef() {
    return (CountCollection<T, C>) super.addRef();
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