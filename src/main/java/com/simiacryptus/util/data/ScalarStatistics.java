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

import com.google.gson.JsonObject;
import com.simiacryptus.util.MonitoredItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class ScalarStatistics implements MonitoredItem, Serializable {
  private static final double zeroTol = 1e-20;
  private volatile double max = -Double.POSITIVE_INFINITY;
  private volatile double min = Double.POSITIVE_INFINITY;
  private volatile int negatives = 0;
  private volatile int positives = 0;
  private volatile int sum0 = 0;
  private volatile double sum1 = 0;
  private volatile double sum2 = 0;
  private volatile double sumLog = 0;
  private volatile int zeros = 0;

  public int getCount() {
    return sum0;
  }

  @Nonnull
  public JsonObject getJson() {
    @Nonnull final JsonObject json = new JsonObject();
    json.addProperty("min", min);
    json.addProperty("max", max);
    json.addProperty("negatives", negatives);
    json.addProperty("positives", positives);
    json.addProperty("zeros", zeros);
    json.addProperty("sum0", sum0);
    json.addProperty("sum1", sum1);
    json.addProperty("sum2", sum2);
    json.addProperty("sumLog", sumLog);
    return json;
  }

  public double getMean() {
    return sum1 / sum0;
  }

  public double getMeanPower() {
    return sumLog / (sum0 - zeros);
  }

  @Override
  public Map<CharSequence, Object> getMetrics() {
    @Nonnull final HashMap<CharSequence, Object> map = new HashMap<>();
    map.put("count", sum0);
    map.put("sum", sum1);
    map.put("negative", negatives);
    map.put("positive", positives);
    map.put("min", min);
    map.put("max", max);
    map.put("mean", getMean());
    map.put("stdDev", getStdDev());
    map.put("meanExponent", getMeanPower());
    map.put("zeros", zeros);
    return map;
  }

  public double getStdDev() {
    return Math.sqrt(Math.abs(Math.pow(getMean(), 2) - sum2 / sum0));
  }

  @Nonnull
  public static ScalarStatistics stats(@Nonnull final double[] data) {
    @Nonnull final ScalarStatistics statistics = new PercentileStatistics();
    Arrays.stream(data).forEach(v -> statistics.add(v));
    return statistics;
  }

  @Nullable
  public ScalarStatistics add(@Nonnull final double... values) {
    double v1 = 0;
    double v2 = 0;
    double vmax = max;
    double vmin = min;
    int z = 0;
    double vlog = 0;
    int n = 0;
    int p = 0;
    for (final double v : values) {
      v1 += v;
      v2 += v * v;
      vmin = Math.min(vmin, v);
      vmax = Math.max(vmax, v);
      if (Math.abs(v) < ScalarStatistics.zeroTol) {
        z++;
      } else {
        if (v < 0) {
          n++;
        } else {
          p++;
        }
        vlog += Math.log10(Math.abs(v));
      }
    }
    synchronized (this) {
      sum0 += values.length;
      sum1 += v1;
      sum2 += v2;
      min = Math.min(min, vmin);
      max = Math.max(max, vmax);
      negatives += n;
      positives += p;
      sumLog += vlog;
      zeros += z;
    }
    return this;
  }

  public final synchronized void add(final double v) {
    sum0 += 1;
    sum1 += v;
    sum2 += v * v;
    min = Math.min(min, v);
    max = Math.max(max, v);
    if (Math.abs(v) < ScalarStatistics.zeroTol) {
      zeros++;
    } else {
      if (v < 0) {
        negatives++;
      } else {
        positives++;
      }
      sumLog += Math.log10(Math.abs(v));
    }
  }

  @Nonnull
  public final synchronized ScalarStatistics add(@Nonnull final ScalarStatistics right) {
    @Nonnull final ScalarStatistics sum = new ScalarStatistics();
    sum.sum0 += sum0;
    sum.sum0 += right.sum0;
    sum.sum1 += sum1;
    sum.sum1 += right.sum1;
    sum.sum2 += sum2;
    sum.sum2 += right.sum2;
    return sum;
  }

  public void clear() {
    min = Double.POSITIVE_INFINITY;
    max = -Double.POSITIVE_INFINITY;
    negatives = 0;
    positives = 0;
    zeros = 0;
    sum0 = 0;
    sum1 = 0;
    sum2 = 0;
    sumLog = 0;
  }

  public void readJson(@Nullable final JsonObject json) {
    if (null == json)
      return;
    min = json.get("min").getAsDouble();
    max = json.get("max").getAsDouble();
    negatives = json.get("negatives").getAsInt();
    positives = json.get("positives").getAsInt();
    zeros = json.get("zeros").getAsInt();
    sum0 = json.get("sum0").getAsInt();
    sum1 = json.get("sum1").getAsDouble();
    sum2 = json.get("sum2").getAsDouble();
    sumLog = json.get("sumLog").getAsDouble();
  }

  @Nonnull
  public final synchronized ScalarStatistics subtract(@Nonnull final ScalarStatistics right) {
    @Nonnull final ScalarStatistics sum = new ScalarStatistics();
    sum.sum0 += sum0;
    sum.sum0 -= right.sum0;
    sum.sum1 += sum1;
    sum.sum1 -= right.sum1;
    sum.sum2 += sum2;
    sum.sum2 -= right.sum2;
    return sum;
  }

  @Override
  public String toString() {
    return getMetrics().toString();
  }
}
