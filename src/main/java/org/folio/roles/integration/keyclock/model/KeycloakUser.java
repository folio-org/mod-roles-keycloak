package org.folio.roles.integration.keyclock.model;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;

@Data
public class KeycloakUser {

  public static final String USER_ID_ATTR = "user_id";

  private UUID id;
  @JsonProperty("username")
  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private Boolean emailVerified;
  private Long createdTimestamp;
  private Boolean enabled;
  private Map<String, List<String>> attributes = new HashMap<>();

  public UUID getUserId() {
    var attrs = MapUtils.emptyIfNull(attributes);

    var values = attrs.get(USER_ID_ATTR);

    if (isEmpty(values)) {
      throw new IllegalStateException("User id attribute is not found");
    } else if (values.size() != 1) {
      throw new IllegalStateException("User id attribute contains too many values");
    }

    return UUID.fromString(values.get(0));
  }
}
