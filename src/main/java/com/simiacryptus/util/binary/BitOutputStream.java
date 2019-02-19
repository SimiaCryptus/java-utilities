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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * The type Bit output stream.
 */
public class BitOutputStream implements AutoCloseable {

  /**
   * The Var long depths.
   */
  static final int varLongDepths[] = {6, 14, 30, 62};
  private final OutputStream inner;

  private Bits remainder = Bits.NULL;
  private int totalBitsWritten = 0;

  /**
   * Instantiates a new Bit output stream.
   *
   * @param inner the inner
   */
  public BitOutputStream(final OutputStream inner) {
    this.inner = inner;
  }

  /**
   * To bits bits.
   *
   * @param fn the fn
   * @return the bits
   */
  public static Bits toBits(Consumer<BitOutputStream> fn) {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      BitOutputStream out = new BitOutputStream(buffer);
      fn.accept(out);
      out.flush();
      return new Bits(buffer.toByteArray(), out.getTotalBitsWritten());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets total bits written.
   *
   * @return the total bits written
   */
  public int getTotalBitsWritten() {
    return totalBitsWritten;
  }

  /**
   * Flush.
   *
   * @throws IOException the io exception
   */
  public synchronized void flush() throws IOException {
    this.inner.write(this.remainder.getBytes());
    this.inner.flush();
    this.remainder = Bits.NULL;
  }

  /**
   * Write.
   *
   * @param bits the bits
   * @throws IOException the io exception
   */
  public synchronized void write(final Bits bits) throws IOException {
    Bits newRemainder = this.remainder.concatenate(bits);
    final int newRemainingBits = newRemainder.bitLength % 8;
    int bitsToWrite = newRemainder.bitLength - newRemainingBits;
    if (bitsToWrite > 0) {
      assert (0 == bitsToWrite % 8);
      final Bits toWrite = newRemainder.range(0, bitsToWrite);
      this.inner.write(toWrite.getBytes());
      newRemainder = newRemainder.range(bitsToWrite);
    }
    this.remainder = newRemainder;
    this.totalBitsWritten += bits.bitLength;
  }

  /**
   * Write.
   *
   * @param b the b
   * @throws IOException the io exception
   */
  public void write(final boolean b) throws IOException {
    this.write(new Bits(b ? 1l : 0l, 1));
  }

  /**
   * Write.
   *
   * @param value the value
   * @throws IOException the io exception
   */
  public void write(final double value) throws IOException {
    this.write(new Bits(Double.doubleToLongBits(value), 64));
  }

  /**
   * Write.
   *
   * @param <T>   the type parameter
   * @param value the value
   * @throws IOException the io exception
   */
  public <T extends Enum<T>> void write(final Enum<T> value) throws IOException {
    final long ordinal = value.ordinal();
    this.write(new Bits(ordinal, 8));
  }

  /**
   * Write.
   *
   * @param value the value
   * @throws IOException the io exception
   */
  public void write(final short value) throws IOException {
    this.write(new Bits(value, 16));
  }

  /**
   * Write.
   *
   * @param value the value
   * @throws IOException the io exception
   */
  public void write(final char value) throws IOException {
    this.write(new Bits(value, 16));
  }

  /**
   * Write.
   *
   * @param value the value
   * @throws IOException the io exception
   */
  public void write(final int value) throws IOException {
    this.write(new Bits(value, 32));
  }

  /**
   * Write bounded long bits.
   *
   * @param value the value
   * @param max   the max
   * @return the bits
   * @throws IOException the io exception
   */
  public Bits writeBoundedLong(final long value, final long max)
      throws IOException {
    final int bits = 0 >= max ? 0 : (int) (Math
        .floor(Math.log(max) / Math.log(2)) + 1);
    if (0 < bits) {
      Bits toWrite = new Bits(value, bits);
      this.write(toWrite);
      return toWrite;
    } else {
      return Bits.NULL;
    }
  }

  /**
   * Write var long.
   *
   * @param value the value
   * @throws IOException the io exception
   */
  public void writeVarLong(final long value) throws IOException {
    final int bitLength = new Bits(value).bitLength;
    int type = Arrays.binarySearch(varLongDepths, bitLength);
    if (type < 0) {
      type = -type - 1;
    }
    this.write(new Bits(type, 2));
    this.write(new Bits(value, varLongDepths[type]));
  }

  /**
   * Write var short.
   *
   * @param value the value
   * @throws IOException the io exception
   */
  public void writeVarShort(final short value) throws IOException {
    writeVarShort(value, 7);
  }

  /**
   * Write var short.
   *
   * @param value   the value
   * @param optimal the optimal
   * @throws IOException the io exception
   */
  public void writeVarShort(final short value, int optimal) throws IOException {
    if (value < 0) throw new IllegalArgumentException();
    int[] varShortDepths = {optimal, 16};
    final int bitLength = new Bits(value).bitLength;
    int type = Arrays.binarySearch(varShortDepths, bitLength);
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
