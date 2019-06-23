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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ArrayUtil {

  @javax.annotation.Nonnull
  public static double[] add(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final double[] b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x + y);
  }

  public static List<double[]> add(@javax.annotation.Nonnull final List<double[]> a, @javax.annotation.Nonnull final List<double[]> b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x + y);
  }

  public static double dot(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final double[] b) {
    return com.simiacryptus.util.ArrayUtil.sum(com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x * y));
  }

  public static double dot(@javax.annotation.Nonnull final List<double[]> a, @javax.annotation.Nonnull final List<double[]> b) {
    return com.simiacryptus.util.ArrayUtil.sum(com.simiacryptus.util.ArrayUtil.multiply(a, b));
  }

  public static double magnitude(@javax.annotation.Nonnull final double[] a) {
    return Math.sqrt(com.simiacryptus.util.ArrayUtil.dot(a, a));
  }

  public static double mean(@javax.annotation.Nonnull final double[] op) {
    return com.simiacryptus.util.ArrayUtil.sum(op) / op.length;
  }

  public static List<double[]> minus(@javax.annotation.Nonnull final List<double[]> a, @javax.annotation.Nonnull final List<double[]> b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x - y);
  }

  @javax.annotation.Nonnull
  public static double[] multiply(@javax.annotation.Nonnull final double[] a, final double b) {
    return com.simiacryptus.util.ArrayUtil.op(a, (x) -> x * b);
  }

  @javax.annotation.Nonnull
  public static double[] multiply(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final double[] b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x * y);
  }

  @javax.annotation.Nonnull
  public static List<double[]> multiply(@javax.annotation.Nonnull final List<double[]> a, final double b) {
    return com.simiacryptus.util.ArrayUtil.op(a, x -> x * b);
  }

  public static List<double[]> multiply(@javax.annotation.Nonnull final List<double[]> a, @javax.annotation.Nonnull final List<double[]> b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x * y);
  }

  @javax.annotation.Nonnull
  public static double[] op(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final double[] b, @javax.annotation.Nonnull final DoubleBinaryOperator fn) {
    assert a.length == b.length;
    @javax.annotation.Nonnull final double[] c = new double[a.length];
    for (int j = 0; j < a.length; j++) {
      c[j] = fn.applyAsDouble(a[j], b[j]);
    }
    return c;
  }

  @javax.annotation.Nonnull
  public static double[] op(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final DoubleUnaryOperator fn) {
    @javax.annotation.Nonnull final double[] c = new double[a.length];
    for (int j = 0; j < a.length; j++) {
      c[j] = fn.applyAsDouble(a[j]);
    }
    return c;
  }

  @javax.annotation.Nonnull
  public static List<double[]> op(@javax.annotation.Nonnull final List<double[]> a, @javax.annotation.Nonnull final DoubleUnaryOperator fn) {
    @javax.annotation.Nonnull final ArrayList<double[]> list = new ArrayList<>();
    for (int i = 0; i < a.size(); i++) {
      @javax.annotation.Nonnull final double[] c = new double[a.get(i).length];
      for (int j = 0; j < a.get(i).length; j++) {
        c[j] = fn.applyAsDouble(a.get(i)[j]);
      }
      list.add(c);
    }
    return list;
  }

  public static List<double[]> op(@javax.annotation.Nonnull final List<double[]> a, @javax.annotation.Nonnull final List<double[]> b, @javax.annotation.Nonnull final DoubleBinaryOperator fn) {
    assert a.size() == b.size();
    return IntStream.range(0, a.size()).parallel().mapToObj(i -> {
      assert a.get(i).length == b.get(i).length;
      @javax.annotation.Nonnull final double[] c = new double[a.get(i).length];
      for (int j = 0; j < a.get(i).length; j++) {
        c[j] = fn.applyAsDouble(a.get(i)[j], b.get(i)[j]);
      }
      return c;
    }).collect(Collectors.toList());
  }

  @javax.annotation.Nonnull
  public static double[] subtract(@javax.annotation.Nonnull final double[] a, @javax.annotation.Nonnull final double[] b) {
    return com.simiacryptus.util.ArrayUtil.op(a, b, (x, y) -> x - y);
  }

  public static double sum(@javax.annotation.Nonnull final double[] op) {
    return Arrays.stream(op).sum();
  }

  @javax.annotation.Nonnull
  public static double[] sum(@javax.annotation.Nonnull final double[] a, final double b) {
    return com.simiacryptus.util.ArrayUtil.op(a, (x) -> x + b);
  }

  public static double sum(@javax.annotation.Nonnull final List<double[]> a) {
    return a.stream().parallel().mapToDouble(x -> Arrays.stream(x).sum()).sum();
  }

}
