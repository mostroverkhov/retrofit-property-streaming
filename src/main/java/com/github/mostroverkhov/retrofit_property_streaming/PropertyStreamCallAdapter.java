package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
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
        return Flowable.create(new OnSubscribe<>(call, propOwnerType, gson),
                BackpressureStrategy.BUFFER);
    }

    private static class OnSubscribe<R> implements FlowableOnSubscribe<Prop<R>> {

        private final Call<R> call;
        private final Type targetType;
        private final Gson gson;
        private final AtomicBoolean cancelSignal = new AtomicBoolean(false);

        public OnSubscribe(Call<R> call, Type targetType, Gson gson) {
            this.targetType = targetType;
            this.gson = gson;
            this.call = call;
        }

        @Override
        public void subscribe(FlowableEmitter<Prop<R>> emitter) throws Exception {
            try {
                Call<R> clone = call.clone();
                Response<R> response = clone.execute();
                final Reader reader = ((ResponseBody) response.body()).charStream();
                PropertySlicer<R> propertySlicer = new PropertySlicerBuilder<R>(
                        targetType,
                        gson.newJsonReader(reader))
                        .build();

                emitter.setDisposable(new Disposable() {
                    @SuppressWarnings("EmptyCatchBlock")
                    @Override
                    public void dispose() {
                        if (cancelSignal.compareAndSet(false, true)) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                            }
                        }
                    }

                    @Override
                    public boolean isDisposed() {
                        return cancelSignal.get();
                    }
                });

                boolean hasMore = true;
                while (hasMore && !cancelSignal.get()) {

                    Prop<R> prop = propertySlicer.nextProp();
                    if (!cancelSignal.get()) {
                        if (!prop.isDocumentEnd()) {
                            emitter.onNext(prop);
                        } else {
                            emitter.onComplete();
                            hasMore = false;
                        }
                    }
                }
            } catch (Exception e) {
                Exceptions.throwIfFatal(e);
                if (!cancelSignal.get()) {
                    emitter.onError(e);
                }
            }
        }
    }
}
