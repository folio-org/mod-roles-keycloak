package org.folio.roles.utils;

import static org.folio.roles.utils.JsonHelper.AS_JSON_STRING_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class JsonHelperTest {

  private static final User TEST_USER = new User("test", "user");
  private static final String TEST_USER_JSON = """
    {
      "name": "test",
      "surname": "user"
    }
    """;

  @Mock private ObjectMapper mapper;
  @InjectMocks private JsonHelper helper;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void asJsonString_positive() throws JsonProcessingException {
    when(mapper.writeValueAsString(TEST_USER)).thenReturn(TEST_USER_JSON);

    var actual = helper.asJsonString(TEST_USER);

    assertEquals(TEST_USER_JSON, actual);
  }

  @Test
  void asJsonString_negative_mapperException() throws JsonProcessingException {
    when(mapper.writeValueAsString(TEST_USER)).thenThrow(new TestJsonProcessingException("Failed"));

    assertThrows(JsonProcessingException.class, () -> helper.asJsonString(TEST_USER), "Failed");
  }

  @Test
  void asJsonStringSafe_positive() throws JsonProcessingException {
    when(mapper.writeValueAsString(TEST_USER)).thenReturn(TEST_USER_JSON);

    var actual = helper.asJsonStringSafe(TEST_USER);

    assertEquals(TEST_USER_JSON, actual);
  }

  @Test
  void asJsonString_positive_mapperException() throws JsonProcessingException {
    when(mapper.writeValueAsString(TEST_USER)).thenThrow(new TestJsonProcessingException("Failed"));

    var actual = helper.asJsonStringSafe(TEST_USER);

    assertEquals(AS_JSON_STRING_FAILED, actual);
  }

  @Test
  void parse_positive() throws JsonProcessingException {
    when(mapper.readValue(TEST_USER_JSON, User.class)).thenReturn(TEST_USER);

    var actual = helper.parse(TEST_USER_JSON, User.class);

    assertEquals(TEST_USER, actual);
  }

  @Test
  void parse_negative_mapperException() throws JsonProcessingException {
    when(mapper.readValue(TEST_USER_JSON, User.class)).thenThrow(new TestJsonProcessingException("Failed"));

    assertThrows(JsonProcessingException.class, () -> helper.parse(TEST_USER_JSON, User.class), "Failed");
  }

  private record User(String name, String surname) {
  }

  private static final class TestJsonProcessingException extends JsonProcessingException {

    TestJsonProcessingException(String msg) {
      super(msg);
    }
  }
}
