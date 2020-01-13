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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IntArray {

  public final int[] data;

  public IntArray(final int[] data) {
    this.data = data;
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    @Nonnull
    final IntArray intArray = (IntArray) o;

    return RefArrays.equals(data, intArray.data);
  }

  public int get(final int i) {
    return data[i];
  }

  @Override
  public int hashCode() {
    return RefArrays.hashCode(data);
  }

  public int size() {
    return data.length;
  }
}
