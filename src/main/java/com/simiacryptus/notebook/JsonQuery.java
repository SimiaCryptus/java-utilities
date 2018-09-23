package com.simiacryptus.notebook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class JsonQuery<T> extends StringQuery<T> {
  private ObjectMapper mapper = new ObjectMapper()
      //.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
      .enable(SerializationFeature.INDENT_OUTPUT);

  public JsonQuery(MarkdownNotebookOutput log) {
    super(log);
  }

  @Override
  protected T fromString(String text) throws IOException {
    return (T) mapper.readValue(new ByteArrayInputStream(text.getBytes()), value.getClass());
  }

  @Override
  protected String getString(T value) throws JsonProcessingException {
    return mapper.writeValueAsString(value);
  }

  public ObjectMapper getMapper() {
    return mapper;
  }

  public JsonQuery<T> setMapper(ObjectMapper mapper) {
    this.mapper = mapper;
    return this;
  }
}
