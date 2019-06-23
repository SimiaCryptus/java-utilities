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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class LOG {

  private static final long startTime = System.nanoTime();

  public static void d(final String msg, final Object... args) {
    LOG.preprocessArgs(args);
    LOG.log(Severity.Debug, msg, args);
  }

  public static void d(final Throwable e, final CharSequence msg, final Object... args) {
    LOG.d(msg + "\n  " + LOG.toString(e).replace("\n", "\n  "), args);
  }

  private static void log(final Severity debug, final String msg, final Object[] args) {
    final String formatted = String.format(msg, args);
    final StackTraceElement caller = Arrays.stream(Thread.currentThread().getStackTrace()).filter((stack) -> {
      Class<?> clazz;
      try {
        clazz = Class.forName(stack.getClassName());
      } catch (final Exception e) {
        return true;
      }
      if (clazz == Thread.class) return false;
      return clazz != LOG.class;
    }).findFirst().get();
    final double time = (System.nanoTime() - LOG.startTime) / 1000000000.;
    final String line = String.format("[%.5f] (%s:%s) %s", time, caller.getFileName(), caller.getLineNumber(), formatted.replaceAll("\n", "\n\t"));
    System.out.println(line);
  }

  private static void preprocessArgs(final Object... args) {
    for (int i = 0; i < args.length; i++) {
      if (null == args[i]) {
        continue;
      }
      final Class<?> c = args[i].getClass();
      if (c.isArray()) {
        if (args[i] instanceof double[]) {
          args[i] = LOG.toString((double[]) args[i]);
        } else if (args[i] instanceof int[]) {
          args[i] = Arrays.toString((int[]) args[i]);
        } else if (args[i] instanceof long[]) {
          args[i] = Arrays.toString((long[]) args[i]);
        } else if (args[i] instanceof byte[]) {
          args[i] = Arrays.toString((byte[]) args[i]);
        } else {
          args[i] = Arrays.toString((Object[]) args[i]);
        }
      }
    }
  }

  private static CharSequence toString(final double[] point) {
    final StringBuffer sb = new StringBuffer();
    for (final double v : point) {
      if (0 < sb.length()) {
        sb.append(", ");
      }
      sb.append(String.format("%.3f", v));
    }
    return "[" + sb + "]";
  }

  public static String toString(final Throwable e) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream s = new PrintStream(out);
    try {
      e.printStackTrace(s);
    } finally {
      s.close();
    }
    final String exception = out.toString();
    return exception;
  }

  public enum Severity {
    Debug
  }

}
