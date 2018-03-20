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

package com.simiacryptus.util.binary.bitset;

import com.simiacryptus.util.CountCollection;
import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.BitOutputStream;
import com.simiacryptus.util.binary.Bits;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type Bits collection.
 *
 * @param <C> the type parameter
 */
public abstract class BitsCollection<C extends Map<Bits, AtomicInteger>>
  extends CountCollection<Bits, C> {
  
  /**
   * The Bit depth.
   */
  public final Integer bitDepth;
  
  /**
   * Instantiates a new Bits collection.
   *
   * @param collection the collection
   */
  public BitsCollection(final C collection) {
    super(collection);
    this.bitDepth = null;
  }
  
  /**
   * Instantiates a new Bits collection.
   *
   * @param bitDepth   the bit depth
   * @param collection the collection
   */
  public BitsCollection(final int bitDepth, final C collection) {
    super(collection);
    this.bitDepth = bitDepth;
  }
  
  /**
   * Gets type.
   *
   * @param bits the bits
   * @return the type
   */
  public CodeType getType(final Bits bits) {
    if (null != this.bitDepth) {
      if (bits.bitLength == this.bitDepth) {
        return CodeType.Terminal;
      }
      if (bits.bitLength < this.bitDepth) {
        return CodeType.Prefix;
      }
      throw new IllegalArgumentException();
    }
    return CodeType.Unknown;
  }
  
  /**
   * Read.
   *
   * @param in the in
   * @throws IOException the io exception
   */
  public abstract void read(BitInputStream in) throws IOException;
  
  /**
   * Write.
   *
   * @param out the out
   * @throws IOException the io exception
   */
  public abstract void write(BitOutputStream out) throws IOException;
  
  /**
   * The enum Code type.
   */
  public enum CodeType {
    /**
     * Terminal code type.
     */
    Terminal, /**
     * Prefix code type.
     */
    Prefix, /**
     * Unknown code type.
     */
    Unknown
  }
  
}