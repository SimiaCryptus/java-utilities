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

package com.simiacryptus.util.binary.codes;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefCollection;
import com.simiacryptus.ref.wrappers.RefTreeSet;
import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.Bits;
import com.simiacryptus.util.binary.bitset.CountTreeBitsCollection;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public @RefAware
class HammingCode<T extends Comparable<T>> {
  protected final TreeMap<Bits, T> forwardIndex = new TreeMap<Bits, T>();
  protected final HashMap<T, Bits> reverseIndex = new HashMap<T, Bits>();
  protected final HashMap<T, Integer> weights = new HashMap<T, Integer>();
  protected final long totalWeight;

  public HammingCode(final RefCollection<HammingSymbol<T>> symbols) {
    if (0 < symbols.size()) {
      final RefTreeSet<SubCode<T>> assemblySet = new RefTreeSet<SubCode<T>>();
      for (final HammingSymbol<T> s : symbols) {
        this.weights.put(s.key, s.count);
        assemblySet.add(new SubCode<T>(s.count, s.key));
      }
      while (assemblySet.size() > 1) {
        final SubCode<T> zero = assemblySet.pollFirst();
        final SubCode<T> one = assemblySet.pollFirst();
        assemblySet.add(new SubCode<T>(zero, one));
      }
      final SubCode<T> root = assemblySet.first();
      if (null != assemblySet)
        assemblySet.freeRef();
      this.forwardIndex.putAll(root.codes);
      this.reverseIndex.putAll(root.index);
      totalWeight = root.count;
    } else {
      totalWeight = 0;
    }
    assert this.verifyIndexes();
    assert this.forwardIndex.size() == symbols.size();
    if (null != symbols)
      symbols.freeRef();
  }

  public CountTreeBitsCollection getSetEncoder() {
    return new HammingCodeCollection(HammingCode.this);
  }

  public Map<T, Integer> getWeights() {
    return Collections.unmodifiableMap(this.weights);
  }

  public static boolean isPrefixFreeCode(final Set<Bits> keySet) {
    final RefTreeSet<Bits> check = new RefTreeSet<Bits>();
    for (final Bits code : keySet) {
      final Bits ceiling = check.ceiling(code);
      if (null != ceiling && (ceiling.startsWith(code) || code.startsWith(ceiling))) {
        if (null != check)
          check.freeRef();
        return false;
      }
      final Bits floor = check.floor(code);
      if (null != floor && (floor.startsWith(code) || code.startsWith(floor))) {
        if (null != check)
          check.freeRef();
        return false;
      }
      check.add(code);
    }
    if (null != check)
      check.freeRef();
    return true;
  }

  public int codeSize() {
    return this.forwardIndex.size();
  }

  public T decode(final BitInputStream in) throws IOException {
    Bits remainder = in.readAhead(0);
    Entry<Bits, T> entry = this.forwardIndex.floorEntry(remainder);
    while (entry == null || !remainder.startsWith(entry.getKey())) {
      remainder = in.readAhead();
      entry = this.forwardIndex.floorEntry(remainder);
    }
    in.read(entry.getKey().bitLength);
    T temp_05_0001 = entry.getValue();
    if (null != entry)
      RefUtil.freeRef(entry);
    return temp_05_0001;
  }

  public Entry<Bits, T> decode(final Bits data) {
    if (null == data) {
      throw new IllegalArgumentException();
    }
    Entry<Bits, T> entry = this.forwardIndex.floorEntry(data);
    // TestUtil.openJson(new JSONObject(forwardIndex));
    if (entry != null && !data.startsWith(entry.getKey())) {
      entry = null;
    }
    // assert(null != entry || verifyIndexes());
    return entry;
  }

  public Bits encode(final T key) {
    final Bits bits = this.reverseIndex.get(key);
    assert null != bits || this.verifyIndexes();
    return bits;
  }

  public SortedMap<Bits, T> getCodes(final Bits fromKey) {
    final Bits next = fromKey.next();
    return null == next ? this.forwardIndex.tailMap(fromKey) : this.forwardIndex.subMap(fromKey, next);
  }

  public CountTreeBitsCollection getSetEncoder(final BitInputStream data) throws IOException {
    return new HammingCodeCollection(data, HammingCode.this);
  }

  public CountTreeBitsCollection getSetEncoder(final byte[] data) throws IOException {
    return new HammingCodeCollection(data, HammingCode.this);
  }

  public boolean verifyIndexes() {
    if (!isPrefixFreeCode(this.forwardIndex.keySet())) {
      return false;
    }
    for (final Entry<Bits, T> e : this.forwardIndex.entrySet()) {
      if (!e.getKey().equals(this.reverseIndex.get(e.getValue()))) {
        return false;
      }
      if (!e.getValue().equals(this.forwardIndex.get(e.getKey()))) {
        return false;
      }
    }
    for (final Entry<T, Bits> e : this.reverseIndex.entrySet()) {
      if (!e.getKey().equals(this.forwardIndex.get(e.getValue()))) {
        return false;
      }
      if (!e.getValue().equals(this.reverseIndex.get(e.getKey()))) {
        return false;
      }
    }
    return this.reverseIndex.size() == this.forwardIndex.size();
  }

  public int totalWeight() {
    // TODO Auto-generated method stub
    return 0;
  }

  public static @RefAware
  class HammingCodeCollection<T extends Comparable<T>> extends CountTreeBitsCollection {

    private final HammingCode<T> parent;

    public HammingCodeCollection(HammingCode<T> parent) {
      super();
      this.parent = parent;
    }

    public HammingCodeCollection(final BitInputStream data, HammingCode<T> parent) throws IOException {
      super(data);
      this.parent = parent;
    }

    public HammingCodeCollection(final byte[] data, HammingCode<T> parent) throws IOException {
      super(data);
      this.parent = parent;
    }

    public static @SuppressWarnings("unused")
    HammingCodeCollection[] addRefs(HammingCodeCollection[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(HammingCodeCollection::addRef)
          .toArray((x) -> new HammingCodeCollection[x]);
    }

    @Override
    public CodeType getType(final Bits bits) {
      final Entry<Bits, T> code = parent.decode(bits);
      if (null == code) {
        if (null != code)
          RefUtil.freeRef(code);
        return CodeType.Prefix;
      }
      assert bits.equals(code.getKey());
      if (null != code)
        RefUtil.freeRef(code);
      return CodeType.Terminal;
    }

    public @SuppressWarnings("unused")
    void _free() {
    }

    public @Override
    @SuppressWarnings("unused")
    HammingCodeCollection<T> addRef() {
      return (HammingCodeCollection<T>) super.addRef();
    }
  }

  private static @RefAware
  class SubCode<X extends Comparable<X>> implements Comparable<SubCode<X>> {
    final long count;
    final TreeMap<Bits, X> codes;
    final TreeMap<X, Bits> index;

    public SubCode(final long count, final X item) {
      super();
      this.count = count;
      this.codes = new TreeMap<Bits, X>();
      this.index = new TreeMap<X, Bits>();
      this.codes.put(Bits.NULL, item);
      this.index.put(item, Bits.NULL);
    }

    public SubCode(final SubCode<X> zero, final SubCode<X> one) {
      super();
      this.count = zero.count + one.count;
      this.codes = new TreeMap<Bits, X>();
      this.index = new TreeMap<X, Bits>();
      for (final Entry<Bits, X> e : zero.codes.entrySet()) {
        assert !one.index.containsKey(e.getValue());
        final Bits code = Bits.ZERO.concatenate(e.getKey());
        this.assertNull(this.codes.put(code, e.getValue()));
        this.assertNull(this.index.put(e.getValue(), code));
      }
      for (final Entry<Bits, X> e : one.codes.entrySet()) {
        assert !zero.index.containsKey(e.getValue());
        final Bits code = Bits.ONE.concatenate(e.getKey());
        this.assertNull(this.codes.put(code, e.getValue()));
        this.assertNull(this.index.put(e.getValue(), code));
      }
    }

    @Override
    public int compareTo(final SubCode<X> o) {
      if (this.count < o.count) {
        return -1;
      }
      if (this.count > o.count) {
        return 1;
      }
      final int compareTo = this.index.firstKey().compareTo(o.index.firstKey());
      assert 0 != compareTo;
      return compareTo;
    }

    private void assertNull(final Object obj) {
      assert null == obj;
    }
  }

}