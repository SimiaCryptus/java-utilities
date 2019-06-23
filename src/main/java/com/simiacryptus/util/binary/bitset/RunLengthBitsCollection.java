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

import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.BitOutputStream;
import com.simiacryptus.util.binary.Bits;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class RunLengthBitsCollection extends
    BitsCollection<HashMap<Bits, AtomicInteger>> {
  public RunLengthBitsCollection(final int bitDepth) {
    super(bitDepth, new HashMap<Bits, AtomicInteger>());
  }

  @Override
  public void read(final BitInputStream in) throws IOException {
    final int size = (int) in.read(32).toLong();
    for (int i = 0; i < size; i++) {
      final Bits bits = in.read(this.bitDepth);
      final int count = (int) in.read(32).toLong();
      this.map.put(bits, new AtomicInteger(count));
    }
  }

  @Override
  public void write(final BitOutputStream out) throws IOException {
    out.write(new Bits(this.getList().size(), 32));
    for (final Entry<Bits, AtomicInteger> e : this.map.entrySet()) {
      out.write(e.getKey());
      out.write(new Bits(e.getValue().get(), 32));
    }
  }

}