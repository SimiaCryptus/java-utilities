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

package com.simiacryptus.util.data;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DensityTree {

  private final CharSequence[] columnNames;
  private double minSplitFract = 0.05;
  private int splitSizeThreshold = 10;
  private double minFitness = 4.0;
  private int maxDepth = Integer.MAX_VALUE;

  public DensityTree(CharSequence... columnNames) {
    this.columnNames = columnNames;
  }

  public CharSequence[] getColumnNames() {
    return columnNames;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  @Nonnull
  public DensityTree setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
    return this;
  }

  public double getMinFitness() {
    return minFitness;
  }

  @Nonnull
  public DensityTree setMinFitness(double minFitness) {
    this.minFitness = minFitness;
    return this;
  }

  public double getMinSplitFract() {
    return minSplitFract;
  }

  @Nonnull
  public DensityTree setMinSplitFract(double minSplitFract) {
    this.minSplitFract = minSplitFract;
    return this;
  }

  public int getSplitSizeThreshold() {
    return splitSizeThreshold;
  }

  @Nonnull
  public DensityTree setSplitSizeThreshold(int splitSizeThreshold) {
    this.splitSizeThreshold = splitSizeThreshold;
    return this;
  }

  @Nonnull
  public Bounds getBounds(@Nonnull double[][] points) {
    int dim = points[0].length;
    double[] max = RefIntStream.range(0, dim).mapToDouble(d -> {
      return RefArrays.stream(points).mapToDouble(pt -> pt[d]).filter(x -> Double.isFinite(x)).max().orElse(Double.NaN);
    }).toArray();
    double[] min = RefIntStream.range(0, dim).mapToDouble(d -> {
      return RefArrays.stream(points).mapToDouble(pt -> pt[d]).filter(x -> Double.isFinite(x)).min().orElse(Double.NaN);
    }).toArray();
    return new Bounds(max, min);
  }

  public class Bounds {
    @Nonnull
    public final double[] max;
    @Nonnull
    public final double[] min;

    public Bounds(@Nonnull double[] max, @Nonnull double[] min) {
      this.max = max;
      this.min = min;
      assert (max.length == min.length);
      assert (RefIntStream.range(0, max.length).filter(i -> Double.isFinite(max[i])).allMatch(i -> max[i] >= min[i]));
    }

    public double getVolume() {
      int dim = min.length;
      return RefIntStream.range(0, dim).mapToDouble(d -> {
        return max[d] - min[d];
      }).filter(x -> Double.isFinite(x) && x > 0.0).reduce((a, b) -> a * b).orElse(Double.NaN);
    }

    @Nonnull
    public Bounds union(@Nonnull double[] pt) {
      int dim = pt.length;
      return new Bounds(RefIntStream.range(0, dim).mapToDouble(d -> {
        return Double.isFinite(pt[d]) ? Math.max(max[d], pt[d]) : max[d];
      }).toArray(), RefIntStream.range(0, dim).mapToDouble(d -> {
        return Double.isFinite(pt[d]) ? Math.min(min[d], pt[d]) : min[d];
      }).toArray());
    }

    @Nonnull
    public String toString() {
      return "[" + RefUtil.get(RefIntStream.range(0, min.length).mapToObj(d -> {
        return RefString.format("%s: %s - %s", columnNames[d], min[d], max[d]);
      }).reduce((a, b) -> a + "; " + b)) + "]";
    }

  }

  public class OrthoRule extends Rule {
    private final int dim;
    private final double value;

    public OrthoRule(int dim, double value) {
      super(RefString.format("%s < %s", columnNames[dim], value));
      this.dim = dim;
      this.value = value;
    }

    @Override
    public boolean eval(double[] pt) {
      return pt[dim] < value;
    }
  }

  public abstract class Rule {
    public final String name;
    public double fitness;

    public Rule(String name) {
      this.name = name;
    }

    public abstract boolean eval(double[] pt);

    @Override
    public String toString() {
      return name;
    }
  }

  public class Node {
    @Nonnull
    public final double[][] points;
    @Nonnull
    public final Bounds bounds;
    private final int depth;
    @Nullable
    private Node left = null;
    @Nullable
    private Node right = null;
    @Nullable
    private Rule rule = null;

    public Node(@Nonnull double[][] points) {
      this(points, 0);
    }

    public Node(@Nonnull double[][] points, int depth) {
      this.points = points;
      this.bounds = getBounds(points);
      this.depth = depth;
      split();
    }

    public int getDepth() {
      return depth;
    }

    @Nullable
    public Node getLeft() {
      return left;
    }

    @Nonnull
    protected Node setLeft(Node left) {
      this.left = left;
      return this;
    }

    @Nullable
    public Node getRight() {
      return right;
    }

    @Nonnull
    protected Node setRight(Node right) {
      this.right = right;
      return this;
    }

    @Nullable
    public Rule getRule() {
      return rule;
    }

    @Nonnull
    protected Node setRule(Rule rule) {
      this.rule = rule;
      return this;
    }

    public int predict(double[] pt) {
      if (null == rule) {
        return 0;
      } else if (rule.eval(pt)) {
        return 1 + 2 * left.predict(pt);
      } else {
        return 0 + 2 * right.predict(pt);
      }
    }

    @Override
    public String toString() {
      return code();
    }

    public String code() {
      if (null != rule) {
        return RefString.format("// %s\nif(%s) { // Fitness %s\n  %s\n} else {\n  %s\n}", dataInfo(), rule,
            rule.fitness, left.code().replaceAll("\n", "\n  "), right.code().replaceAll("\n", "\n  "));
      } else {
        return "// " + dataInfo();
      }
    }

    public void split() {
      if (points.length <= splitSizeThreshold)
        return;
      if (maxDepth <= depth)
        return;
      this.rule = RefIntStream.range(0, points[0].length).mapToObj(x -> x).flatMap(dim -> split_ortho(dim))
          .filter(x -> Double.isFinite(x.fitness)).max(RefComparator.comparing(x -> x.fitness)).orElse(null);
      if (null == this.rule)
        return;
      double[][] leftPts = RefArrays.stream(this.points).filter(pt -> rule.eval(pt)).toArray(i -> new double[i][]);
      double[][] rightPts = RefArrays.stream(this.points).filter(pt -> !rule.eval(pt)).toArray(i -> new double[i][]);
      assert (leftPts.length + rightPts.length == this.points.length);
      if (rightPts.length == 0 || leftPts.length == 0)
        return;
      this.left = new Node(leftPts, depth + 1);
      this.right = new Node(rightPts, depth + 1);
    }

    public RefStream<Rule> split_ortho(int dim) {
      double[][] sortedPoints = RefArrays.stream(points).filter(pt -> Double.isFinite(pt[dim]))
          .sorted(RefComparator.comparing(pt -> pt[dim])).toArray(i -> new double[i][]);
      if (0 == sortedPoints.length)
        return RefStream.empty();
      final int minSize = (int) Math.max(sortedPoints.length * minSplitFract, 1);
      @Nonnull
      Bounds[] left = new Bounds[sortedPoints.length];
      @Nonnull
      Bounds[] right = new Bounds[sortedPoints.length];
      left[0] = getBounds(new double[][] { sortedPoints[0] });
      right[sortedPoints.length - 1] = getBounds(new double[][] { sortedPoints[sortedPoints.length - 1] });
      for (int i = 1; i < sortedPoints.length; i++) {
        left[i] = left[i - 1].union(sortedPoints[i]);
        right[(sortedPoints.length - 1) - i] = right[((sortedPoints.length - 1) - (i - 1))]
            .union(sortedPoints[(sortedPoints.length - 1) - i]);
      }
      return RefIntStream.range(1, sortedPoints.length - 1).filter(i -> {
        return sortedPoints[i - 1][dim] < sortedPoints[i][dim];
      }).mapToObj(i -> {
        int leftCount = i;
        int rightCount = sortedPoints.length - leftCount;
        if (minSize >= leftCount || minSize >= rightCount)
          return null;
        @Nonnull
        OrthoRule rule = new OrthoRule(dim, sortedPoints[i][dim]);
        Bounds l = left[i - 1];
        Bounds r = right[i];
        rule.fitness = -(leftCount * Math.log(l.getVolume() / Node.this.bounds.getVolume())
            + rightCount * Math.log(r.getVolume() / Node.this.bounds.getVolume()))
            / (sortedPoints.length * Math.log(2));
        return (Rule) rule;
      }).filter(i -> null != i && i.fitness > minFitness);
    }

    private CharSequence dataInfo() {
      return RefString.format("Count: %s Volume: %s Region: %s", points.length, bounds.getVolume(), bounds);
    }
  }
}
