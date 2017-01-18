package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Flowable;
import okio.Okio;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by Maksym Ostroverkhov on 11.06.2016.
 */
public class CallAdapterTests {

    private MockWebServer mockWebServer;
    private Retrofit retrofit;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse().setBody(mockResponse()));
        retrofit = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/").toString())
                .addCallAdapterFactory(PropertyStreamCallAdapterFactory.create())
                .addConverterFactory(PropertyStreamConverterFactory.create())
                .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void modelSimpleTypeNoBackpressure() throws Exception {
        try {
            MockService mockService = retrofit.create(MockService.class);
            Subscriber subscriber = Mockito.mock(Subscriber.class);
            mockService.mockResponse("42")
                    .blockingSubscribe(new Subscriber<Prop<TestCommons.MockResponse>>() {

                        @Override
                        public void onError(Throwable e) {
                            subscriber.onError(e);
                        }

                        @Override
                        public void onComplete() {
                            subscriber.onComplete();
                        }

                        @Override
                        public void onSubscribe(Subscription s) {
                            s.request(Long.MAX_VALUE);
                        }

                        @Override
                        public void onNext(Prop<TestCommons.MockResponse> mockResponseProp) {
                            subscriber.onNext(mockResponseProp);
                        }
                    });
            InOrder inOrder = Mockito.inOrder(subscriber);
            inOrder.verify(subscriber, Mockito.times(7)).onNext(Matchers.any(Prop.class));
            inOrder.verify(subscriber).onComplete();
            inOrder.verify(subscriber, Mockito.never()).onError(Matchers.any());
        } finally {
            //noinspection ThrowFromFinallyBlock
            mockWebServer.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void modelSimpleTypeBackpressure() throws Exception {
        try {
            MockService mockService = retrofit.create(MockService.class);
            Subscriber subscriber = Mockito.mock(Subscriber.class);
            mockService.mockResponse("42")
                    .blockingSubscribe(new Subscriber<Prop<TestCommons.MockResponse>>() {

                        private Subscription s;

                        @Override
                        public void onError(Throwable e) {
                            subscriber.onError(e);
                        }

                        @Override
                        public void onComplete() {
                            subscriber.onComplete();
                        }

                        @Override
                        public void onSubscribe(Subscription s) {
                            this.s = s;
                            this.s.request(1);
                        }

                        @Override
                        public void onNext(Prop<TestCommons.MockResponse> mockResponseProp) {
                            subscriber.onNext(mockResponseProp);
                            s.request(1);
                        }
                    });
            InOrder inOrder = Mockito.inOrder(subscriber);
            inOrder.verify(subscriber, Mockito.times(7)).onNext(Matchers.any(Prop.class));
            inOrder.verify(subscriber).onComplete();
            inOrder.verify(subscriber, Mockito.never()).onError(Matchers.any());
        } finally {
            //noinspection ThrowFromFinallyBlock
            mockWebServer.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void modelSpecialTypes() throws Exception {
        try {
            MockServiceSpecialTypes serviceSpecialTypes = retrofit
                    .create(MockServiceSpecialTypes.class);
            Subscriber subscriber = Mockito.mock(Subscriber.class);
            serviceSpecialTypes.mockResponse("42").blockingSubscribe(
                    new Subscriber<Prop<TestCommons.SpecialTypes>>() {

                        private Subscription s;

                        @Override
                        public void onError(Throwable e) {
                            subscriber.onError(e);
                        }

                        @Override
                        public void onComplete() {
                            subscriber.onComplete();
                        }

                        @Override
                        public void onSubscribe(Subscription s) {
                            this.s = s;
                            s.request(1);
                        }

                        @Override
                        public void onNext(Prop<TestCommons.SpecialTypes> mockResponseProp) {
                            subscriber.onNext(mockResponseProp);
                            s.request(1);
                        }
                    });

            InOrder inOrder = Mockito.inOrder(subscriber);
            inOrder.verify(subscriber, Mockito.times(1)).onNext(Matchers.any(Prop.class));
            inOrder.verify(subscriber).onComplete();
            inOrder.verify(subscriber, Mockito.never()).onError(Matchers.any());
        } finally {
            //noinspection ThrowFromFinallyBlock
            mockWebServer.shutdown();
        }
    }

    private String mockResponse() throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("mock_response.json");
        return Okio.buffer(Okio.source(stream)).readUtf8();
    }

    interface MockService {
        @GET("/{id}")
        Flowable<Prop<TestCommons.MockResponse>> mockResponse(@Path("id") String id);
    }

    interface MockServiceSpecialTypes {
        @GET("/{id}")
        Flowable<Prop<TestCommons.SpecialTypes>> mockResponse(@Path("id") String id);
    }
}
