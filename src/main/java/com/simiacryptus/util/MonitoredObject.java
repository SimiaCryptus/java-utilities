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

import java.util.function.Supplier;

public @com.simiacryptus.ref.lang.RefAware
class MonitoredObject extends ReferenceCountingBase implements MonitoredItem {

  private final com.simiacryptus.ref.wrappers.RefMap<CharSequence, Object> items = new com.simiacryptus.ref.wrappers.RefHashMap<>();

  @javax.annotation.Nonnull
  @Override
  public com.simiacryptus.ref.wrappers.RefMap<CharSequence, Object> getMetrics() {
    @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefHashMap<CharSequence, Object> returnValue = new com.simiacryptus.ref.wrappers.RefHashMap<>();
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

  @javax.annotation.Nonnull
  public com.simiacryptus.util.MonitoredObject addConst(final CharSequence key, final Object item) {
    items.put(key, item);
    return this;
  }

  @javax.annotation.Nonnull
  public com.simiacryptus.util.MonitoredObject addField(final CharSequence key, final Supplier<Object> item) {
    items.put(key, item);
    return this;
  }

  @javax.annotation.Nonnull
  public com.simiacryptus.util.MonitoredObject addObj(final CharSequence key, final MonitoredItem item) {
    items.put(key, item);
    return this;
  }

  @javax.annotation.Nonnull
  public com.simiacryptus.util.MonitoredObject clearConstants() {
    @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefHashSet<CharSequence> keys = new com.simiacryptus.ref.wrappers.RefHashSet<>(
        items.keySet());
    for (final CharSequence k : keys) {
      final Object v = items.get(k);
      if (v instanceof com.simiacryptus.util.MonitoredObject) {
        ((com.simiacryptus.util.MonitoredObject) v).clearConstants();
      } else if (!(v instanceof Supplier) && !(v instanceof MonitoredItem)) {
        items.remove(k);
      }
    }
    return this;
  }
}
