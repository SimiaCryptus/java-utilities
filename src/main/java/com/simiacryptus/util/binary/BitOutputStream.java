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

package com.simiacryptus.util.binary;

import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefConsumer;
import com.simiacryptus.util.Util;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream implements AutoCloseable {

  static final int varLongDepths[] = {6, 14, 30, 62};
  private final OutputStream inner;

  @Nonnull
  private Bits remainder = Bits.NULL;
  private int totalBitsWritten = 0;

  public BitOutputStream(final OutputStream inner) {
    this.inner = inner;
  }

  public int getTotalBitsWritten() {
    return totalBitsWritten;
  }

  @Nonnull
  public static Bits toBits(@Nonnull RefConsumer<BitOutputStream> fn) {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      BitOutputStream out = new BitOutputStream(buffer);
      fn.accept(out);
      out.flush();
      return new Bits(buffer.toByteArray(), out.getTotalBitsWritten());
    } catch (IOException e) {
      throw Util.throwException(e);
    }
  }

  public synchronized void flush() throws IOException {
    this.inner.write(this.remainder.getBytes());
    this.inner.flush();
    this.remainder = Bits.NULL;
  }

  public synchronized void write(@Nonnull final Bits bits) throws IOException {
    Bits newRemainder = this.remainder.concatenate(bits);
    final int newRemainingBits = newRemainder.bitLength % 8;
    int bitsToWrite = newRemainder.bitLength - newRemainingBits;
    if (bitsToWrite > 0) {
      assert 0 == bitsToWrite % 8;
      final Bits toWrite = newRemainder.range(0, bitsToWrite);
      this.inner.write(toWrite.getBytes());
      newRemainder = newRemainder.range(bitsToWrite);
    }
    this.remainder = newRemainder;
    this.totalBitsWritten += bits.bitLength;
  }

  public void write(final boolean b) throws IOException {
    this.write(new Bits(b ? 1l : 0l, 1));
  }

  public void write(final double value) throws IOException {
    this.write(new Bits(Double.doubleToLongBits(value), 64));
  }

  public <T extends Enum<T>> void write(@Nonnull final Enum<T> value) throws IOException {
    final long ordinal = value.ordinal();
    this.write(new Bits(ordinal, 8));
  }

  public void write(final short value) throws IOException {
    this.write(new Bits(value, 16));
  }

  public void write(final char value) throws IOException {
    this.write(new Bits(value, 16));
  }

  public void write(final int value) throws IOException {
    this.write(new Bits(value, 32));
  }

  @Nonnull
  public Bits writeBoundedLong(final long value, final long max) throws IOException {
    final int bits = 0 >= max ? 0 : (int) (Math.floor(Math.log(max) / Math.log(2)) + 1);
    if (0 < bits) {
      Bits toWrite = new Bits(value, bits);
      this.write(toWrite);
      return toWrite;
    } else {
      return Bits.NULL;
    }
  }

  public void writeVarLong(final long value) throws IOException {
    final int bitLength = new Bits(value).bitLength;
    int type = RefArrays.binarySearch(varLongDepths, bitLength);
    if (type < 0) {
      type = -type - 1;
    }
    this.write(new Bits(type, 2));
    this.write(new Bits(value, varLongDepths[type]));
  }

  public void writeVarShort(final short value) throws IOException {
    writeVarShort(value, 7);
  }

  public void writeVarShort(final short value, int optimal) throws IOException {
    if (value < 0)
      throw new IllegalArgumentException();
    int[] varShortDepths = {optimal, 16};
    final int bitLength = new Bits(value).bitLength;
    int type = RefArrays.binarySearch(varShortDepths, bitLength);
    if (type < 0) {
      type = -type - 1;
    }
    this.write(new Bits(type, 1));
    this.write(new Bits(value, varShortDepths[type]));
  }

  @Override
  public void close() throws IOException {
    flush();
    inner.close();
  }

}
