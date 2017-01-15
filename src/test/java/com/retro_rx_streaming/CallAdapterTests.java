package com.retro_rx_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.PropertyStreamCallAdapterFactory;
import com.github.mostroverkhov.retrofit_property_streaming.PropertyStreamConverterFactory;
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
    private MockService service;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse().setBody(mockResponse()));
        mockWebServer.enqueue(new MockResponse());
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/").toString())
                .addCallAdapterFactory(PropertyStreamCallAdapterFactory.create())
                .addConverterFactory(PropertyStreamConverterFactory.create())
                .build();

        service = retrofit.create(MockService.class);
    }

    @Test
    public void propertyCallAdapterIntegrationNoBackpressure() throws Exception {
        try {
            Observer observer = Mockito.mock(Observer.class);
            service.mockResponse("42").toBlocking().subscribe(new Observer<Prop<TestModels.MockResponse>>() {
                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(Prop<TestModels.MockResponse> mockResponseProp) {
                    observer.onNext(mockResponseProp);
                }
            });

            InOrder inOrder = Mockito.inOrder(observer);
            inOrder.verify(observer, Mockito.times(7)).onNext(Matchers.any(Prop.class));
            inOrder.verify(observer).onCompleted();
            inOrder.verify(observer, Mockito.never()).onError(Matchers.any());

        } finally {
            mockWebServer.shutdown();
        }
    }

    @Test
    public void propertyCallAdapterIntegrationBackpressure() throws Exception {
        try {
            Observer observer = Mockito.mock(Observer.class);
            service.mockResponse("42").toBlocking().subscribe(new Subscriber<Prop<TestModels.MockResponse>>() {
                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(Prop<TestModels.MockResponse> mockResponseProp) {
                    observer.onNext(mockResponseProp);
                    request(1);
                }

                @Override
                public void onStart() {
                    request(1);
                }
            });

            InOrder inOrder = Mockito.inOrder(observer);
            inOrder.verify(observer, Mockito.times(7)).onNext(Matchers.any(Prop.class));
            inOrder.verify(observer).onCompleted();
            inOrder.verify(observer, Mockito.never()).onError(Matchers.any());

        } finally {
            mockWebServer.shutdown();
        }
    }

    private String mockResponse() throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("mock_response.json");
        String str = Okio.buffer(Okio.source(stream)).readUtf8();
        return str;
    }

    public interface MockService {

        @GET("/{id}")
        Observable<Prop<TestModels.MockResponse>> mockResponse(@Path("id") String id);
    }
}
