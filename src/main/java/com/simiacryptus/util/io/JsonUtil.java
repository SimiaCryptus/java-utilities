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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * The type Json util.
 */
public class JsonUtil {
  
  /**
   * Get double array double [ ].
   *
   * @param array the array
   * @return the double [ ]
   */
  public static double[] getDoubleArray(@javax.annotation.Nonnull final JsonArray array) {
    return IntStream.range(0, array.size()).mapToDouble(i -> array.get(i).getAsDouble()).toArray();
  }
  
  /**
   * Get int array int [ ].
   *
   * @param array the array
   * @return the int [ ]
   */
  @Nullable
  public static int[] getIntArray(@Nullable final JsonArray array) {
    if (null == array) return null;
    return IntStream.range(0, array.size()).map(i -> array.get(i).getAsInt()).toArray();
  }
  
  /**
   * Gets json.
   *
   * @param kernelDims the kernel dims
   * @return the json
   */
  @javax.annotation.Nonnull
  public static JsonArray getJson(@javax.annotation.Nonnull final double[] kernelDims) {
    @javax.annotation.Nonnull final JsonArray array = new JsonArray();
    for (final double k : kernelDims) {
      array.add(new JsonPrimitive(k));
    }
    return array;
  }
  
  /**
   * Gets json.
   *
   * @param kernelDims the kernel dims
   * @return the json
   */
  @javax.annotation.Nonnull
  public static JsonArray getJson(@javax.annotation.Nonnull final int[] kernelDims) {
    @javax.annotation.Nonnull final JsonArray array = new JsonArray();
    for (final int k : kernelDims) {
      array.add(new JsonPrimitive(k));
    }
    return array;
  }
  
  /**
   * Write json.
   *
   * @param obj the obj
   * @return the char sequence
   */
  public static CharSequence toJson(final Object obj) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      writeJson(outputStream, obj);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new String(outputStream.toByteArray(), Charset.forName("UTF-8"));
  }
  
  /**
   * Write json.
   *
   * @param out the out
   * @param obj the obj
   * @throws IOException the io exception
   */
  public static void writeJson(@javax.annotation.Nonnull final OutputStream out, final Object obj) throws IOException {
    final ObjectMapper mapper = getMapper();
    @javax.annotation.Nonnull final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    mapper.writeValue(buffer, obj);
    out.write(buffer.toByteArray());
  }
  
  public static <T> T cache(final File file, Class<T> clazz, Supplier<T> intializer) throws IOException {
    if (file.exists()) {
      return getMapper().readValue(FileUtils.readFileToString(file, Charset.defaultCharset()), clazz);
    }
    else {
      T obj = intializer.get();
      FileUtils.write(file, toJson(obj), Charset.defaultCharset());
      return obj;
    }
  }
  
  public static ObjectMapper getMapper() {
    return new ObjectMapper()
      //.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
      .enable(SerializationFeature.INDENT_OUTPUT);
  }
}
