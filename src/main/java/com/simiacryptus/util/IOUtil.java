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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * The type Io util.
 */
public class IOUtil {
  private static final ObjectMapper objectMapper = JsonUtil.getMapper();
  private static final ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>() {
    @Override
    protected Kryo initialValue() {
      Kryo kryo = new Kryo();
      kryo.setRegistrationRequired(false);
      return kryo;
    }
  };
  private static final ThreadLocal<byte[]> buffer = new ThreadLocal<byte[]>() {
    @Override
    protected byte[] initialValue() {
      return new byte[8 * 1024 * 1024];
    }
  };

  /**
   * Write json.
   *
   * @param <T>  the type parameter
   * @param obj  the obj
   * @param file the file
   */
  public static <T> void writeJson(T obj, File file) {
    StringWriter writer = new StringWriter();
    try {
      objectMapper.writeValue(writer, obj);
      Files.write(file.toPath(), writer.toString().getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Read json t.
   *
   * @param <T>  the type parameter
   * @param file the file
   * @return the t
   */
  public static <T> T readJson(File file) {
    try {
      return objectMapper.readValue(new String(Files.readAllBytes(file.toPath())), new TypeReference<T>() {
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write kryo.
   *
   * @param <T>  the type parameter
   * @param obj  the obj
   * @param file the file
   */
  public static <T> void writeKryo(T obj, OutputStream file) {
    try {
      Output output = new Output(buffer.get());
      new KryoReflectionFactorySupport().writeClassAndObject(output, obj);
      output.close();
      IOUtils.write(Arrays.copyOf(output.getBuffer(), output.position()), file);
      file.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write string.
   *
   * @param obj  the obj
   * @param file the file
   */
  public static void writeString(String obj, OutputStream file) {
    try {
      IOUtils.write(obj.getBytes("UTF-8"), file);
      file.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Read kryo t.
   *
   * @param <T>  the type parameter
   * @param file the file
   * @return the t
   */
  public static <T> T readKryo(File file) {
    try {
      byte[] bytes = Files.readAllBytes(file.toPath());
      Input input = new Input(buffer.get(), 0, bytes.length);
      return (T) new KryoReflectionFactorySupport().readClassAndObject(input);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
