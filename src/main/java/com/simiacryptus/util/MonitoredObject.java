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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefHashMap;
import com.simiacryptus.ref.wrappers.RefHashSet;
import com.simiacryptus.ref.wrappers.RefMap;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public @RefAware
class MonitoredObject extends ReferenceCountingBase implements MonitoredItem {

  private final RefMap<CharSequence, Object> items = new RefHashMap<>();

  @Nonnull
  @Override
  public RefMap<CharSequence, Object> getMetrics() {
    @Nonnull final RefHashMap<CharSequence, Object> returnValue = new RefHashMap<>();
    items.entrySet().stream().parallel().forEach(e -> {
      final CharSequence k = e.getKey();
      final Object v = e.getValue();
      if (v instanceof MonitoredItem) {
        returnValue.put(k, ((MonitoredItem) v).getMetrics());
      } else if (v instanceof Supplier) {
        returnValue.put(k, ((Supplier<?>) v).get());
      } else {
        returnValue.put(k, v);
      }
    });
    return returnValue;
  }

  @Nonnull
  public MonitoredObject addConst(final CharSequence key, final Object item) {
    items.put(key, item);
    return this;
  }

  @Nonnull
  public MonitoredObject addField(final CharSequence key, final Supplier<Object> item) {
    items.put(key, item);
    return this;
  }

  @Nonnull
  public MonitoredObject addObj(final CharSequence key, final MonitoredItem item) {
    items.put(key, item);
    return this;
  }

  @Nonnull
  public MonitoredObject clearConstants() {
    @Nonnull final RefHashSet<CharSequence> keys = new RefHashSet<>(
        items.keySet());
    for (final CharSequence k : keys) {
      final Object v = items.get(k);
      if (v instanceof MonitoredObject) {
        ((MonitoredObject) v).clearConstants();
      } else if (!(v instanceof Supplier) && !(v instanceof MonitoredItem)) {
        items.remove(k);
      }
    }
    return this;
  }
}
