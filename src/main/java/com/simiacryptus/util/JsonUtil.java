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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Supplier;

public class JsonUtil {

  public static ObjectMapper getMapper() {
    return new ObjectMapper()
        .setSerializerFactory(new RefBeanSerializerFactory())
        //.setSerializerProvider(new RefSerializerProvider())
        //.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
        .enable(SerializationFeature.INDENT_OUTPUT);
  }

  public static double[] getDoubleArray(@Nonnull final JsonArray array) {
    return RefIntStream.range(0, array.size()).mapToDouble(i -> array.get(i).getAsDouble()).toArray();
  }

  @Nullable
  public static int[] getIntArray(@Nullable final JsonArray array) {
    if (null == array)
      return null;
    return RefIntStream.range(0, array.size()).map(i -> array.get(i).getAsInt()).toArray();
  }

  @Nonnull
  public static JsonArray getJson(@Nonnull final double[] kernelDims) {
    @Nonnull final JsonArray array = new JsonArray();
    for (final double k : kernelDims) {
      array.add(new JsonPrimitive(k));
    }
    return array;
  }

  @Nonnull
  public static JsonArray getJson(@Nonnull final int[] kernelDims) {
    @Nonnull final JsonArray array = new JsonArray();
    for (final int k : kernelDims) {
      array.add(new JsonPrimitive(k));
    }
    return array;
  }

  @Nonnull
  public static CharSequence toJson(@RefAware final Object obj) {
    return toJson(obj, getMapper());
  }

  @Nonnull
  @RefIgnore
  public static CharSequence toJson(@RefAware final Object obj, @Nonnull final ObjectMapper objectMapper) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      objectMapper.writeValue(outputStream, obj);
      RefUtil.freeRef(obj);
    } catch (IOException e) {
      throw Util.throwException(e);
    }
    return new String(outputStream.toByteArray(), Charset.forName("UTF-8"));
  }

  public static <T> T cache(@Nonnull final File file, Class<T> clazz, @Nonnull Supplier<T> intializer) throws IOException {
    if (file.exists()) {
      return getMapper().readValue(FileUtils.readFileToString(file, Charset.defaultCharset()), clazz);
    } else {
      T obj = intializer.get();
      FileUtils.write(file, toJson(obj), Charset.defaultCharset());
      return obj;
    }
  }

  @Nonnull
  public static int[] toIntArray(@Nonnull JsonArray array) {
    int[] ints = new int[array.size()];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = array.get(i).getAsInt();
    }
    return ints;
  }

  @Nonnull
  public static JsonArray toIntArray(@Nonnull int[] array) {
    JsonArray jsonElements = new JsonArray();
    RefArrays.stream(array).forEach(number -> jsonElements.add(number));
    return jsonElements;
  }

  public static JsonObject toJson(@Nonnull byte[] buf) {
    return new GsonBuilder().create().fromJson(new InputStreamReader(new ByteArrayInputStream(buf)), JsonObject.class);
  }

}
