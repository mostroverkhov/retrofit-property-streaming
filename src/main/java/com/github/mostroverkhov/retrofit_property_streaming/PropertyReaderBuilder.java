package com.github.mostroverkhov.retrofit_property_streaming;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.lang.reflect.Type;

/**
 * Created by Maksym Ostroverkhov on 16.01.2017.
 */

public class PropertyReaderBuilder<T> {

  private final Type propTarget;
  private final JsonReader jsonStream;
  private final Gson gson;

  public PropertyReaderBuilder(Type propTarget, JsonReader jsonStream) {
    this.propTarget = propTarget;
    this.jsonStream = jsonStream;
    this.gson = new GsonBuilder().registerTypeAdapterFactory(new MapDeserializerFactory()).create();
  }

  public PropertyReader<T> build() {
    return new PropertyReader<>(propTarget, gson, jsonStream);
  }
}
