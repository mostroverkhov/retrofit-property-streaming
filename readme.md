Streaming of [retrofit](https://square.github.io/retrofit/) json response as sequence of model properties, with array contents flattened into stream. Intended for processing of responses containing large arrays

Json payloads only 

For arrays, java model property should be ```java.util.List```

Produced property types:

**property** - non-list property  
**arr_property** - element of json array 
**arr_start** - start of array, contains property name  
**arr_end** - end of of array, contains property name  

### How to use

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

### Supported types

RxJava1 ```Observable```  
RxJava2 ```Flowable```  

### Artifacts

 Binaries are available on jitpack

     repositories {
       maven { url 'https://jitpack.io' }
     }

RxJava1  
 
    dependencies {
     compile 'com.github.mostroverkhov:retrofit-property-streaming:1.0.1'
    }

RxJava2  

     dependencies {
       compile 'com.github.mostroverkhov:retrofit-property-streaming:2.0.1'
     }
