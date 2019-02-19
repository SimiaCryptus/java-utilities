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

package com.simiacryptus.util.binary.codes;

/**
 * The type Hamming symbol.
 *
 * @param <T> the type parameter
 */
public class HammingSymbol<T extends Comparable<T>> implements
    Comparable<HammingSymbol<T>> {

  /**
   * The Key.
   */
  public final T key;
  /**
   * The Count.
   */
  public final int count;

  /**
   * Instantiates a new Hamming symbol.
   *
   * @param count the count
   * @param key   the key
   */
  public HammingSymbol(final int count, final T key) {
    this.count = count;
    this.key = key;
  }

  @Override
  public int compareTo(final HammingSymbol<T> o) {
    return this.key.compareTo(o.key);
  }

}