package org.folio.roles.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityEventModuleIdExtractorTest {

  private static final String MODULE_ID = "foo-module-1.0.0";

  @InjectMocks private CapabilityEventModuleIdExtractor extractor;
  @Mock private ObjectMapper objectMapper;

  @Test
  void apply_positive_extractsModuleIdFromNewValue() {
    var value = Map.of("moduleId", MODULE_ID);
    var event = ResourceEvent.baseBuilder().newValue(value).build();
    when(objectMapper.convertValue(value, CapabilityEvent.class))
      .thenReturn(new CapabilityEvent().moduleId(MODULE_ID));

    var result = extractor.apply(event);

    assertThat(result).isEqualTo(MODULE_ID);
  }

  @Test
  void apply_positive_usesOldValueWhenNewValueIsNull() {
    var value = Map.of("moduleId", MODULE_ID);
    var event = ResourceEvent.baseBuilder().oldValue(value).build();
    when(objectMapper.convertValue(value, CapabilityEvent.class))
      .thenReturn(new CapabilityEvent().moduleId(MODULE_ID));

    var result = extractor.apply(event);

    assertThat(result).isEqualTo(MODULE_ID);
  }

  @Test
  void apply_negative_returnsNullOnConversionFailure() {
    var value = "not-a-map";
    var event = ResourceEvent.baseBuilder().newValue(value).build();
    when(objectMapper.convertValue(value, CapabilityEvent.class))
      .thenThrow(new IllegalArgumentException("cannot convert"));

    var result = extractor.apply(event);

    assertThat(result).isNull();
  }
}
