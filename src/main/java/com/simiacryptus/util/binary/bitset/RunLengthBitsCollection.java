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

package com.simiacryptus.util.binary.bitset;

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefHashMap;
import com.simiacryptus.ref.wrappers.RefHashSet;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.BitOutputStream;
import com.simiacryptus.util.binary.Bits;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class RunLengthBitsCollection extends BitsCollection<RefHashMap<Bits, AtomicInteger>> {
  public RunLengthBitsCollection(final int bitDepth) {
    super(bitDepth, new RefHashMap<Bits, AtomicInteger>());
  }

  @Override
  public void read(@Nonnull final BitInputStream in) throws IOException {
    final int size = (int) in.read(32).toLong();
    for (int i = 0; i < size; i++) {
      final Bits bits = in.read(this.bitDepth);
      final int count = (int) in.read(32).toLong();
      assert this.map != null;
      this.map.put(bits, new AtomicInteger(count));
    }
  }

  @Override
  public void write(@Nonnull final BitOutputStream out) throws IOException {
    RefList<Bits> temp_12_0001 = this.getList();
    out.write(new Bits(temp_12_0001.size(), 32));
    temp_12_0001.freeRef();
    assert this.map != null;
    RefHashSet<Entry<Bits, AtomicInteger>> entries = this.map.entrySet();
    entries.forEach(e -> {
      try {
        out.write(e.getKey());
        out.write(new Bits(e.getValue().get(), 32));
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      } finally {
        RefUtil.freeRef(e);
      }
    });
    entries.freeRef();
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  RunLengthBitsCollection addRef() {
    return (RunLengthBitsCollection) super.addRef();
  }

}