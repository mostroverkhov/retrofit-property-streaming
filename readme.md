####Backpressured [rxjava](https://github.com/ReactiveX/RxJava) streaming support for [retrofit](https://square.github.io/retrofit/) as events of model properties.  

Json payloads only 

Supported property types:

**property** - non-list property  
**arr_property** - item of json array property  
**arr_start** - start of json array notification, contains property name  
**arr_end** - end of of json array notification, contains property name  

###Limitations
binding for properties and array contents only
supported java types for json array: ```java.util.List```

###How to use

Given that retrofit created as

    Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("base_url")
                .addCallAdapterFactory(PropertyStreamCallAdapterFactory.create())
                .addConverterFactory(PropertyStreamConverterFactory.create())
                .build();

with service definition (note that in ```Prop<T>```, parameter T for model type is mandatory)

    service = retrofit.create(MockService.class);
    
    public interface MockService {
        @GET("/{id}")
        Observable<Prop<MockResponse>> mockResponse(@Path("id") String id);
    }

and model

    public static class MockResponse {

        private Owner owner;
        private List<Item> items;
        private long id;
        private String name;

        public static class Owner {
            private String login;
            private int id;
        }

        public static class Item {
            private String name;
            private boolean val;
        }
    }

 Subscription

     service.mockResponse("42").subscribe(new Subscriber<Prop<TestModels.MockResponse>>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                }

                @Override
                public void onNext(Prop<TestModels.MockResponse> mockResponseProp) {
                    request(1);
                }

                @Override
                public void onStart() {
                    request(1);
                }
            });

will produce property events
```
Prop{propTypes=[PROPERTY], name='id', value=20284864}
Prop{propTypes=[PROPERTY], name='name', value=Simplistic-RSS}
Prop{propTypes=[PROPERTY], name='owner', value=Owner{login='null', id=3848588}}
Prop{propTypes=[ARR_START], name='items', value=null}
Prop{propTypes=[ARR_PROPERTY], name='items', value=Item{name='name1', val=true}}
Prop{propTypes=[ARR_PROPERTY], name='items', value=Item{name='name2', val=true}}
Prop{propTypes=[ARR_END], name='items', value=null}
 ```  

###Supported types

RxJava1 ```Observable```  
RxJava2 ```Flowable```  

###Artifacts

 Binaries are available on jitpack

     repositories {
			maven { url 'https://jitpack.io' }
	 }

     compile 'com.github.mostroverkhov:retrofit-property-streaming:1.0.1'
