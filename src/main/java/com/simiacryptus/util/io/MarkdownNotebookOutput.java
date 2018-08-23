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

import com.simiacryptus.util.FileHTTPD;
import com.simiacryptus.util.FileNanoHTTPD;
import com.simiacryptus.util.TableOutput;
import com.simiacryptus.util.Util;
import com.simiacryptus.util.lang.CodeUtil;
import com.simiacryptus.util.lang.TimedResult;
import com.simiacryptus.util.lang.UncheckedSupplier;
import com.simiacryptus.util.test.SysOutInterceptor;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ext.escaped.character.EscapedCharacterExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
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
import java.io.Closeable;
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.simiacryptus.util.Util.*;

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
  private final boolean autobrowse;
  private int maxImageSize = 1600;
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
  private final String baseCodeUrl = CodeUtil.getGitBase();
  public static final Random random = new Random();
  
  /**
   * Instantiates a new Markdown notebook output.
   *
   * @param reportFile the file name
   * @param name       the name
   * @throws FileNotFoundException the file not found exception
   */
  public MarkdownNotebookOutput(@Nonnull final File reportFile, final String name, boolean autobrowse) throws FileNotFoundException {
    this(
      reportFile,
      name,
      random.nextInt(2 * 1024) + 2 * 1024,
      autobrowse
    );
  }
  
  /**
   * Instantiates a new Markdown notebook output.
   *
   * @param reportFile the file name
   * @param name       the name
   * @param httpPort   the http port
   * @param autobrowse
   * @throws FileNotFoundException the file not found exception
   */
  public MarkdownNotebookOutput(
    @javax.annotation.Nonnull final File reportFile,
    final String name,
    final int httpPort,
    final boolean autobrowse
  ) throws FileNotFoundException
  {
    this.name = name;
    reportFile.getAbsoluteFile().getParentFile().mkdirs();
    primaryOut = new PrintStream(new FileOutputStream(reportFile));
    this.reportFile = reportFile;
    this.httpd = httpPort <= 0 ? null : new FileNanoHTTPD(reportFile.getParentFile(), httpPort);
    if (null != this.httpd) this.httpd.addHandler("", "text/html", out -> {
      try {
        writeHtmlAndPdf(getRoot(), testName());
        try (FileInputStream input = new FileInputStream(new File(getRoot(), testName() + ".html"))) {
          IOUtils.copy(input, out);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    if (null != this.httpd) this.httpd.addHandler("pdf", "application/pdf", out -> {
      try {
        writeHtmlAndPdf(getRoot(), testName());
        try (FileInputStream input = new FileInputStream(new File(getRoot(), testName() + ".pdf"))) {
          IOUtils.copy(input, out);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    if (null != this.httpd) this.httpd.addHandler("shutdown", "text/plain", out -> {
      try (PrintStream printStream = new PrintStream(out)) {
        printStream.print("Closing...");
        try {
          close();
          printStream.print("Done");
        } catch (IOException e) {
          e.printStackTrace(printStream);
        }
      }
      System.exit(0);
    });
    this.autobrowse = autobrowse;
    try {
      log.info(String.format("Serving %s at http://localhost:%d", reportFile.getAbsoluteFile(), httpPort));
      if (null != this.httpd) this.httpd.init();
      if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        if (null != this.httpd) new Thread(() -> {
          try {
            while (!httpd.isAlive()) Thread.sleep(100);
            if (isAutobrowse()) Desktop.getDesktop().browse(new URI(String.format("http://localhost:%d", httpPort)));
          } catch (InterruptedException | IOException | URISyntaxException e) {
            e.printStackTrace();
          }
        }).start();
        onComplete(file -> {
          try {
            if (isAutobrowse()) Desktop.getDesktop().browse(new File(file, testName() + ".html").toURI());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      }
      if (null != this.httpd) onComplete(file -> {
        httpd.stop();
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  /**
   * Wrap frontmatter consumer.
   *
   * @param fn the fn
   * @return the consumer
   */
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
   * @param path       the path
   * @param autobrowse
   * @return the markdown notebook output
   */
  public static NotebookOutput get(File path, final boolean autobrowse) {
    try {
      StackTraceElement callingFrame = Thread.currentThread().getStackTrace()[2];
      String methodName = callingFrame.getMethodName();
      path.getAbsoluteFile().getParentFile().mkdirs();
      return new MarkdownNotebookOutput(path, methodName, autobrowse);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Gets exception string.
   *
   * @param e the e
   * @return the exception string
   */
  @Nonnull
  public static CharSequence getExceptionString(Throwable e) {
    if (e instanceof RuntimeException && e.getCause() != null && e.getCause() != e)
      return getExceptionString(e.getCause());
    if (e.getCause() != null && e.getCause() != e)
      return e.getClass().getSimpleName() + " / " + getExceptionString(e.getCause());
    return e.getClass().getSimpleName();
  }
  
  public static NotebookOutput get(final String s) {
    return get(new File(s), true);
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
      try {
        fn.accept(root);
      } catch (Throwable e) {
        log.info("Error closing log", e);
      }
    });
  }
  
  /**
   * Gets root.
   *
   * @return the root
   */
  @Nonnull
  public File getRoot() {
    return new File(reportFile.getParent());
  }
  
  /**
   * Test name string.
   *
   * @return the string
   */
  public String testName() {
    String[] split = reportFile.getName().split(".");
    return 0 == split.length ? reportFile.getName() : split[0];
  }
  
  /**
   * Write zip.
   *
   * @param root     the root
   * @param baseName the base name
   * @throws IOException the io exception
   */
  public void writeZip(final File root, final String baseName) throws IOException {
    try (@Nonnull ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(root, baseName + ".zip")))) {
      writeArchive(root, root, out, file -> !file.getName().equals(baseName + ".zip") && !file.getName().endsWith(".pdf"));
    }
  }
  
  /**
   * On complete markdown notebook output.
   *
   * @param tasks the tasks
   * @return the markdown notebook output
   */
  @Override
  @Nonnull
  public NotebookOutput onComplete(Consumer<File>... tasks) {
    Arrays.stream(tasks).forEach(onComplete::add);
    return this;
  }
  
  /**
   * To string string.
   *
   * @param list the list
   * @return the string
   */
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
  
  /**
   * Write archive.
   *
   * @param root   the root
   * @param dir    the dir
   * @param out    the out
   * @param filter the filter
   */
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
  
  /**
   * Write markdown with frontmatter.
   *
   * @param out the out
   */
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
  
  /**
   * Write html and pdf.
   *
   * @param root     the root
   * @param baseName the base name
   * @throws IOException the io exception
   */
  public void writeHtmlAndPdf(final File root, final CharSequence baseName) throws IOException {
    MutableDataSet options = new MutableDataSet();
    List<Extension> extensions = Arrays.asList(
      TablesExtension.create(),
      SubscriptExtension.create(),
      EscapedCharacterExtension.create()
    );
    Parser parser = Parser.builder(options).extensions(extensions).build();
    HtmlRenderer renderer = HtmlRenderer.builder(options).extensions(extensions).escapeHtml(false).indentSize(2).softBreak("\n").build();
    File htmlFile = new File(root, baseName + ".html");
    String html = renderer.render(parser.parse(toString(toc) + "\n\n" + toString(markdownData)));
    html = "<html><body>" + html + "</body></html>";
    try (FileOutputStream out = new FileOutputStream(htmlFile)) {
      IOUtils.write(html, out, Charset.forName("UTF-8"));
    }
    try (FileOutputStream out = new FileOutputStream(new File(root, baseName + ".pdf"))) {
      PdfConverterExtension.exportToPdf(out, html, htmlFile.getAbsoluteFile().toURI().toString(), options);
    }
  }
  
  @javax.annotation.Nonnull
  @Override
  public OutputStream file(@javax.annotation.Nonnull final CharSequence name) {
    try {
      return new FileOutputStream(resolveResource(name));
    } catch (@javax.annotation.Nonnull final FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Nonnull
  public File resolveResource(@Nonnull final CharSequence name) {
    return new File(getResourceDir(), Util.stripPrefix(name.toString(), "etc/"));
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
   * Gets resource dir.
   *
   * @return the resource dir
   */
  @javax.annotation.Nonnull
  public File getResourceDir() {
    @javax.annotation.Nonnull final File etc = new File(reportFile.getParentFile(), "etc").getAbsoluteFile();
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
  public String png(@Nullable final BufferedImage rawImage, final CharSequence caption) {
    if (null == rawImage) return "";
    @Nonnull final File file = pngFile(rawImage, new File(getResourceDir(), name + "." + ++MarkdownNotebookOutput.imageNumber + ".png"));
    return anchor(anchorId()) + "![" + caption + "](etc/" + file.getName() + ")";
  }
  
  @Override
  @Nonnull
  public File pngFile(@Nonnull final BufferedImage rawImage, final File file) {
    @Nullable final BufferedImage stdImage = Util.maximumSize(rawImage, getMaxImageSize());
    try {
      if (stdImage != rawImage) {
        @Nonnull final String rawName = file.getName().replace(".png", "_raw.png");
        ImageIO.write(rawImage, "png", new File(file.getParent(), rawName));
      }
      ImageIO.write(stdImage, "png", file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return file;
  }
  
  @javax.annotation.Nonnull
  @Override
  public String jpg(@Nullable final BufferedImage rawImage, final CharSequence caption) {
    if (null == rawImage) return "";
    @Nonnull final File file = jpgFile(rawImage, new File(getResourceDir(), name + "." + ++MarkdownNotebookOutput.imageNumber + ".jpg"));
    return anchor(anchorId()) + "![" + caption + "](etc/" + file.getName() + ")";
  }
  
  @Override
  @Nonnull
  public File jpgFile(@Nonnull final BufferedImage rawImage, final File file) {
    @Nullable final BufferedImage stdImage = Util.maximumSize(rawImage, getMaxImageSize());
    try {
      if (stdImage != rawImage) {
        @Nonnull final String rawName = file.getName().replace(".jpg", "_raw.jpg");
        ImageIO.write(rawImage, "jpg", new File(file.getParent(), rawName));
      }
      ImageIO.write(stdImage, "jpg", file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return file;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public <T> T eval(@javax.annotation.Nonnull final UncheckedSupplier<T> fn, final int maxLog, final int framesNo) {
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
      CharSequence codeLink;
      try {
        codeLink = pathToGitResource(CodeUtil.findFile(callingFrame));
      } catch (Throwable e) {
        String[] split = callingFrame.getClassName().split("\\.");
        String packagePath = Arrays.asList(split).subList(0, split.length - 1).stream().reduce((a, b) -> a + "/" + b).get();
        codeLink = this.baseCodeUrl + "/src/main/java/" + packagePath + "/" + callingFrame.getFileName();
      }
      out(anchor(anchorId()) + "Code from [%s:%s](%s#L%s) executed in %.2f seconds (%.3f gc): ",
          callingFrame.getFileName(), callingFrame.getLineNumber(),
          codeLink, callingFrame.getLineNumber(), result.obj.seconds(), result.obj.gc_seconds()
      );
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
          str = png(Util.toImage((Component) eval), "Result");
          escape = false;
        }
        else if (eval instanceof BufferedImage) {
          str = png((BufferedImage) eval, "Result");
          escape = false;
        }
        else if (eval instanceof TableOutput) {
          str = ((TableOutput) eval).toMarkdownTable();
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
  public CharSequence link(@javax.annotation.Nonnull final File file, final CharSequence text) {
    return "[" + text + "](" + pathToResource(file) + ")";
  }
  
  /**
   * Code file string.
   *
   * @param file the file
   * @return the string
   */
  public CharSequence pathToResource(@javax.annotation.Nonnull File file) {
    return stripPrefix(Util.toString(pathToFile(reportFile, file)), "/");
  }
  
  public CharSequence pathToGitResource(@javax.annotation.Nonnull File file) {
    Path path = pathToFile(new File("."), file);
    String pathSlash = Util.toString(path);
    if (null != baseCodeUrl) {
      try {
        URI resolve = new URI(baseCodeUrl).resolve(pathSlash);
        return resolve.normalize().toString();
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      return pathSlash;
    }
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
        logSrc.length() - 2 * maxLog
      ) + logSrc.substring(logSrc.length() - maxLog);
    }
    return logSrc;
  }
  
  public int getMaxOutSize() {
    return MAX_OUTPUT;
  }
  
  public FileHTTPD getHttpd() {
    return (null != this.httpd) ? httpd : new FileHTTPD() {
      @Override
      public Closeable addHandler(final CharSequence path, final String mimeType, @Nonnull final Consumer<OutputStream> logic) {
        return () -> {};
      }
    };
  }
  
  @Override
  public <T> T subreport(String reportName, Function<NotebookOutput, T> fn) {
    MarkdownNotebookOutput outer = this;
    try {
      File root = getRoot();
      File subreportFile = new File(root, reportName);
      MarkdownNotebookOutput subreport = new MarkdownNotebookOutput(subreportFile, reportName, -1, false) {
        @Override
        public FileHTTPD getHttpd() {
          return outer.getHttpd();
        }
  
        @Override
        public void writeZip(final File root, final String baseName) {}
      };
      try {
        outer.p("Subreport: %s %s %s %s", reportName,
                outer.link(subreportFile, "markdown"),
                outer.link(new File(root, reportName + ".html"), "html"),
                outer.link(new File(root, reportName + ".pdf"), "pdf")
        );
        getHttpd().addHandler(reportName + ".html", "text/html", out -> {
          try {
            subreport.writeHtmlAndPdf(root, subreport.getName());
            try (FileInputStream input = new FileInputStream(new File(root, subreport.getName() + ".html"))) {
              IOUtils.copy(input, out);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
        getHttpd().addHandler(reportName + ".pdf", "application/pdf", out -> {
          try {
            subreport.writeHtmlAndPdf(root, subreport.getName());
            try (FileInputStream input = new FileInputStream(new File(root, subreport.getName() + ".pdf"))) {
              IOUtils.copy(input, out);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
        try {
          return fn.apply(subreport);
        } catch (Throwable e) {
          return subreport.eval(() -> {
            throw e;
          });
        }
      } finally {
        try {
          subreport.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  public boolean isAutobrowse() {
    return autobrowse;
  }
  
  public int getMaxImageSize() {
    return maxImageSize;
  }
  
  public NotebookOutput setMaxImageSize(int maxImageSize) {
    this.maxImageSize = maxImageSize;
    return this;
  }
}
