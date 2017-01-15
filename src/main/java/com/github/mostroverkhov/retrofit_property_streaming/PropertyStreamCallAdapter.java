package com.github.mostroverkhov.retrofit_property_streaming;

import com.google.gson.Gson;
import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import rx.Observable;
import rx.Observer;
import rx.exceptions.Exceptions;
import rx.observables.SyncOnSubscribe;

/**
 * Created by Maksym Ostroverkhov on 11.06.2016.
 */
final class PropertyStreamCallAdapter implements CallAdapter<Observable<?>> {
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
    public <R> Observable<Prop<R>> adapt(Call<R> call) {
        return Observable.create(new OnSubscribe<>(call, propOwnerType, gson));
    }

    private static class State {

        private volatile boolean isInit = true;

        public State() {
        }

        public void resetInit() {
            isInit = false;
        }

        public boolean isInit() {
            return isInit;
        }
    }

    private static class OnSubscribe<R> extends SyncOnSubscribe<State, Prop<R>> {

        private final Call<R> call;
        private final Type targetType;
        private final Gson gson;
        private Reader reader;
        private PropertySlicer<R> propertySlicer;
        private boolean thrown = false;

        public OnSubscribe(Call<R> call, Type targetType, Gson gson) {
            this.targetType = targetType;
            this.gson = gson;
            this.call = call.clone();
        }

        @Override
        protected State generateState() {
            return new State();
        }

        @Override
        protected State next(State state, Observer<? super Prop<R>> observer) {
            /*race is impossible according to SyncOnSubscribe contract*/
            if (state.isInit()) {
                state.resetInit();
                try {
                    Response<R> response = call.execute();
                    reader = ((ResponseBody) response.body()).charStream();
                    propertySlicer = new PropertySlicer<>(targetType, gson, gson.newJsonReader(reader));
                } catch (Exception e) {
                    thrown = true;
                    Exceptions.throwIfFatal(e);
                    observer.onError(e);
                }
            }
            if (!thrown) {
                try {
                    Prop<R> prop = propertySlicer.nextProp();

                    if (prop != null) {
                        observer.onNext(prop);
                    } else {
                        observer.onCompleted();
                    }
                } catch (Exception e) {
                    Exceptions.throwIfFatal(e);
                    observer.onError(e);
                }
            }
            return state;
        }

        @Override
        protected void onUnsubscribe(State state) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log?
                }
            }
        }
    }
}
