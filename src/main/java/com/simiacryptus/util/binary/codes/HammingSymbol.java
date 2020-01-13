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

import com.simiacryptus.ref.lang.RefAware;

public class HammingSymbol<T extends Comparable<T>> implements Comparable<HammingSymbol<T>> {

  public final T key;
  public final int count;

  public HammingSymbol(final int count, final T key) {
    this.count = count;
    this.key = key;
  }

  @Override
  public int compareTo(final HammingSymbol<T> o) {
    return this.key.compareTo(o.key);
  }

}