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

import java.util.DoubleSummaryStatistics;
import java.util.stream.Collector;

public @com.simiacryptus.ref.lang.RefAware
class DoubleStatistics extends DoubleSummaryStatistics {

  @javax.annotation.Nonnull
  public static Collector<Double, com.simiacryptus.util.data.DoubleStatistics, com.simiacryptus.util.data.DoubleStatistics> COLLECTOR = Collector
      .of(com.simiacryptus.util.data.DoubleStatistics::new, com.simiacryptus.util.data.DoubleStatistics::accept,
          com.simiacryptus.util.data.DoubleStatistics::combine, d -> d);

  @javax.annotation.Nonnull
  public static Collector<Number, com.simiacryptus.util.data.DoubleStatistics, com.simiacryptus.util.data.DoubleStatistics> NUMBERS = Collector
      .of(com.simiacryptus.util.data.DoubleStatistics::new, (a, n) -> a.accept(n.doubleValue()),
          com.simiacryptus.util.data.DoubleStatistics::combine, d -> d);

  private double simpleSumOfSquare; // Used to compute right sum for non-finite inputs
  private double sumOfSquare = 0.0d;
  private double sumOfSquareCompensation; // Low order bits of sum

  public final double getStandardDeviation() {
    return getCount() > 0 ? Math.sqrt(getSumOfSquare() / getCount() - Math.pow(getAverage(), 2)) : 0.0d;
  }

  public double getSumOfSquare() {
    final double tmp = sumOfSquare + sumOfSquareCompensation;
    if (Double.isNaN(tmp) && Double.isInfinite(simpleSumOfSquare)) {
      return simpleSumOfSquare;
    }
    return tmp;
  }

  @Override
  public synchronized void accept(final double value) {
    super.accept(value);
    final double squareValue = value * value;
    simpleSumOfSquare += squareValue;
    sumOfSquareWithCompensation(squareValue);
  }

  @javax.annotation.Nonnull
  public com.simiacryptus.util.data.DoubleStatistics accept(@javax.annotation.Nonnull final double[] value) {
    com.simiacryptus.ref.wrappers.RefArrays.stream(value).forEach(this::accept);
    return this;
  }

  @javax.annotation.Nonnull
  public com.simiacryptus.util.data.DoubleStatistics combine(
      @javax.annotation.Nonnull final com.simiacryptus.util.data.DoubleStatistics other) {
    super.combine(other);
    simpleSumOfSquare += other.simpleSumOfSquare;
    sumOfSquareWithCompensation(other.sumOfSquare);
    sumOfSquareWithCompensation(other.sumOfSquareCompensation);
    return this;
  }

  @Override
  public String toString() {
    return toString(1).toString();
  }

  public CharSequence toString(final double scale) {
    return String.format("%.4e +- %.4e [%.4e - %.4e] (%d#)", getAverage() * scale, getStandardDeviation() * scale,
        getMin() * scale, getMax() * scale, getCount());
  }

  private void sumOfSquareWithCompensation(final double value) {
    final double tmp = value - sumOfSquareCompensation;
    final double velvel = sumOfSquare + tmp; // Little wolf of rounding error
    sumOfSquareCompensation = velvel - sumOfSquare - tmp;
    sumOfSquare = velvel;
  }
}
