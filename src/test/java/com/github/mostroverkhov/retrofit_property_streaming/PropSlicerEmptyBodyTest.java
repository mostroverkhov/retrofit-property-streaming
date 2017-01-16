package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 16.01.2017.
 */

public class PropSlicerEmptyBodyTest {

    private Gson gson;

    @Before
    public void setUp() throws Exception {
        gson = new Gson();
    }

    @Test
    public void emptyArray() throws Exception {
        assertEmpty("empty_array.json");
    }

    @Test
    public void emptyObject() throws Exception {
        assertEmpty("empty_object.json");
    }

    private void assertEmpty(String resourceFile) throws IOException {
        InputStreamReader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resourceFile),
                "UTF-8");

        try (JsonReader jsonReader = gson.newJsonReader(reader)) {

            PropertySlicer<TestCommons.MockResponse> propertySlicer = new PropertySlicer<>(
                    TestCommons.MockResponse.class,
                    gson,
                    jsonReader);

            List<Prop<TestCommons.MockResponse>> props = TestCommons.getProps(propertySlicer);
            Assertions.assertThat(props).hasSize(1);
            Assertions.assertThat(props.get(0).isDocumentEnd()).isTrue();
        }
    }
}
