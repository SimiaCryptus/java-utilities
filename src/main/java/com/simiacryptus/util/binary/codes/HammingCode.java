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

package com.simiacryptus.util.binary.codes;

import com.simiacryptus.util.binary.BitInputStream;
import com.simiacryptus.util.binary.Bits;
import com.simiacryptus.util.binary.bitset.CountTreeBitsCollection;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The type Hamming code.
 *
 * @param <T> the type parameter
 */
public class HammingCode<T extends Comparable<T>> {
  /**
   * The Forward index.
   */
  protected final TreeMap<Bits, T> forwardIndex = new TreeMap<Bits, T>();
  /**
   * The Reverse index.
   */
  protected final HashMap<T, Bits> reverseIndex = new HashMap<T, Bits>();
  /**
   * The Weights.
   */
  protected final HashMap<T, Integer> weights = new HashMap<T, Integer>();
  /**
   * The Total weight.
   */
  protected final long totalWeight;
  
  /**
   * Instantiates a new Hamming code.
   *
   * @param symbols the symbols
   */
  public HammingCode(final Collection<HammingSymbol<T>> symbols) {
    if (0 < symbols.size()) {
      final TreeSet<SubCode<T>> assemblySet = new TreeSet<SubCode<T>>();
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
      this.forwardIndex.putAll(root.codes);
      this.reverseIndex.putAll(root.index);
      totalWeight = root.count;
    }
    else {
      totalWeight = 0;
    }
    assert this.verifyIndexes();
    assert this.forwardIndex.size() == symbols.size();
  }
  
  /**
   * Is prefix free code boolean.
   *
   * @param keySet the key set
   * @return the boolean
   */
  public static boolean isPrefixFreeCode(final Set<Bits> keySet) {
    final TreeSet<Bits> check = new TreeSet<Bits>();
    for (final Bits code : keySet) {
      final Bits ceiling = check.ceiling(code);
      if (null != ceiling
        && (ceiling.startsWith(code) || code.startsWith(ceiling))) {
        return false;
      }
      final Bits floor = check.floor(code);
      if (null != floor && (floor.startsWith(code) || code.startsWith(floor))) {
        return false;
      }
      check.add(code);
    }
    return true;
  }
  
  /**
   * Code size int.
   *
   * @return the int
   */
  public int codeSize() {
    return this.forwardIndex.size();
  }
  
  /**
   * Decode t.
   *
   * @param in the in
   * @return the t
   * @throws IOException the io exception
   */
  public T decode(final BitInputStream in) throws IOException {
    Bits remainder = in.readAhead(0);
    Entry<Bits, T> entry = this.forwardIndex.floorEntry(remainder);
    while (entry == null || !remainder.startsWith(entry.getKey())) {
      remainder = in.readAhead();
      entry = this.forwardIndex.floorEntry(remainder);
    }
    in.read(entry.getKey().bitLength);
    return entry.getValue();
  }
  
  /**
   * Decode entry.
   *
   * @param data the data
   * @return the entry
   */
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
  
  /**
   * Encode bits.
   *
   * @param key the key
   * @return the bits
   */
  public Bits encode(final T key) {
    final Bits bits = this.reverseIndex.get(key);
    assert null != bits || this.verifyIndexes();
    return bits;
  }
  
  /**
   * Gets codes.
   *
   * @param fromKey the from key
   * @return the codes
   */
  public SortedMap<Bits, T> getCodes(final Bits fromKey) {
    final Bits next = fromKey.next();
    final SortedMap<Bits, T> subMap = null == next ? this.forwardIndex
      .tailMap(fromKey) : this.forwardIndex.subMap(fromKey, next);
    return subMap;
  }
  
  /**
   * Gets set encoder.
   *
   * @return the set encoder
   */
  public CountTreeBitsCollection getSetEncoder() {
    return new HammingCodeCollection();
  }
  
  /**
   * Gets set encoder.
   *
   * @param data the data
   * @return the set encoder
   * @throws IOException the io exception
   */
  public CountTreeBitsCollection getSetEncoder(final BitInputStream data)
    throws IOException {
    return new HammingCodeCollection(data);
  }
  
  /**
   * Gets set encoder.
   *
   * @param data the data
   * @return the set encoder
   * @throws IOException the io exception
   */
  public CountTreeBitsCollection getSetEncoder(final byte[] data)
    throws IOException {
    return new HammingCodeCollection(data);
  }
  
  /**
   * Gets weights.
   *
   * @return the weights
   */
  public Map<T, Integer> getWeights() {
    return Collections.unmodifiableMap(this.weights);
  }
  
  /**
   * Verify indexes boolean.
   *
   * @return the boolean
   */
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
  
  /**
   * Total weight int.
   *
   * @return the int
   */
  public int totalWeight() {
    // TODO Auto-generated method stub
    return 0;
  }
  
  private static class SubCode<X extends Comparable<X>> implements
    Comparable<SubCode<X>> {
    /**
     * The Count.
     */
    final long count;
    /**
     * The Codes.
     */
    final TreeMap<Bits, X> codes;
    /**
     * The Index.
     */
    final TreeMap<X, Bits> index;
    
    /**
     * Instantiates a new Sub code.
     *
     * @param count the count
     * @param item  the item
     */
    public SubCode(final long count, final X item) {
      super();
      this.count = count;
      this.codes = new TreeMap<Bits, X>();
      this.index = new TreeMap<X, Bits>();
      this.codes.put(Bits.NULL, item);
      this.index.put(item, Bits.NULL);
    }
    
    /**
     * Instantiates a new Sub code.
     *
     * @param zero the zero
     * @param one  the one
     */
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
    
    private void assertNull(final Object obj) {
      assert null == obj;
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
  }
  
  /**
   * The type Hamming code collection.
   */
  public class HammingCodeCollection extends CountTreeBitsCollection {
    /**
     * Instantiates a new Hamming code collection.
     */
    public HammingCodeCollection() {
      super();
    }
    
    /**
     * Instantiates a new Hamming code collection.
     *
     * @param data the data
     * @throws IOException the io exception
     */
    public HammingCodeCollection(final BitInputStream data) throws IOException {
      super(data);
    }
    
    /**
     * Instantiates a new Hamming code collection.
     *
     * @param data the data
     * @throws IOException the io exception
     */
    public HammingCodeCollection(final byte[] data) throws IOException {
      super(data);
    }
    
    @Override
    public CodeType getType(final Bits bits) {
      final Entry<Bits, T> code = HammingCode.this.decode(bits);
      if (null == code) {
        return CodeType.Prefix;
      }
      assert bits.equals(code.getKey());
      return CodeType.Terminal;
    }
  }
  
}