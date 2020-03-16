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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefString;
import com.simiacryptus.ref.wrappers.RefStringBuilder;
import com.simiacryptus.ref.wrappers.RefSystem;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class LOG {

  private static final long startTime = RefSystem.nanoTime();

  public static void d(@Nonnull final String msg, final Object... args) {
    LOG.preprocessArgs(args);
    LOG.log(msg, args);
  }

  public static void d(@Nonnull final Throwable e, final CharSequence msg, final Object... args) {
    LOG.d(msg + "\n  " + LOG.toString(e).replace("\n", "\n  "), args);
  }

  public static String toString(@Nonnull final Throwable e) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream s = new PrintStream(out);
    try {
      e.printStackTrace(s);
    } finally {
      s.close();
    }
    return out.toString();
  }

  private static void log(@Nonnull final String msg, final Object[] args) {
    final String formatted = RefString.format(msg, args);
    final StackTraceElement caller = RefUtil.get(Arrays.stream(Thread.currentThread().getStackTrace()).filter(stack -> {
      Class<?> clazz;
      try {
        clazz = Class.forName(stack.getClassName());
      } catch (@Nonnull final Exception e) {
        return true;
      }
      if (clazz == Thread.class)
        return false;
      return clazz != LOG.class;
    }).findFirst());
    final double time = (RefSystem.nanoTime() - LOG.startTime) / 1000000000.;
    final String line = RefString.format("[%.5f] (%s:%s) %s", time, caller.getFileName(), caller.getLineNumber(),
        formatted.replaceAll("\n", "\n\t"));
    RefSystem.out.println(line);
  }

  private static void preprocessArgs(@Nonnull final Object... args) {
    for (int i = 0; i < args.length; i++) {
      if (null == args[i]) {
        continue;
      }
      final Class<?> c = args[i].getClass();
      if (c.isArray()) {
        if (args[i] instanceof double[]) {
          args[i] = LOG.toString((double[]) args[i]);
        } else if (args[i] instanceof int[]) {
          args[i] = RefArrays.toString((int[]) args[i]);
        } else if (args[i] instanceof long[]) {
          args[i] = RefArrays.toString((long[]) args[i]);
        } else if (args[i] instanceof byte[]) {
          args[i] = RefArrays.toString((byte[]) args[i]);
        } else {
          args[i] = RefArrays.toString((Object[]) args[i]);
        }
      }
    }
  }

  @Nonnull
  private static CharSequence toString(@Nonnull final double[] point) {
    final RefStringBuilder sb = new RefStringBuilder();
    for (final double v : point) {
      if (0 < sb.length()) {
        sb.append(", ");
      }
      sb.append(RefString.format("%.3f", v));
    }
    return "[" + sb + "]";
  }

  public enum Severity {
    Debug
  }

}
