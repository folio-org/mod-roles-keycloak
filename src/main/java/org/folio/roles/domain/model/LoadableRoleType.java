package org.folio.roles.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LoadableRoleType {

  DEFAULT(Values.DEFAULT_VALUE),
  SUPPORT(Values.SUPPORT_VALUE);

  private final String value;

  LoadableRoleType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static LoadableRoleType fromValue(String value) {
    for (LoadableRoleType b : LoadableRoleType.values()) {
      if (b.value.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static final class Values {
    public static final String DEFAULT_VALUE = "default";
    public static final String SUPPORT_VALUE = "support";
  }
}
