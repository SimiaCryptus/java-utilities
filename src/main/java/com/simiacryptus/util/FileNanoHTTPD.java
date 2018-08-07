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

package com.simiacryptus.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The type File nano httpd.
 */
public class FileNanoHTTPD extends NanoHTTPD implements FileHTTPD {
  static final Logger log = LoggerFactory.getLogger(FileNanoHTTPD.class);
  
  /**
   * The Custom handlers.
   */
  public final Map<CharSequence, Function<IHTTPSession, Response>> handlers = new HashMap<>();
  /**
   * The Pool.
   */
  protected final ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).build());
  private final File root;
  
  /**
   * Instantiates a new File nano httpd.
   *
   * @param root the root
   * @param port the port
   */
  public FileNanoHTTPD(File root, final int port) {
    super(port);
    this.root = root;
  }
  
  /**
   * Create output stream.
   *
   * @param port     the port
   * @param path     the path
   * @param mimeType the mime type
   * @return the output stream
   * @throws IOException the io exception
   */
  @javax.annotation.Nonnull
  public static FileNanoHTTPD create(final int port, @Nonnull final File path, final String mimeType) throws IOException {
    return new FileNanoHTTPD(path, port).init();
  }
  
  /**
   * Sync handler function.
   *
   * @param mimeType the mime type
   * @param logic    the logic
   * @return the function
   */
  public static Function<IHTTPSession, Response> handler(
    final String mimeType,
    @Nonnull final Consumer<OutputStream> logic
  )
  {
    return session -> {
      try (@javax.annotation.Nonnull ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        logic.accept(out);
        out.flush();
        final byte[] bytes = out.toByteArray();
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeType, new ByteArrayInputStream(bytes), bytes.length);
      } catch (@javax.annotation.Nonnull final IOException e) {
        throw new RuntimeException(e);
      }
    };
  }
  
  /**
   * Add session handler function.
   *
   * @param path  the path
   * @param value the value
   * @return the function
   */
  public Function<IHTTPSession, Response> addSessionHandler(final CharSequence path, final Function<IHTTPSession, Response> value) {
    return handlers.put(path, value);
  }
  
  /**
   * Add sync handler.
   *
   * @param path     the path
   * @param mimeType the mime type
   * @param logic    the logic
   */
  public void addHandler(final CharSequence path, final String mimeType, @Nonnull final Consumer<OutputStream> logic) {
    addSessionHandler(path, FileNanoHTTPD.handler(mimeType, logic));
  }
  
  /**
   * Init stream nano httpd.
   *
   * @return the stream nano httpd
   * @throws IOException the io exception
   */
  @javax.annotation.Nonnull
  public FileNanoHTTPD init() throws IOException {
    start(30000);
    return this;
  }
  
  @Override
  public Response serve(final IHTTPSession session) {
    String requestPath = Util.stripPrefix(session.getUri(), "/");
    @javax.annotation.Nonnull final File file = new File(root, requestPath);
    if (null != file && file.exists() && file.isFile()) {
      handlers.remove(requestPath);
      try {
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, null, new FileInputStream(file), file.length());
      } catch (@javax.annotation.Nonnull final FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      if (handlers.containsKey(requestPath)) {
        try {
          return handlers.get(requestPath).apply(session);
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      }
      else {
        log.warn(String.format(
          "Not Found: %s\n\tCurrent Path: %s\n\t%s",
          requestPath,
          root.getAbsolutePath(),
          handlers.keySet().stream()
            .map(handlerPath -> "Installed Handler: " + handlerPath)
            .reduce((a, b) -> a + "\n\t" + b).get()
        ));
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
      }
    }
  }
  
}
