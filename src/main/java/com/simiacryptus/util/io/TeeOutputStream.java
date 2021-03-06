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

package com.simiacryptus.util.io;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TeeOutputStream extends OutputStream {
  public final List<OutputStream> branches = new ArrayList<>();
  public final OutputStream primary;
  @Nullable
  private final ByteArrayOutputStream heapBuffer;
  private boolean chainCloses;

  public TeeOutputStream(final OutputStream primary, final boolean buffer) {
    setChainCloses(false);
    this.primary = primary;
    if (buffer) {
      heapBuffer = new ByteArrayOutputStream();
      branches.add(heapBuffer);
    } else {
      heapBuffer = null;
    }
  }

  public TeeOutputStream(final OutputStream primary, final OutputStream... secondaries) {
    this(primary, false);
    branches.addAll(Arrays.asList(secondaries));
  }

  public boolean isChainCloses() {
    return chainCloses;
  }

  @Nonnull
  public TeeOutputStream setChainCloses(boolean chainCloses) {
    this.chainCloses = chainCloses;
    return this;
  }

  @Override
  public void close() throws IOException {
    primary.close();
    if (isChainCloses())
      for (@Nonnull final OutputStream branch : branches) {
        branch.close();
      }
  }

  @Override
  public void flush() throws IOException {
    primary.flush();
    for (@Nonnull final OutputStream branch : branches) {
      branch.flush();
    }
  }

  @Nonnull
  public PipedInputStream newInputStream() throws IOException {
    @Nonnull final TeeOutputStream outTee = this;
    @Nonnull final AtomicReference<Runnable> onClose = new AtomicReference<>();
    @Nonnull final PipedOutputStream outPipe = new PipedOutputStream();
    @Nonnull final PipedInputStream in = new PipedInputStream() {
      @Override
      public void close() throws IOException {
        outPipe.close();
        super.close();
      }
    };
    outPipe.connect(in);
    @Nonnull final OutputStream outAsync = new AsyncOutputStream(outPipe);
    new Thread(() -> {
      try {
        if (null != heapBuffer) {
          outAsync.write(heapBuffer.toByteArray());
          outAsync.flush();
        }
        outTee.branches.add(outAsync);
      } catch (@Nonnull final IOException e) {
        e.printStackTrace();
      }
    }).start();
    onClose.set(() -> {
      outTee.branches.remove(outAsync);
      System.err.println("END HTTP Session");
    });
    return in;
  }

  @Override
  public synchronized void write(@Nonnull final byte[] b) throws IOException {
    primary.write(b);
    for (@Nonnull final OutputStream branch : branches) {
      branch.write(b);
    }
  }

  @Override
  public synchronized void write(@Nonnull final byte[] b, final int off, final int len) throws IOException {
    primary.write(b, off, len);
    for (@Nonnull final OutputStream branch : branches) {
      branch.write(b, off, len);
    }
  }

  @Override
  public synchronized void write(final int b) throws IOException {
    primary.write(b);
    for (@Nonnull final OutputStream branch : branches) {
      branch.write(b);
    }
  }
}
