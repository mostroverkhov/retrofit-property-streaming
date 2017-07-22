package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;
import com.google.gson.Gson;

import io.reactivex.Emitter;
import io.reactivex.functions.Cancellable;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.exceptions.Exceptions;
import java.util.concurrent.atomic.AtomicInteger;
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

      private final Prop<R> completeSignal = new Prop<>();
      private final Prop<R> cancelSignal = new Prop<>();
      private final Prop<R> errorSignal = new Prop<>();
      private volatile Throwable error;
      private volatile boolean finished;

      private final Emitter<Prop<R>> emitter;
      private final Call<R> call;
      private final Queue<Prop<R>> q = new ConcurrentLinkedQueue<>();
      private final AtomicInteger qSize = new AtomicInteger();

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
            succ(response);
          } else {
            error(httpException(response));
          }
        } catch (Exception e) {
          error(e);
        }
      }

      @Override
      public void onFailure(Call<R> call, Throwable t) {
        error(t);
      }

      void succ(Response<R> response) {
        Reader bodyReader = ((ResponseBody) response.body()).charStream();
        PropertyReader<R> propReader = new PropertyReaderBuilder<R>(
            targetType,
            gson.newJsonReader(bodyReader))
            .build();
        boolean hasMore = true;
        while (hasMore) {
          Prop<R> prop = propReader.nextProp();
          if (finished()) {
            hasMore = false;
          } else if (prop.isDocumentEnd()) {
            hasMore = false;
            next(completeSignal);
          } else {
            next(prop);
          }
        }
      }

      void error(Throwable t) {
        Exceptions.throwIfFatal(t);
        error = t;
        next(errorSignal);
      }

      void cancel() {
        next(cancelSignal);
        call.cancel();
      }

      void toFinished() {
        finished = true;
      }

      boolean finished() {
        return finished;
      }

      void next(Prop<R> prop) {
        if (finished()) {
          return;
        }
        if (prop == cancelSignal
            || prop == completeSignal
            || prop == errorSignal) {
          toFinished();
        }
        if (prop == cancelSignal) {
          q.clear();
        } else {
          q.offer(prop);
          if (qSize.getAndIncrement() == 0) {
            do {
              Prop<R> next = q.poll();
              if (next == completeSignal) {
                emitter.onComplete();
                break;
              } else if (next == errorSignal) {
                emitter.onError(error);
                break;
              } else {
                emitter.onNext(next);
              }
            } while (qSize.decrementAndGet() != 0);
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
    }
  }
}
