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

package com.simiacryptus.util;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public @com.simiacryptus.ref.lang.RefAware
class ArrayUtil {

  @javax.annotation.Nonnull
  public static double[] add(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final double[] b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x + y);
  }

  public static com.simiacryptus.ref.wrappers.RefList<double[]> add(
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> a,
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x + y);
  }

  public static double dot(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final double[] b) {
    return com.simiacryptus.util.ArrayUtil.sum(com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x * y));
  }

  public static double dot(@javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> a,
                           @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> b) {
    return com.simiacryptus.util.ArrayUtil.sum(com.simiacryptus.util.ArrayUtil.multiply(a, b));
  }

  public static double magnitude(@javax.annotation.Nonnull final double[] a) {
    return Math.sqrt(com.simiacryptus.util.ArrayUtil.dot(a, a));
  }

  public static double mean(@javax.annotation.Nonnull final double[] op) {
    return com.simiacryptus.util.ArrayUtil.sum(op) / op.length;
  }

  public static com.simiacryptus.ref.wrappers.RefList<double[]> minus(
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> a,
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x - y);
  }

  @javax.annotation.Nonnull
  public static double[] multiply(@javax.annotation.Nonnull final double[] a, final double b) {
    return com.simiacryptus.util.ArrayUtil.op(a, (x) -> x * b);
  }

  @javax.annotation.Nonnull
  public static double[] multiply(@javax.annotation.Nonnull final double[] a,
                                  @javax.annotation.Nonnull final double[] b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x * y);
  }

  @javax.annotation.Nonnull
  public static com.simiacryptus.ref.wrappers.RefList<double[]> multiply(
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> a, final double b) {
    return com.simiacryptus.util.ArrayUtil.op(a, x -> x * b);
  }

  public static com.simiacryptus.ref.wrappers.RefList<double[]> multiply(
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> a,
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x * y);
  }

  @javax.annotation.Nonnull
  public static double[] op(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final double[] b,
                            @javax.annotation.Nonnull final DoubleBinaryOperator fn) {
    assert a.length == b.length;
    @javax.annotation.Nonnull final double[] c = new double[a.length];
    for (int j = 0; j < a.length; j++) {
      c[j] = fn.applyAsDouble(a[j], b[j]);
    }
    return c;
  }

  @javax.annotation.Nonnull
  public static double[] op(@javax.annotation.Nonnull final double[] a,
                            @javax.annotation.Nonnull final DoubleUnaryOperator fn) {
    @javax.annotation.Nonnull final double[] c = new double[a.length];
    for (int j = 0; j < a.length; j++) {
      c[j] = fn.applyAsDouble(a[j]);
    }
    return c;
  }

  @javax.annotation.Nonnull
  public static com.simiacryptus.ref.wrappers.RefList<double[]> op(
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> a,
      @javax.annotation.Nonnull final DoubleUnaryOperator fn) {
    @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefArrayList<double[]> list = new com.simiacryptus.ref.wrappers.RefArrayList<>();
    for (int i = 0; i < a.size(); i++) {
      @javax.annotation.Nonnull final double[] c = new double[a.get(i).length];
      for (int j = 0; j < a.get(i).length; j++) {
        c[j] = fn.applyAsDouble(a.get(i)[j]);
      }
      list.add(c);
    }
    return list;
  }

  public static com.simiacryptus.ref.wrappers.RefList<double[]> op(
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> a,
      @javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> b,
      @javax.annotation.Nonnull final DoubleBinaryOperator fn) {
    assert a.size() == b.size();
    return com.simiacryptus.ref.wrappers.RefIntStream.range(0, a.size()).parallel().mapToObj(i -> {
      assert a.get(i).length == b.get(i).length;
      @javax.annotation.Nonnull final double[] c = new double[a.get(i).length];
      for (int j = 0; j < a.get(i).length; j++) {
        c[j] = fn.applyAsDouble(a.get(i)[j], b.get(i)[j]);
      }
      return c;
    }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
  }

  @javax.annotation.Nonnull
  public static double[] subtract(@javax.annotation.Nonnull final double[] a,
                                  @javax.annotation.Nonnull final double[] b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x - y);
  }

  public static double sum(@javax.annotation.Nonnull final double[] op) {
    return com.simiacryptus.ref.wrappers.RefArrays.stream(op).sum();
  }

  @javax.annotation.Nonnull
  public static double[] sum(@javax.annotation.Nonnull final double[] a, final double b) {
    return com.simiacryptus.util.ArrayUtil.op(a, (x) -> x + b);
  }

  public static double sum(@javax.annotation.Nonnull final com.simiacryptus.ref.wrappers.RefList<double[]> a) {
    return a.stream().parallel().mapToDouble(x -> com.simiacryptus.ref.wrappers.RefArrays.stream(x).sum()).sum();
  }

}
