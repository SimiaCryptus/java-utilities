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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.simiacryptus.util.io.BinaryChunkIterator;
import com.simiacryptus.util.io.TeeInputStream;
import com.simiacryptus.util.test.LabeledObject;
import com.simiacryptus.util.test.SysOutInterceptor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

public class Util {

  private static final Logger log = LoggerFactory.getLogger(Util.class);
  public static final ThreadLocal<Random> R = new ThreadLocal<Random>() {
    public final Random r = new Random(System.nanoTime());

    @Override
    protected Random initialValue() {
      return new Random(r.nextLong());
    }
  };
  private static final java.util.concurrent.atomic.AtomicInteger idcounter = new java.util.concurrent.atomic.AtomicInteger(0);
  private static final String jvmId = UUID.randomUUID().toString();

  public static void add(@javax.annotation.Nonnull final DoubleSupplier f, @javax.annotation.Nonnull final double[] data) {
    for (int i = 0; i < data.length; i++) {
      data[i] += f.getAsDouble();
    }
  }

  public static Stream<byte[]> binaryStream(final String path, @javax.annotation.Nonnull final String name, final int skip, final int recordSize) throws IOException {
    @javax.annotation.Nonnull final File file = new File(path, name);
    final byte[] fileData = IOUtils.toByteArray(new BufferedInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)))));
    @javax.annotation.Nonnull final DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
    in.skip(skip);
    return com.simiacryptus.util.Util.toIterator(new BinaryChunkIterator(in, recordSize));
  }

  public static <F, T> Function<F, T> cache(@javax.annotation.Nonnull final Function<F, T> inner) {
    @javax.annotation.Nonnull final LoadingCache<F, T> cache = CacheBuilder.newBuilder().build(new CacheLoader<F, T>() {
      @Override
      public T load(final F key) {
        return inner.apply(key);
      }
    });
    return cache::getUnchecked;
  }

  public static InputStream cacheLocal(String file, URI url) throws IOException {
    return cacheLocal(file, getStreamSupplier(url));
  }

  public static InputStream cacheLocal(String file, Supplier<InputStream> fn) throws FileNotFoundException {
    File f = new File(file);
    if (f.exists()) {
      return new FileInputStream(f);
    } else {
      FileOutputStream cache = new FileOutputStream(file);
      return new TeeInputStream(fn.get(), cache);
    }
  }

  public static Supplier<InputStream> getStreamSupplier(URI url) {
    return () -> {
      TrustManager[] trustManagers = {
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }

            public void checkClientTrusted(
                X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(
                X509Certificate[] certs, String authType) {
            }
          }
      };
      try {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustManagers, null);
        SSLSocketFactory sslFactory = ctx.getSocketFactory();
        URLConnection urlConnection = url.toURL().openConnection();
        if (urlConnection instanceof HttpsURLConnection) {
          HttpsURLConnection conn = (HttpsURLConnection) urlConnection;
          conn.setSSLSocketFactory(sslFactory);
          conn.setRequestMethod("GET");
        }
        return urlConnection.getInputStream();
      } catch (KeyManagementException | NoSuchAlgorithmException | IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static InputStream cacheStream(@javax.annotation.Nonnull final String url, @javax.annotation.Nonnull final String file) throws IOException, NoSuchAlgorithmException, KeyManagementException {
    if (new File(file).exists()) {
      return new FileInputStream(file);
    } else {
      return new TeeInputStream(get(url), new FileOutputStream(file));
    }
  }

  public static File cacheFile(@javax.annotation.Nonnull final String url, @javax.annotation.Nonnull final String file) throws IOException, NoSuchAlgorithmException, KeyManagementException {
    final File fileLoc = new File(file).getCanonicalFile().getAbsoluteFile();
    if (!fileLoc.exists()) {
      log.info(String.format("Downloading %s to %s", url, fileLoc));
      IOUtils.copy(get(url), new FileOutputStream(fileLoc));
      log.info(String.format("Finished Download of %s to %s", url, fileLoc));
    }
    return fileLoc;
  }

  public static InputStream get(@javax.annotation.Nonnull String url) throws NoSuchAlgorithmException, KeyManagementException, IOException {
    @javax.annotation.Nonnull final TrustManager[] trustManagers = {
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(
              final X509Certificate[] certs, final String authType) {
          }

          @Override
          public void checkServerTrusted(
              final X509Certificate[] certs, final String authType) {
          }

          @javax.annotation.Nonnull
          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }
        }
    };
    @javax.annotation.Nonnull final SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, trustManagers, null);
    final SSLSocketFactory sslFactory = ctx.getSocketFactory();
    final URLConnection urlConnection = new URL(url).openConnection();
    if (urlConnection instanceof HttpsURLConnection) {
      @javax.annotation.Nonnull final HttpsURLConnection conn = (HttpsURLConnection) urlConnection;
      conn.setSSLSocketFactory(sslFactory);
      conn.setRequestMethod("GET");
    }
    return urlConnection.getInputStream();
  }

  public static InputStream cacheStream(@javax.annotation.Nonnull final URI url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
    return com.simiacryptus.util.Util.cacheStream(url.toString(), new File(url.getPath()).getName());
  }

  public static File cacheFile(@javax.annotation.Nonnull final URI url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
    return com.simiacryptus.util.Util.cacheFile(url.toString(), new File(url.getPath()).getName());
  }

  public static CharSequence[] currentStack() {
    return Stream.of(Thread.currentThread().getStackTrace()).map(Object::toString).toArray(i -> new CharSequence[i]);
  }

  @javax.annotation.Nonnull
  public static TemporalUnit cvt(@javax.annotation.Nonnull final TimeUnit units) {
    switch (units) {
      case DAYS:
        return ChronoUnit.DAYS;
      case HOURS:
        return ChronoUnit.HOURS;
      case MINUTES:
        return ChronoUnit.MINUTES;
      case SECONDS:
        return ChronoUnit.SECONDS;
      case NANOSECONDS:
        return ChronoUnit.NANOS;
      case MICROSECONDS:
        return ChronoUnit.MICROS;
      case MILLISECONDS:
        return ChronoUnit.MILLIS;
      default:
        throw new IllegalArgumentException(units.toString());
    }
  }

  public static void layout(@javax.annotation.Nonnull final Component c) {
    c.doLayout();
    if (c instanceof Container) {
      Arrays.stream(((Container) c).getComponents()).forEach(com.simiacryptus.util.Util::layout);
    }
  }

  public static String mkString(@javax.annotation.Nonnull final CharSequence separator, final CharSequence... strs) {
    return Arrays.asList(strs).stream().collect(Collectors.joining(separator));
  }

  public static String pathTo(@javax.annotation.Nonnull final File from, @javax.annotation.Nonnull final File to) {
    return from.toPath().relativize(to.toPath()).toString().replaceAll("\\\\", "/");
  }

  @javax.annotation.Nonnull
  public static byte[] read(@javax.annotation.Nonnull final DataInputStream i, final int s) throws IOException {
    @javax.annotation.Nonnull final byte[] b = new byte[s];
    int pos = 0;
    while (b.length > pos) {
      final int read = i.read(b, pos, b.length - pos);
      if (0 == read) {
        throw new RuntimeException();
      }
      pos += read;
    }
    return b;
  }

  @Nullable
  public static BufferedImage maximumSize(@Nullable final BufferedImage image, int width) {
    if (null == image) return image;
    width = Math.min(image.getWidth(), width);
    if (width == image.getWidth()) return image;
    final int height = image.getHeight() * width / image.getWidth();
    @javax.annotation.Nonnull final BufferedImage rerender = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics gfx = rerender.getGraphics();
    @javax.annotation.Nonnull final RenderingHints hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    ((Graphics2D) gfx).setRenderingHints(hints);
    gfx.drawImage(image, 0, 0, rerender.getWidth(), rerender.getHeight(), null);
    return rerender;
  }

  public static BufferedImage toImage(final Component component) {
    if (null == component) return null;
    try {
      com.simiacryptus.util.Util.layout(component);
      @javax.annotation.Nonnull final BufferedImage img = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
      final Graphics2D g = img.createGraphics();
      g.setColor(component.getForeground());
      g.setFont(component.getFont());
      component.print(g);
      return img;
    } catch (@javax.annotation.Nonnull final Exception e) {
      return null;
    }
  }

  public static CharSequence toInlineImage(final BufferedImage img, final String alt) {
    return com.simiacryptus.util.Util.toInlineImage(new LabeledObject<>(img, alt));
  }

  public static CharSequence toInlineImage(@javax.annotation.Nonnull final LabeledObject<BufferedImage> img) {
    @javax.annotation.Nonnull final ByteArrayOutputStream b = new ByteArrayOutputStream();
    try {
      ImageIO.write(img.data, "png", b);
    } catch (@javax.annotation.Nonnull final RuntimeException e) {
      throw e;
    } catch (@javax.annotation.Nonnull final Exception e) {
      throw new RuntimeException(e);
    }
    final byte[] byteArray = b.toByteArray();
    final CharSequence encode = Base64.getEncoder().encodeToString(byteArray);
    return "<img src=\"data:image/png;base64," + encode + "\" alt=\"" + img.label + "\" />";
  }

  public static <T> Stream<T> toIterator(@javax.annotation.Nonnull final Iterator<T> iterator) {
    return StreamSupport.stream(Spliterators.spliterator(iterator, 1, Spliterator.ORDERED), false);
  }

  public static <T> Stream<T> toStream(@javax.annotation.Nonnull final Iterator<T> iterator) {
    return com.simiacryptus.util.Util.toStream(iterator, 0);
  }

  public static <T> Stream<T> toStream(@javax.annotation.Nonnull final Iterator<T> iterator, final int size) {
    return com.simiacryptus.util.Util.toStream(iterator, size, false);
  }

  public static <T> Stream<T> toStream(@javax.annotation.Nonnull final Iterator<T> iterator, final int size, final boolean parallel) {
    return StreamSupport.stream(Spliterators.spliterator(iterator, size, Spliterator.ORDERED), parallel);
  }

  public static UUID uuid() {
    @javax.annotation.Nonnull String index = Integer.toHexString(com.simiacryptus.util.Util.idcounter.incrementAndGet());
    while (index.length() < 8) {
      index = "0" + index;
    }
    @javax.annotation.Nonnull final String tempId = com.simiacryptus.util.Util.jvmId.substring(0, com.simiacryptus.util.Util.jvmId.length() - index.length()) + index;
    return UUID.fromString(tempId);
  }

  public static void sleep(int i) {
    try {
      Thread.sleep(i);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Nonnull
  public static String dateStr(final String formatStr) {
    return new SimpleDateFormat(formatStr).format(new Date());
  }

  @Nonnull
  public static String stripPrefix(String str, final String prefix) {
    while (str.startsWith(prefix)) {
      str = str.substring(prefix.length());
    }
    return str;
  }

  public static Path pathToFile(final File baseFile, @Nonnull File file) {
    try {
      Path basePath = baseFile.getCanonicalFile().toPath().getParent();
      Path path = file.getCanonicalFile().toPath();
      return basePath.relativize(path);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Base: %s; File: %s", baseFile, file), e);
    }
  }

  @Nonnull
  public static String toString(final Path path) {
    return path.normalize().toString().replaceAll("\\\\", "/");
  }

  public static void runAllParallel(@Nonnull Runnable... runnables) {
    Arrays.stream(runnables)
        .parallel()
        .forEach(Runnable::run);
  }

  public static void runAllSerial(@Nonnull Runnable... runnables) {
    Arrays.stream(runnables)
        .forEach(Runnable::run);
  }

  public static String toString(@Nonnull Consumer<PrintStream> fn) {
    @Nonnull java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
    try (@Nonnull PrintStream out = new PrintStream(buffer)) {
      fn.accept(out);
    }
    return new String(buffer.toByteArray(), Charset.forName("UTF-8"));
  }

  public static String toString(final StackTraceElement[] stack) {
    return toString(stack, "\n");
  }

  public static String toString(final StackTraceElement[] stack, final CharSequence delimiter) {
    return Arrays.stream(stack).map(x -> x.getFileName() + ":" + x.getLineNumber()).reduce((a, b) -> a + delimiter + b).orElse("");
  }

  public static StackTraceElement[] getStackTrace() {
    return getStackTrace(4);
  }

  public static StackTraceElement[] getStackTrace(final int skip) {
    return Arrays.stream(Thread.currentThread().getStackTrace()).skip(skip)
        .filter(x -> x.getClassName().startsWith("com.simiacryptus."))
        .limit(500)
        .toArray(i -> new StackTraceElement[i]);
  }

  public static CharSequence getCaller() {
    return toString(getStackTrace(4));
  }

  public static float[] getFloats(double[] doubles) {
    float[] floats = new float[doubles.length];
    for (int i = 0; i < doubles.length; i++) {
      floats[i] = (float) doubles[i];
    }
    return floats;
  }

  public static double[] getDoubles(float[] floats) {
    double[] doubles = new double[floats.length];
    for (int i = 0; i < floats.length; i++) {
      doubles[i] = floats[i];
    }
    return doubles;
  }

  public static long[] toLong(int[] ints) {
    long[] longs = new long[ints.length];
    for (int i = 0; i < ints.length; i++) {
      longs[i] = ints[i];
    }
    return longs;
  }

  public static int[] toInt(long[] longs) {
    int[] ints = new int[longs.length];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = (int) longs[i];
    }
    return ints;
  }

  public static String toString(Throwable e) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (PrintStream printStream = new PrintStream(out)) {
      e.printStackTrace(printStream);
    }
    try {
      return out.toString("UTF-8");
    } catch (UnsupportedEncodingException e1) {
      throw new RuntimeException(e1);
    }
  }
}
