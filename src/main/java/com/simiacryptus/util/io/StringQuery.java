package com.simiacryptus.util.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.simiacryptus.util.FileNanoHTTPD;
import com.simiacryptus.util.NanoHTTPD;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public abstract class StringQuery<T> {

  public static class SimpleStringQuery extends StringQuery<String> {

    public SimpleStringQuery(MarkdownNotebookOutput log) {
      super(log);
    }

    @Override
    protected String fromString(String text) throws IOException {
      return text;
    }

    @Override
    protected String getString(String value) throws JsonProcessingException {
      return value;
    }
  }

  protected static final Logger logger = LoggerFactory.getLogger(JsonQuery.class);
  protected final String id = "input_" + UUID.randomUUID().toString() + ".html";
  protected final Closeable handler_get;
  protected final Semaphore done = new Semaphore(0);
  protected final Closeable handler_post;
  final MarkdownNotebookOutput log;
  protected T value = null;
  String height1 = "200px";
  String height2 = "240px";
  String width = "100%";
  String formVar = "data";

  public StringQuery(MarkdownNotebookOutput log) {
    this.log = log;
    FileNanoHTTPD httpd = (FileNanoHTTPD) this.log.getHttpd();
    this.handler_get = httpd.addGET(id, "text/html", out -> {
      try {
        if (done.tryAcquire()) {
          done.release();
          IOUtil.writeString(getRead(), out);
        } else {
          IOUtil.writeString(getWrite(), out);
        }
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    });
    this.handler_post = httpd.addPOST(id, request -> {
      String responseHtml;
      try {
        Map<String, String> parms = request.getParms();
        HashMap<String, String> files = new HashMap<>();
        request.parseBody(files);
        String text = parms.get(formVar);
        logger.info("Json input: " + text);
        value = fromString(text);
        done.release();
        responseHtml = getRead();
        FileUtils.write(new File(log.getRoot(), id), responseHtml, "UTF-8");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      byte[] bytes = responseHtml.getBytes();
      return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", new ByteArrayInputStream(bytes), bytes.length);
    });
  }

  protected String getWrite() throws JsonProcessingException {
    return "<html><body style=\"margin: 0;\">" +
        "<form action=\"" + id + "\" method=\"POST\">" +
        "<textarea name=\"" + formVar + "\" style=\"margin: 0px; width: " + width + "; height: " + height1 + ";\">" + getString(value) + "</textarea>" +
        "<br/><input type=\"submit\">" +
        "</form></body></html>";
  }

  private String getRead() throws JsonProcessingException {
    return "<html><body style=\"margin: 0;\">" +
        "<textarea name=\"" + formVar + "\" style=\"margin: 0px; width: " + width + "; height: " + height2 + ";\">" + getString(value) + "</textarea>" +
        "</body></html>";
  }

  protected abstract T fromString(String text) throws IOException;

  protected abstract String getString(T value) throws JsonProcessingException;

  public StringQuery<T> print(@Nonnull T initial) {
    value = initial;
    log.p("<iframe src=" + id + " style=\"margin: 0px; width: 100%; height: " + height2 + ";\"></iframe>");
    return this;
  }

  public T get() {
    try {
      done.acquire();
      done.release();
      return value;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public T get(long t, TimeUnit u) {
    try {
      if (done.tryAcquire(t, u)) {
        done.release();
        return value;
      } else {
        return value;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    if (null != handler_get) handler_get.close();
    if (null != handler_post) handler_post.close();
    super.finalize();
  }
}