package com.github.mostroverkhov.retrofit_property_streaming;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Maksym Ostroverkhov on 13.06.2016.
 */
public class PropSlicerTest {

    private JsonReader jsonReader;
    private Gson gson;

    @Before
    public void setUp() throws Exception {
        InputStreamReader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("mock_response.json"),
                "UTF-8");
        this.gson = new Gson();
        this.jsonReader = gson.newJsonReader(reader);
    }

    @After
    public void tearDown() throws Exception {

        if (jsonReader != null) {
            jsonReader.close();
        }
    }

    @Test
    public void propertyCalculatorAllFields() throws Exception {

        PropertySlicer<TestModels.MockResponse> calc = new PropertySlicer<>(
                TestModels.MockResponse.class,
                gson,
                jsonReader);

        List<Prop<TestModels.MockResponse>> props = new ArrayList<>();
        Prop<TestModels.MockResponse> prop = calc.nextProp();

        while (prop != null) {
            props.add(prop);
            prop = calc.nextProp();
        }
        assertThat(props).hasSize(7);
        assertThat(props.get(0).isSimpleProperty());
        assertThat(props.get(1).isSimpleProperty());
        assertThat(props.get(2).isSimpleProperty());
        assertThat(props.get(3).isArrayItemStart());
        assertThat(props.get(4).isArrayItem());
        assertThat(props.get(5).isArrayItem());
        assertThat(props.get(6).isArrayItemEnd());
    }

    @Test
    public void propertyCalculatorSkipFields() throws Exception {

        PropertySlicer<TestModels.MockResponseShort> calc = new PropertySlicer<>(
                TestModels.MockResponseShort.class,
                gson,
                jsonReader);
        List<Prop<TestModels.MockResponseShort>> props = new ArrayList<>();
        Prop<TestModels.MockResponseShort> prop = calc.nextProp();
        while (prop != null) {
            props.add(prop);
            prop = calc.nextProp();
        }
        assertThat(props).hasSize(5);
        assertThat(props.get(0).isSimpleProperty());
        assertThat(props.get(1).isArrayItemStart());
        assertThat(props.get(2).isArrayItem());
        assertThat(props.get(3).isArrayItem());
        assertThat(props.get(4).isArrayItemEnd());
    }

}
