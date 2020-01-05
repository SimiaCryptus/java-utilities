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

package com.simiacryptus.util.data;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public @RefAware
class PercentileStatistics extends ScalarStatistics {

  private final List<double[]> values = new ArrayList<>();

  @Override
  public RefMap<CharSequence, Object> getMetrics() {
    final RefMap<CharSequence, Object> map = super.getMetrics().addRef();
    map.put("tp50", getPercentile(0.5));
    map.put("tp75", getPercentile(0.75));
    map.put("tp90", getPercentile(0.9));
    return map;
  }

  @Nullable
  @Override
  public synchronized ScalarStatistics add(@Nonnull final double... values) {
    if (null != this.values) {
      this.values.add(RefArrays.copyOf(values, values.length));
    }
    super.add(values);
    return null;
  }

  @Override
  public void clear() {
    values.clear();
    super.clear();
  }

  public synchronized Double getPercentile(final double percentile) {
    if (null == values)
      return Double.NaN;
    return values.parallelStream().flatMapToDouble(x -> RefArrays.stream(x)).sorted()
        .skip((int) (percentile * values.size())).findFirst().orElse(Double.NaN);
  }

}
