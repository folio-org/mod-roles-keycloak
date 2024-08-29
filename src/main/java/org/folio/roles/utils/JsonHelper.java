package org.folio.roles.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class JsonHelper {

  public static final String SERIALIZATION_ERROR_MSG_TEMPLATE = "Failed to serialize value: message: %s";
  public static final String DESERIALIZATION_ERROR_MSG_TEMPLATE = "Failed to deserialize value: value = {}";

  public static final String AS_JSON_STRING_FAILED = StringUtils.EMPTY;
  private static final String TO_STRING_ERROR_MSG = "Failed to deserialize an object to json string";

  private final ObjectMapper mapper;

  @SneakyThrows
  public String asJsonString(Object value) {
    return mapper.writeValueAsString(value);
  }

  public String asJsonStringSafe(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.info(TO_STRING_ERROR_MSG, e);
      return AS_JSON_STRING_FAILED;
    }
  }

  @SneakyThrows
  public <T> T parse(String value, Class<T> type) {
    return mapper.readValue(value, type);
  }

  @SneakyThrows
  public <T> T fromJsonStream(InputStream inputStream, Class<T> valueType) throws IOException {
    return mapper.readValue(inputStream, valueType);
  }

  /**
   * Converts {@link String} value as {@link T} class value.
   *
   * @param value json value as {@link String} object
   * @param type  target class to conversion value from json
   * @param <T>   generic type for class.
   * @return converted {@link T} from json value
   */
  @SuppressWarnings("unused")
  public <T> T fromJson(String value, Class<T> type) {
    if (value == null) {
      return null;
    }
    try {
      return mapper.readValue(value, type);
    } catch (JsonProcessingException e) {
      log.warn(DESERIALIZATION_ERROR_MSG_TEMPLATE, value, e);
      throw deserializationException(value, e);
    }
  }

  /**
   * Converts {@link String} value as {@link T} class value.
   *
   * @param value json value as {@link String} object
   * @param type  target class for conversion value from json
   * @param <T>   generic type for class.
   * @return converted {@link T} from json value
   */
  @SuppressWarnings("unused")
  public <T> T fromJson(String value, TypeReference<T> type) {
    if (value == null) {
      return null;
    }

    try {
      return mapper.readValue(value, type);
    } catch (JsonProcessingException e) {
      log.warn(DESERIALIZATION_ERROR_MSG_TEMPLATE, value, e);
      throw deserializationException(value, e);
    }
  }

  private static RuntimeException deserializationException(String value, Throwable e) {
    log.warn(DESERIALIZATION_ERROR_MSG_TEMPLATE, value, e);
    return new SerializationException(String.format(
      "Failed to deserialize: value = %s, message = %s", value, e.getMessage()));
  }
}
