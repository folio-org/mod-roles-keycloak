package org.folio.roles.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.lang3.SerializationException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class JsonHelperTest {

  private static final User TEST_USER = new User("test", "user");
  private static final String TEST_USER_JSON = "{\"name\": \"test\", \"surname\": \"user\"}";

  @InjectMocks private JsonHelper helper;
  @Mock private ObjectMapper mapper;

  @Nested
  @DisplayName("parse")
  class Parse {

    @Test
    void positive_stringInput() throws JacksonException {
      when(mapper.readValue(TEST_USER_JSON, User.class)).thenReturn(TEST_USER);
      var actual = helper.parse(TEST_USER_JSON, User.class);
      assertThat(actual).isEqualTo(TEST_USER);
    }

    @Test
    void positive_nullStringInput() {
      var actual = helper.parse((String) null, User.class);
      assertThat(actual).isNull();
    }

    @Test
    void negative_stringInputMapperException() throws JacksonException {
      when(mapper.readValue(TEST_USER_JSON, User.class)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(TEST_USER_JSON, User.class))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", TEST_USER_JSON);
    }

    @Test
    void positive_stringInputTypeReference() throws JacksonException {
      var typeReference = new TypeReference<User>() {};
      when(mapper.readValue(TEST_USER_JSON, typeReference)).thenReturn(TEST_USER);
      var actual = helper.parse(TEST_USER_JSON, typeReference);
      assertThat(actual).isEqualTo(TEST_USER);
    }

    @Test
    void positive_nullStringForTypeReference() {
      var typeReference = new TypeReference<User>() {};
      var actual = helper.parse(null, typeReference);
      assertThat(actual).isNull();
    }

    @Test
    void negative_stringInputTypeRefMapperException() throws JacksonException {
      var typeReference = new TypeReference<User>() {};
      when(mapper.readValue(TEST_USER_JSON, typeReference)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(TEST_USER_JSON, typeReference))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", TEST_USER_JSON);
    }

    @Test
    void positive_inputStream() throws JacksonException {
      var inputStream = new ByteArrayInputStream(TEST_USER_JSON.getBytes(UTF_8));
      when(mapper.readValue(inputStream, User.class)).thenReturn(TEST_USER);
      var actual = helper.parse(inputStream, User.class);
      assertThat(actual).isEqualTo(TEST_USER);
    }

    @Test
    void positive_nullInputStream() {
      var actual = helper.parse((InputStream) null, User.class);
      assertThat(actual).isNull();
    }

    @Test
    void positive_inputStreamMapperException() throws JacksonException {
      var inputStream = new ByteArrayInputStream(TEST_USER_JSON.getBytes(UTF_8));
      when(mapper.readValue(inputStream, User.class)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.parse(inputStream, User.class))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to deserialize: value = %s, message = Failed", inputStream);
    }
  }

  @Nested
  @DisplayName("asJsonStringSafe")
  class AsJsonStringSafe {

    @Test
    void positive() throws JacksonException {
      when(mapper.writeValueAsString(TEST_USER)).thenReturn(TEST_USER_JSON);
      var actual = helper.asJsonStringSafe(TEST_USER);
      assertEquals(TEST_USER_JSON, actual);
    }

    @Test
    void positive_mapperException() throws JacksonException {
      when(mapper.writeValueAsString(TEST_USER)).thenThrow(new TestJsonProcessingException("Failed"));
      var actual = helper.asJsonStringSafe(TEST_USER);
      assertThat(actual).isEmpty();
    }
  }

  @Nested
  @DisplayName("asJsonString")
  class AsJsonString {

    @Test
    void positive() throws JacksonException {
      when(mapper.writeValueAsString(TEST_USER)).thenReturn(TEST_USER_JSON);

      var actual = helper.asJsonString(TEST_USER);

      assertThat(actual).isEqualTo(TEST_USER_JSON);
      verify(mapper).writeValueAsString(TEST_USER);
    }

    @Test
    void toJson_positive_nullValue() {
      var actual = helper.asJsonString(null);
      assertThat(actual).isNull();
    }

    @Test
    void asJsonString_negative_mapperException() throws JacksonException {
      when(mapper.writeValueAsString(TEST_USER)).thenThrow(new TestJsonProcessingException("Failed"));
      assertThatThrownBy(() -> helper.asJsonString(TEST_USER))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Failed to serialize value: message = Failed");
    }
  }

  private record User(String name, String surname) {}

  private static final class TestJsonProcessingException extends JacksonException {

    TestJsonProcessingException(String msg) {
      super(msg);
    }
  }
}
