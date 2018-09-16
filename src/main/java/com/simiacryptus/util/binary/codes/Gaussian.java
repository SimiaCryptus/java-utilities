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

package com.simiacryptus.util.binary.codes;

import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.BitOutputStream;

import java.io.IOException;

/**
 * The type Gaussian.
 */
public class Gaussian {

  /**
   * The constant LOG2.
   */
  public static final double LOG2 = Math.log(2);
  /**
   * The Mean.
   */
  public final double mean;
  /**
   * The Std dev.
   */
  public final double stdDev;

  /**
   * Instantiates a new Gaussian.
   *
   * @param mean   the mean
   * @param stdDev the std dev
   */
  public Gaussian(final double mean, final double stdDev) {
    super();
    if (Double.isNaN(mean)) {
      throw new IllegalArgumentException();
    }
    if (Double.isInfinite(mean)) {
      throw new IllegalArgumentException();
    }
    if (Double.isNaN(stdDev)) {
      throw new IllegalArgumentException();
    }
    if (Double.isInfinite(stdDev)) {
      throw new IllegalArgumentException();
    }
    if (0. >= stdDev) {
      throw new IllegalArgumentException();
    }
    this.mean = mean;
    this.stdDev = stdDev;
  }

  /**
   * From binomial gaussian.
   *
   * @param probability     the probability
   * @param totalPopulation the total population
   * @return the gaussian
   */
  public static Gaussian fromBinomial(final double probability,
                                      final long totalPopulation) {
    if (0. >= totalPopulation) {
      throw new IllegalArgumentException();
    }
    if (0. >= probability) {
      throw new IllegalArgumentException();
    }
    if (1. <= probability) {
      throw new IllegalArgumentException();
    }
    if (Double.isNaN(probability)) {
      throw new IllegalArgumentException();
    }
    if (Double.isInfinite(probability)) {
      throw new IllegalArgumentException();
    }
    return new Gaussian(
        probability * totalPopulation,
        Math.sqrt(totalPopulation * probability * (1 - probability)));
  }

  /**
   * Log 2 double.
   *
   * @param d the d
   * @return the double
   */
  public static double log2(final double d) {
    return Math.log(d) / LOG2;
  }

  /**
   * Decode long.
   *
   * @param in  the in
   * @param max the max
   * @return the long
   * @throws IOException the io exception
   */
  public long decode(final BitInputStream in, final long max)
      throws IOException {
    if (0 == max) {
      return 0;
    }
    int bits = (int) (Math.round(log2(2 * this.stdDev)) - 1);
    if (0 > bits) {
      bits = 0;
    }
    final long centralWindow = 1l << bits;
    if (centralWindow >= (max + 1) / 2.) {
      return in.readBoundedLong(max + 1);
    }
    long stdDevWindowStart = (long) (this.mean - centralWindow / 2);
    long stdDevWindowEnd = stdDevWindowStart + centralWindow;
    if (stdDevWindowStart < 0) {
      stdDevWindowEnd += -stdDevWindowStart;
      stdDevWindowStart += -stdDevWindowStart;
    } else {
      final long delta = stdDevWindowEnd - (max + 1);
      if (delta > 0) {
        stdDevWindowStart -= delta;
        stdDevWindowEnd -= delta;
      }
    }
    if (in.readBool()) {
      return in.readBoundedLong(centralWindow) + stdDevWindowStart;
    } else {
      boolean side;
      if (stdDevWindowStart <= 0) {
        side = true;
      } else if (stdDevWindowEnd > max) {
        side = false;
      } else {
        side = in.readBool();
      }
      if (side) {
        return stdDevWindowEnd + in.readBoundedLong(1 + max - stdDevWindowEnd);
      } else {
        return in.readBoundedLong(stdDevWindowStart);
      }
    }
  }

  /**
   * Encode.
   *
   * @param out   the out
   * @param value the value
   * @param max   the max
   * @throws IOException the io exception
   */
  public void encode(final BitOutputStream out, final long value, final long max)
      throws IOException {
    if (0 == max) {
      return;
    }
    int bits = (int) (Math.round(log2(2 * this.stdDev)) - 1);
    if (0 > bits) {
      bits = 0;
    }
    final long centralWindow = 1l << bits;
    if (centralWindow >= (max + 1) / 2.) {
      out.writeBoundedLong(value, max + 1);
      return;
    }
    long stdDevWindowStart = (long) (this.mean - centralWindow / 2);
    long stdDevWindowEnd = stdDevWindowStart + centralWindow;
    if (stdDevWindowStart < 0) {
      stdDevWindowEnd += -stdDevWindowStart;
      stdDevWindowStart += -stdDevWindowStart;
    } else {
      final long delta = stdDevWindowEnd - (max + 1);
      if (delta > 0) {
        stdDevWindowStart -= delta;
        stdDevWindowEnd -= delta;
      }
    }
    if (value < stdDevWindowStart) {
      out.write(false);
      if (stdDevWindowEnd <= max) {
        out.write(false);
      }
      out.writeBoundedLong(value, stdDevWindowStart);
    } else if (value < stdDevWindowEnd) {
      out.write(true);
      out.writeBoundedLong(value - stdDevWindowStart, centralWindow);
    } else {
      out.write(false);
      if (stdDevWindowStart > 0) {
        out.write(true);
      }
      out.writeBoundedLong(value - stdDevWindowEnd, 1 + max - stdDevWindowEnd);
    }
  }

}
