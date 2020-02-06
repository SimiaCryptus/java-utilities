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

import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Collector;

public class DoubleStatistics extends DoubleSummaryStatistics {

  @Nonnull
  public static Collector<Double, DoubleStatistics, DoubleStatistics> COLLECTOR = Collector.of(() -> new DoubleStatistics(),
      (doubleStatistics, value) -> doubleStatistics.accept(value), (doubleStatistics, other) -> doubleStatistics.combine(other), d -> d);

  @Nonnull
  public static Collector<Number, DoubleStatistics, DoubleStatistics> NUMBERS = Collector.of(() -> new DoubleStatistics(),
      (a, n) -> a.accept(n.doubleValue()), (doubleStatistics, other) -> doubleStatistics.combine(other), d -> d);

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

  @Nonnull
  public DoubleStatistics accept(@Nonnull final double[] value) {
    RefArrays.stream(value).forEach(value1 -> accept(value1));
    return this;
  }

  @Nonnull
  public DoubleStatistics combine(@Nonnull final DoubleStatistics other) {
    super.combine(other);
    simpleSumOfSquare += other.simpleSumOfSquare;
    sumOfSquareWithCompensation(other.sumOfSquare);
    sumOfSquareWithCompensation(other.sumOfSquareCompensation);
    return this;
  }

  @Nonnull
  @Override
  public String toString() {
    return toString(1).toString();
  }

  @Nonnull
  public CharSequence toString(final double scale) {
    return RefString.format("%.4e +- %.4e [%.4e - %.4e] (%d#)", getAverage() * scale, getStandardDeviation() * scale,
        getMin() * scale, getMax() * scale, getCount());
  }

  private void sumOfSquareWithCompensation(final double value) {
    final double tmp = value - sumOfSquareCompensation;
    final double velvel = sumOfSquare + tmp; // Little wolf of rounding error
    sumOfSquareCompensation = velvel - sumOfSquare - tmp;
    sumOfSquare = velvel;
  }
}
