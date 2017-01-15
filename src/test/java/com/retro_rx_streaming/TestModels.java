package com.retro_rx_streaming;

import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 14.06.2016.
 */
class TestModels {
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

    public static class MockResponseShort {

        private Owner owner;
        private List<Item> items;

        public static class Owner {
            private String login;
            private int id;
        }

        public static class Item {
            private String name;
            private boolean val;
        }
    }
}
