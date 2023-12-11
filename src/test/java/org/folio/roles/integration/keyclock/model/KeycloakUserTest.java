package org.folio.roles.integration.keyclock.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.integration.keyclock.model.KeycloakUser.USER_ID_ATTR;
import static org.folio.roles.support.TestConstants.USER_ID;

import java.util.List;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KeycloakUserTest {

  private final KeycloakUser user = new KeycloakUser();

  @Test
  void getUserId_positive() {
    user.getAttributes().put(USER_ID_ATTR, List.of(USER_ID.toString()));

    var actual = user.getUserId();
    assertThat(actual).isEqualTo(USER_ID);
  }

  @Test
  void getUserId_positive_noAttribute() {
    user.setAttributes(null);

    assertThatThrownBy(user::getUserId)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("User id attribute is not found");
  }

  @Test
  void getUserId_negative_tooManyValues() {
    user.getAttributes().put(USER_ID_ATTR, List.of(randomUuidAsString(), randomUuidAsString()));

    assertThatThrownBy(user::getUserId)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("User id attribute contains too many values");
  }

  private static String randomUuidAsString() {
    return UUID.randomUUID().toString();
  }
}
