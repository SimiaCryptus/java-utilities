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

public class FastRandom {
  public static final FastRandom INSTANCE = new FastRandom();
  private final long t;
  private long x;
  private long y;
  private long z;

  public FastRandom() {
    this(com.simiacryptus.ref.wrappers.RefSystem.nanoTime());
  }

  public FastRandom(final long seed) {
    t = seed >>> 24;
    x = seed;
    y = seed >>> 8;
    z = seed >>> 16;
  }

  public static long xorshift(long x) {
    x ^= x << 16;
    x ^= x >> 5;
    x ^= x << 1;
    return x;
  }

  public double random() {
    long z = next();
    int exponentMag = 4;
    double resolution = 1e8;
    double x = ((z / 2) % resolution) / resolution;
    double y = z % exponentMag - exponentMag / 2;
    while (y > 1) {
      y--;
      x = 2;
    }
    while (y < -1) {
      y++;
      x /= 2;
    }
    return x;
  }

  public long next() {
    long x = xorshift(this.x);
    this.x = y;
    y = z;
    z = this.x ^ x ^ y;
    return z;
  }

}
