package com.github.mostroverkhov.retrofit_property_streaming;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Maksym Ostroverkhov on 16.01.2017.
 */

public class MapDeserializerFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

        Class<? super T> rawType = type.getRawType();

        if (Map.class.isAssignableFrom(rawType)) {
            TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, type);

            return new TypeAdapter<T>() {

                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    delegateAdapter.write(out, value);
                }

                @SuppressWarnings("unchecked")
                @Override
                public T read(JsonReader in) throws IOException {

                    Map map = (Map) delegateAdapter.read(in);

                    Map<Object, Object> res = new HashMap<>();
                    for (Object key : map.keySet()) {
                        Object val = map.get(key);
                        if (val != null) {
                            res.put(key, val);
                        }
                    }
                    return (T) res;
                }
            };
        } else {
            return null;
        }
    }
}
