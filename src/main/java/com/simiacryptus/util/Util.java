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
import com.simiacryptus.util.io.BinaryChunkIterator;
import com.simiacryptus.util.io.TeeInputStream;
import com.simiacryptus.util.test.LabeledObject;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class Util {

  public static final ThreadLocal<Random> R = new ThreadLocal<Random>() {
    public final Random r = new Random(RefSystem.nanoTime());

    @Nonnull
    @Override
    protected Random initialValue() {
      return new Random(r.nextLong());
    }
  };
  private static final Logger log = LoggerFactory.getLogger(Util.class);
  private static final AtomicInteger idcounter = new AtomicInteger(0);
  private static final String jvmId = UUID.randomUUID().toString();

  public static CharSequence getCaller() {
    return toString(getStackTrace(4));
  }

  @Nonnull
  public static StackTraceElement[] getStackTrace() {
    return getStackTrace(4);
  }

  public static void add(@Nonnull final DoubleSupplier f, @Nonnull final double[] data) {
    for (int i = 0; i < data.length; i++) {
      data[i] += f.getAsDouble();
    }
  }

  public static RefStream<byte[]> binaryStream(final String path, @Nonnull final String name, final int skip,
                                               final int recordSize) throws IOException {
    @Nonnull final File file = new File(path, name);
    final byte[] fileData = IOUtils
        .toByteArray(new BufferedInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)))));
    @Nonnull final DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
    in.skip(skip);
    return Util.toIterator(new BinaryChunkIterator(in, recordSize));
  }

  public static <F, T> Function<F, T> cache(@Nonnull final Function<F, T> inner) {
    RefConcurrentHashMap<F, T> cache = new RefConcurrentHashMap<>();
    return RefUtil.wrapInterface(key -> cache.computeIfAbsent(key, inner::apply), cache, inner);
  }

  @Nonnull
  public static InputStream cacheLocal(@Nonnull String file, @Nonnull URI url) throws IOException {
    return cacheLocal(file, getStreamSupplier(url));
  }

  @Nonnull
  public static InputStream cacheLocal(@Nonnull String file, @Nonnull Supplier<InputStream> fn) throws FileNotFoundException {
    File f = new File(file);
    if (f.exists()) {
      return new FileInputStream(f);
    } else {
      FileOutputStream cache = new FileOutputStream(file);
      return new TeeInputStream(fn.get(), cache);
    }
  }

  @Nonnull
  public static Supplier<InputStream> getStreamSupplier(@Nonnull URI url) {
    return () -> {
      TrustManager[] trustManagers = {new X509TrustManager() {
        @Nonnull
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }};
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
      } catch (@Nonnull KeyManagementException | NoSuchAlgorithmException | IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @Nonnull
  public static InputStream cacheStream(@Nonnull final String url, @Nonnull final String file)
      throws IOException, NoSuchAlgorithmException, KeyManagementException {
    if (new File(file).exists()) {
      return new FileInputStream(file);
    } else {
      return new TeeInputStream(get(url), new FileOutputStream(file));
    }
  }

  @Nonnull
  public static File cacheFile(@Nonnull final String url, @Nonnull final String file)
      throws IOException, NoSuchAlgorithmException, KeyManagementException {
    final File fileLoc = new File(file).getCanonicalFile().getAbsoluteFile();
    if (!fileLoc.exists()) {
      log.info(RefString.format("Downloading %s to %s", url, fileLoc));
      IOUtils.copy(get(url), new FileOutputStream(fileLoc));
      log.info(RefString.format("Finished Download of %s to %s", url, fileLoc));
    }
    return fileLoc;
  }

  public static InputStream get(@Nonnull String url)
      throws NoSuchAlgorithmException, KeyManagementException, IOException {
    @Nonnull final TrustManager[] trustManagers = {new X509TrustManager() {
      @Nonnull
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }

      @Override
      public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
      }

      @Override
      public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
      }
    }};
    @Nonnull final SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, trustManagers, null);
    final SSLSocketFactory sslFactory = ctx.getSocketFactory();
    final URLConnection urlConnection = new URL(url).openConnection();
    if (urlConnection instanceof HttpsURLConnection) {
      @Nonnull final HttpsURLConnection conn = (HttpsURLConnection) urlConnection;
      conn.setSSLSocketFactory(sslFactory);
      conn.setRequestMethod("GET");
    }
    return urlConnection.getInputStream();
  }

  @Nonnull
  public static InputStream cacheStream(@Nonnull final URI url)
      throws IOException, NoSuchAlgorithmException, KeyManagementException {
    return Util.cacheStream(url.toString(), new File(url.getPath()).getName());
  }

  @Nonnull
  public static File cacheFile(@Nonnull final URI url)
      throws IOException, NoSuchAlgorithmException, KeyManagementException {
    return Util.cacheFile(url.toString(), new File(url.getPath()).getName());
  }

  @Nonnull
  public static CharSequence[] currentStack() {
    return Stream.of(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString).toArray(i -> new CharSequence[i]);
  }

  @Nonnull
  public static TemporalUnit cvt(@Nonnull final TimeUnit units) {
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

  public static void layout(@Nonnull final Component c) {
    c.doLayout();
    if (c instanceof Container) {
      RefArrays.stream(((Container) c).getComponents()).forEach(c1 -> layout(c1));
    }
  }

  public static String mkString(@Nonnull final CharSequence separator, final CharSequence... strs) {
    RefList<CharSequence> temp_09_0006 = RefArrays.asList(strs);
    String temp_09_0005 = temp_09_0006.stream().collect(RefCollectors.joining(separator));
    temp_09_0006.freeRef();
    return temp_09_0005;
  }

  @Nonnull
  public static String pathTo(@Nonnull final File from, @Nonnull final File to) {
    return from.toPath().relativize(to.toPath()).toString().replaceAll("\\\\", "/");
  }

  @Nonnull
  public static byte[] read(@Nonnull final DataInputStream i, final int s) throws IOException {
    @Nonnull final byte[] b = new byte[s];
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
    if (null == image)
      return null;
    width = Math.min(image.getWidth(), width);
    if (width == image.getWidth())
      return image;
    final int height = image.getHeight() * width / image.getWidth();
    @Nonnull final BufferedImage rerender = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics gfx = rerender.getGraphics();
    @Nonnull final RenderingHints hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    ((Graphics2D) gfx).setRenderingHints(hints);
    gfx.drawImage(image, 0, 0, rerender.getWidth(), rerender.getHeight(), null);
    return rerender;
  }

  @Nullable
  public static BufferedImage toImage(@Nullable final Component component) {
    if (null == component)
      return null;
    try {
      Util.layout(component);
      @Nonnull final BufferedImage img = new BufferedImage(component.getWidth(), component.getHeight(),
          BufferedImage.TYPE_INT_RGB);
      final Graphics2D g = img.createGraphics();
      g.setColor(component.getForeground());
      g.setFont(component.getFont());
      component.print(g);
      return img;
    } catch (@Nonnull final Exception e) {
      return null;
    }
  }

  @Nonnull
  public static CharSequence toInlineImage(final BufferedImage img, final String alt) {
    return Util.toInlineImage(new LabeledObject<>(img, alt));
  }

  @Nonnull
  public static CharSequence toInlineImage(@Nonnull final LabeledObject<BufferedImage> img) {
    @Nonnull final ByteArrayOutputStream b = new ByteArrayOutputStream();
    try {
      ImageIO.write(img.data, "png", b);
    } catch (@Nonnull final RuntimeException e) {
      RefUtil.freeRef(img);
      throw e;
    } catch (@Nonnull final Exception e) {
      RefUtil.freeRef(img);
      throw new RuntimeException(e);
    }
    final byte[] byteArray = b.toByteArray();
    final CharSequence encode = Base64.getEncoder().encodeToString(byteArray);
    String s = "<img src=\"data:image/png;base64," + encode + "\" alt=\"" + img.label + "\" />";
    RefUtil.freeRef(img);
    return s;
  }

  public static <T> RefStream<T> toIterator(@Nonnull final RefIteratorBase<T> iterator) {
    return RefStreamSupport
        .stream(RefSpliterators.spliterator(iterator, 1, Spliterator.ORDERED), false);
  }

  public static <T> RefStream<T> toStream(@Nonnull final RefIteratorBase<T> iterator) {
    return Util.toStream(iterator, 0);
  }

  public static <T> RefStream<T> toStream(@Nonnull final @RefAware RefIteratorBase<T> iterator, final int size) {
    return Util.toStream(iterator, size, false);
  }

  public static <T> RefStream<T> toStream(@Nonnull final @RefAware RefIteratorBase<T> iterator, final int size,
                                          final boolean parallel) {
    return RefStreamSupport
        .stream(RefSpliterators.spliterator(iterator, size, Spliterator.ORDERED), parallel);
  }

  @Nonnull
  public static UUID uuid() {
    @Nonnull
    String index = Integer.toHexString(Util.idcounter.incrementAndGet());
    while (index.length() < 8) {
      index = "0" + index;
    }
    @Nonnull final String tempId = Util.jvmId.substring(0, Util.jvmId.length() - index.length()) + index;
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
  public static String dateStr(@Nonnull final String formatStr) {
    return new SimpleDateFormat(formatStr).format(new Date());
  }

  @Nonnull
  public static String stripPrefix(@Nonnull String str, @Nonnull final String prefix) {
    while (str.startsWith(prefix)) {
      str = str.substring(prefix.length());
    }
    return str;
  }

  @Nonnull
  public static Path pathToFile(@Nonnull final File baseFile, @Nonnull File file) {
    try {
      Path basePath = baseFile.getCanonicalFile().toPath().getParent();
      Path path = file.getCanonicalFile().toPath();
      return basePath.relativize(path);
    } catch (IOException e) {
      throw new RuntimeException(RefString.format("Base: %s; File: %s", baseFile, file), e);
    }
  }

  @Nonnull
  public static String toString(@Nonnull final Path path) {
    return path.normalize().toString().replaceAll("\\\\", "/");
  }

  public static void runAllParallel(@RefAware @Nonnull Runnable... runnables) {
    RefArrays.stream(runnables).parallel().forEach(runnable -> {
      try {
        runnable.run();
      } finally {
        RefUtil.freeRef(runnable);
      }
    });
  }

  public static void runAllSerial(@RefAware @Nonnull Runnable... runnables) {
    RefArrays.stream(runnables).forEach(runnable -> {
      try {
        runnable.run();
      } finally {
        RefUtil.freeRef(runnable);
      }
    });
  }

  @Nonnull
  public static String toString(@Nonnull RefConsumer<PrintStream> fn) {
    @Nonnull
    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
    try (@Nonnull
         PrintStream out = new PrintStream(buffer)) {
      fn.accept(out);
    }
    return new String(buffer.toByteArray(), Charset.forName("UTF-8"));
  }

  public static String toString(@Nonnull final StackTraceElement[] stack) {
    return toString(stack, "\n");
  }

  public static String toString(@Nonnull final StackTraceElement[] stack, final CharSequence delimiter) {
    return Arrays.stream(stack).map(x -> x.getFileName() + ":" + x.getLineNumber())
        .reduce((a, b) -> a + delimiter + b).orElse("");
  }

  @Nonnull
  public static StackTraceElement[] getStackTrace(final int skip) {
    return Arrays.stream(Thread.currentThread().getStackTrace()).skip(skip)
        .filter(x -> x.getClassName().startsWith("com.simiacryptus.")).limit(500)
        .toArray(i -> new StackTraceElement[i]);
  }

  @Nonnull
  public static float[] getFloats(@Nonnull double[] doubles) {
    float[] floats = new float[doubles.length];
    for (int i = 0; i < doubles.length; i++) {
      floats[i] = (float) doubles[i];
    }
    return floats;
  }

  @Nonnull
  public static double[] getDoubles(@Nonnull float[] floats) {
    double[] doubles = new double[floats.length];
    for (int i = 0; i < floats.length; i++) {
      doubles[i] = floats[i];
    }
    return doubles;
  }

  @Nonnull
  public static long[] toLong(@Nonnull int[] ints) {
    long[] longs = new long[ints.length];
    for (int i = 0; i < ints.length; i++) {
      longs[i] = ints[i];
    }
    return longs;
  }

  @Nonnull
  public static int[] toInt(@Nonnull long[] longs) {
    int[] ints = new int[longs.length];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = (int) longs[i];
    }
    return ints;
  }

  public static String toString(@Nonnull Throwable e) {
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
