package com.github.mostroverkhov.retrofit_property_streaming;

import com.google.gson.Gson;
import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Created by Maksym Ostroverkhov on 14.06.2016.
 */
public final class PropertyStreamCallAdapterFactory extends CallAdapter.Factory {

    private final Gson gson = new Gson();
    private static final PropertyStreamCallAdapterFactory INSTANCE = new PropertyStreamCallAdapterFactory();

    private PropertyStreamCallAdapterFactory() {
    }

    public static PropertyStreamCallAdapterFactory create() {
        return INSTANCE;
    }

    @Override
    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {

        if ((returnType instanceof ParameterizedType)
                && getRawType(returnType).getCanonicalName().equals("io.reactivex.Flowable")) {
            Type typeGenericArg = getParameterUpperBound(0, (ParameterizedType) returnType);
            if (typeGenericArg instanceof ParameterizedType && getRawType(typeGenericArg).equals(Prop.class)) {
                Type propOwnerType = getParameterUpperBound(0, (ParameterizedType) typeGenericArg);
                return new PropertyStreamCallAdapter(Prop.class, propOwnerType, gson);
            }
        }
        return null;
    }
}
