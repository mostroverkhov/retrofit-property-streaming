package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

import okio.Okio;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;

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
            Observer observer = Mockito.mock(Observer.class);
            mockService.mockResponse("42").toBlocking()
                    .subscribe(new Observer<Prop<TestCommons.MockResponse>>() {
                        @Override
                        public void onCompleted() {
                            observer.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            observer.onError(e);
                        }

                        @Override
                        public void onNext(Prop<TestCommons.MockResponse> mockResponseProp) {
                            observer.onNext(mockResponseProp);
                        }
                    });
            InOrder inOrder = Mockito.inOrder(observer);
            inOrder.verify(observer, Mockito.times(7)).onNext(Matchers.any(Prop.class));
            inOrder.verify(observer).onCompleted();
            inOrder.verify(observer, Mockito.never()).onError(Matchers.any());
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
            Observer observer = Mockito.mock(Observer.class);
            mockService.mockResponse("42").toBlocking()
                    .subscribe(new Subscriber<Prop<TestCommons.MockResponse>>() {
                        @Override
                        public void onCompleted() {
                            observer.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            observer.onError(e);
                        }

                        @Override
                        public void onStart() {
                            request(1);
                        }

                        @Override
                        public void onNext(Prop<TestCommons.MockResponse> mockResponseProp) {
                            observer.onNext(mockResponseProp);
                            request(1);
                        }
                    });
            InOrder inOrder = Mockito.inOrder(observer);
            inOrder.verify(observer, Mockito.times(7)).onNext(Matchers.any(Prop.class));
            inOrder.verify(observer).onCompleted();
            inOrder.verify(observer, Mockito.never()).onError(Matchers.any());
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
            Observer observer = Mockito.mock(Observer.class);
            serviceSpecialTypes.mockResponse("42").toBlocking().subscribe(
                    new Subscriber<Prop<TestCommons.SpecialTypes>>() {
                        @Override
                        public void onCompleted() {
                            observer.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            observer.onError(e);
                        }

                        @Override
                        public void onNext(Prop<TestCommons.SpecialTypes> mockResponseProp) {
                            observer.onNext(mockResponseProp);
                            request(1);
                        }

                        @Override
                        public void onStart() {
                            request(1);
                        }
                    });

            InOrder inOrder = Mockito.inOrder(observer);
            inOrder.verify(observer, Mockito.times(1)).onNext(Matchers.any(Prop.class));
            inOrder.verify(observer).onCompleted();
            inOrder.verify(observer, Mockito.never()).onError(Matchers.any());
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
        Observable<Prop<TestCommons.MockResponse>> mockResponse(@Path("id") String id);
    }

    interface MockServiceSpecialTypes {
        @GET("/{id}")
        Observable<Prop<TestCommons.SpecialTypes>> mockResponse(@Path("id") String id);
    }
}
