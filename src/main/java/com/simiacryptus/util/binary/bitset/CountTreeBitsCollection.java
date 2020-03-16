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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.ref.wrappers.RefMaps.EntryTransformer;
import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.BitOutputStream;
import com.simiacryptus.util.binary.Bits;
import com.simiacryptus.util.binary.codes.Gaussian;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CountTreeBitsCollection extends BitsCollection<RefTreeMap<Bits, AtomicInteger>> {

  public static boolean SERIALIZATION_CHECKS = false;
  private boolean useBinomials = true;

  public CountTreeBitsCollection() {
    super(new RefTreeMap<Bits, AtomicInteger>());
  }

  public CountTreeBitsCollection(@Nonnull final BitInputStream bitStream) throws IOException {
    this();
    this.read(bitStream);
  }

  public CountTreeBitsCollection(@Nonnull final BitInputStream bitStream, final int bitDepth) throws IOException {
    this(bitDepth);
    this.read(bitStream);
  }

  public CountTreeBitsCollection(@Nonnull final byte[] data) throws IOException {
    this(BitInputStream.toBitStream(data));
  }

  public CountTreeBitsCollection(@Nonnull final byte[] data, final int bitDepth) throws IOException {
    this(BitInputStream.toBitStream(data), bitDepth);
  }

  public CountTreeBitsCollection(final int bitDepth) {
    super(bitDepth, new RefTreeMap<Bits, AtomicInteger>());
  }

  public void setUseBinomials(final boolean useBinomials) {
    this.useBinomials = useBinomials;
  }

  public static <T> T isNull(@Nullable final T value, final T defaultValue) {
    return null == value ? defaultValue : value;
  }

  @Nonnull
  public RefTreeMap<Bits, Long> computeSums() {
    final RefTreeMap<Bits, Long> sums = new RefTreeMap<Bits, Long>();
    AtomicLong total = new AtomicLong();
    assert this.map != null;
    RefHashSet<Entry<Bits, AtomicInteger>> entries = this.map.entrySet();
    entries.forEach(e -> {
      RefUtil.freeRef(sums.put(e.getKey(), total.addAndGet(e.getValue().get())));
      RefUtil.freeRef(e);
    });
    entries.freeRef();
    return sums;
  }

  @Override
  public void read(@Nonnull final BitInputStream in) throws IOException {
    RefMap<Bits, Integer> temp_13_0001 = this.getMap();
    temp_13_0001.clear();
    temp_13_0001.freeRef();
    final long size = in.readVarLong();
    if (0 < size) {
      this.read(in, Bits.NULL, size);
    }
  }

  public void read(@Nonnull final BitInputStream in, final int size) throws IOException {
    RefMap<Bits, Integer> temp_13_0002 = this.getMap();
    temp_13_0002.clear();
    temp_13_0002.freeRef();
    if (0 < size) {
      this.read(in, Bits.NULL, size);
    }
  }

  public long sum(@Nonnull final @RefAware RefCollection<Long> values) {
    long total = values.stream().mapToLong(v -> v).sum();
    values.freeRef();
    return total;
  }

  @Nonnull
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
  public void write(@Nonnull final BitOutputStream out) throws IOException {
    final RefTreeMap<Bits, Long> sums = this.computeSums();
    Map.Entry<Bits, Long> temp_13_0003 = sums.lastEntry();
    final long value = 0 == sums.size() ? 0 : temp_13_0003.getValue();
    RefUtil.freeRef(temp_13_0003);
    out.writeVarLong(value);
    if (0 < value) {
      this.write(out, Bits.NULL, RefUtil.addRef(sums));
    }
    sums.freeRef();
  }

  public void write(@Nonnull final BitOutputStream out, final int size) throws IOException {
    final RefTreeMap<Bits, Long> sums = this.computeSums();
    Map.Entry<Bits, Long> temp_13_0004 = sums.lastEntry();
    final long value = 0 == sums.size() ? 0 : temp_13_0004.getValue();
    RefUtil.freeRef(temp_13_0004);
    if (value != size) {
      sums.freeRef();
      throw new RuntimeException();
    }
    if (0 < value) {
      this.write(out, Bits.NULL, RefUtil.addRef(sums));
    }
    sums.freeRef();
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  CountTreeBitsCollection addRef() {
    return (CountTreeBitsCollection) super.addRef();
  }

  @Nonnull
  protected BranchCounts readBranchCounts(@Nonnull final BitInputStream in, @Nonnull final Bits code, final long size)
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
      branchCounts.zeroCount = this.readZeroBranchSize(in, maximum);
    }
    maximum -= branchCounts.zeroCount;
    branchCounts.oneCount = maximum;
    return branchCounts;
  }

  protected long readTerminalCount(@Nonnull final BitInputStream in, final long size) throws IOException {
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.BeforeTerminal);
    }
    final long readBoundedLong = in.readBoundedLong(1 + size);
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.AfterTerminal);
    }
    return readBoundedLong;
  }

  protected long readZeroBranchSize(@Nonnull final BitInputStream in, final long max) throws IOException {
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

  protected void writeBranchCounts(@Nonnull final BranchCounts branch, @Nonnull final BitOutputStream out) throws IOException {
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
      this.writeZeroBranchSize(out, branch.zeroCount, maximum);
      maximum -= branch.zeroCount;
    } else {
      assert 0 == branch.zeroCount;
    }
    assert maximum == branch.oneCount;
  }

  protected void writeTerminalCount(@Nonnull final BitOutputStream out, final long value, final long max) throws IOException {
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

  protected void writeZeroBranchSize(@Nonnull final BitOutputStream out, final long value, final long max)
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

  private void read(@Nonnull final BitInputStream in, @Nonnull final Bits code, final long size) throws IOException {
    if (SERIALIZATION_CHECKS) {
      in.expect(SerializationChecks.StartTree);
    }
    final BranchCounts branchCounts = this.readBranchCounts(in, code, size);
    if (0 < branchCounts.terminals) {
      assert this.map != null;
      RefUtil.freeRef(this.map.put(code, new AtomicInteger((int) branchCounts.terminals)));
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

  private void write(@Nonnull final BitOutputStream out, @Nonnull final Bits currentCode,
                     @Nonnull final @RefAware RefNavigableMap<Bits, Long> sums) throws IOException {
    final Entry<Bits, Long> firstEntry = sums.firstEntry();
    final RefNavigableMap<Bits, Long> remainder = sums.tailMap(currentCode, false);
    final Bits splitCode = currentCode.concatenate(Bits.ONE);
    final RefNavigableMap<Bits, Long> zeroMap = remainder.headMap(splitCode, false);
    final RefNavigableMap<Bits, Long> oneMap = remainder.tailMap(splitCode, true);

    remainder.freeRef();
    assert this.map != null;
    AtomicInteger atomicInteger = this.map.get(firstEntry.getKey());
    final int firstEntryCount = atomicInteger.get();
    final long baseCount = firstEntry.getValue() - firstEntryCount;
    Map.Entry<Bits, Long> temp_13_0005 = sums.lastEntry();
    final long endCount = temp_13_0005.getValue();
    RefUtil.freeRef(temp_13_0005);
    final long size = endCount - baseCount;

    final long terminals = firstEntry.getKey().equals(currentCode) ? firstEntryCount : 0;
    RefUtil.freeRef(firstEntry);
    Map.Entry<Bits, Long> temp_13_0006 = zeroMap.lastEntry();
    final long zeroCount = 0 == zeroMap.size() ? 0 : temp_13_0006.getValue() - baseCount - terminals;
    if (null != temp_13_0006)
      RefUtil.freeRef(temp_13_0006);
    final long oneCount = size - terminals - zeroCount;

    final EntryTransformer<Bits, Long, Long> transformer = new EntryTransformer<Bits, Long, Long>() {
      @Nonnull
      @Override
      public Long transformEntry(final @RefAware Bits key, final Long value) {
        AtomicInteger atomicInteger = CountTreeBitsCollection.this.map.get(key);
        return (long) atomicInteger.get();
      }
    };
    RefMap<Bits, Long> temp_13_0007 = RefMaps.transformEntries(RefUtil.addRef(sums), transformer);
    assert size == this.sum(temp_13_0007.values());
    temp_13_0007.freeRef();
    sums.freeRef();
    RefMap<Bits, Long> temp_13_0008 = RefMaps.transformEntries(RefUtil.addRef(zeroMap), transformer);
    assert zeroCount == this.sum(temp_13_0008.values());
    temp_13_0008.freeRef();
    RefMap<Bits, Long> temp_13_0009 = RefMaps.transformEntries(RefUtil.addRef(oneMap), transformer);
    assert oneCount == this.sum(temp_13_0009.values());

    temp_13_0009.freeRef();
    final BranchCounts branchCounts = new BranchCounts(currentCode, size, terminals, zeroCount, oneCount);

    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.StartTree);
    }
    this.writeBranchCounts(branchCounts, out);
    if (0 < zeroCount) {
      this.write(out, currentCode.concatenate(Bits.ZERO), RefUtil.addRef(zeroMap));
    }
    zeroMap.freeRef();
    if (0 < oneCount) {
      this.write(out, currentCode.concatenate(Bits.ONE), RefUtil.addRef(oneMap));
    }
    oneMap.freeRef();
    if (SERIALIZATION_CHECKS) {
      out.write(SerializationChecks.EndTree);
    }
  }

  public enum SerializationChecks {
    StartTree, EndTree, BeforeCount, AfterCount, BeforeTerminal, AfterTerminal
  }

  public static class BranchCounts {
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