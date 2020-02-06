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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

public class ArrayUtil {

  @Nonnull
  public static double[] add(@Nonnull final double[] a, @Nonnull final double[] b) {
    return ArrayUtil.op(a, b, (x, y) -> x + y);
  }

  public static RefList<double[]> add(@Nonnull final RefList<double[]> a, @Nonnull final RefList<double[]> b) {
    RefList<double[]> temp_08_0001 = ArrayUtil.op(a, b, (x, y) -> x + y);
    return temp_08_0001;
  }

  public static double dot(@Nonnull final double[] a, @Nonnull final double[] b) {
    return ArrayUtil.sum(ArrayUtil.op(a, b, (x, y) -> x * y));
  }

  public static double dot(@Nonnull final RefList<double[]> a, @Nonnull final RefList<double[]> b) {
    double temp_08_0002 = ArrayUtil.sum(ArrayUtil.multiply(a, b));
    return temp_08_0002;
  }

  public static double magnitude(@Nonnull final double[] a) {
    return Math.sqrt(ArrayUtil.dot(a, a));
  }

  public static double mean(@Nonnull final double[] op) {
    return ArrayUtil.sum(op) / op.length;
  }

  public static RefList<double[]> minus(@Nonnull final RefList<double[]> a, @Nonnull final RefList<double[]> b) {
    RefList<double[]> temp_08_0003 = ArrayUtil.op(a, b, (x, y) -> x - y);
    return temp_08_0003;
  }

  @Nonnull
  public static double[] multiply(@Nonnull final double[] a, final double b) {
    return ArrayUtil.op(a, x -> x * b);
  }

  @Nonnull
  public static double[] multiply(@Nonnull final double[] a, @Nonnull final double[] b) {
    return ArrayUtil.op(a, b, (x, y) -> x * y);
  }

  @Nonnull
  public static RefList<double[]> multiply(@Nonnull final RefList<double[]> a, final double b) {
    RefList<double[]> temp_08_0004 = ArrayUtil.op(a, x -> x * b);
    return temp_08_0004;
  }

  public static RefList<double[]> multiply(@Nonnull final @RefAware RefList<double[]> a,
                                           @Nonnull final @RefAware RefList<double[]> b) {
    RefList<double[]> temp_08_0005 = ArrayUtil.op(a, b, (x, y) -> x * y);
    return temp_08_0005;
  }

  @Nonnull
  public static double[] op(@Nonnull final double[] a, @Nonnull final double[] b,
                            @Nonnull final DoubleBinaryOperator fn) {
    assert a.length == b.length;
    @Nonnull final double[] c = new double[a.length];
    for (int j = 0; j < a.length; j++) {
      c[j] = fn.applyAsDouble(a[j], b[j]);
    }
    return c;
  }

  @Nonnull
  public static double[] op(@Nonnull final double[] a, @Nonnull final DoubleUnaryOperator fn) {
    @Nonnull final double[] c = new double[a.length];
    for (int j = 0; j < a.length; j++) {
      c[j] = fn.applyAsDouble(a[j]);
    }
    return c;
  }

  @Nonnull
  public static RefList<double[]> op(@Nonnull final @RefAware RefList<double[]> a,
                                     @Nonnull final DoubleUnaryOperator fn) {
    @Nonnull final RefArrayList<double[]> list = new RefArrayList<>();
    for (int i = 0; i < a.size(); i++) {
      double[] doubles = a.get(i);
      @Nonnull final double[] c = new double[doubles.length];
      for (int j = 0; j < doubles.length; j++) {
        c[j] = fn.applyAsDouble(doubles[j]);
      }
      list.add(c);
    }
    a.freeRef();
    return list;
  }

  public static RefList<double[]> op(@Nonnull final @RefAware RefList<double[]> a,
                                     @Nonnull final @RefAware RefList<double[]> b, @Nonnull final DoubleBinaryOperator fn) {
    assert a.size() == b.size();
    return RefIntStream.range(0, a.size()).parallel()
        .mapToObj(RefUtil.wrapInterface((IntFunction<? extends double[]>) i -> {
          double[] doubles_a = a.get(i);
          double[] doubles_b = b.get(i);
          assert doubles_a.length == doubles_b.length;
          @Nonnull final double[] c = new double[doubles_a.length];
          for (int j = 0; j < doubles_a.length; j++) {
            c[j] = fn.applyAsDouble(doubles_a[j], doubles_b[j]);
          }
          return c;
        }, a, b)).collect(RefCollectors.toList());
  }

  @Nonnull
  public static double[] subtract(@Nonnull final double[] a, @Nonnull final double[] b) {
    return ArrayUtil.op(a, b, (x, y) -> x - y);
  }

  public static double sum(@Nonnull final double[] op) {
    return RefArrays.stream(op).sum();
  }

  @Nonnull
  public static double[] sum(@Nonnull final double[] a, final double b) {
    return ArrayUtil.op(a, x -> x + b);
  }

  public static double sum(@Nonnull final @RefAware RefList<double[]> a) {
    double temp_08_0007 = a.stream().parallel().mapToDouble(x -> RefArrays.stream(x).sum()).sum();
    a.freeRef();
    return temp_08_0007;
  }

}
