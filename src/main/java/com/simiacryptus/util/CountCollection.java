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

package com.simiacryptus.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type Count collection.
 *
 * @param <T> the type parameter
 * @param <C> the type parameter
 */
public class CountCollection<T, C extends Map<T, AtomicInteger>> {
  
  /**
   * The Map.
   */
  protected final C map;
  
  /**
   * Instantiates a new Count collection.
   *
   * @param collection the collection
   */
  public CountCollection(final C collection) {
    super();
    this.map = collection;
  }
  
  /**
   * Add int.
   *
   * @param bits the bits
   * @return the int
   */
  public int add(final T bits) {
    return this.getCounter(bits).incrementAndGet();
  }
  
  /**
   * Add int.
   *
   * @param bits  the bits
   * @param count the count
   * @return the int
   */
  public int add(final T bits, final int count) {
    return this.getCounter(bits).addAndGet(count);
  }
  
  /**
   * Count int.
   *
   * @param key the key
   * @return the int
   */
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
  
  /**
   * Gets list.
   *
   * @return the list
   */
  public List<T> getList() {
    final ArrayList<T> list = new ArrayList<T>();
    for (final Entry<T, AtomicInteger> e : this.map.entrySet()) {
      for (int i = 0; i < e.getValue().get(); i++) {
        list.add(e.getKey());
      }
    }
    return list;
  }
  
  /**
   * Gets map.
   *
   * @return the map
   */
  public Map<T, Integer> getMap() {
    return Maps.transformEntries(this.map,
      new EntryTransformer<T, AtomicInteger, Integer>() {
        @Override
        public Integer transformEntry(final T key, final AtomicInteger value) {
          return value.get();
        }
      });
  }
  
}