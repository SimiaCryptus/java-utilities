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
import com.simiacryptus.ref.wrappers.RefHashMap;
import com.simiacryptus.ref.wrappers.RefHashSet;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefSet;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MonitoredObject extends ReferenceCountingBase implements MonitoredItem {

  private final RefMap<CharSequence, Object> items = new RefHashMap<>();

  @Nonnull
  @Override
  public Map<CharSequence, Object> getMetrics() {
    @Nonnull
    final HashMap<CharSequence, Object> returnValue = new HashMap<>();
    RefSet<Map.Entry<CharSequence, Object>> temp_14_0001 = items.entrySet();
    temp_14_0001.stream().parallel()
        .forEach(RefUtil.wrapInterface((Consumer<? super Map.Entry<CharSequence, Object>>) e -> {
          final CharSequence k = e.getKey();
          final Object v = e.getValue();
          if (null != e)
            RefUtil.freeRef(e);
          if (v instanceof MonitoredItem) {
            returnValue.put(k, ((MonitoredItem) v).getMetrics());
          } else if (v instanceof Supplier) {
            returnValue.put(k, ((Supplier<?>) v).get());
          } else {
            returnValue.put(k, v);
          }
        }, RefUtil.addRef(returnValue)));
    if (null != temp_14_0001)
      temp_14_0001.freeRef();
    return returnValue;
  }

  public static @SuppressWarnings("unused") MonitoredObject[] addRefs(MonitoredObject[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MonitoredObject::addRef)
        .toArray((x) -> new MonitoredObject[x]);
  }

  public static @SuppressWarnings("unused") MonitoredObject[][] addRefs(MonitoredObject[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MonitoredObject::addRefs)
        .toArray((x) -> new MonitoredObject[x][]);
  }

  @Nonnull
  public MonitoredObject addConst(final CharSequence key, final Object item) {
    items.put(key, item);
    return this.addRef();
  }

  @Nonnull
  public MonitoredObject addField(final CharSequence key, final Supplier<Object> item) {
    items.put(key, item);
    return this.addRef();
  }

  @Nonnull
  public MonitoredObject addObj(final CharSequence key, @RefAware final MonitoredItem item) {
    items.put(key, item);
    return this.addRef();
  }

  @Nonnull
  public MonitoredObject clearConstants() {
    @Nonnull
    final RefHashSet<CharSequence> keys = new RefHashSet<>(items.keySet());
    for (final CharSequence k : keys) {
      final Object v = items.get(k);
      if (v instanceof MonitoredObject) {
        RefUtil.freeRef(((MonitoredObject) v).clearConstants());
      } else if (!(v instanceof Supplier) && !(v instanceof MonitoredItem)) {
        items.remove(k);
      }
    }
    keys.freeRef();
    return this.addRef();
  }

  public @SuppressWarnings("unused") void _free() {
    if (null != items)
      items.freeRef();
  }

  public @Override @SuppressWarnings("unused") MonitoredObject addRef() {
    return (MonitoredObject) super.addRef();
  }
}
