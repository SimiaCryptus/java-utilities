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

import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.util.Util;

import javax.annotation.Nonnull;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AsyncOutputStream extends FilterOutputStream {

  private final FairAsyncWorkQueue queue = new FairAsyncWorkQueue();

  public AsyncOutputStream(@Nonnull final OutputStream stream) {
    super(stream);
  }

  @Override
  public synchronized void close() {
    queue.submit(() -> {
      try {
        out.close();
      } catch (@Nonnull final IOException e) {
        throw Util.throwException(e);
      }
    });
  }

  @Override
  public synchronized void flush() {
    queue.submit(() -> {
      try {
        out.flush();
      } catch (@Nonnull final IOException e) {
        throw Util.throwException(e);
      }
    });
  }

  @Override
  public synchronized void write(@Nonnull final byte[] b, final int off, final int len) {
    @Nonnull final byte[] _b = RefArrays.copyOfRange(b, off, Math.min(b.length, off + len));
    queue.submit(() -> {
      try {
        out.write(_b);
      } catch (@Nonnull final IOException e) {
        throw Util.throwException(e);
      }
    });
  }

  @Override
  public synchronized void write(final int b) {
    queue.submit(() -> {
      try {
        out.write(b);
      } catch (@Nonnull final IOException e) {
        throw Util.throwException(e);
      }
    });
  }

}
