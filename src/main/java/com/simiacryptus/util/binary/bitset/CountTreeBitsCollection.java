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

import com.simiacryptus.ref.wrappers.RefMaps.EntryTransformer;
import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.BitOutputStream;
import com.simiacryptus.util.binary.Bits;
import com.simiacryptus.util.binary.codes.Gaussian;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public @com.simiacryptus.ref.lang.RefAware
class CountTreeBitsCollection
    extends BitsCollection<com.simiacryptus.ref.wrappers.RefTreeMap<Bits, AtomicInteger>> {

  public static boolean SERIALIZATION_CHECKS = false;
  private boolean useBinomials = true;

  public CountTreeBitsCollection() {
    super(new com.simiacryptus.ref.wrappers.RefTreeMap<Bits, AtomicInteger>());
  }

  public CountTreeBitsCollection(final BitInputStream bitStream) throws IOException {
    this();
    this.read(bitStream);
  }

  public CountTreeBitsCollection(final BitInputStream bitStream, final int bitDepth) throws IOException {
    this(bitDepth);
    this.read(bitStream);
  }

  public CountTreeBitsCollection(final byte[] data) throws IOException {
    this(BitInputStream.toBitStream(data));
  }

  public CountTreeBitsCollection(final byte[] data, final int bitDepth) throws IOException {
    this(BitInputStream.toBitStream(data), bitDepth);
  }

  public CountTreeBitsCollection(final int bitDepth) {
    super(bitDepth, new com.simiacryptus.ref.wrappers.RefTreeMap<Bits, AtomicInteger>());
  }

  public CountTreeBitsCollection setUseBinomials(final boolean useBinomials) {
    this.useBinomials = useBinomials;
    return this;
  }

  public static <T> T isNull(final T value, final T defaultValue) {
    return null == value ? defaultValue : value;
  }

  public com.simiacryptus.ref.wrappers.RefTreeMap<Bits, Long> computeSums() {
    final com.simiacryptus.ref.wrappers.RefTreeMap<Bits, Long> sums = new com.simiacryptus.ref.wrappers.RefTreeMap<Bits, Long>();
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

  public void read(final BitInputStream in, final int size) throws IOException {
    this.getMap().clear();
    if (0 < size) {
      this.read(in, Bits.NULL, size);
    }
  }

  public long sum(final com.simiacryptus.ref.wrappers.RefCollection<Long> values) {
    long total = 0;
    for (final Long v : values) {
      total += v;
    }
    return total;
  }

  public byte[] toBytes() throws IOException {
    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final BitOutputStream out = new BitOutputStream(outBuffer);
    this.write(out);
    out.flush();
    return outBuffer.toByteArray();
  }

  public boolean useBinomials() {
    return this.useBinomials;
  }

  @Override
  public void write(final BitOutputStream out) throws IOException {
    final com.simiacryptus.ref.wrappers.RefTreeMap<Bits, Long> sums = this.computeSums();
    final long value = 0 == sums.size() ? 0 : sums.lastEntry().getValue();
    out.writeVarLong(value);
    if (0 < value) {
      this.write(out, Bits.NULL, sums);
    }
  }

  public void write(final BitOutputStream out, final int size) throws IOException {
    final com.simiacryptus.ref.wrappers.RefTreeMap<Bits, Long> sums = this.computeSums();
    final long value = 0 == sums.size() ? 0 : sums.lastEntry().getValue();
    if (value != size) {
      throw new RuntimeException();
    }
    if (0 < value) {
      this.write(out, Bits.NULL, sums);
    }
  }

  protected BranchCounts readBranchCounts(final BitInputStream in, final Bits code, final long size)
      throws IOException {
    final BranchCounts branchCounts = new BranchCounts(code, size);
    final CodeType currentCodeType = this.getType(code);
    long maximum = size;

    // Get terminals
    if (currentCodeType == CodeType.Unknown) {
      branchCounts.terminals = this.readTerminalCount(in, maximum);
    } else if (currentCodeType == CodeType.Terminal) {
      branchCounts.terminals = size;
    } else {
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

  protected long readTerminalCount(final BitInputStream in, final long size) throws IOException {
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.BeforeTerminal);
    }
    final long readBoundedLong = in.readBoundedLong(1 + size);
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.AfterTerminal);
    }
    return readBoundedLong;
  }

  protected long readZeroBranchSize(final BitInputStream in, final long max, final Bits code) throws IOException {
    if (0 == max) {
      return 0;
    }
    final long value;
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.BeforeCount);
    }
    if (this.useBinomials) {
      value = Gaussian.fromBinomial(0.5, max).decode(in, max);
    } else {
      value = in.readBoundedLong(1 + max);
    }
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.AfterCount);
    }
    return value;
  }

  protected void writeBranchCounts(final BranchCounts branch, final BitOutputStream out) throws IOException {
    final CodeType currentCodeType = this.getType(branch.path);
    long maximum = branch.size;
    assert maximum >= branch.terminals;
    if (currentCodeType == CodeType.Unknown) {
      this.writeTerminalCount(out, branch.terminals, maximum);
    } else if (currentCodeType == CodeType.Terminal) {
      assert branch.size == branch.terminals;
      assert 0 == branch.zeroCount;
      assert 0 == branch.oneCount;
    } else
      assert currentCodeType != CodeType.Prefix || 0 == branch.terminals;
    maximum -= branch.terminals;

    assert maximum >= branch.zeroCount;
    if (0 < maximum) {
      this.writeZeroBranchSize(out, branch.zeroCount, maximum, branch.path);
      maximum -= branch.zeroCount;
    } else {
      assert 0 == branch.zeroCount;
    }
    assert maximum == branch.oneCount;
  }

  protected void writeTerminalCount(final BitOutputStream out, final long value, final long max) throws IOException {
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

  protected void writeZeroBranchSize(final BitOutputStream out, final long value, final long max, final Bits bits)
      throws IOException {
    assert 0 <= value;
    assert max >= value;
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.BeforeCount);
    }
    if (this.useBinomials) {
      Gaussian.fromBinomial(0.5, max).encode(out, value, max);
    } else {
      out.writeBoundedLong(value, 1 + max);
    }
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.AfterCount);
    }
  }

  private void read(final BitInputStream in, final Bits code, final long size) throws IOException {
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

  private void write(final BitOutputStream out, final Bits currentCode,
                     final com.simiacryptus.ref.wrappers.RefNavigableMap<Bits, Long> sums) throws IOException {
    final Entry<Bits, Long> firstEntry = sums.firstEntry();
    final com.simiacryptus.ref.wrappers.RefNavigableMap<Bits, Long> remainder = sums.tailMap(currentCode, false);
    final Bits splitCode = currentCode.concatenate(Bits.ONE);
    final com.simiacryptus.ref.wrappers.RefNavigableMap<Bits, Long> zeroMap = remainder.headMap(splitCode, false);
    final com.simiacryptus.ref.wrappers.RefNavigableMap<Bits, Long> oneMap = remainder.tailMap(splitCode, true);

    final int firstEntryCount = this.map.get(firstEntry.getKey()).get();
    final long baseCount = firstEntry.getValue() - firstEntryCount;
    final long endCount = sums.lastEntry().getValue();
    final long size = endCount - baseCount;

    final long terminals = firstEntry.getKey().equals(currentCode) ? firstEntryCount : 0;
    final long zeroCount = 0 == zeroMap.size() ? 0 : zeroMap.lastEntry().getValue() - baseCount - terminals;
    final long oneCount = size - terminals - zeroCount;

    final EntryTransformer<Bits, Long, Long> transformer = new EntryTransformer<Bits, Long, Long>() {
      @Override
      public Long transformEntry(final Bits key, final Long value) {
        return (long) CountTreeBitsCollection.this.map.get(key).get();
      }
    };
    assert size == this.sum(com.simiacryptus.ref.wrappers.RefMaps.transformEntries(sums, transformer).values());
    assert zeroCount == this.sum(com.simiacryptus.ref.wrappers.RefMaps.transformEntries(zeroMap, transformer).values());
    assert oneCount == this.sum(com.simiacryptus.ref.wrappers.RefMaps.transformEntries(oneMap, transformer).values());

    final BranchCounts branchCounts = new BranchCounts(currentCode, size, terminals, zeroCount, oneCount);

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

  public enum SerializationChecks {
    StartTree, EndTree, BeforeCount, AfterCount, BeforeTerminal, AfterTerminal
  }

  public static @com.simiacryptus.ref.lang.RefAware
  class BranchCounts {
    public Bits path;
    public long size;
    public long terminals;
    public long zeroCount;
    public long oneCount;

    public BranchCounts(final Bits path, final long size) {
      this.path = path;
      this.size = size;
    }

    public BranchCounts(final Bits path, final long size, final long terminals, final long zeroCount,
                        final long oneCount) {
      this.path = path;
      this.size = size;
      this.terminals = terminals;
      this.zeroCount = zeroCount;
      this.oneCount = oneCount;
    }
  }

}