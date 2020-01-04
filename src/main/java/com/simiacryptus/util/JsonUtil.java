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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Supplier;

public @com.simiacryptus.ref.lang.RefAware
class JsonUtil {

  public static ObjectMapper getMapper() {
    ObjectMapper enable = new ObjectMapper()
        //.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
        .enable(SerializationFeature.INDENT_OUTPUT);
    return enable;
  }

  public static double[] getDoubleArray(@javax.annotation.Nonnull final JsonArray array) {
    return com.simiacryptus.ref.wrappers.RefIntStream.range(0, array.size())
        .mapToDouble(i -> array.get(i).getAsDouble()).toArray();
  }

  @Nullable
  public static int[] getIntArray(@Nullable final JsonArray array) {
    if (null == array)
      return null;
    return com.simiacryptus.ref.wrappers.RefIntStream.range(0, array.size()).map(i -> array.get(i).getAsInt())
        .toArray();
  }

  @javax.annotation.Nonnull
  public static JsonArray getJson(@javax.annotation.Nonnull final double[] kernelDims) {
    @javax.annotation.Nonnull final JsonArray array = new JsonArray();
    for (final double k : kernelDims) {
      array.add(new JsonPrimitive(k));
    }
    return array;
  }

  @javax.annotation.Nonnull
  public static JsonArray getJson(@javax.annotation.Nonnull final int[] kernelDims) {
    @javax.annotation.Nonnull final JsonArray array = new JsonArray();
    for (final int k : kernelDims) {
      array.add(new JsonPrimitive(k));
    }
    return array;
  }

  public static CharSequence toJson(final Object obj) {
    return toJson(obj, getMapper());
  }

  @Nonnull
  public static CharSequence toJson(final Object obj, final ObjectMapper objectMapper) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      objectMapper.writeValue(outputStream, obj);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new String(outputStream.toByteArray(), Charset.forName("UTF-8"));
  }

  public static <T> T cache(final File file, Class<T> clazz, Supplier<T> intializer) throws IOException {
    if (file.exists()) {
      return getMapper().readValue(FileUtils.readFileToString(file, Charset.defaultCharset()), clazz);
    } else {
      T obj = intializer.get();
      FileUtils.write(file, toJson(obj), Charset.defaultCharset());
      return obj;
    }
  }

  public static int[] toIntArray(JsonArray array) {
    int[] ints = new int[array.size()];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = array.get(i).getAsInt();
    }
    return ints;
  }

  public static JsonArray toIntArray(int[] array) {
    JsonArray jsonElements = new JsonArray();
    com.simiacryptus.ref.wrappers.RefArrays.stream(array).forEach(jsonElements::add);
    return jsonElements;
  }

  public static JsonObject toJson(byte[] buf) {
    return new GsonBuilder().create().fromJson(new InputStreamReader(new ByteArrayInputStream(buf)), JsonObject.class);
  }
}
