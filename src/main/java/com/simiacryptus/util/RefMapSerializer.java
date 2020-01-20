/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefIterator;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefSet;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class RefMapSerializer extends MapSerializer {
  public RefMapSerializer(Set<String> ignoredEntries, JavaType keyType, JavaType valueType, boolean valueTypeIsStatic, TypeSerializer vts, JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer) {
    super(ignoredEntries, keyType, valueType, valueTypeIsStatic, vts, keySerializer, valueSerializer);
  }

  public RefMapSerializer(MapSerializer src, BeanProperty property, JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer, Set<String> ignoredEntries) {
    super(src, property, keySerializer, valueSerializer, ignoredEntries);
  }

  public RefMapSerializer(MapSerializer src, TypeSerializer vts, Object suppressableValue, boolean suppressNulls) {
    super(src, vts, suppressableValue, suppressNulls);
  }

  public RefMapSerializer(MapSerializer src, Object filterId, boolean sortKeys) {
    super(src, filterId, sortKeys);
  }

  public static <T> JsonSerializer<T> wrap(JsonSerializer<T> jsonSerializer) {
    if (jsonSerializer instanceof MapSerializer) {
      return (JsonSerializer<T>) wrap((MapSerializer) jsonSerializer);
    } else {
      return jsonSerializer;
    }
  }

  public static MapSerializer wrap(MapSerializer mapSerializer) {
    if (mapSerializer instanceof RefMapSerializer) {
      return mapSerializer;
    } else {
      return new RefMapSerializer(
          ReflectionUtil.getField(mapSerializer, "_ignoredEntries"),
          ReflectionUtil.getField(mapSerializer, "_keyType"),
          ReflectionUtil.getField(mapSerializer, "_valueType"),
          ReflectionUtil.getField(mapSerializer, "_valueTypeIsStatic"),
          ReflectionUtil.getField(mapSerializer, "_valueTypeSerializer"),
          ReflectionUtil.getField(mapSerializer, "_keySerializer"),
          ReflectionUtil.getField(mapSerializer, "_valueSerializer")
      );
    }
  }

  @Override
  public MapSerializer _withValueTypeSerializer(TypeSerializer vts) {
    return wrap(super._withValueTypeSerializer(vts));
  }

  @Override
  public MapSerializer withResolved(BeanProperty property, JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer, Set<String> ignored, boolean sortKeys) {
    return wrap(super.withResolved(property, keySerializer, valueSerializer, ignored, sortKeys));
  }

  @Override
  public MapSerializer withFilterId(Object filterId) {
    return wrap(super.withFilterId(filterId));
  }

  @Override
  public MapSerializer withContentInclusion(Object suppressableValue, boolean suppressNulls) {
    return wrap(super.withContentInclusion(suppressableValue, suppressNulls));
  }

  @Override
  public MapSerializer withContentInclusion(Object suppressableValue) {
    return wrap(super.withContentInclusion(suppressableValue));
  }

  @Override
  public void serializeFields(Map<?, ?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    if (value instanceof RefMap) {
      this.serializeFields((RefMap<?, ?>) value, gen, provider);
    } else {
      super.serializeFields(value, gen, provider);
    }
  }

  public void serializeTypedFields(RefMap<?, ?> value, JsonGenerator gen, SerializerProvider provider, Object suppressableValue) throws IOException {
    final Set<String> ignored = this._ignoredEntries;
    final boolean checkEmpty = (MARKER_FOR_EMPTY == suppressableValue);

    RefSet<? extends Map.Entry<?, ?>> entries = value.entrySet();
    RefIterator<? extends Map.Entry<?, ?>> entryIterator = entries.iterator();
    try {
      while (entryIterator.hasNext()) {
        Map.Entry<?, ?> entry = entryIterator.next();
        Object keyElem = entry.getKey();
        final Object valueElem = entry.getValue();
        try {
          JsonSerializer<Object> keySerializer;
          if (keyElem == null) {
            keySerializer = provider.findNullKeySerializer(this._keyType, this._property);
          } else {
            // One twist: is entry ignorable? If so, skip
            if (ignored != null && ignored.contains(keyElem)) continue;
            keySerializer = this._keySerializer;
          }

          // And then value
          JsonSerializer<Object> valueSer;
          if (valueElem == null) {
            if (this._suppressNulls) { // all suppression include null suppression
              continue;
            }
            valueSer = provider.getDefaultNullValueSerializer();
          } else {
            valueSer = this._valueSerializer;
            if (valueSer == null) {
              valueSer = this._findSerializer(provider, valueElem);
            }
            // also may need to skip non-empty values:
            if (checkEmpty) {
              if (valueSer.isEmpty(provider, valueElem)) {
                continue;
              }
            } else if (suppressableValue != null) {
              if (suppressableValue.equals(valueElem)) {
                continue;
              }
            }
          }
          keySerializer.serialize(keyElem, gen, provider);
          valueSer.serializeWithType(valueElem, gen, provider, this._valueTypeSerializer);
        } catch (Exception e) {
          wrapAndThrow(provider, e, value.addRef(), String.valueOf(keyElem));
        } finally {
          RefUtil.freeRef(entry);
          RefUtil.freeRef(valueElem);
          RefUtil.freeRef(keyElem);
        }
      }
    } finally {
      value.freeRef();
      entryIterator.freeRef();
      entries.freeRef();
    }
  }

  @Override
  @RefIgnore
  public void wrapAndThrow(SerializerProvider provider, Throwable t, @RefAware Object bean, String fieldName) throws IOException {
    super.wrapAndThrow(provider, t, bean, fieldName);
  }

  public void serializeFields(RefMap<?, ?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    // If value type needs polymorphic type handling, some more work needed:
    if (_valueTypeSerializer != null) {
      serializeTypedFields(value, gen, provider, null);
      return;
    }
    final JsonSerializer<Object> keySerializer = _keySerializer;
    final Set<String> ignored = _ignoredEntries;

    RefSet<? extends Map.Entry<?, ?>> entries = value.entrySet();
    RefIterator<? extends Map.Entry<?, ?>> entryIterator = entries.iterator();
    try {
      while (entryIterator.hasNext()) {
        Map.Entry<?, ?> entry = entryIterator.next();
        Object valueElem = entry.getValue();
        Object keyElem = entry.getKey();
        try {
          // First, serialize key
          if (keyElem == null) {
            provider.findNullKeySerializer(_keyType, _property).serialize(null, gen, provider);
          } else {
            // One twist: is entry ignorable? If so, skip
            if ((ignored != null) && ignored.contains(keyElem)) {
              continue;
            }
            keySerializer.serialize(keyElem, gen, provider);
          }
          // And then value
          if (valueElem == null) {
            provider.defaultSerializeNull(gen);
            continue;
          }
          JsonSerializer<Object> serializer = _valueSerializer;
          if (serializer == null) {
            serializer = _findSerializer(provider, valueElem);
          }
          serializer.serialize(valueElem, gen, provider);
        } catch (Exception e) { // Add reference information
          wrapAndThrow(provider, e, value.addRef(), String.valueOf(keyElem));
        } finally {
          RefUtil.freeRef(entry);
          RefUtil.freeRef(valueElem);
          RefUtil.freeRef(keyElem);
        }
      }
    } finally {
      value.freeRef();
      entryIterator.freeRef();
      entries.freeRef();
    }
  }

  @Override
  protected void _ensureOverride(String method) {
  }

  private final JsonSerializer<Object> _findSerializer(SerializerProvider provider,
                                                       Object value) throws JsonMappingException {
    final Class<?> cc = value.getClass();
    JsonSerializer<Object> valueSer = _dynamicValueSerializers.serializerFor(cc);
    if (valueSer != null) {
      return valueSer;
    }
    if (_valueType.hasGenericTypes()) {
      return _findAndAddDynamic(_dynamicValueSerializers,
          provider.constructSpecializedType(_valueType, cc), provider);
    }
    return _findAndAddDynamic(_dynamicValueSerializers, cc, provider);
  }
}
