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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public @com.simiacryptus.ref.lang.RefAware
class BitInputStream {

  private final InputStream inner;
  private Bits remainder = new Bits(0);

  public BitInputStream(final InputStream inner) {
    this.inner = inner;
  }

  public static BitInputStream toBitStream(final byte[] data) {
    return new BitInputStream(new ByteArrayInputStream(data));
  }

  public void close() throws IOException {
    this.inner.close();
  }

  public int availible() throws IOException {
    return remainder.bitLength + 8 * inner.available();
  }

  public <T extends Enum<T>> void expect(final Enum<T> expect) throws IOException {
    final Bits checkBits = this.read(8);
    final long expectedLong = expect.ordinal();
    if (checkBits.toLong() != expectedLong) {
      final Bits expectedBits = new Bits(expectedLong, 8);
      throw new IOException(String.format("Check for %s failed: %s != %s", expect, checkBits, expectedBits));
    }
  }

  public void expect(final Bits bits) throws IOException {
    int size = Math.min(availible(), bits.bitLength);
    Bits read = read(size);
    if (!bits.range(0, size).equals(read)) {
      throw new RuntimeException(String.format("%s is not expected %s", read, bits));
    }
  }

  public Bits read(final int bits) throws IOException {
    final int additionalBitsNeeded = bits - this.remainder.bitLength;
    final int additionalBytesNeeded = (int) Math.ceil(additionalBitsNeeded / 8.);
    if (additionalBytesNeeded > 0)
      this.readAhead(additionalBytesNeeded);
    final Bits readBits = this.remainder.range(0, bits);
    this.remainder = this.remainder.range(bits);
    return readBits;
  }

  public Bits peek(final int bits) throws IOException {
    final int additionalBitsNeeded = bits - this.remainder.bitLength;
    final int additionalBytesNeeded = (int) Math.ceil(additionalBitsNeeded / 8.);
    if (additionalBytesNeeded > 0)
      this.readAhead(additionalBytesNeeded);
    return this.remainder.range(0, Math.min(bits, this.remainder.bitLength));
  }

  public Bits readAhead() throws IOException {
    return this.readAhead(1);
  }

  public Bits readAhead(final int bytes) throws IOException {
    assert (0 < bytes);
    if (0 < bytes) {
      final byte[] buffer = new byte[bytes];
      int bytesRead = this.inner.read(buffer);
      if (bytesRead > 0) {
        this.remainder = this.remainder
            .concatenate(new Bits(com.simiacryptus.ref.wrappers.RefArrays.copyOf(buffer, bytesRead)));
      }
    }
    return this.remainder;
  }

  public boolean readBool() throws IOException {
    return Bits.ONE.equals(this.read(1));
  }

  public long readBoundedLong(final long max) throws IOException {
    final int bits = 0 >= max ? 0 : (int) (Math.floor(Math.log(max) / Math.log(2)) + 1);
    return 0 < bits ? this.read(bits).toLong() : 0;
  }

  public long readVarLong() throws IOException {
    final int type = (int) this.read(2).toLong();
    return this.read(BitOutputStream.varLongDepths[type]).toLong();
  }

  public long peekLongCoord(long max) throws IOException {
    if (1 >= max)
      return 0;
    int bits = 1 + (int) Math.ceil(Math.log(max) / Math.log(2));
    Bits peek = this.peek(bits);
    double divisor = 1 << peek.bitLength;
    long value = (int) (peek.toLong() * ((double) max) / divisor);
    assert (0 <= value);
    assert (max >= value);
    return value;
  }

  public int peekIntCoord(int max) throws IOException {
    if (1 >= max)
      return 0;
    int bits = 1 + (int) Math.ceil(Math.log(max) / Math.log(2));
    Bits peek = this.peek(bits);
    double divisor = 1 << peek.bitLength;
    int value = (int) (peek.toLong() * ((double) max) / divisor);
    assert (0 <= value);
    assert (max >= value);
    return value;
  }

  public short readVarShort() throws IOException {
    return readVarShort(7);
  }

  public short readVarShort(int optimal) throws IOException {
    int[] varShortDepths = {optimal, 16};
    final int type = (int) this.read(1).toLong();
    return (short) this.read(varShortDepths[type]).toLong();
  }

  public char readChar() throws IOException {
    return (char) read(16).toLong();
  }
}
