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

package com.simiacryptus.util.test;

import com.simiacryptus.lang.UncheckedSupplier;
import com.simiacryptus.util.io.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class SysOutInterceptor extends PrintStream {

  public static final PrintStream ORIGINAL_OUT = System.out;
  public static final SysOutInterceptor INSTANCE = new SysOutInterceptor(ORIGINAL_OUT);
  private static final Logger log = LoggerFactory.getLogger(SysOutInterceptor.class);
  private final ThreadLocal<Boolean> isMonitoring = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };
  private final ThreadLocal<PrintStream> threadHandler = new ThreadLocal<PrintStream>() {
    @javax.annotation.Nonnull
    @Override
    protected PrintStream initialValue() {
      return getInner();
    }
  };

  private final AtomicBoolean initialized = new AtomicBoolean(false);

  private SysOutInterceptor(@javax.annotation.Nonnull final PrintStream out) {
    super(out);
  }

  public static LoggedResult<Void> withOutput(@javax.annotation.Nonnull final Runnable fn) {
    try {
      if (SysOutInterceptor.INSTANCE.isMonitoring.get()) throw new IllegalStateException();
      PrintStream prev = SysOutInterceptor.INSTANCE.threadHandler.get();
      @javax.annotation.Nonnull final ByteArrayOutputStream buff = new ByteArrayOutputStream();
      try (@javax.annotation.Nonnull PrintStream ps = new PrintStream(new TeeOutputStream(buff, prev))) {
        SysOutInterceptor.INSTANCE.threadHandler.set(ps);
        SysOutInterceptor.INSTANCE.isMonitoring.set(true);
        fn.run();
        ps.close();
        return new LoggedResult<>(null, buff.toString());
      }
    } catch (@javax.annotation.Nonnull final RuntimeException e) {
      throw e;
    } catch (@javax.annotation.Nonnull final Exception e) {
      throw new RuntimeException(e);
    } finally {
      SysOutInterceptor.INSTANCE.threadHandler.remove();
      SysOutInterceptor.INSTANCE.isMonitoring.remove();
    }
  }


  public static <T> LoggedResult<T> withOutput(@javax.annotation.Nonnull final UncheckedSupplier<T> fn) {
    try {
      if (SysOutInterceptor.INSTANCE.isMonitoring.get()) throw new IllegalStateException();
      PrintStream prev = SysOutInterceptor.INSTANCE.threadHandler.get();
      @javax.annotation.Nonnull final ByteArrayOutputStream buff = new ByteArrayOutputStream();
      try (@javax.annotation.Nonnull PrintStream ps = new PrintStream(new TeeOutputStream(buff, prev))) {
        SysOutInterceptor.INSTANCE.threadHandler.set(ps);
        SysOutInterceptor.INSTANCE.isMonitoring.set(true);
        T result = fn.get();
        ps.close();
        return new LoggedResult<>(result, buff.toString());
      }
    } catch (@javax.annotation.Nonnull final RuntimeException e) {
      throw e;
    } catch (@javax.annotation.Nonnull final Exception e) {
      throw new RuntimeException(e);
    } finally {
      SysOutInterceptor.INSTANCE.threadHandler.remove();
      SysOutInterceptor.INSTANCE.isMonitoring.remove();
    }
  }


  @javax.annotation.Nonnull
  public SysOutInterceptor init() {
    if (!initialized.getAndSet(true)) {
      ch.qos.logback.classic.Logger root = ((ch.qos.logback.classic.Logger) log).getLoggerContext().getLogger("ROOT");
      @javax.annotation.Nonnull ch.qos.logback.core.ConsoleAppender stdout = (ch.qos.logback.core.ConsoleAppender) root.getAppender("STDOUT");
      if (null != stdout) {
        stdout.setOutputStream(this);
      }
      System.setOut(this);
    }
    return this;
  }

  public PrintStream currentHandler() {
    return threadHandler.get();
  }

  @javax.annotation.Nonnull
  public PrintStream getInner() {
    return (PrintStream) out;
  }

  @Override
  public void print(final String s) {
    currentHandler().print(s);
  }

  @Override
  public void write(byte[] b) {
    currentHandler().print(new String(b));
  }

  @Override
  public void println(final String x) {
    final PrintStream currentHandler = currentHandler();
    currentHandler.println(x);
  }

  public PrintStream setCurrentHandler(final PrintStream out) {
    PrintStream previous = threadHandler.get();
    threadHandler.set(out);
    return previous;
  }

  public static class LoggedResult<T> {
    public final String log;
    public final T obj;

    public LoggedResult(final T obj, final String log) {
      this.obj = obj;
      this.log = log;
    }
  }
}
