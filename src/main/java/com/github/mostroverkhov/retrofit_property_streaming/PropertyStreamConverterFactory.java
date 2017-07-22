package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Maksym Ostroverkhov on 14.06.2016.
 */
public class PropertyStreamConverterFactory extends Converter.Factory {

  private final GsonConverterFactory gsonConverterFactory;

  private PropertyStreamConverterFactory(GsonConverterFactory gsonConverterFactory) {
    this.gsonConverterFactory = gsonConverterFactory;
  }

  public static PropertyStreamConverterFactory create() {
    return new PropertyStreamConverterFactory(GsonConverterFactory.create());
  }

  public static PropertyStreamConverterFactory create(GsonConverterFactory gsonConverterFactory) {
    return new PropertyStreamConverterFactory(gsonConverterFactory);
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    if (type == Prop.class) {
      return new Converter<ResponseBody, ResponseBody>() {
        @Override
        public ResponseBody convert(ResponseBody value) throws IOException {
          return value;
        }
      };
    }
    return null;
  }

  @Override
  public Converter<?, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    return gsonConverterFactory
        .requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
  }

  @Override
  public Converter<?, String> stringConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    return gsonConverterFactory.stringConverter(type, annotations, retrofit);
  }
}
