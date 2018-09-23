package com.simiacryptus.notebook;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Function;

class NullHTTPD implements FileHTTPD {
  @Override
  public Closeable addGET(CharSequence path, Function<NanoHTTPD.IHTTPSession, NanoHTTPD.Response> value) {
    return () -> {
    };
  }

  @Override
  public Closeable addPOST(CharSequence path, Function<NanoHTTPD.IHTTPSession, NanoHTTPD.Response> value) {
    return () -> {
    };
  }

  @Override
  public Closeable addGET(final CharSequence path, final String mimeType, @Nonnull final Consumer<OutputStream> logic) {
    return () -> {
    };
  }
}
