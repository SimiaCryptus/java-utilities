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

package com.simiacryptus.util.binary;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * The type Bit input stream.
 */
public class BitInputStream {

  private final InputStream inner;
  private Bits remainder = new Bits(0);

  /**
   * Instantiates a new Bit input stream.
   *
   * @param inner the inner
   */
  public BitInputStream(final InputStream inner) {
    this.inner = inner;
  }

  /**
   * To bit stream bit input stream.
   *
   * @param data the data
   * @return the bit input stream
   */
  public static BitInputStream toBitStream(final byte[] data) {
    return new BitInputStream(new ByteArrayInputStream(data));
  }

  /**
   * Close.
   *
   * @throws IOException the io exception
   */
  public void close() throws IOException {
    this.inner.close();
  }

  /**
   * Availible int.
   *
   * @return the int
   * @throws IOException the io exception
   */
  public int availible() throws IOException {
    return remainder.bitLength + 8 * inner.available();
  }

  /**
   * Expect.
   *
   * @param <T>    the type parameter
   * @param expect the expect
   * @throws IOException the io exception
   */
  public <T extends Enum<T>> void expect(final Enum<T> expect) throws IOException {
    final Bits checkBits = this.read(8);
    final long expectedLong = expect.ordinal();
    if (checkBits.toLong() != expectedLong) {
      final Bits expectedBits = new Bits(expectedLong, 8);
      throw new IOException(String.format("Check for %s failed: %s != %s", expect, checkBits, expectedBits));
    }
  }

  /**
   * Expect.
   *
   * @param bits the bits
   * @throws IOException the io exception
   */
  public void expect(final Bits bits) throws IOException {
    int size = Math.min(availible(), bits.bitLength);
    Bits read = read(size);
    if (!bits.range(0, size).equals(read)) {
      throw new RuntimeException(String.format("%s is not expected %s", read, bits));
    }
  }

  /**
   * Read bits.
   *
   * @param bits the bits
   * @return the bits
   * @throws IOException the io exception
   */
  public Bits read(final int bits) throws IOException {
    final int additionalBitsNeeded = bits - this.remainder.bitLength;
    final int additionalBytesNeeded = (int) Math.ceil(additionalBitsNeeded / 8.);
    if (additionalBytesNeeded > 0) this.readAhead(additionalBytesNeeded);
    final Bits readBits = this.remainder.range(0, bits);
    this.remainder = this.remainder.range(bits);
    return readBits;
  }

  /**
   * Peek bits.
   *
   * @param bits the bits
   * @return the bits
   * @throws IOException the io exception
   */
  public Bits peek(final int bits) throws IOException {
    final int additionalBitsNeeded = bits - this.remainder.bitLength;
    final int additionalBytesNeeded = (int) Math.ceil(additionalBitsNeeded / 8.);
    if (additionalBytesNeeded > 0) this.readAhead(additionalBytesNeeded);
    return this.remainder.range(0, Math.min(bits, this.remainder.bitLength));
  }

  /**
   * Read ahead bits.
   *
   * @return the bits
   * @throws IOException the io exception
   */
  public Bits readAhead() throws IOException {
    return this.readAhead(1);
  }

  /**
   * Read ahead bits.
   *
   * @param bytes the bytes
   * @return the bits
   * @throws IOException the io exception
   */
  public Bits readAhead(final int bytes) throws IOException {
    assert (0 < bytes);
    if (0 < bytes) {
      final byte[] buffer = new byte[bytes];
      int bytesRead = this.inner.read(buffer);
      if (bytesRead > 0) {
        this.remainder = this.remainder.concatenate(new Bits(Arrays.copyOf(buffer, bytesRead)));
      }
    }
    return this.remainder;
  }

  /**
   * Read bool boolean.
   *
   * @return the boolean
   * @throws IOException the io exception
   */
  public boolean readBool() throws IOException {
    return Bits.ONE.equals(this.read(1));
  }

  /**
   * Reads a single positive bounded integral value (up to 64-bit, including 0, excluding max)
   *
   * @param max Maximum value (exclusive)
   * @return A long within the range [0, max)
   * @throws IOException the io exception
   */
  public long readBoundedLong(final long max) throws IOException {
    final int bits = 0 >= max ? 0 : (int) (Math.floor(Math.log(max) / Math.log(2)) + 1);
    return 0 < bits ? this.read(bits).toLong() : 0;
  }

  /**
   * Read var long long.
   *
   * @return the long
   * @throws IOException the io exception
   */
  public long readVarLong() throws IOException {
    final int type = (int) this.read(2).toLong();
    return this.read(BitOutputStream.varLongDepths[type]).toLong();
  }

  /**
   * Peek long coord long.
   *
   * @param max the max
   * @return the long
   * @throws IOException the io exception
   */
  public long peekLongCoord(long max) throws IOException {
    if (1 >= max) return 0;
    int bits = 1 + (int) Math.ceil(Math.log(max) / Math.log(2));
    Bits peek = this.peek(bits);
    double divisor = 1 << peek.bitLength;
    long value = (int) (peek.toLong() * ((double) max) / divisor);
    assert (0 <= value);
    assert (max >= value);
    return value;
  }

  /**
   * Peek int coord int.
   *
   * @param max the max
   * @return the int
   * @throws IOException the io exception
   */
  public int peekIntCoord(int max) throws IOException {
    if (1 >= max) return 0;
    int bits = 1 + (int) Math.ceil(Math.log(max) / Math.log(2));
    Bits peek = this.peek(bits);
    double divisor = 1 << peek.bitLength;
    int value = (int) (peek.toLong() * ((double) max) / divisor);
    assert (0 <= value);
    assert (max >= value);
    return value;
  }

  /**
   * Read var short short.
   *
   * @return the short
   * @throws IOException the io exception
   */
  public short readVarShort() throws IOException {
    return readVarShort(7);
  }

  /**
   * Read var short short.
   *
   * @param optimal the optimal
   * @return the short
   * @throws IOException the io exception
   */
  public short readVarShort(int optimal) throws IOException {
    int[] varShortDepths = {optimal, 16};
    final int type = (int) this.read(1).toLong();
    return (short) this.read(varShortDepths[type]).toLong();
  }

  /**
   * Read char char.
   *
   * @return the char
   * @throws IOException the io exception
   */
  public char readChar() throws IOException {
    return (char) read(16).toLong();
  }
}
