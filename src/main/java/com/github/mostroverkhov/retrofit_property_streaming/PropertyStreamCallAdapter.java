package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;
import com.google.gson.Gson;

import io.reactivex.Emitter;
import io.reactivex.functions.Cancellable;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.exceptions.Exceptions;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Maksym Ostroverkhov on 11.06.2016.
 */
final class PropertyStreamCallAdapter implements CallAdapter<Flowable<?>> {

  private final Type responseType;
  private final Type propOwnerType;
  private final Gson gson;

  PropertyStreamCallAdapter(Type responseType, Type propOwnerType, Gson gson) {
    this.responseType = responseType;
    this.propOwnerType = propOwnerType;
    this.gson = gson;
  }

  @Override
  public Type responseType() {
    return responseType;
  }

  @Override
  public <R> Flowable<Prop<R>> adapt(Call<R> call) {
    return Flowable.create(
        new OnSubscribe<>(call, propOwnerType, gson),
        BackpressureStrategy.BUFFER);
  }

  private static class OnSubscribe<R> implements FlowableOnSubscribe<Prop<R>> {

    private final Call<R> call;
    private final Type targetType;
    private final Gson gson;

    public OnSubscribe(
        Call<R> call,
        Type targetType,
        Gson gson) {
      this.targetType = targetType;
      this.gson = gson;
      this.call = call;
    }

    @Override
    public void subscribe(FlowableEmitter<Prop<R>> emitter) throws Exception {
      final HttpRequest req = new HttpRequest(emitter, call);
      req.execute();
      emitter.setCancellable(new Cancellable() {
        @Override
        public void cancel() throws Exception {
          req.cancel();
        }
      });
    }

    class HttpRequest implements Callback<R> {

      private final Emitter<Prop<R>> emitter;
      private Call<R> call;
      private final AtomicBoolean finalSignal = new AtomicBoolean(false);

      public HttpRequest(Emitter<Prop<R>> emitter, Call<R> call) {
        this.emitter = emitter;
        this.call = call.clone();
      }

      public void execute() {
        call.enqueue(this);
      }

      @Override
      public void onResponse(Call<R> call, Response<R> response) {
        try {
          if (response.isSuccessful()) {
            responseSuccess(response);
          } else {
            responseError(emitter, response);
          }
        } catch (Exception e) {
          error(e);
        }
      }

      @Override
      public void onFailure(Call<R> call, Throwable t) {
        error(t);
      }

      void responseError(Emitter<Prop<R>> emitter, Response<R> response) {
        if (transitToFinalState()) {
          emitter.onError(httpException(response));
        }
      }

      void responseSuccess(Response<R> response) {
        if (nonFinalState()) {
          Reader succRespReader = ((ResponseBody) response.body()).charStream();
          PropertySlicer<R> propertySlicer = new PropertySlicerBuilder<R>(
              targetType,
              gson.newJsonReader(succRespReader))
              .build();
          boolean hasMore = true;
          while (hasMore) {
            Prop<R> prop = propertySlicer.nextProp();
            if (finalState()) {
              hasMore = false;
            }
            //TODO: can emit after onError, fix with drain-queue
            if (!prop.isDocumentEnd()) {
              emitter.onNext(prop);
            } else {
              hasMore = false;
              if (transitToFinalState()) {
                emitter.onComplete();
              }
            }
          }
        }
      }

      HttpException httpException(Response<R> response) {
        String nullableStatusMsg = response.message();
        String statusMsg = nullableStatusMsg == null ? "" : nullableStatusMsg;
        int code = response.code();
        String body = body(response);
        return new HttpException(body, code, statusMsg);
      }

      String body(Response<R> r) {
        try {
          return r.errorBody().string();
        } catch (IOException e) {
          return "";
        }
      }

      void error(Throwable t) {
        Exceptions.throwIfFatal(t);
        if (transitToFinalState()) {
          emitter.onError(t);
        }
      }

      void cancel() {
        if (transitToFinalState()) {
          call.cancel();
        }
      }

      boolean transitToFinalState() {
        return finalSignal.compareAndSet(false, true);
      }

      boolean nonFinalState() {
        return !finalSignal.get();
      }

      boolean finalState() {
        return !nonFinalState();
      }
    }
  }
}
