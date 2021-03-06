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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.type.MapType;

class RefBeanSerializerFactory extends BeanSerializerFactory {
  public RefBeanSerializerFactory() {
    this(null);
  }

  public RefBeanSerializerFactory(SerializerFactoryConfig config) {
    super(config);
  }

  @Override
  public SerializerFactory withConfig(SerializerFactoryConfig config) {
    if (_factoryConfig == config) {
      return this;
    }
    /* 22-Nov-2010, tatu: Handling of subtypes is tricky if we do immutable-with-copy-ctor;
     *    and we pretty much have to here either choose between losing subtype instance
     *    when registering additional serializers, or losing serializers.
     *    Instead, let's actually just throw an error if this method is called when subtype
     *    has not properly overridden this method; this to indicate problem as soon as possible.
     */
    if (getClass() != RefBeanSerializerFactory.class) {
      throw new IllegalStateException("Subtype of RefBeanSerializerFactory (" + getClass().getName()
          + ") has not properly overridden method 'withAdditionalSerializers': cannot instantiate subtype with "
          + "additional serializer definitions");
    }
    return new RefBeanSerializerFactory(config);
  }

  @Override
  protected MapSerializer _checkMapContentInclusion(SerializerProvider prov, BeanDescription beanDesc, MapSerializer mapSer) throws JsonMappingException {
    return RefMapSerializer.wrap(super._checkMapContentInclusion(prov, beanDesc, mapSer));
  }

  @Override
  protected JsonSerializer<?> buildMapSerializer(SerializerProvider prov, MapType type, BeanDescription beanDesc, boolean staticTyping, JsonSerializer<Object> keySerializer, TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) throws JsonMappingException {
    return RefMapSerializer.wrap(super.buildMapSerializer(prov, type, beanDesc, staticTyping, keySerializer, elementTypeSerializer, elementValueSerializer));
  }

  @Override
  protected JsonSerializer<Object> constructBeanOrAddOnSerializer(SerializerProvider prov, JavaType type, BeanDescription beanDesc, boolean staticTyping) throws JsonMappingException {
    return RefMapSerializer.wrap(super.constructBeanOrAddOnSerializer(prov, type, beanDesc, staticTyping));
  }

}
