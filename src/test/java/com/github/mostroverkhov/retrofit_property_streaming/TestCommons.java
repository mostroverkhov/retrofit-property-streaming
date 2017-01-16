package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Maksym Ostroverkhov on 14.06.2016.
 */
class TestCommons {

    static <T> List<Prop<T>> getProps(PropertySlicer<T> calc) {
       List<Prop<T>> props = new ArrayList<>();
       Prop<T> prop = calc.nextProp();

       boolean shouldStop = false;

       while (!shouldStop) {
           props.add(prop);
           if (prop.isDocumentEnd()) {
               shouldStop = true;
           } else {
               prop = calc.nextProp();
           }
       }
       return props;
   }

    public static class MockResponse {

        private Owner owner;
        private List<Item<String>> items;
        private long id;
        private String name;

        public static class Owner {
            private String login;
            private int id;

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Owner{");
                sb.append("login='").append(login).append('\'');
                sb.append(", id=").append(id);
                sb.append('}');
                return sb.toString();
            }
        }

        public static class Item<T> {
            private T name;
            private boolean val;

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Item{");
                sb.append("name='").append(name).append('\'');
                sb.append(", val=").append(val);
                sb.append('}');
                return sb.toString();
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("MockResponse{");
            sb.append("owner=").append(owner);
            sb.append(", items=").append(items);
            sb.append(", id=").append(id);
            sb.append(", name='").append(name).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static class MockResponseShort {
        private Owner owner;
        private List<Item> items;

        public static class Owner {
            private String login;
            private int id;

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Owner{");
                sb.append("login='").append(login).append('\'');
                sb.append(", id=").append(id);
                sb.append('}');
                return sb.toString();
            }
        }

        public static class Item {
            private String name;
            private boolean val;

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Item{");
                sb.append("name='").append(name).append('\'');
                sb.append(", val=").append(val);
                sb.append('}');
                return sb.toString();
            }
        }
    }

    public static class SpecialTypes {
        private Map<String, Float> owner;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SpecialTypes{");
            sb.append("owner=").append(owner);
            sb.append('}');
            return sb.toString();
        }
    }
}
