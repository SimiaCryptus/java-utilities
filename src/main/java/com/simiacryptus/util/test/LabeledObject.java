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

package com.simiacryptus.util.test;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefFunction;
import com.simiacryptus.ref.wrappers.RefStringBuilder;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class LabeledObject<T> extends ReferenceCountingBase {
  public final @RefAware T data;
  public final String label;

  public LabeledObject(final @RefAware T img, final String name) {
    super();
    this.data = img;
    this.label = name;
  }

  @Nonnull
  public <U> LabeledObject<U> map(@Nonnull final RefFunction<T, U> f) {
    return new LabeledObject<>(f.apply(this.data), this.label);
  }

  @Nonnull
  @Override
  public String toString() {
    @Nonnull final RefStringBuilder sb = new RefStringBuilder(
        "LabeledObject{");
    sb.append("data=").append(RefUtil.addRef(data));
    sb.append(", label='").append(label).append('\'');
    sb.append('}');
    return sb.toString();
  }

  @Override
  protected void _free() {
    RefUtil.freeRef(data);
    super._free();
  }
}
