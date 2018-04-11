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

package com.simiacryptus.util.io;

import com.simiacryptus.util.FileNanoHTTPD;
import com.simiacryptus.util.TableOutput;
import com.simiacryptus.util.Util;
import com.simiacryptus.util.lang.CodeUtil;
import com.simiacryptus.util.lang.TimedResult;
import com.simiacryptus.util.lang.UncheckedSupplier;
import com.simiacryptus.util.test.SysOutInterceptor;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The type Markdown notebook output.
 */
public class MarkdownNotebookOutput implements NotebookOutput {
  /**
   * The Logger.
   */
  static final Logger log = LoggerFactory.getLogger(com.simiacryptus.util.io.MarkdownNotebookOutput.class);
  /**
   * The constant MAX_OUTPUT.
   */
  public static int MAX_OUTPUT = 4 * 1024;
  private static int excerptNumber = 0;
  private static int imageNumber = 0;
  @javax.annotation.Nonnull
  private final File reportFile;
  private final String name;
  @javax.annotation.Nonnull
  private final PrintStream primaryOut;
  private final List<CharSequence> markdownData = new ArrayList<>();
  private final List<Consumer<File>> onComplete = new ArrayList<>();
  private final Map<CharSequence, CharSequence> frontMatter = new HashMap<>();
  private final FileNanoHTTPD httpd;
  /**
   * The Toc.
   */
  @javax.annotation.Nonnull
  public List<CharSequence> toc = new ArrayList<>();
  /**
   * The Anchor.
   */
  int anchor = 0;
  @Nullable
  private String baseCodeUrl = null;
  
  /**
   * Instantiates a new Markdown notebook output.
   *
   * @param reportFile the file name
   * @param name       the name
   * @throws FileNotFoundException the file not found exception
   */
  public MarkdownNotebookOutput(@javax.annotation.Nonnull final File reportFile, final String name) throws FileNotFoundException {
    this.name = name;
    primaryOut = new PrintStream(new FileOutputStream(reportFile));
    this.reportFile = reportFile;
    int port = new Random().nextInt(2 * 1024) + 2 * 1024;
    this.httpd = new FileNanoHTTPD(reportFile.getParentFile(), port);
    this.httpd.addHandler("", "text/html", out -> {
      try {
        writeHtmlAndPdf(getRoot(), testName());
        try (FileInputStream input = new FileInputStream(new File(getRoot(), testName() + ".html"))) {
          IOUtils.copy(input, out);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    this.httpd.addHandler("pdf", "application/pdf", out -> {
      try {
        writeHtmlAndPdf(getRoot(), testName());
        try (FileInputStream input = new FileInputStream(new File(getRoot(), testName() + ".pdf"))) {
          IOUtils.copy(input, out);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    try {
      this.httpd.init();
      new Thread(() -> {
        try {
          while (!httpd.isAlive()) Thread.sleep(100);
          Desktop.getDesktop().browse(new URI(String.format("http://localhost:%d", port)));
        } catch (InterruptedException | IOException | URISyntaxException e) {
          e.printStackTrace();
        }
      }).start();
      onComplete(file -> {
        try {
          Desktop.getDesktop().browse(new File(file, testName() + ".html").toURI());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        httpd.stop();
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Nonnull
  public static MarkdownNotebookOutput get(final File path, @Nullable final CharSequence codeBaseUrl) {
    try {
      @Nonnull MarkdownNotebookOutput notebookOutput = new MarkdownNotebookOutput(path, path.getName());
      if (null != codeBaseUrl) {
        try {
          String url = new URI(codeBaseUrl + "/" + path.toPath().toString().replaceAll("\\\\", "/")).normalize().toString();
          notebookOutput.setBaseCodeUrl(url);
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
      }
      return notebookOutput;
    } catch (@Nonnull final FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static Consumer<NotebookOutput> wrapFrontmatter(@Nonnull final Consumer<NotebookOutput> fn) {
    return log -> {
      @Nonnull TimedResult<Void> time = TimedResult.time(() -> {
        try {
          fn.accept(log);
          log.setFrontMatterProperty("result", "OK");
        } catch (Throwable e) {
          log.setFrontMatterProperty("result", getExceptionString(e).toString().replaceAll("\n", "<br/>").trim());
          throw (RuntimeException) (e instanceof RuntimeException ? e : new RuntimeException(e));
        }
      });
      log.setFrontMatterProperty("execution_time", String.format("%.6f", time.timeNanos / 1e9));
    };
  }
  
  /**
   * Get markdown notebook output.
   *
   * @return the markdown notebook output
   */
  public static com.simiacryptus.util.io.MarkdownNotebookOutput get() {
    try {
      final StackTraceElement callingFrame = Thread.currentThread().getStackTrace()[2];
      final String className = callingFrame.getClassName();
      final String methodName = callingFrame.getMethodName();
      @javax.annotation.Nonnull final String fileName = methodName + ".md";
      @javax.annotation.Nonnull File path = new File(Util.mkString(File.separator, "reports", className.replaceAll("\\.", "/").replaceAll("\\$", "/")));
      path = new File(path, fileName);
      path.getParentFile().mkdirs();
      return new com.simiacryptus.util.io.MarkdownNotebookOutput(path, methodName);
    } catch (@javax.annotation.Nonnull final FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Get markdown notebook output.
   *
   * @param source the source
   * @return the markdown notebook output
   */
  public static MarkdownNotebookOutput get(Object source) {
    try {
      StackTraceElement callingFrame = Thread.currentThread().getStackTrace()[2];
      String className = null == source ? callingFrame.getClassName() : source.getClass().getCanonicalName();
      String methodName = callingFrame.getMethodName();
      CharSequence fileName = methodName + ".md";
      File path = new File(Util.mkString(File.separator, "reports", className.replaceAll("\\.", "/").replaceAll("\\$", "/"), fileName));
      path.getParentFile().mkdirs();
      return new MarkdownNotebookOutput(path, methodName);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Nonnull
  public static CharSequence getExceptionString(Throwable e) {
    if (e instanceof RuntimeException && e.getCause() != null && e.getCause() != e)
      return getExceptionString(e.getCause());
    if (e.getCause() != null && e.getCause() != e)
      return e.getClass().getSimpleName() + " / " + getExceptionString(e.getCause());
    return e.getClass().getSimpleName();
  }
  
  @Nonnull
  public MarkdownNotebookOutput onComplete(Consumer<File>... tasks) {
    Arrays.stream(tasks).forEach(onComplete::add);
    return this;
  }
  
  @Override
  public void close() throws IOException {
    if (null != primaryOut) {
      primaryOut.close();
    }
    try (@javax.annotation.Nonnull PrintWriter out = new PrintWriter(new FileOutputStream(reportFile))) {
      writeMarkdownWithFrontmatter(out);
    }
    File root = getRoot();
    writeHtmlAndPdf(root, testName());
    writeZip(root, testName());
    onComplete.stream().forEach(fn -> {
      fn.accept(root);
    });
  }
  
  @Nonnull
  public File getRoot() {
    return new File(reportFile.getParent());
  }
  
  public String testName() {
    String[] split = reportFile.getName().split(".");
    return 0 == split.length ? reportFile.getName() : split[0];
  }
  
  public void writeZip(final File root, final String baseName) throws IOException {
    try (@Nonnull ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(root, baseName + ".zip")))) {
      writeArchive(root, root, out, file -> !file.getName().equals(baseName + ".zip"));
    }
  }
  
  public void writeHtmlAndPdf(final File root, final String baseName) throws IOException {
    MutableDataSet options = new MutableDataSet();
    Parser parser = Parser.builder(options).build();
    HtmlRenderer renderer = HtmlRenderer.builder(options).build();
    File htmlFile = new File(root, baseName + ".html");
    String html = renderer.render(parser.parse(toString(toc) + "\n\n" + toString(markdownData)));
    try (FileOutputStream out = new FileOutputStream(htmlFile)) {
      IOUtils.write(html, out, Charset.forName("UTF-8"));
    }
    try (FileOutputStream out = new FileOutputStream(new File(root, baseName + ".pdf"))) {
      PdfConverterExtension.exportToPdf(out, html, htmlFile.getAbsoluteFile().toURI().toString(), options);
    }
  }
  
  @Nonnull
  public String toString(final List<CharSequence> list) {
    if (list.size() > 0 && list.stream().allMatch(x -> {
      if (x.length() > 1) {
        char c = x.charAt(0);
        return c == ' ' || c == '\t';
      }
      return false;
    })) return toString(list.stream().map(x -> x.subSequence(1, x.length()).toString()).collect(Collectors.toList()));
    else return list.stream().reduce((a, b) -> a + "\n" + b).orElse("").toString();
  }
  
  public void writeArchive(final File root, final File dir, final ZipOutputStream out, final Predicate<? super File> filter) {
    Arrays.stream(dir.listFiles()).filter(filter).forEach(file ->
    {
      if (file.isDirectory()) {
        writeArchive(root, file, out, filter);
      }
      else {
        String absRoot = root.getAbsolutePath();
        String absFile = file.getAbsolutePath();
        String relativeFile = absFile.substring(absRoot.length());
        if (relativeFile.startsWith(File.separator)) relativeFile = relativeFile.substring(1);
        try {
          out.putNextEntry(new ZipEntry(relativeFile));
          IOUtils.copy(new FileInputStream(file), out);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }
  
  public void writeMarkdownWithFrontmatter(final PrintWriter out) {
    if (!frontMatter.isEmpty()) {
      out.println("---");
      frontMatter.forEach((key, value) -> {
        CharSequence escaped = StringEscapeUtils.escapeJson(String.valueOf(value))
          .replaceAll("\n", " ")
          .replaceAll(":", "&#58;")
          .replaceAll("\\{", "\\{")
          .replaceAll("\\}", "\\}");
        out.println(String.format("%s: %s", key, escaped));
      });
      out.println("---");
    }
    toc.forEach(out::println);
    out.print("\n\n");
    markdownData.forEach(out::println);
  }
  
  public void setFrontMatterProperty(CharSequence key, CharSequence value) {
    frontMatter.put(key, value);
  }
  
  @Override
  public CharSequence getFrontMatterProperty(CharSequence key) {
    return frontMatter.get(key);
  }
  
  @Override
  public CharSequence getName() {
    return name;
  }
  
  /**
   * Anchor string.
   *
   * @param anchorId the anchor id
   * @return the string
   */
  public CharSequence anchor(CharSequence anchorId) {
    return String.format("<a id=\"%s\"></a>", anchorId);
  }
  
  /**
   * Anchor id string.
   *
   * @return the string
   */
  public CharSequence anchorId() {
    return String.format("p-%d", anchor++);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public <T> T code(@javax.annotation.Nonnull final UncheckedSupplier<T> fn, final int maxLog, final int framesNo) {
    try {
      final StackTraceElement callingFrame = Thread.currentThread().getStackTrace()[framesNo];
      final String sourceCode = CodeUtil.getInnerText(callingFrame);
      @javax.annotation.Nonnull final SysOutInterceptor.LoggedResult<TimedResult<Object>> result = SysOutInterceptor.withOutput(() -> {
        long priorGcMs = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x -> x.getCollectionTime()).sum();
        final long start = System.nanoTime();
        try {
          @Nullable Object result1 = null;
          try {
            result1 = fn.get();
          } catch (@javax.annotation.Nonnull final RuntimeException e) {
            throw e;
          } catch (@javax.annotation.Nonnull final Exception e) {
            throw new RuntimeException(e);
          }
          long gcTime = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x -> x.getCollectionTime()).sum() - priorGcMs;
          return new TimedResult<Object>(result1, System.nanoTime() - start, gcTime);
        } catch (@javax.annotation.Nonnull final Throwable e) {
          long gcTime = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x -> x.getCollectionTime()).sum() - priorGcMs;
          return new TimedResult<Object>(e, System.nanoTime() - start, gcTime);
        }
      });
      out(anchor(anchorId()) + "Code from [%s:%s](%s#L%s) executed in %.2f seconds (%.3f gc): ",
        callingFrame.getFileName(), callingFrame.getLineNumber(),
        linkTo(CodeUtil.findFile(callingFrame)), callingFrame.getLineNumber(), result.obj.seconds(), result.obj.gc_seconds());
      CharSequence text = sourceCode.replaceAll("\n", "\n  ");
      out("```java");
      out("  " + text);
      out("```");
      
      if (!result.log.isEmpty()) {
        CharSequence summary = summarize(result.log, maxLog).replaceAll("\n", "\n    ").replaceAll("    ~", "");
        out(anchor(anchorId()) + "Logging: ");
        out("```");
        out("    " + summary);
        out("```");
      }
      out("");
      
      final Object eval = result.obj.result;
      if (null != eval) {
        out(anchor(anchorId()) + "Returns: \n");
        String str;
        boolean escape;
        if (eval instanceof Throwable) {
          @javax.annotation.Nonnull final ByteArrayOutputStream out = new ByteArrayOutputStream();
          ((Throwable) eval).printStackTrace(new PrintStream(out));
          str = new String(out.toByteArray(), "UTF-8");
          escape = true;//
        }
        else if (eval instanceof Component) {
          str = image(Util.toImage((Component) eval), "Result");
          escape = false;
        }
        else if (eval instanceof BufferedImage) {
          str = image((BufferedImage) eval, "Result");
          escape = false;
        }
        else if (eval instanceof TableOutput) {
          str = ((TableOutput) eval).toTextTable();
          escape = false;
        }
        else {
          str = eval.toString();
          escape = true;
        }
        @javax.annotation.Nonnull String fmt = escape ? "    " + summarize(str, maxLog).replaceAll("\n", "\n    ").replaceAll("    ~", "") : str;
        if (escape) {
          out("```");
          out(fmt);
          out("```");
        }
        else {
          out(fmt);
        }
        out("\n\n");
        if (eval instanceof RuntimeException) {
          throw ((RuntimeException) result.obj.result);
        }
        if (eval instanceof Throwable) {
          throw new RuntimeException((Throwable) result.obj.result);
        }
      }
      return (T) eval;
    } catch (@javax.annotation.Nonnull final IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @javax.annotation.Nonnull
  @Override
  public OutputStream file(@javax.annotation.Nonnull final CharSequence name) {
    try {
      return new FileOutputStream(new File(getResourceDir(), name.toString()));
    } catch (@javax.annotation.Nonnull final FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  @javax.annotation.Nonnull
  @Override
  public String file(final CharSequence data, final CharSequence caption) {
    return file(data, ++com.simiacryptus.util.io.MarkdownNotebookOutput.excerptNumber + ".txt", caption);
  }
  
  @javax.annotation.Nonnull
  @Override
  public CharSequence file(@javax.annotation.Nonnull byte[] data, @javax.annotation.Nonnull CharSequence filename, CharSequence caption) {
    return file(new String(data, Charset.forName("UTF-8")), filename, caption);
  }
  
  @javax.annotation.Nonnull
  @Override
  public String file(@Nullable final CharSequence data, @javax.annotation.Nonnull final CharSequence fileName, final CharSequence caption) {
    try {
      if (null != data) {
        IOUtils.write(data, new FileOutputStream(new File(getResourceDir(), fileName.toString())), Charset.forName("UTF-8"));
      }
    } catch (@javax.annotation.Nonnull final IOException e) {
      throw new RuntimeException(e);
    }
    return "[" + caption + "](etc/" + fileName + ")";
  }
  
  /**
   * Gets absolute url.
   *
   * @return the absolute url
   */
  @Nullable
  public String getBaseCodeUrl() {
    return baseCodeUrl;
  }
  
  /**
   * Sets absolute url.
   *
   * @param baseCodeUrl the absolute url
   * @return the absolute url
   */
  @javax.annotation.Nonnull
  public com.simiacryptus.util.io.MarkdownNotebookOutput setBaseCodeUrl(final String baseCodeUrl) {
    this.baseCodeUrl = baseCodeUrl;
    return this;
  }
  
  /**
   * Gets resource dir.
   *
   * @return the resource dir
   */
  @javax.annotation.Nonnull
  public File getResourceDir() {
    @javax.annotation.Nonnull final File etc = new File(reportFile.getParentFile(), "etc");
    etc.mkdirs();
    return etc;
  }
  
  @Override
  public void h1(@javax.annotation.Nonnull final CharSequence fmt, final Object... args) {
    CharSequence anchorId = anchorId();
    @javax.annotation.Nonnull CharSequence msg = format(fmt, args);
    toc.add(String.format("1. [%s](#%s)", msg, anchorId));
    out("# " + anchor(anchorId) + msg);
  }
  
  @Override
  public void h2(@javax.annotation.Nonnull final CharSequence fmt, final Object... args) {
    CharSequence anchorId = anchorId();
    @javax.annotation.Nonnull CharSequence msg = format(fmt, args);
    toc.add(String.format("   1. [%s](#%s)", msg, anchorId));
    out("## " + anchor(anchorId) + fmt, args);
  }
  
  @Override
  public void h3(@javax.annotation.Nonnull final CharSequence fmt, final Object... args) {
    CharSequence anchorId = anchorId();
    @javax.annotation.Nonnull CharSequence msg = format(fmt, args);
    toc.add(String.format("      1. [%s](#%s)", msg, anchorId));
    out("### " + anchor(anchorId) + fmt, args);
  }
  
  @javax.annotation.Nonnull
  @Override
  public String image(@Nullable final BufferedImage rawImage, final CharSequence caption) {
    if (null == rawImage) return "";
    new ByteArrayOutputStream();
    final int thisImage = ++com.simiacryptus.util.io.MarkdownNotebookOutput.imageNumber;
    @javax.annotation.Nonnull final String fileName = name + "." + thisImage + ".png";
    @javax.annotation.Nonnull final File file = new File(getResourceDir(), fileName);
    @Nullable final BufferedImage stdImage = Util.resize(rawImage);
    try {
      if (stdImage != rawImage) {
        @javax.annotation.Nonnull final String rawName = name + "_raw." + thisImage + ".png";
        ImageIO.write(rawImage, "png", new File(getResourceDir(), rawName));
      }
      ImageIO.write(stdImage, "png", file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return anchor(anchorId()) + "![" + caption + "](etc/" + file.getName() + ")";
  }
  
  @javax.annotation.Nonnull
  @Override
  public CharSequence link(@javax.annotation.Nonnull final File file, final CharSequence text) {
    try {
      return "[" + text + "](" + codeFile(file) + ")";
    } catch (@javax.annotation.Nonnull final IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Code file string.
   *
   * @param file the file
   * @return the string
   * @throws IOException the io exception
   */
  public CharSequence codeFile(@javax.annotation.Nonnull File file) throws IOException {
    Path path = pathToCodeFile(file);
    if (null != getBaseCodeUrl()) {
      try {
        return new URI(getBaseCodeUrl()).resolve(path.normalize().toString().replaceAll("\\\\", "/")).normalize().toString();
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      return path.normalize().toString().replaceAll("\\\\", "/");
    }
  }
  
  /**
   * Path to code file path.
   *
   * @param file the file
   * @return the path
   * @throws IOException the io exception
   */
  public Path pathToCodeFile(@javax.annotation.Nonnull File file) throws IOException {
    return reportFile.getCanonicalFile().toPath().relativize(file.getCanonicalFile().toPath());
  }
  
  /**
   * Link to string.
   *
   * @param file the file
   * @return the string
   * @throws IOException the io exception
   */
  public CharSequence linkTo(@javax.annotation.Nonnull final File file) throws IOException {
    return codeFile(file);
  }
  
  @Override
  public void out(@javax.annotation.Nonnull final String fmt, final Object... args) {
    @javax.annotation.Nonnull final String msg = format(fmt, args);
    markdownData.add(msg);
    primaryOut.println(msg);
    log.info(msg);
  }
  
  /**
   * Format string.
   *
   * @param fmt  the fmt
   * @param args the args
   * @return the string
   */
  @javax.annotation.Nonnull
  public String format(@javax.annotation.Nonnull CharSequence fmt, @javax.annotation.Nonnull Object... args) {
    return 0 == args.length ? fmt.toString() : String.format(fmt.toString(), args);
  }
  
  @Override
  public void p(final CharSequence fmt, final Object... args) {
    out(anchor(anchorId()).toString() + fmt + "\n", args);
  }
  
  /**
   * Summarize string.
   *
   * @param logSrc the log src
   * @param maxLog the max log
   * @return the string
   */
  @javax.annotation.Nonnull
  public String summarize(@javax.annotation.Nonnull String logSrc, final int maxLog) {
    if (logSrc.length() > maxLog * 2) {
      @javax.annotation.Nonnull final String prefix = logSrc.substring(0, maxLog);
      logSrc = prefix + String.format(
        (prefix.endsWith("\n") ? "" : "\n") + "~```\n~..." + file(logSrc, "skipping %s bytes") + "...\n~```\n",
        logSrc.length() - 2 * maxLog) + logSrc.substring(logSrc.length() - maxLog);
    }
    return logSrc;
  }
  
  public int getMaxOutSize() {
    return MAX_OUTPUT;
  }
  
  public FileNanoHTTPD getHttpd() {
    return httpd;
  }
}
