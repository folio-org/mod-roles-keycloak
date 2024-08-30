package org.folio.roles.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class JsonHelper {

  private static final String SERIALIZATION_ERROR_MSG_TEMPLATE = "Failed to serialize value: message = %s";
  private static final String DESERIALIZATION_ERROR_MSG_TEMPLATE = "Failed to deserialize value: value = {}";
  private static final String TO_STRING_ERROR_MSG = "Failed to deserialize an object to json string";

  private final ObjectMapper mapper;

  /**
   * Converts given {@link Object} value to json string.
   *
   * @param value - value to convert
   * @return json value as {@link String}.
   */
  public String asJsonString(Object value) {
    if (value == null) {
      return null;
    }

    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new SerializationException(String.format(
        SERIALIZATION_ERROR_MSG_TEMPLATE, e.getMessage()));
    }
  }

  /**
   * Converts given {@link Object} value to json string safe, returns empty string if exception occurred.
   *
   * @param value - value to convert
   * @return json value as {@link String}.
   */
  public String asJsonStringSafe(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.debug(TO_STRING_ERROR_MSG, e);
      return StringUtils.EMPTY;
    }
  }

  /**
   * Converts {@link String} value as {@link T} class value.
   *
   * @param value - json value as {@link String} object
   * @param type - target class for conversion value from json
   * @param <T> - generic type for class.
   * @return converted {@link T} from json value
   */
  public <T> T parse(String value, Class<T> type) {
    if (value == null) {
      return null;
    }

    try {
      return mapper.readValue(value, type);
    } catch (JsonProcessingException e) {
      throw deserializationException(value, e);
    }
  }

  /**
   * Converts {@link String} value as {@link T} class value.
   *
   * @param value - json value as {@link String} object
   * @param type -target class for conversion value from json
   * @param <T> - generic type for class.
   * @return converted {@link T} from json value
   */
  @SuppressWarnings("unused")
  public <T> T parse(String value, TypeReference<T> type) {
    if (value == null) {
      return null;
    }

    try {
      return mapper.readValue(value, type);
    } catch (JsonProcessingException e) {
      throw deserializationException(value, e);
    }
  }

  /**
   * Converts {@link InputStream} value as {@link T} class value.
   *
   * @param inputStream - json value stream as {@link InputStream} object
   * @param valueType - target class to conversion value from json
   * @param <T> - generic type for class.
   * @return converted {@link T} object from input stream
   */
  public <T> T parse(InputStream inputStream, Class<T> valueType) {
    if (inputStream == null) {
      return null;
    }
    try {
      return mapper.readValue(inputStream, valueType);
    } catch (IOException e) {
      throw deserializationException(inputStream.toString(), e);
    }
  }

  private static RuntimeException deserializationException(String value, Throwable e) {
    log.warn(DESERIALIZATION_ERROR_MSG_TEMPLATE, value, e);
    return new SerializationException(String.format(
      "Failed to deserialize: value = %s, message = %s", value, e.getMessage()));
  }
}
