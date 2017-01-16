package com.github.mostroverkhov.retrofit_property_streaming;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.lang.reflect.Type;

/**
 * Created by Maksym Ostroverkhov on 16.01.2017.
 */

public class PropertySlicerBuilder<T> {

    private final Type propTarget;
    private final JsonReader jsonStream;
    private final Gson gson;

    public PropertySlicerBuilder(Type propTarget, JsonReader jsonStream) {
        this.propTarget = propTarget;
        this.jsonStream = jsonStream;
        this.gson = new GsonBuilder().registerTypeAdapterFactory(new MapDeserializerFactory()).create();
    }

    public PropertySlicer<T> build() {
        return new PropertySlicer<>(propTarget, gson, jsonStream);
    }
}
