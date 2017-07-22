package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.TestCommons.MockResponse;
import com.github.mostroverkhov.retrofit_property_streaming.TestCommons.MockResponseShort;
import com.github.mostroverkhov.retrofit_property_streaming.TestCommons.SpecialTypes;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Maksym Ostroverkhov on 13.06.2016.
 */
public class PropReaderTest {

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

  @SuppressWarnings("InfiniteLoopStatement")
  @Test(expected = IllegalStateException.class)
  public void throwsOnNextAfterDocumentEnd() throws Exception {

    PropertyReader<MockResponse> propertyReader =
        new PropertyReaderBuilder<MockResponse>(
            TestCommons.MockResponse.class,
            jsonReader).build();

    while (true) {
      propertyReader.nextProp();
    }
  }

  @Test
  public void allFields() throws Exception {

    PropertyReader<MockResponse> calc =
        new PropertyReaderBuilder<MockResponse>(
            TestCommons.MockResponse.class,
            jsonReader).build();

    List<Prop<TestCommons.MockResponse>> props = TestCommons.getProps(calc);

    assertThat(props).hasSize(8);
    assertThat(props.get(0).isSimpleProperty());
    assertThat(props.get(1).isSimpleProperty());
    assertThat(props.get(2).isSimpleProperty());
    assertThat(props.get(3).isArrayItemStart());
    assertThat(props.get(4).isArrayItem());
    assertThat(props.get(5).isArrayItem());
    assertThat(props.get(6).isArrayItemEnd());
    assertThat(props.get(7).isDocumentEnd());
  }

  @Test
  public void specialTypesTest() throws Exception {

    PropertyReader<SpecialTypes> slicer =
        new PropertyReaderBuilder<SpecialTypes>(
            TestCommons.SpecialTypes.class,
            jsonReader).build();

    List<Prop<TestCommons.SpecialTypes>> props = TestCommons.getProps(slicer);

    assertThat(props).hasSize(2);
    assertThat(props.get(0).isSimpleProperty());
    assertThat(props.get(1).isDocumentEnd());
  }

  @Test
  public void skipFields() throws Exception {

    PropertyReader<MockResponseShort> calc =
        new PropertyReaderBuilder<MockResponseShort>(
            TestCommons.MockResponseShort.class,
            jsonReader).build();

    List<Prop<TestCommons.MockResponseShort>> props = TestCommons.getProps(calc);

    assertThat(props).hasSize(6);
    assertThat(props.get(0).isSimpleProperty());
    assertThat(props.get(1).isArrayItemStart());
    assertThat(props.get(2).isArrayItem());
    assertThat(props.get(3).isArrayItem());
    assertThat(props.get(4).isArrayItemEnd());
    assertThat(props.get(5).isDocumentEnd());
  }

}
