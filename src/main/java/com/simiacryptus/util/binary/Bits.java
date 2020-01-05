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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefArrays;

import java.util.Base64;
import java.util.Random;

public @RefAware
class Bits implements Comparable<Bits> {
  public static Bits ONE = new Bits(1, 1);
  public static Bits ZERO = new Bits(0, 1);
  public static Bits NULL = new Bits(0, 0);
  public final int bitLength;
  private final byte[] bytes;

  public Bits(final byte... data) {
    this(data, data.length * 8);
  }

  public Bits(final byte[] data, final int length) {
    super();
    if (0 > length) {
      throw new IllegalArgumentException();
    }
    if (data.length * 8 < length) {
      throw new IllegalArgumentException();
    }
    if (length < (data.length - 1) * 8) {
      throw new IllegalArgumentException();
    }
    this.bitLength = length;
    this.bytes = RefArrays.copyOf(data, data.length); // Bits.shiftLeft(data, (data.length * 8 - length) % 8);
  }

  public Bits(final long data) {
    this(data, highestOneBit(data));
  }

  public Bits(final long value, final int length) {
    super();

    if (0 > length) {
      throw new IllegalArgumentException();
    }
    final byte highestOneBit = highestOneBit(value);
    if (highestOneBit > length) {
      throw new IllegalArgumentException();
    }
    this.bitLength = length;
    this.bytes = new byte[(int) Math.ceil(length / 8.)];

    byte[] data = toBytes(value);
    data = trim(data);
    final int leftShift = (data.length * 8 - highestOneBit) % 8;
    final int rightShift = length - highestOneBit;

    if (leftShift > rightShift) {
      Bits.shiftLeft(data, leftShift - rightShift, this.bytes);
    } else {
      Bits.shiftRight(data, rightShift - leftShift, this.bytes);
    }
    assert value == this.toLong();
  }

  public Bits(final Random random, final int length) {
    this.bitLength = length;
    this.bytes = new byte[(int) Math.ceil(length / 8.)];
    random.nextBytes(this.bytes);
    final int excessBits = this.bytes.length * 8 - this.bitLength;
    this.bytes[this.bytes.length - 1] &= 0xFF << excessBits;
  }

  public byte[] getBytes() {
    return RefArrays.copyOf(this.bytes, this.bytes.length);
  }

  public static Bits divide(long numerator, long denominator, long maxBits) {
    if (maxBits <= 0)
      return NULL;
    if (numerator == 0)
      return ZERO;
    if (numerator == denominator)
      return ONE;
    if (numerator < denominator) {
      return ZERO.concatenate(divide(numerator * 2, denominator, maxBits - 1));
    } else {
      return ONE.concatenate(divide(2 * (numerator - denominator), denominator, maxBits - 1));
    }
  }

  public static int dataCompare(final Bits left, final Bits right) {
    for (int i = 0; i < left.bytes.length; i++) {
      if (right.bytes.length <= i) {
        return 1;
      }
      final int a = left.bytes[i] & 0xFF;
      final int b = right.bytes[i] & 0xFF;
      if (a < b) {
        return -1;
      }
      if (a > b) {
        return 1;
      }
    }
    if (left.bitLength < right.bitLength) {
      return -1;
    }
    if (left.bitLength > right.bitLength) {
      return 1;
    }
    return 0;
  }

  public static byte highestOneBit(final long v) {
    final long h = Long.highestOneBit(v);
    if (0 == v) {
      return 0;
    }
    for (byte i = 0; i < 64; i++) {
      if (h == 1l << i) {
        return (byte) (i + 1);
      }
    }
    throw new RuntimeException();
  }

  public static byte[] padLeftBytes(final byte[] src, final int bytes) {
    final byte[] dst = new byte[bytes];
    for (int i = 1; i <= src.length; i++) {
      dst[dst.length - i] = src[src.length - i];
    }
    return dst;
  }

  public static byte[] shiftLeft(final byte[] src, final int bits) {
    final byte[] dst = new byte[src.length];
    shiftLeft(src, bits, dst);
    return dst;
  }

  public static void shiftLeft(final byte[] src, final int bits, final byte[] dst) {
    final int bitPart = bits % 8;
    final int bytePart = bits / 8;
    for (int i = 0; i < dst.length; i++) {
      final int a = i + bytePart;
      if (a >= 0 && src.length > a) {
        dst[i] |= (byte) ((src[a] & 0xFF) << bitPart & 0xFF);
      }
      final int b = i + bytePart + 1;
      if (b >= 0 && src.length > b) {
        dst[i] |= (byte) ((src[b] & 0xFF) >> 8 - bitPart & 0xFF);
      }
    }
  }

  public static byte[] shiftRight(final byte[] src, final int bits) {
    final byte[] dst = new byte[src.length];
    shiftRight(src, bits, dst);
    return dst;
  }

  public static byte[] toBytes(final long data) {
    return new byte[]{(byte) (data >> 56 & 0xFF), (byte) (data >> 48 & 0xFF), (byte) (data >> 40 & 0xFF),
        (byte) (data >> 32 & 0xFF), (byte) (data >> 24 & 0xFF), (byte) (data >> 16 & 0xFF), (byte) (data >> 8 & 0xFF),
        (byte) (data & 0xFF)};
  }

  public static byte[] trim(final byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      if (bytes[i] != 0) {
        return RefArrays.copyOfRange(bytes, i, bytes.length);
      }
    }
    return new byte[]{};
  }

  private static void shiftRight(final byte[] src, final int bits, final byte[] dst) {
    final int bitPart = bits % 8;
    final int bytePart = bits / 8;
    for (int i = 0; i < dst.length; i++) {
      final int a = i - bytePart;
      if (a >= 0 && src.length > a) {
        dst[i] |= (byte) ((src[a] & 0xFF) >> bitPart & 0xFF);
      }
      final int b = i - bytePart - 1;
      if (b >= 0 && src.length > b) {
        dst[i] |= (byte) ((src[b] & 0xFF) << 8 - bitPart & 0xFF);
      }
    }
  }

  public Bits bitwiseAnd(final Bits right) {
    final int lengthDifference = this.bitLength - right.bitLength;
    if (lengthDifference < 0) {
      return this.concatenate(new Bits(0l, -lengthDifference)).bitwiseAnd(right);
    }
    if (lengthDifference > 0) {
      return this.bitwiseAnd(right.concatenate(new Bits(0l, lengthDifference)));
    }
    final Bits returnValue = new Bits(new byte[this.bytes.length], this.bitLength);
    for (int i = 0; i < this.bytes.length; i++) {
      returnValue.bytes[i] = this.bytes[i];
    }
    for (int i = 0; i < right.bytes.length; i++) {
      returnValue.bytes[i] &= right.bytes[i];
    }
    return returnValue;
  }

  public Bits bitwiseOr(final Bits right) {
    final int lengthDifference = this.bitLength - right.bitLength;
    if (lengthDifference < 0) {
      return this.concatenate(new Bits(0l, -lengthDifference)).bitwiseOr(right);
    }
    if (lengthDifference > 0) {
      return this.bitwiseOr(right.concatenate(new Bits(0l, lengthDifference)));
    }
    final Bits returnValue = new Bits(new byte[this.bytes.length], this.bitLength);
    for (int i = 0; i < this.bytes.length; i++) {
      returnValue.bytes[i] = this.bytes[i];
    }
    for (int i = 0; i < right.bytes.length; i++) {
      returnValue.bytes[i] |= right.bytes[i];
    }
    return returnValue;
  }

  public Bits bitwiseXor(final Bits right) {
    final int lengthDifference = this.bitLength - right.bitLength;
    if (lengthDifference < 0) {
      return this.concatenate(new Bits(0l, -lengthDifference)).bitwiseXor(right);
    }
    if (lengthDifference > 0) {
      return this.bitwiseXor(right.concatenate(new Bits(0l, lengthDifference)));
    }
    final Bits returnValue = new Bits(new byte[this.bytes.length], this.bitLength);
    for (int i = 0; i < this.bytes.length; i++) {
      returnValue.bytes[i] = this.bytes[i];
    }
    for (int i = 0; i < right.bytes.length; i++) {
      returnValue.bytes[i] ^= right.bytes[i];
    }
    return returnValue;
  }

  @Override
  public int compareTo(final Bits arg0) {
    return dataCompare(this, arg0);
  }

  public Bits concatenate(final Bits right) {
    final int newBitLength = this.bitLength + right.bitLength;
    final int newByteLength = (int) Math.ceil(newBitLength / 8.);
    final Bits result = new Bits(new byte[newByteLength], newBitLength);
    shiftLeft(this.bytes, 0, result.bytes);
    shiftRight(right.bytes, this.bitLength, result.bytes);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final Bits other = (Bits) obj;
    if (!RefArrays.equals(this.bytes, other.bytes)) {
      return false;
    }
    return this.bitLength == other.bitLength;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + RefArrays.hashCode(this.bytes);
    result = prime * result + this.bitLength;
    return result;
  }

  public Bits leftShift(final int bits) {
    return this.concatenate(new Bits(0l, bits));
  }

  public Bits next() {
    if (!this.toBitString().contains("0")) {
      return null;
    }
    return new Bits(this.toLong() + 1, this.bitLength);
  }

  public Bits range(final int start) {
    return this.range(start, this.bitLength - start);
  }

  public Bits range(final int start, final int length) {
    if (0 == length) {
      return Bits.NULL;
    }
    if (start < 0) {
      throw new IllegalArgumentException();
    }
    if (start + length > this.bitLength) {
      throw new IllegalArgumentException();
    }
    final Bits returnValue = new Bits(new byte[(int) Math.ceil(length / 8.)], length);
    shiftLeft(this.bytes, start, returnValue.bytes);
    int bitsInLastByte = length % 8;
    if (0 == bitsInLastByte) {
      bitsInLastByte = 8;
    }
    returnValue.bytes[returnValue.bytes.length - 1] &= 0xFF << 8 - bitsInLastByte;
    return returnValue;
  }

  public boolean startsWith(final Bits key) {
    if (key.bitLength > this.bitLength) {
      return false;
    }
    final Bits prefix = key.bitLength < this.bitLength ? this.range(0, key.bitLength) : this;
    return prefix.compareTo(key) == 0;
  }

  public String toBitString() {
    StringBuffer sb = new StringBuffer();
    final int shift = this.bytes.length * 8 - this.bitLength;
    final byte[] shiftRight = shiftRight(this.bytes, shift);
    for (final byte b : shiftRight) {
      String asString = Integer.toBinaryString(b & 0xFF);
      while (asString.length() < 8) {
        asString = "0" + asString;
      }
      sb.append(asString);
    }
    if (sb.length() >= this.bitLength) {
      return sb.substring(sb.length() - this.bitLength, sb.length());
    } else {
      final CharSequence n = sb.toString();
      sb = new StringBuffer();
      while (sb.length() + n.length() < this.bitLength) {
        sb.append("0");
      }
      return sb.toString() + n;
    }
  }

  public CharSequence toHexString() {
    final StringBuffer sb = new StringBuffer();
    for (final byte b : this.bytes) {
      sb.append(Integer.toHexString(b & 0xFF));
    }
    return sb.substring(0, Math.min(this.bitLength / 4, sb.length()));
  }

  public CharSequence toBase64String() {
    return Base64.getEncoder().encodeToString(this.bytes);
  }

  public long toLong() {
    long value = 0;
    for (final byte b : shiftRight(this.bytes, this.bytes.length * 8 - this.bitLength)) {
      final long shifted = value << 8;
      value = shifted;
      final int asInt = b & 0xFF;
      value += asInt;
    }
    return value;
  }

  @Override
  public String toString() {
    return this.toBitString();
  }

  public Bits padRight(long targetLength) {
    if (bitLength >= targetLength)
      return this;
    return this.concatenate(ZERO).padRight(targetLength);
  }

  public Bits padLeft(int targetLength) {
    if (bitLength >= targetLength)
      return this;
    return ZERO.concatenate(this).padLeft(targetLength);
  }

}
