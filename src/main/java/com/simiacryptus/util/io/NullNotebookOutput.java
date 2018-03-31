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

import com.simiacryptus.util.lang.UncheckedSupplier;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * The type Null notebook output.
 */
public class NullNotebookOutput implements NotebookOutput {
  private final String name;
  
  /**
   * Instantiates a new Null notebook output.
   *
   * @param name the name
   */
  public NullNotebookOutput(String name) {this.name = name;}
  
  /**
   * Instantiates a new Null notebook output.
   */
  public NullNotebookOutput() {
    this("null");
  }
  
  @Override
  public void close() {
  
  }
  
  @Override
  public <T> T code(@javax.annotation.Nonnull UncheckedSupplier<T> fn, int maxLog, int framesNo) {
    try {
      return fn.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
  }
  
  @javax.annotation.Nonnull
  @Override
  public OutputStream file(@javax.annotation.Nonnull CharSequence name) {
    try {
      return new FileOutputStream(name.toString());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  @javax.annotation.Nonnull
  @Override
  public String file(@javax.annotation.Nonnull CharSequence data, CharSequence caption) {
    try {
      @javax.annotation.Nonnull File file = File.createTempFile("temp", "bin");
      IOUtils.write(data.toString().getBytes(Charset.forName("UTF-8")), new FileOutputStream(file));
      return file.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @javax.annotation.Nonnull
  @Override
  public CharSequence file(@javax.annotation.Nonnull byte[] data, @javax.annotation.Nonnull CharSequence filename, CharSequence caption) {
    return file(new String(data, Charset.forName("UTF-8")), filename, caption);
  }
  
  @javax.annotation.Nonnull
  @Override
  public String file(@javax.annotation.Nonnull CharSequence data, @javax.annotation.Nonnull CharSequence fileName, CharSequence caption) {
    try {
      @javax.annotation.Nonnull File file = new File(fileName.toString());
      IOUtils.write(data.toString().getBytes(Charset.forName("UTF-8")), new FileOutputStream(file));
      return file.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void h1(CharSequence fmt, Object... args) {
  
  }
  
  @Override
  public void h2(CharSequence fmt, Object... args) {
  
  }
  
  @Override
  public void h3(CharSequence fmt, Object... args) {
  
  }
  
  @javax.annotation.Nonnull
  @Override
  public String image(BufferedImage rawImage, CharSequence caption) {
    return "";
  }
  
  @javax.annotation.Nonnull
  @Override
  public CharSequence link(File file, CharSequence text) {
    return "";
  }
  
  @Override
  public void p(CharSequence fmt, Object... args) {
  
  }
  
  @Nullable
  @Override
  public CharSequence getFrontMatterProperty(CharSequence key) {
    return null;
  }
  
  @Override
  public CharSequence getName() {
    return name;
  }
  
  @javax.annotation.Nonnull
  @Override
  public File getResourceDir() {
    return new File(".");
  }
  
  @Override
  public int getMaxOutSize() {
    return 0;
  }
}
