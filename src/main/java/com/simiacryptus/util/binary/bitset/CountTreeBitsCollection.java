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

import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.BitOutputStream;
import com.simiacryptus.util.binary.Bits;
import com.simiacryptus.util.binary.codes.Gaussian;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type Count tree bits collection.
 */
public class CountTreeBitsCollection extends
  BitsCollection<TreeMap<Bits, AtomicInteger>> {
  
  /**
   * The constant SERIALIZATION_CHECKS.
   */
  public static boolean SERIALIZATION_CHECKS = false;
  private boolean useBinomials = true;
  
  /**
   * Instantiates a new Count tree bits collection.
   */
  public CountTreeBitsCollection() {
    super(new TreeMap<Bits, AtomicInteger>());
  }
  
  /**
   * Instantiates a new Count tree bits collection.
   *
   * @param bitStream the bit stream
   * @throws IOException the io exception
   */
  public CountTreeBitsCollection(final BitInputStream bitStream)
    throws IOException {
    this();
    this.read(bitStream);
  }
  
  /**
   * Instantiates a new Count tree bits collection.
   *
   * @param bitStream the bit stream
   * @param bitDepth  the bit depth
   * @throws IOException the io exception
   */
  public CountTreeBitsCollection(final BitInputStream bitStream,
    final int bitDepth) throws IOException {
    this(bitDepth);
    this.read(bitStream);
  }
  
  /**
   * Instantiates a new Count tree bits collection.
   *
   * @param data the data
   * @throws IOException the io exception
   */
  public CountTreeBitsCollection(final byte[] data) throws IOException {
    this(BitInputStream.toBitStream(data));
  }
  
  /**
   * Instantiates a new Count tree bits collection.
   *
   * @param data     the data
   * @param bitDepth the bit depth
   * @throws IOException the io exception
   */
  public CountTreeBitsCollection(final byte[] data, final int bitDepth)
    throws IOException {
    this(BitInputStream.toBitStream(data), bitDepth);
  }
  
  /**
   * Instantiates a new Count tree bits collection.
   *
   * @param bitDepth the bit depth
   */
  public CountTreeBitsCollection(final int bitDepth) {
    super(bitDepth, new TreeMap<Bits, AtomicInteger>());
  }
  
  /**
   * Is null t.
   *
   * @param <T>          the type parameter
   * @param value        the value
   * @param defaultValue the default value
   * @return the t
   */
  public static <T> T isNull(final T value, final T defaultValue) {
    return null == value ? defaultValue : value;
  }
  
  /**
   * Compute sums tree apply.
   *
   * @return the tree apply
   */
  public TreeMap<Bits, Long> computeSums() {
    final TreeMap<Bits, Long> sums = new TreeMap<Bits, Long>();
    long total = 0;
    for (final Entry<Bits, AtomicInteger> e : this.map.entrySet()) {
      sums.put(e.getKey(), total += e.getValue().get());
    }
    return sums;
  }
  
  @Override
  public void read(final BitInputStream in) throws IOException {
    this.getMap().clear();
    final long size = in.readVarLong();
    if (0 < size) {
      this.read(in, Bits.NULL, size);
    }
  }
  
  private void read(final BitInputStream in, final Bits code, final long size)
    throws IOException {
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.StartTree);
    }
    final BranchCounts branchCounts = this.readBranchCounts(in, code, size);
    if (0 < branchCounts.terminals) {
      this.map.put(code, new AtomicInteger((int) branchCounts.terminals));
    }
    if (0 < branchCounts.zeroCount) {
      this.read(in, code.concatenate(Bits.ZERO), branchCounts.zeroCount);
    }
    // Get one-suffixed primary
    if (branchCounts.oneCount > 0) {
      this.read(in, code.concatenate(Bits.ONE), branchCounts.oneCount);
    }
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.EndTree);
    }
  }
  
  /**
   * Read.
   *
   * @param in   the in
   * @param size the size
   * @throws IOException the io exception
   */
  public void read(final BitInputStream in, final int size) throws IOException {
    this.getMap().clear();
    if (0 < size) {
      this.read(in, Bits.NULL, size);
    }
  }
  
  /**
   * Read branch counts branch counts.
   *
   * @param in   the in
   * @param code the code
   * @param size the size
   * @return the branch counts
   * @throws IOException the io exception
   */
  protected BranchCounts readBranchCounts(final BitInputStream in,
    final Bits code, final long size) throws IOException {
    final BranchCounts branchCounts = new BranchCounts(code, size);
    final CodeType currentCodeType = this.getType(code);
    long maximum = size;
    
    // Get terminals
    if (currentCodeType == CodeType.Unknown) {
      branchCounts.terminals = this.readTerminalCount(in, maximum);
    }
    else if (currentCodeType == CodeType.Terminal) {
      branchCounts.terminals = size;
    }
    else {
      branchCounts.terminals = 0;
    }
    maximum -= branchCounts.terminals;
    
    // Get zero-suffixed primary
    if (maximum > 0) {
      assert Thread.currentThread().getStackTrace().length < 100;
      branchCounts.zeroCount = this.readZeroBranchSize(in, maximum, code);
    }
    maximum -= branchCounts.zeroCount;
    branchCounts.oneCount = maximum;
    return branchCounts;
  }
  
  /**
   * Read terminal count long.
   *
   * @param in   the in
   * @param size the size
   * @return the long
   * @throws IOException the io exception
   */
  protected long readTerminalCount(final BitInputStream in, final long size)
    throws IOException {
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.BeforeTerminal);
    }
    final long readBoundedLong = in.readBoundedLong(1 + size);
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.AfterTerminal);
    }
    return readBoundedLong;
  }
  
  /**
   * Read zero branch size long.
   *
   * @param in   the in
   * @param max  the max
   * @param code the code
   * @return the long
   * @throws IOException the io exception
   */
  protected long readZeroBranchSize(final BitInputStream in, final long max,
    final Bits code) throws IOException {
    if (0 == max) {
      return 0;
    }
    final long value;
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.BeforeCount);
    }
    if (this.useBinomials) {
      value = Gaussian.fromBinomial(0.5, max).decode(in, max);
    }
    else {
      value = in.readBoundedLong(1 + max);
    }
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.AfterCount);
    }
    return value;
  }
  
  /**
   * Sets use binomials.
   *
   * @param useBinomials the use binomials
   * @return the use binomials
   */
  public CountTreeBitsCollection setUseBinomials(final boolean useBinomials) {
    this.useBinomials = useBinomials;
    return this;
  }
  
  /**
   * Sum long.
   *
   * @param values the values
   * @return the long
   */
  public long sum(final Collection<Long> values) {
    long total = 0;
    for (final Long v : values) {
      total += v;
    }
    return total;
  }
  
  /**
   * To bytes byte [ ].
   *
   * @return the byte [ ]
   * @throws IOException the io exception
   */
  public byte[] toBytes() throws IOException {
    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final BitOutputStream out = new BitOutputStream(outBuffer);
    this.write(out);
    out.flush();
    return outBuffer.toByteArray();
  }
  
  /**
   * Use binomials boolean.
   *
   * @return the boolean
   */
  public boolean useBinomials() {
    return this.useBinomials;
  }
  
  @Override
  public void write(final BitOutputStream out) throws IOException {
    final TreeMap<Bits, Long> sums = this.computeSums();
    final long value = 0 == sums.size() ? 0 : sums.lastEntry().getValue();
    out.writeVarLong(value);
    if (0 < value) {
      this.write(out, Bits.NULL, sums);
    }
  }
  
  private void write(final BitOutputStream out, final Bits currentCode,
    final NavigableMap<Bits, Long> sums) throws IOException {
    final Entry<Bits, Long> firstEntry = sums.firstEntry();
    final NavigableMap<Bits, Long> remainder = sums.tailMap(currentCode, false);
    final Bits splitCode = currentCode.concatenate(Bits.ONE);
    final NavigableMap<Bits, Long> zeroMap = remainder
      .headMap(splitCode, false);
    final NavigableMap<Bits, Long> oneMap = remainder.tailMap(splitCode, true);
    
    final int firstEntryCount = this.map.get(firstEntry.getKey()).get();
    final long baseCount = firstEntry.getValue() - firstEntryCount;
    final long endCount = sums.lastEntry().getValue();
    final long size = endCount - baseCount;
    
    final long terminals = firstEntry.getKey().equals(currentCode) ? firstEntryCount
      : 0;
    final long zeroCount = 0 == zeroMap.size() ? 0 : zeroMap.lastEntry()
      .getValue() - baseCount - terminals;
    final long oneCount = size - terminals - zeroCount;
    
    final EntryTransformer<Bits, Long, Long> transformer = new EntryTransformer<Bits, Long, Long>() {
      @Override
      public Long transformEntry(final Bits key, final Long value) {
        return (long) CountTreeBitsCollection.this.map.get(key).get();
      }
    };
    assert size == this.sum(Maps.transformEntries(sums, transformer).values());
    assert zeroCount == this.sum(Maps.transformEntries(zeroMap, transformer)
      .values());
    assert oneCount == this.sum(Maps.transformEntries(oneMap, transformer)
      .values());
    
    final BranchCounts branchCounts = new BranchCounts(
      currentCode,
      size,
      terminals,
      zeroCount,
      oneCount);
    
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.StartTree);
    }
    this.writeBranchCounts(branchCounts, out);
    if (0 < zeroCount) {
      this.write(out, currentCode.concatenate(Bits.ZERO), zeroMap);
    }
    if (0 < oneCount) {
      this.write(out, currentCode.concatenate(Bits.ONE), oneMap);
    }
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.EndTree);
    }
  }
  
  /**
   * Write.
   *
   * @param out  the out
   * @param size the size
   * @throws IOException the io exception
   */
  public void write(final BitOutputStream out, final int size)
    throws IOException {
    final TreeMap<Bits, Long> sums = this.computeSums();
    final long value = 0 == sums.size() ? 0 : sums.lastEntry().getValue();
    if (value != size) {
      throw new RuntimeException();
    }
    if (0 < value) {
      this.write(out, Bits.NULL, sums);
    }
  }
  
  /**
   * Write branch counts.
   *
   * @param branch the branch
   * @param out    the out
   * @throws IOException the io exception
   */
  protected void writeBranchCounts(final BranchCounts branch,
    final BitOutputStream out) throws IOException {
    final CodeType currentCodeType = this.getType(branch.path);
    long maximum = branch.size;
    assert maximum >= branch.terminals;
    if (currentCodeType == CodeType.Unknown) {
      this.writeTerminalCount(out, branch.terminals, maximum);
    }
    else if (currentCodeType == CodeType.Terminal) {
      assert branch.size == branch.terminals;
      assert 0 == branch.zeroCount;
      assert 0 == branch.oneCount;
    }
    else assert currentCodeType != CodeType.Prefix || 0 == branch.terminals;
    maximum -= branch.terminals;
    
    assert maximum >= branch.zeroCount;
    if (0 < maximum) {
      this.writeZeroBranchSize(out, branch.zeroCount, maximum, branch.path);
      maximum -= branch.zeroCount;
    }
    else {
      assert 0 == branch.zeroCount;
    }
    assert maximum == branch.oneCount;
  }
  
  /**
   * Write terminal count.
   *
   * @param out   the out
   * @param value the value
   * @param max   the max
   * @throws IOException the io exception
   */
  protected void writeTerminalCount(final BitOutputStream out,
    final long value, final long max) throws IOException {
    assert 0 <= value;
    assert max >= value;
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.BeforeTerminal);
    }
    out.writeBoundedLong(value, 1 + max);
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.AfterTerminal);
    }
  }
  
  /**
   * Write zero branch size.
   *
   * @param out   the out
   * @param value the value
   * @param max   the max
   * @param bits  the bits
   * @throws IOException the io exception
   */
  protected void writeZeroBranchSize(final BitOutputStream out,
    final long value, final long max, final Bits bits) throws IOException {
    assert 0 <= value;
    assert max >= value;
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.BeforeCount);
    }
    if (this.useBinomials) {
      Gaussian.fromBinomial(0.5, max).encode(out, value, max);
    }
    else {
      out.writeBoundedLong(value, 1 + max);
    }
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.AfterCount);
    }
  }
  
  /**
   * The enum Serialization checks.
   */
  public enum SerializationChecks {
    /**
     * Start tree serialization checks.
     */
    StartTree, /**
     * End tree serialization checks.
     */
    EndTree, /**
     * Before count serialization checks.
     */
    BeforeCount, /**
     * After count serialization checks.
     */
    AfterCount, /**
     * Before terminal serialization checks.
     */
    BeforeTerminal, /**
     * After terminal serialization checks.
     */
    AfterTerminal
  }
  
  /**
   * The type Branch counts.
   */
  public static class BranchCounts {
    /**
     * The Path.
     */
    public Bits path;
    /**
     * The Size.
     */
    public long size;
    /**
     * The Terminals.
     */
    public long terminals;
    /**
     * The Zero count.
     */
    public long zeroCount;
    /**
     * The One count.
     */
    public long oneCount;
  
    /**
     * Instantiates a new Branch counts.
     *
     * @param path the path
     * @param size the size
     */
    public BranchCounts(final Bits path, final long size) {
      this.path = path;
      this.size = size;
    }
  
    /**
     * Instantiates a new Branch counts.
     *
     * @param path      the path
     * @param size      the size
     * @param terminals the terminals
     * @param zeroCount the zero count
     * @param oneCount  the one count
     */
    public BranchCounts(final Bits path, final long size, final long terminals,
      final long zeroCount, final long oneCount) {
      this.path = path;
      this.size = size;
      this.terminals = terminals;
      this.zeroCount = zeroCount;
      this.oneCount = oneCount;
    }
  }
  
}