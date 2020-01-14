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
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.ref.wrappers.RefMaps.EntryTransformer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class CountCollection<T, C extends RefMap<T, AtomicInteger>> extends ReferenceCountingBase {

  @Nullable
  protected final C map;

  public CountCollection(@Nullable final C collection) {
    super();
    C temp_00_0001 = RefUtil.addRef(collection);
    this.map = RefUtil.addRef(temp_00_0001);
    if (null != temp_00_0001)
      temp_00_0001.freeRef();
    if (null != collection)
      collection.freeRef();
  }

  @Nonnull
  public RefList<T> getList() {
    final RefArrayList<T> list = new RefArrayList<T>();
    assert this.map != null;
    RefSet<Entry<T, AtomicInteger>> entries = this.map.entrySet();
    for (final Entry<T, AtomicInteger> e : entries) {
      for (int i = 0; i < e.getValue().get(); i++) {
        list.add(e.getKey());
      }
    }
    entries.freeRef();
    return list;
  }

  @Nonnull
  public RefMap<T, Integer> getMap() {
    return RefMaps.transformEntries(RefUtil.addRef(this.map), new EntryTransformer<T, AtomicInteger, Integer>() {
      @Override
      public Integer transformEntry(final @RefAware T key,
                                    @Nonnull @RefAware final AtomicInteger value) {
        RefUtil.freeRef(key);
        int i = value.get();
        RefUtil.freeRef(value);
        return i;
      }
    });
  }

  @Nullable
  public static @SuppressWarnings("unused")
  CountCollection[] addRefs(@Nullable CountCollection[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(CountCollection::addRef)
        .toArray((x) -> new CountCollection[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  CountCollection[][] addRefs(@Nullable CountCollection[][] array) {
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

  public @SuppressWarnings("unused")
  void _free() {
    if (null != map)
      map.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  CountCollection<T, C> addRef() {
    return (CountCollection<T, C>) super.addRef();
  }

  protected int count(final T key) {
    assert this.map != null;
    final AtomicInteger counter = this.map.get(key);
    if (null == counter) {
      return 0;
    }
    return counter.get();
  }

  @NotNull
  private AtomicInteger getCounter(final T bits) {
    assert this.map != null;
    AtomicInteger counter = this.map.get(bits);
    if (null == counter) {
      counter = new AtomicInteger();
      this.map.put(bits, counter);
    }
    return counter;
  }

}